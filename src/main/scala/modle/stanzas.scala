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

	def nodeXML(fillContent: NodeBuffer): Elem = <stream:stream version="1.0"
		xmlns:stream="http://etherx.jabber.org/streams">{
		fillContent}</stream:stream>

}

// class StanzasStream(val to: String) extends Packet {
// 
// 	def toXML = <stream:stream version="1.0" xmlns="jabber:client" 
// 		xmlns:stream="http://etherx.jabber.org/streams"
// 		to="{to}"></stream:stream>
// 
// }
 
// abstract class IQ extends Packet {
// 	var msgType = IQ.Type.GET
// 
// 	def getChildElementXML(): Node
// 
// }
// 
// object IQ extends Logging {
// 
// 	object Type extends Enumeration { 
// 		val GET = Value("get")
// 		val SET = Value("set")
// 		val RESULT = Value("result")
// 		val ERROR = Value("error") 
// 	}
// 
// }
