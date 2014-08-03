package jadeutils.xmpp.handler

import scala.xml.Elem
import scala.xml.Node

import jadeutils.common.Logging

import jadeutils.xmpp.model.Packet
import jadeutils.xmpp.model.IQ
import jadeutils.xmpp.model.Bind
import jadeutils.xmpp.model.Session
import jadeutils.xmpp.model.Query
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
		logger.debug("received feature from server")
		if ((elem \ "mechanisms").length > 0) { processMechanisms(elem) }
		if ((elem \ "starttls").length > 0) { startTLS(elem) }
		if ((elem \ "bind").length > 0) { receivedBind() }
		if ((elem \ "session").length > 0) { receivedSession() }
	}

	def receivedSession() {
		conn write new IQ(IQ.Type.GET, null, null, null, null, 
			new Session() :: Nil).toXML.toString
	}

	def receivedBind() {
		conn write new IQ(IQ.Type.GET, null, null, null, null, 
			new Bind(conn.connCfg.resource) :: Nil).toXML.toString
	}

	def startTLS(elem: Elem) {
			if ((elem \ "required").length > 0) {   // server require TLS
			// TODO: check is our config support TLS,
			// if server require but our config not , throw exception
		}
		conn write """<starttls xmlns="urn:ietf:params:xml:ns:xmpp-tls" />"""
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
		logger.debug("Success switch to SSL IO, start new stream:\n\n\n")
		conn.ioStream.openStream
	}

}



class SASLChallengeHandler(conn: XMPPConnection) extends MsgHandler 
	with Logging 
{

	def canProcess(elem: Elem): Boolean = {
		elem.namespace ==  """urn:ietf:params:xml:ns:xmpp-sasl""" && 
			elem.label == """challenge""" //&& elem.prefix == """"""
	}

	def process(elem: Elem) {
		val text = elem.text
		logger.debug("received SASL challenge from server：{}", text)
		conn.saslAuthentication.challengeReceived(text)
	}

}



class SASLSuccessHandler(conn: XMPPConnection) extends MsgHandler 
	with Logging 
{

	def canProcess(elem: Elem): Boolean = {
		elem.namespace ==  """urn:ietf:params:xml:ns:xmpp-sasl""" && 
			elem.label == """success""" //&& elem.prefix == """"""
	}

	def process(elem: Elem) {
		val text = elem.text
		logger.debug("received SASL success from server：{}", text)
		conn.saslAuthentication.authenticated

		logger.debug("Sasl Success from server, start new stream:\n\n\n")
		conn.ioStream.openStream
	}

}

class IQHandler(conn: XMPPConnection) extends MsgHandler 
	with Logging 
{
	import jadeutils.xmpp.model.Jid
	import jadeutils.xmpp.model.Roster
	import jadeutils.xmpp.model.Roster.Item
	import jadeutils.xmpp.model.Roster.Subscription

	def canProcess(elem: Elem): Boolean = {
		//elem.namespace ==  """urn:ietf:params:xml:ns:xmpp-sasl""" && 
			elem.label == """iq""" //&& elem.prefix == """"""
	}

	def process(elem: Elem) {
		if ((elem \ "_").length == 0) {
			requireRoster() 
			requirePresence()
		} else {
			logger.debug("update member 01")
			(elem \ "query" \ "item").foreach((elem: Node) => {
					val jid: Jid = try {
						Jid.fromString((elem \ "@jid").toString).getOrElse(null)
					} catch {
						case _ : Throwable => null
					}
					if (null != jid) {
						logger.debug("update member 02")
						val name = (elem \ "@name").toString
						val group = (elem \ "group").text.toString
						val subscription = Roster.Subscription.both
						val item = new Roster.Item(jid, name, group, subscription)
						conn.roster ! item
					}
				})
		}
	}

	def requirePresence() {
		conn write """<presence id="%s"></presence>""".format(Packet.nextId)
	}

	def requireRoster() {
		conn write new IQ(IQ.Type.GET, null, null, null, null, 
			new Query() :: Nil).toXML.toString
	}

}


class PresenceHandler(conn: XMPPConnection) extends MsgHandler with Logging {

	import jadeutils.xmpp.model.Jid
	import jadeutils.xmpp.model.Roster
	import jadeutils.xmpp.model.Roster.Presence
	
	def canProcess(elem: Elem): Boolean = {
		//elem.namespace ==  """urn:ietf:params:xml:ns:xmpp-sasl""" && 
			elem.label == """presence""" //&& elem.prefix == """"""
	}

	def process(elem: Elem) {
		val jid: Jid = try {
			Jid.fromString((elem \ "@from").toString).getOrElse(null)
		} catch {
			case _ : Throwable => null
		}
		val priority: Int = try {
			Integer.parseInt((elem \ "priority").text.toString)
		} catch {
			case _ : Throwable => 5
		}
		val status: String = (elem \ "status").text.toString
		val show: String = (elem \ "show").text.toString
		if(null != jid) {
			val presence = new Roster.Presence(jid, priority, status, show)
			conn.roster ! presence
			logger.debug("roster after updatePresence: {}", conn.roster)
		}
	}

}


abstract class MessageHandler(conn: XMPPConnection) 
	extends MsgHandler with Logging 
{
	import jadeutils.xmpp.model.Jid
	import jadeutils.xmpp.model.Message

	def canProcess(elem: Elem): Boolean = {
		//elem.namespace ==  """urn:ietf:params:xml:ns:xmpp-sasl""" && 
			elem.label == """message""" //&& elem.prefix == """"""
	}

	def process(elem: Elem) {
		val from: Jid = try {
			Jid.fromString((elem \ "@from").toString).getOrElse(null)
		} catch {
			case _ : Throwable => null
		}
		val to: Jid = try {
			Jid.fromString((elem \ "@to").toString).getOrElse(null)
		} catch {
			case _ : Throwable => null
		}
		val id: String = try {
			(elem \ "@id").text.toString
		} catch {
			case _ : Throwable => null
		}
		val body: String = (elem \ "body").text.toString

		this.onMessageReceive(new Message(id, from.toString, to.toString, body))
	}

	def onMessageReceive(msg: Message)

}
