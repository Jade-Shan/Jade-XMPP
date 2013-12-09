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

import jadeutils.common.Logging
import jadeutils.common.ObjUtils.hashField
import jadeutils.common.StrUtils.randomNumLetterStr
import jadeutils.common.StrUtils.encodeBase64


class Stream(override val to: String, private[this] var props: Map[String, Any], 
	private[this] var pktExts: List[PacketExtension]) extends Packet(
	"jabber:client", null, null, to, null, props, pktExts)
{

	def this(to: String, pkExtList: List[PacketExtension]) {
		this(to, null, pkExtList)
	}

	def nodeXML(childElementXML: NodeBuffer): Elem = <stream:stream version="1.0"
		xmlns:stream="http://etherx.jabber.org/streams">{
		childElementXML}</stream:stream>

}

 
abstract class IQ(private[this] val mType: IQ.Type.Value, val id: String, 
	override val from: String, override val to: String, 
	override val error: XMPPError, private[this] var props: Map[String, Any],
	private[this] var pktExts: List[PacketExtension]) extends Packet(
	null, null, from, to, error, props, pktExts)
{

	val msgType = if (null == mType) IQ.Type.GET else mType

	override def addAttributeToXML(node: Elem): Elem = {
		super.addAttributeToXML(node) % Attribute(None, "id", Text(id), 
			Attribute(None, "type", Text(msgType.toString), Null))
	}

	def nodeXML(childElementXML: NodeBuffer): Elem = <iq>{childElementXML}</iq> 

}

object IQ extends Logging {

	object Type extends Enumeration { 
		val GET = Value("get")
		val SET = Value("set")
		val RESULT = Value("result")
		val ERROR = Value("error") 
	}

}
