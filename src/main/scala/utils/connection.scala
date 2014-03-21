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
		XmppDNSService.resolveXmppClientDomain(serviceName)
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


abstract class XMPPConnection(val serviceName: String, val port: Int, 
	val proxyInfo: ProxyInfo) extends MessageProcesser with AuthInfo  with Logging
{
	val connectionCounterValue = XMPPConnection.connectionCounter.getAndIncrement

	var connCfg: ConnectionConfiguration = new ConnectionConfiguration(
		serviceName, port, proxyInfo)
	var ioStream: IOStream = null

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

		connCfg.username = username
		connCfg.password = password
		connCfg.resource = resource

		if (!ioStream.connected)
			throw new IllegalStateException("Not connected to server.")
		if (authenticated)
			throw new IllegalStateException("Already logged in to server.")

		val resp = if (connCfg. notMatchingDomainCheckEnabled && 
			connCfg.saslAuthenticationEnabled) 
		{
			logger.debug("Authenticate using SASL")
			if (password != null) {
				logger.debug("has passwork")
				saslAuthentication.authenticate(username, password, resource)
			} else {
				logger.debug("has not passwork, using callback")
				saslAuthentication.authenticate(username, resource, 
					connCfg.callbackHandler)
			}
		} else {
			logger.debug("Authenticate using NonSASLAuthentication")
			new NonSASLAuthentication(this).authenticate(username, password, 
				resource)
		}
//        // Set the user.
//        if (response != null) {
//            this.user = response;
//            // Update the serviceName with the one returned by the server
//            config.setServiceName(StringUtils.parseServer(response));
//        }
//        else {
//            this.user = username + "@" + getServiceName();
//            if (resource != null) {
//                this.user += "/" + resource;
//            }
//        }
//
//        // If compression is enabled then request the server to use stream compression
//        if (config.isCompressionEnabled()) {
//            useCompression();
//        }
//
//        // Indicate that we're now authenticated.
//        authenticated = true;
//        anonymous = false;
//
//        // Create the roster if it is not a reconnection or roster already created by getRoster()
//        if (this.roster == null) {
//            this.roster = new Roster(this);
//        }
//        if (config.isRosterLoadedAtLogin()) {
//            this.roster.reload();
//        }
//
//        // Set presence to online.
//        if (config.isSendPresence()) {
//            packetWriter.sendPacket(new Presence(Presence.Type.available));
//        }
//
//        // Stores the authentication for future reconnection
//        config.setLoginInfo(username, password, resource);
//
//        // If debugging is enabled, change the the debug window title to include the
//        // name we are now logged-in as.
//        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
//        // will be null
//        if (config.isDebuggerEnabled() && debugger != null) {
//            debugger.userHasLogged(user);
//        }

		// TODO: login function
	}

	@throws(classOf[XMPPException])
	def login(username: String, password: String) {
		login(username, password, "jadexmpp")
	}

}



object XMPPConnection {
	val connectionCounter = new AtomicInteger(0)
}
