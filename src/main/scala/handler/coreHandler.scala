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
		// process subnode "starttls"
		startTLS(elem)
	}

	def startTLS(elem: Elem) {
		val tlsSeq = 
		if ((elem \ "starttls").length > 0) processMechanisms(elem)
	}

	def processMechanisms(elem: Node) {
		logger.debug("server require TLS")
		val mechanisms = elem \ "mechanisms" \ "mechanism"
		logger.debug(elem.toString)
		logger.debug(mechanisms.length.toString)
		// TODO: create merchains in Connection
	}

}
