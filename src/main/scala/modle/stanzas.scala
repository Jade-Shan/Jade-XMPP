package jadeutils.xmpp.model

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicLong

import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.NodeBuffer
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import jadeutils.common.ObjUtils.hashField
import jadeutils.common.StrUtils.randomNumLetterStr
import jadeutils.common.StrUtils.encodeBase64
import jadeutils.common.XMLUtils.newTextAttr


class IQ(private[this] val mType: IQ.Type.Value, 
	override val packetId: String, val id: String, override val from: String, 
	override val to: String, override val error: XMPPError, 
	private[this] var props: Map[String, Any],
	private[this] var pktExts: List[PacketExtension]) extends Packet( null, 
	packetId, from, to, error, props, pktExts)
{

	val msgType = if (null == mType) IQ.Type.GET else mType

	def this(mType: IQ.Type.Value, id: String, from: String, to: String, 
		error: XMPPError, props: Map[String, Any], pktExts: List[PacketExtension])
	{
		this(mType, null, id, from, to, error, props, pktExts) 
	}

	def this(mType: IQ.Type.Value, from: String, to: String, error: XMPPError, 
		props: Map[String, Any], pktExts: List[PacketExtension])
	{
		this(mType, null, Packet.nextId, from, to, error, props, pktExts) 
	}

	override def childElementXML: NodeBuffer = {
		val nb = super.childElementXML
		if (null != error) nb += error.toXML
		nb
	}

	override def addAttributeToXML(node: Elem): Elem = {
		super.addAttributeToXML(node) % Attribute(None, "id", newTextAttr(id), 
			Attribute(None, "type", newTextAttr(msgType.toString), Null))
	}

	def nodeXML(childElementXML: NodeBuffer): Elem = <iq>{childElementXML}</iq> 

}

object IQ {

	object Type extends Enumeration { 
		val GET = Value("get")
		val SET = Value("set")
		val RESULT = Value("result")
		val ERROR = Value("error") 
	}

	def createResultIQ(req: IQ): IQ = {
		if (req.msgType != Type.GET && req.msgType != Type.SET)
			throw new IllegalArgumentException("IQ type must be 'set' or 'get'. " +
			"Original IQ: " + req.toXML)
		else
			new IQ(req.msgType, req.packetId, null, req.from, req.to, 
			null, null, null)
	}


	def createErrorResponse(req: IQ, error: XMPPError): IQ = {
		if (req.msgType != Type.GET && req.msgType != Type.SET)
			throw new IllegalArgumentException("IQ type must be 'set' or 'get'. " +
			"Original IQ: " + req.toXML)
		else
			new IQ(IQ.Type.ERROR, req.packetId, null, req.from, req.to, 
			error, null, null)
	}

}


class Bind(val resource: String) extends PacketExtension {
	val elementName = "bind"
	val namespace = "urn:ietf:params:xml:ns:xmpp-bind"
	def toXML: Elem = <bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"><resource>{resource}</resource></bind>
}

class Session extends PacketExtension {
	val elementName = "session"
	val namespace = "urn:ietf:params:xml:ns:xmpp-session"
	def toXML: Elem = <session xmlns="urn:ietf:params:xml:ns:xmpp-session"/>
}

class Query extends PacketExtension {
	val elementName = "query"
	val namespace =	"jabber:iq:roster"
	def toXML: Elem = <query xmlns="jabber:iq:roster"></query>
}
