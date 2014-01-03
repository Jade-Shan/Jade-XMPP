package jadeutils.xmpp.handler

import scala.xml.Elem

import jadeutils.common.Logging
import jadeutils.xmpp.utils.MsgHandler 
import jadeutils.xmpp.utils.Connection

class StreamHandler(conn: Connection) extends MsgHandler with Logging {

	def canProcess(elem: Elem): Boolean = {
		elem.namespace == """http://etherx.jabber.org/streams""" && 
			elem.label == """stream""" && 
			elem.prefix == """stream"""
	}

	def process(elem: Elem) {
	}
}