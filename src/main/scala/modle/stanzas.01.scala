package jadeutils.xmpp.model

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicLong

import scala.collection.immutable.HashMap
import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeBuffer
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import jadeutils.common.ObjUtils.hashField
import jadeutils.common.StrUtils.randomNumLetterStr
import jadeutils.common.StrUtils.encodeBase64
import jadeutils.common.XMLUtils.newTextAttr

class Presence (val id: String, val language: String, 
	val prsType: Presence.Type.Value, val status: String, 
	val priority: Int, val mode: Presence.Mode.Value,
	private[this] var pktExts: List[PacketExtension]) extends Packet(
	null, null, null, null, null, null, pktExts)
{

	def isAvailable = prsType == Presence.Type.available

	def isAway = {
		prsType == Presence.Type.available && (mode == Presence.Mode.away || 
			mode == Presence.Mode.xa || 
			mode == Presence.Mode.dnd); 
	}

	override def childElementXML: NodeBuffer = {
		val nb = super.childElementXML
		if (null != status) nb += <status>{status}</status>
		nb += <priority>{
			if (priority > 128) 128
			else if (priority < -128) -128
			else priority
		}</priority>
		if (null != mode) nb += <show>{mode.toString}</show>
		nb
	}

	override def addAttributeToXML(node: Elem): Elem = { 
		super.addAttributeToXML(node) % 
		Attribute(None, "id", newTextAttr(id), 
			Attribute(None, "type", newTextAttr(prsType.toString), Null))
	}

	def nodeXML(childElementXML: NodeBuffer): Elem = <presence>{
		childElementXML
	}</presence>

}

object Presence {

	object Type extends Enumeration { 
		val available, unavailable, subscribe, subscribed, unsubscribe, 
		unsubscribed, error = Value
	}

	object Mode extends Enumeration { 
		val chat, available, away, xa, dnd = Value
	}
}

