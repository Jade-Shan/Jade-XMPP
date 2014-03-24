package jadeutils.xmpp.handler

import scala.xml.Elem
import scala.xml.Node

import jadeutils.common.Logging
import jadeutils.xmpp.utils.MsgHandler 
import jadeutils.xmpp.utils.XMPPConnection

class StreamHandler(conn: XMPPConnection) extends MsgHandler with Logging {

	def canProcess(elem: Elem): Boolean = {
		elem.namespace == """http://etherx.jabber.org/streams""" && 
			elem.label == """stream""" && elem.prefix == """stream"""
	}

	def process(elem: Elem) {
		val serverAddress = (elem \ "@from").toString
		logger.debug("XMPP Stream open! Server Addr is: {}", serverAddress)
		conn.connCfg.serviceName = serverAddress
	}

}



class StreamFeatureHandler(conn: XMPPConnection) extends MsgHandler 
	with Logging 
{

	def canProcess(elem: Elem): Boolean = {
		elem.namespace ==  null && 
			elem.label == """features""" && elem.prefix == """stream"""
	}

	def process(elem: Elem) {
		logger.debug("received login feature from server")
		startTLS(elem)
	}

	def startTLS(elem: Elem) {
		processMechanisms(elem)
		if ((elem \ "starttls").length > 0) {   // server support TLS
			if ((elem \ "required").length > 0) {   // server require TLS
				// TODO: check is our config support TLS,
				// if server require but our config not , throw exception
			}
			conn write """<starttls xmlns="urn:ietf:params:xml:ns:xmpp-tls" />"""
		}
	}

	def processMechanisms(elem: Node) {
		logger.debug("server require TLS")
		val mchs = (elem \ "mechanisms" \ "mechanism") map (_.text)
		logger.debug("Mechanisms server support : {}", mchs)
		if (mchs.length == 0) {
			logger.debug("no TLS mechanism that server support...")
		} else {
			conn.saslAuthentication.setServerMechNameList(mchs)
		}
	}

}



class ProceedTLSHandler(conn: XMPPConnection) extends MsgHandler 
	with Logging 
{
	import java.security.KeyStore

	import javax.net.ssl.KeyManager
	import javax.net.ssl.SSLContext
	import javax.net.ssl.SSLSocket
	import javax.net.ssl.TrustManager

	import javax.security.auth.callback.PasswordCallback

	import jadeutils.xmpp.utils.ServerTrustManager

	def canProcess(elem: Elem): Boolean = {
		elem.namespace ==  """urn:ietf:params:xml:ns:xmpp-tls""" && 
			elem.label == """proceed""" // && elem.prefix == """"""
	}

	def process(elem: Elem) {
		logger.debug("porceed TLS form server")
		proceedTLSReceived()
	}

	def proceedTLSReceived() {
		var context: SSLContext = null
		var ks: KeyStore = null
		var kms: Array[KeyManager] = null
		var pcb: PasswordCallback = null

		if (null == context) {
			context = SSLContext.getInstance("TLS")
			logger.debug("kms: {}", kms);
			logger.debug("servername: {}", conn.connCfg.serviceName);
			val trustManager: TrustManager = new ServerTrustManager(
				conn.connCfg.serviceName, conn.connCfg)
			context.init(kms, Array(trustManager), new java.security.SecureRandom())
		}
		val plain = conn.ioStream.socket
		conn.ioStream.socket = context.getSocketFactory.createSocket(plain,
			plain.getInetAddress.getHostAddress, plain.getPort, true)
		conn.ioStream.socket.setSoTimeout(0)
		conn.ioStream.socket.setKeepAlive(true)
		conn.ioStream.initReaderAndWriter
		logger.debug("start handshake");
		(conn.ioStream.socket.asInstanceOf[SSLSocket]).startHandshake
		logger.debug("after handshake")
		conn.usingTLS = true

		/* user new IO stream and open new XMPP stream */
		conn.ioStream.packetWriter.writer = conn.ioStream.writer
		conn.ioStream.packetReader.helper.reader = conn.ioStream.reader
		conn.ioStream.openStream
	}

}


// challenge xmlns=""

class SASLChallengeHandler(conn: XMPPConnection) extends MsgHandler 
	with Logging 
{

	def canProcess(elem: Elem): Boolean = {
		elem.namespace ==  """urn:ietf:params:xml:ns:xmpp-sasl""" && 
			elem.label == """challenge""" //&& elem.prefix == """"""
	}

	def process(elem: Elem) {
		logger.debug("received SASL challenge from server：", {})
		conn.saslAuthentication.challengeReceived(elem.text)
	}

}
