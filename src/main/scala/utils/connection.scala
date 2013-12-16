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
import jadeutils.xmpp.model.Packet

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

	var callbackHandler: CallbackHandler = null // auth call back handler

	var compressionEnabled = false

}



class XMPPConnection(val serviceName: String, val port: Int, 
	proxyInfo: ProxyInfo)
{
	val logger = XMPPConnection.logger

	var connCfg = new ConnectionConfiguration(serviceName, port, proxyInfo)

	val connectionCounterValue = XMPPConnection.connectionCounter.getAndIncrement


	val saslAuthentication: SASLAuthentication = new SASLAuthentication(this);

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
		} catch {
			case e: Exception => 
				logger.error("fail create reader & writer from socket")
		}
		if (null == this.writer) {
			logger.error("error create writer from socket")
			throw new XMPPException("fail create writer from socket")
		} else if (null == this.reader) {
			logger.error("error create reader from socket")
			throw new XMPPException("fail create reader from socket")
		} else {
			logger.debug("Success create reader/writer from socket")
		}
	}

	private[this] def initConnection() {
		val isFirstInit = (null == this.packetWriter) || (null == this.packetReader)
		initReaderAndWriter
		try {
			if (isFirstInit) {
				logger.debug("1st time init Connection, create new Reader and Writer")
				this.packetReader = new PacketReader(this)
				this.packetWriter = new PacketWriter(this)
			}
			packetReader.init
			packetReader.start
			packetWriter.init
			packetWriter.start

			connected = true
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

	def sendPacket(stanza: Packet) {
		packetWriter ! stanza.toXML.toString
	}

	@throws(classOf[XMPPException])
	def login(username: String, password: String, resource: String) {
		logger.debug("try login with (%s , %s , %s)".format(
			username, password, resource))

		this.connCfg.username = username
		this.connCfg.password = password
		this.connCfg.resource = resource

		if (!connected)
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

