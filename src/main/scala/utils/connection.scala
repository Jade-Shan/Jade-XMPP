package jadeutils.xmpp.utils



import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Reader
import java.io.Writer
import java.io.InputStreamReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.net.UnknownHostException
import javax.net.SocketFactory
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import java.security.KeyStore
import java.security.Provider
import java.security.Security
import java.util.concurrent.atomic.AtomicInteger

import jadeutils.common.Logging

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
	val proxyUsername: String, val proxyPassword: String)
{
	val logger = ProxyInfo.logger

	def getSocketFactory: SocketFactory = {
		this.proxyType match {
			case ProxyInfo.ProxyType.NONE   => new DirectSocketFactory()
			case ProxyInfo.ProxyType.SOCKS4 => {
				// TODO: 末实现
				logger.error("UnImplement Socket4 proxy")
				null
			}
			case ProxyInfo.ProxyType.SOCKS5 => {
				// TODO: 末实现
				logger.error("UnImplement Socket5 proxy")
				null
			}
			case ProxyInfo.ProxyType.HTTP   => {
				// TODO: 末实现
				logger.error("UnImplement Http Proxy")
				null
			}
		}
	}

}



object ProxyInfo extends Logging {

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





class ConnectionConfiguration(val serviceName: String, val port: Int, 
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

	var compressionEnabled = false

}



class XMPPConnection(val serviceName: String, val port: Int, 
	proxyInfo: ProxyInfo)
{
	val logger = XMPPConnection.logger

	var connCfg = new ConnectionConfiguration(serviceName, port, proxyInfo)

	val connectionCounterValue = XMPPConnection.connectionCounter.getAndIncrement

	/* Flag that indicates if the user is currently authenticated with the 
	 server.  */
	var authenticated = false;
	/* Flag that indicates if the user was authenticated with the server when 
	 the connection to the server was closed (abruptly or not).  */
	private[this] var wasAuth = false;
	var anonymous = false;


	var reader: Reader = null
	var writer: Writer = null
	var packetReader: PacketReader = null
	var packetWriter: PacketWriter = null
	var compressionHandler: XMPPInputOutputStream = null

	var usingTLS = false

	var socketClosed = true
	var connected = false

	var socket: Socket = null

	def this(serviceName: String, port: Int) {
		this(serviceName, port, ProxyInfo.forNoProxy)
	}

	def this(serviceName: String) {
		this(serviceName, 5222, ProxyInfo.forNoProxy)
	}

	private[this] def initReaderAndWriter() {
		try {
			if (this.compressionHandler == null) {
				logger.debug("try create no compress reader & writer from socket")
				this.reader = new BufferedReader(new InputStreamReader(
					this.socket.getInputStream(), connCfg.charEncoding))
				this.writer = new BufferedWriter( new OutputStreamWriter(
					this.socket.getOutputStream(), connCfg.charEncoding))
			} else {
				logger.debug("try create compress reader & writer from socket")
				this.writer = new BufferedWriter(new OutputStreamWriter(
					this.compressionHandler.getOutputStream(
						this.socket.getOutputStream()), connCfg.charEncoding));
				this.reader = new BufferedReader(new InputStreamReader(
					this.compressionHandler.getInputStream(
						this.socket.getInputStream()), connCfg.charEncoding));
			}
			logger.debug("success create reader & writer from socket")
		} catch {
			case e: Exception => 
				logger.error("fail create reader & writer from socket")
		}
	}

	private[this] def initConnection() {
		val isFirstInit = (null == this.packetWriter) || (null == this.packetReader)
		try {
			if (isFirstInit) {
				logger.debug("1st time init Connection, create new Reader and Writer")
				this.packetReader = new PacketReader(this)
				this.packetWriter = new PacketWriter(this)
			}
		} catch {
			case e: Exception => {
				logger.error("fail init connection")
				/* close reader writer IO Socket */
				if (null != this.packetReader)
					this.packetReader.close
				this.packetReader = null
				if (null != this.packetWriter)
					this.packetWriter.close
				this.packetWriter = null
				if (null != this.reader) {
					try {
						this.reader.close
					} catch {
						case t: Throwable => /* do nothing */
					}
				}
				this.reader = null
				if (null != this.writer) {
					try {
						this.writer.close
					} catch {
						case t: Throwable => /* do nothing */
					}
				}
				this.writer = null
				if (null != this.socket) {
					try {
						this.socket.close
					} catch {
						case e: Exception => /* do nothing */
					}
				}
				/* change state to not auth on server */
				this.wasAuthenticated = this.authenticated
				this.authenticated = false
				this.connected = false
				/* throw exeption */
				throw new XMPPException("init Connection Failed!")
			}
		}
	}

	private[this] def connectUsingConfiguration() {
		for (host <- this.connCfg.hostAddresses) if (null == this.socket) {
			try {
				if (null == this.connCfg.socketFactory) {
					logger.debug("No SocketFacoty, create new Socket({}:{})", host.fqdn, port)
					this.socket = new Socket(host.fqdn, port)
				} else {
					logger.debug("get Socket({}:{}) from SocketFactory", host.fqdn, port)
					this.socket = connCfg.socketFactory.createSocket(host.fqdn, port)
				}
				this.connCfg.currAddress = host
				logger.debug("Success get Socket({}:{})", host.fqdn, port)
			} catch {
				case ex: Exception => {
					ex match {
						case e: UnknownHostException => 
							logger.error("Socket({}:{}) Connect time out", host.fqdn, port)
						case e: IOException => 
							logger.error("Socket({}:{}) Remote Server error", host.fqdn, port)
						case _ => 
							logger.error("Socket({}:{}) unknow error", host.fqdn, port)
					}
					// add this host to bad host list
					this.connCfg.badHostAddresses == host :: this.connCfg.badHostAddresses
				}
			}
		}
		if (null == this.socket) {
			throw new XMPPException("None of Address list can create Socket")
		}
		this.socketClosed = false
		this.initConnection
	}

	@throws(classOf[XMPPException])
	def login(username: String, password: String, resource: String) {
		logger.debug("try login with (%s , %s , %s)".format(
			username, password, resource))

		this.connCfg.username = username
		this.connCfg.password = password
		this.connCfg.resource = resource
		// TODO: login function
	}

	@throws(classOf[XMPPException])
	def login(username: String, password: String) {
		this.login(username, password, "jadexmpp")
	}

	@throws(classOf[XMPPException])
	def connect() {
		this.connectUsingConfiguration()

		/* auto login again when login time out*/
		if (this.connected && this.wasAuthenticated) {
			if (this.anonymous) {
				// TODO: anonymous login
			} else {
				this.login(this.connCfg.username, this.connCfg.password, 
					this.connCfg.resource)
			}
		}
	}


	def proceedTLSReceived() {
		var ks: KeyStore = null;
		var kms: Array[KeyManager] = null;

		// Secure the plain connection
		var context = SSLContext.getInstance("TLS")
		var sslSocket: SSLSocket = context.getSocketFactory.createSocket(
			this.socket, this.socket.getInetAddress().getHostAddress(), 
			this.socket.getPort(), true).asInstanceOf[SSLSocket];
		this.socket = sslSocket
		sslSocket.setSoTimeout(0);
		sslSocket.setKeepAlive(true);
		// Initialize the reader and writer with the new secured version
		initReaderAndWriter();
		// Proceed to do the handshake
		sslSocket.startHandshake();
		this.usingTLS = true;

		// Set the new  writer to use
		// packetWriter.setWriter(writer);
		// Send a new opening stream to the server
		// packetWriter.openStream();
	}

	def wasAuthenticated: Boolean = this.wasAuth

	def wasAuthenticated_=(wasAuthenticated: Boolean) {
		if (!this.wasAuth)
			this.wasAuth = wasAuthenticated
	}

}



object XMPPConnection extends Logging {
	val connectionCounter = new AtomicInteger(0)
}

