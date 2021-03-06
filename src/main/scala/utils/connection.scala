package jadeutils.xmpp.utils

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.security.KeyStore
import java.security.Provider
import java.security.Security
import java.util.concurrent.atomic.AtomicInteger
import javax.net.SocketFactory
import javax.security.auth.callback.CallbackHandler

import jadeutils.common.Logging
import jadeutils.xmpp.model.Jid
import jadeutils.xmpp.model.Roster

class DirectSocketFactory extends SocketFactory {

	def createSocket(host: String, port: Int): Socket = {
		val newSocket = new Socket(Proxy.NO_PROXY);
		newSocket.connect(new InetSocketAddress(host, port));
		return newSocket;
	}

	def createSocket(host: String, port: Int, 
		localAddress: InetAddress, localPort: Int): Socket =
	{
		return new Socket(host,port, localAddress, localPort);
	}

	def createSocket(host: InetAddress, port: Int): Socket = {
		val newSocket = new Socket(Proxy.NO_PROXY);
		newSocket.connect(new InetSocketAddress(host, port));
		return newSocket;
	}

	def createSocket(address: InetAddress, port: Int, 
		localAddress: InetAddress, localPort: Int): Socket = 
	{
		return new Socket(address, port, localAddress, localPort);
	}

}





class ProxyInfo(val proxyType: ProxyInfo.ProxyType.Value, 
	val proxyAddress: String, val proxyPort: Int,
	val proxyUsername: String, val proxyPassword: String) extends Logging
{
	import ProxyInfo.ProxyType

	def getSocketFactory: SocketFactory = {
		proxyType match {
			case ProxyType.NONE   => new DirectSocketFactory()
			case ProxyType.SOCKS4 => {
				// TODO: next stage
				logger.error("UnImplement Socket4 proxy")
				null
			}
			case ProxyType.SOCKS5 => {
				// TODO: next stage
				logger.error("UnImplement Socket5 proxy")
				null
			}
			case ProxyType.HTTP   => {
				// TODO: next stage
				logger.error("UnImplement Http Proxy")
				null
			}
		}
	}

}



object ProxyInfo {

	object ProxyType extends Enumeration {val NONE, HTTP, SOCKS4, SOCKS5 = Value}

	def forHttpProxy(pHost: String, pPort: Int, 
		pUser: String, pPass: String): ProxyInfo = 
	{
		new ProxyInfo(ProxyType.HTTP, pHost, pPort, pUser, pPass);
	}

	def forSocks4Proxy(pHost: String, pPort: Int, 
		pUser: String, pPass: String): ProxyInfo = 
	{
		new ProxyInfo(ProxyType.SOCKS4, pHost, pPort, pUser, pPass);
	}

	def forSocks5Proxy(pHost: String, pPort: Int, 
		pUser: String, pPass: String): ProxyInfo = 
	{
		new ProxyInfo(ProxyType.SOCKS5, pHost, pPort, pUser, pPass);
	}

	def forNoProxy(): ProxyInfo = {
		new ProxyInfo(ProxyType.NONE, null, 0, null, null);
	}

}





class ConnectionConfiguration(var serviceName: String, val port: Int, 
	proxyInfo: ProxyInfo)
{

	var username: String = null
	var password: String = null 
	var resource: String = null

	var socketFactory: SocketFactory = proxyInfo.getSocketFactory

	/* host Info */
	var hostAddresses: List[HostAddress] = 
		XmppFqdnService.resolveXmppClientDomain(serviceName)
	var badHostAddresses: List[HostAddress] = Nil  // Host cannot connect
	var currAddress: HostAddress = null            // current using address
	var charEncoding = "UTF-8"


	/* cacerts info */
	val trustStorePath = System.getProperty("java.home") + 
		java.io.File.separator + "lib" +
		java.io.File.separator + "security" +
		java.io.File.separator + "cacerts"
	val truststoreType = "jks"; // Set the default store type
	// Set the default password of the cacert file that is "changeit"
	val truststorePassword = "changeit"
	val keystorePath = System.getProperty("javax.net.ssl.keyStore")
	val keystoreType = "jks"
	val pkcs11Library = "pkcs11.config"

	var verifyChainEnabled = false;
	var verifyRootCAEnabled = false;
	var selfSignedCertificateEnabled = false;
	var expiredCertificatesCheckEnabled = false;
	var notMatchingDomainCheckEnabled = false;

	var saslAuthenticationEnabled = true

	var callbackHandler: CallbackHandler = null // auth call back handler

	var compressionEnabled = false

}





abstract class Connection(val serviceName: String, val port: Int, 
	val proxyInfo: ProxyInfo)
{
	var ioStream: IOStream = null

	val connectionCounterValue = Connection.connectionCounter.getAndIncrement

	var connCfg: ConnectionConfiguration  = new ConnectionConfiguration(
		serviceName, port, proxyInfo)

	def currHost = { connCfg.currAddress.fqdn }
}



object Connection {
	val connectionCounter = new AtomicInteger(0)
}



abstract class XMPPConnection(override val serviceName: String, 
	override val port: Int, override val proxyInfo: ProxyInfo) 
	extends Connection(serviceName: String, port: Int, proxyInfo: ProxyInfo) 
	with MessageProcesser with AuthModule with Logging
{
	val roster = new Roster(this);
	roster.init
	roster.start


	def this(serviceName: String, port: Int) {
		this(serviceName, port, ProxyInfo.forNoProxy)
	}

	def this(serviceName: String) {
		this(serviceName, 5222, ProxyInfo.forNoProxy)
	}

	def write(str: String) {
		if (null != str) ioStream.packetWriter ! str
	}

	@throws(classOf[XMPPException])
	def connect() {
		try {
			ioStream = new IOStream(this)
			ioStream.connectUsingConfiguration()
		} catch {
			case e: Exception => {
				logger.error("connection failed")
				/* change state to not auth on server */
				wasAuthenticated = authenticated
				authenticated = false
				/* throw exeption */
				throw new XMPPException("Connection Failed!")
			}
		}

		/* auto login again when login time out*/
		if (ioStream.connected && wasAuthenticated) {
			if (anonymous) {
				// TODO: anonymous login
			} else {
				login(connCfg.username, connCfg.password, connCfg.resource)
			}
		}
	}



	@throws(classOf[XMPPException])
	def login(username: String, password: String, resource: String) {
		logger.debug("Start Login ...\n\n\n\n\nLoginWith：({} , {} , {})", 
			Array(username, password, resource))

		connCfg.username = username.toLowerCase.trim
		connCfg.password = password
		connCfg.resource = resource

		if (!ioStream.connected) {
			throw new IllegalStateException("Not connected to server.")
		}
		if (authenticated) {
			throw new IllegalStateException("Already logged in to server.")
		}

		val resp = if (connCfg.saslAuthenticationEnabled &&
			this.saslAuthentication.hasNonAnonymousAuthentication)
		{
			logger.debug("Authenticate using SASL")
			if (password != null) {
				logger.debug("has password")
				saslAuthentication.authenticate(username, password, resource)
			} else {
				logger.debug("has not password, using callback")
				saslAuthentication.authenticate(username, resource, 
					connCfg.callbackHandler)
			}
		} else {
			logger.debug("Authenticate using NonSASLAuthentication")
			new NonSASLAuthentication(this).authenticate(username, password, 
				resource)
		}
	}

	@throws(classOf[XMPPException])
	def login(username: String, password: String) {
		login(username, password, "jadexmpp")
	}

}

