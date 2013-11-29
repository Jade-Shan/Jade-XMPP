package jadeutils.xmpp.utils

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Reader
import java.io.Writer
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
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

class ProxyInfo(val proxyType: ProxyInfo.ProxyType.Value, 
	val proxyAddress: String, val proxyPort: Int,
	val proxyUsername: String, val proxyPassword: String)
{
	def getSocketFactory: SocketFactory = {
		this.proxyType match {
			case ProxyInfo.ProxyType.NONE   => new DirectSocketFactory()
			case ProxyInfo.ProxyType.SOCKS4 => null // TODO: 末实现
			case ProxyInfo.ProxyType.SOCKS5 => null // TODO: 末实现
			case ProxyInfo.ProxyType.HTTP   => null // TODO: 末实现
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

	def forDefaultProxy(): ProxyInfo = {
		new ProxyInfo(ProxyType.NONE, null, 0, null, null);
	}

}



class DirectSocketFactory extends SocketFactory {

	def createSocket(host: String, port: Int): Socket = {
		val newSocket = new Socket(Proxy.NO_PROXY);
		newSocket.connect(new InetSocketAddress(host,port));
		return newSocket;
	}

	def createSocket(host: String, port: Int, 
		localAddress: InetAddress, localPort: Int): Socket =
	{
		return new Socket(host,port,localAddress,localPort);
	}

	def createSocket(host: InetAddress, port: Int): Socket = {
		val newSocket = new Socket(Proxy.NO_PROXY);
		newSocket.connect(new InetSocketAddress(host,port));
		return newSocket;
	}

	def createSocket(address: InetAddress, port: Int, 
		localAddress: InetAddress, localPort: Int): Socket = 
	{
		return new Socket(address, port, localAddress, localPort);
	}

}



class ConnectionConfiguration(val serviceName: String, val port: Int) {
	var hostAddresses: List[HostAddress] = null

	var compressionEnabled = false

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

	var socketFactory: SocketFactory = ProxyInfo.forDefaultProxy.getSocketFactory
}



class XMPPConnection(val serviceName: String, val port: Int) {
	val logger = XMPPConnection.logger

	var connCfg = new ConnectionConfiguration(serviceName, port)
	connCfg.hostAddresses = XmppDNSService.resolveXmppClientDomain(serviceName)

	var compressionHandler: XMPPInputOutputStream = null

	val connectionCounterValue = XMPPConnection.connectionCounter.getAndIncrement

	var reader: Reader = null
	var writer: Writer = null

	var usingTLS = false

	var socketClosed = true
	var connected = false

	var socket: Socket = null

	def connect() {
		for (host <- this.connCfg.hostAddresses) if (null == this.socket) {
			if (null == this.connCfg.socketFactory) {
				this.socket = new Socket(host.fqdn, port)
			} else {
				this.socket = connCfg.socketFactory.createSocket(host.fqdn, port)
			}
			logger.debug("create Socket({}:{}) Success", host.fqdn, port)
		}
		this.socketClosed = false
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

	def initReaderAndWriter() {
		if (this.compressionHandler == null) {
			this.reader = new BufferedReader(new InputStreamReader(
				this.socket.getInputStream(), "UTF-8"))
			this.writer = new BufferedWriter( new OutputStreamWriter(
				this.socket.getOutputStream(), "UTF-8"))
		} else {
			this.writer = new BufferedWriter(new OutputStreamWriter(
				this.compressionHandler.getOutputStream(this.socket.getOutputStream()), 
				"UTF-8"));
			this.reader = new BufferedReader(new InputStreamReader(
				this.compressionHandler.getInputStream(this.socket.getInputStream()), 
				"UTF-8"));
		}
	}

}



object XMPPConnection extends Logging {
	val connectionCounter = new AtomicInteger(0)
}

