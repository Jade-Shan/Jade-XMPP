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
import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
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

	var callbackHandler: CallbackHandler = null // auth call back handler

	var compressionEnabled = false

}



class XMPPConnection(val serviceName: String, val port: Int, 
	proxyInfo: ProxyInfo) extends Logging
{
	val connectionCounterValue = XMPPConnection.connectionCounter.getAndIncrement

	def this(serviceName: String, port: Int) {
		this(serviceName, port, ProxyInfo.forNoProxy)
	}

	def this(serviceName: String) {
		this(serviceName, 5222, ProxyInfo.forNoProxy)
	}


	var connCfg = new ConnectionConfiguration(serviceName, port, proxyInfo)
	var ioStream: IOStream = null
	// var connected = false
	// var socketClosed = true
	// var socket: Socket = null
	// var reader: Reader = null
	// var writer: Writer = null
	// var packetReader: PacketReader = null
	// var packetWriter: PacketWriter = null
	// var compressHandler: CompressHandler = null

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
		if (ioStream.connected && this.wasAuthenticated) {
			if (this.anonymous) {
				// TODO: anonymous login
			} else {
				this.login(this.connCfg.username, this.connCfg.password, 
					this.connCfg.resource)
			}
		}
	}




	/* Flag that indicates if the user is currently authenticated with the 
	 server.  */
	var authenticated = false;
	/* Flag that indicates if the user was authenticated with the server when 
	 the connection to the server was closed (abruptly or not).  */
	var anonymous = false;
	var usingTLS = false

	val saslAuthentication: SASLAuthentication = new SASLAuthentication(this);
	private[this] var wasAuth = false;

	def wasAuthenticated: Boolean = this.wasAuth

	def wasAuthenticated_=(wasAuthenticated: Boolean) {
		if (!this.wasAuth)
			this.wasAuth = wasAuthenticated
	}

	def proceedTLSReceived() {
		var ks: KeyStore = null;
		var kms: Array[KeyManager] = null;

		// Secure the plain connection
		var context = SSLContext.getInstance("TLS")
		var sslSocket: SSLSocket = context.getSocketFactory.createSocket(
			ioStream.socket, ioStream.socket.getInetAddress().getHostAddress(), 
			ioStream.socket.getPort(), true).asInstanceOf[SSLSocket];
		ioStream.socket = sslSocket
		sslSocket.setSoTimeout(0);
		sslSocket.setKeepAlive(true);
		// Initialize the reader and writer with the new secured version
		ioStream.initReaderAndWriter();
		// Proceed to do the handshake
		sslSocket.startHandshake();
		this.usingTLS = true;

		// Set the new  writer to use
		// packetWriter.setWriter(writer);
		// Send a new opening stream to the server
		// packetWriter.openStream();
	}















	@throws(classOf[XMPPException])
	def login(username: String, password: String, resource: String) {
		logger.debug("try login with ({} , {} , {})", Array(username, password, 
			resource))

		this.connCfg.username = username
		this.connCfg.password = password
		this.connCfg.resource = resource

		if (!ioStream.connected)
			throw new IllegalStateException("Not connected to server.")
		if (authenticated)
			throw new IllegalStateException("Already logged in to server.")

		val resp = if (connCfg. notMatchingDomainCheckEnabled && 
			connCfg.saslAuthenticationEnabled) 
		{
			logger.debug("Authenticate using SASL")
			if (password != null) {
				saslAuthentication.authenticate(username, password, resource)
			} else {
				saslAuthentication.authenticate(username, resource, 
					connCfg.callbackHandler)
			}
		} else {
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
		this.login(username, password, "jadexmpp")
	}

}



object XMPPConnection {
	val connectionCounter = new AtomicInteger(0)
}

