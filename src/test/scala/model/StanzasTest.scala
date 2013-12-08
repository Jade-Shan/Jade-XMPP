package jadeutils.xmpp.model

import scala.xml.Attribute
import scala.xml.Node
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

class TestSub(val value: String) extends PacketExtension {
	val elementName = "testSub"
	val namespace = "xml:ns:xmpp-stanzas"
	def toXML = <testSub>{value}</testSub>
}



class TestPacket(override val xmlns: String, override val packetId: String, 
	override val from: String, override val to: String, 
	override val error: XMPPError, private[this] var props: Map[String, Any],
	private[this] var pktExts: List[PacketExtension]) extends Packet(
	xmlns, packetId, from, to, error, props, pktExts)
{

	def this(xmlns: String, from: String, to: String, error: XMPPError, 
		props: Map[String, Any], pktExts: List[PacketExtension])
	{
		this(xmlns, Packet.nextId, from, to, error, props, pktExts)
	}

// 	def this(xmlns: String, from: String, to: String, error: XMPPError) {
// 		this(xmlns, Packet.nextId, from, to, error, Nil)
// 	}
// 
// 	def this(from: String, to: String, error: XMPPError, 
// 		pktExts: List[PacketExtension])
// 	{
// 		this(Packet.defaultXmlns, Packet.nextId, from, to, null, pktExts)
// 	}
// 
// 	def this(from: String, to: String) {
// 		this(Packet.defaultXmlns, Packet.nextId, from, to, null, Nil)
// 	}

	def this(p: Packet) {
		this(p.xmlns, p.packetId, p.from, p.to, p.error, p.properties, p.packetExtensions)
	}

	def toXML = addAttributeToXML(<testPacket>{ packetExtensionsXML }{ 
			propertiesXML }{ error.toXML
			}</testPacket>)
}




@RunWith(classOf[JUnitRunner])
class StanzasTest extends FunSuite {

	val cdt = XMPPError.Condition.interna_server_error

	val appExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val pkExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val pktProps: Map[String, Any] = Map("version" -> 50, "name" -> "account",
		"balance" -> 55.35)

	val testPacket = new TestPacket("sor:tkow:xml","from@gmail.com", 
		"to@gmail.com", new XMPPError( XMPPError.Condition.interna_server_error, 
			"Oops", appExtList),
		pktProps, pkExtList)

	test("Test-Condition") {
		assert("""<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
			XMPPError.Condition.toXML(XMPPError.Condition.interna_server_error).toString)

		assert("""<redirect xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
			XMPPError.Condition.toXML(XMPPError.Condition.redirect).toString)

		assert("""<request-timeout xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
			XMPPError.Condition.toXML(XMPPError.Condition.request_timeout).toString)
	}

	test("Test-XMPPError") {
		assert("""<error type="WAIT" code="500">""" + 
			"""<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" + 
			"""<text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text>""" + 
			"""<testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub>""" + 
			"""</error>""" ==
			new XMPPError(cdt, "Oops", appExtList).toXML.toString
		)
		
		assert("""<error type="WAIT" code="500">""" + 
			"""<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" + 
			"""<text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text>""" + 
			"""</error>""" ==
			new XMPPError(cdt, "Oops").toXML.toString
		)
		
		assert("""<error type="WAIT" code="500">""" + 
			"""<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" + 
			"""</error>""" ==
			new XMPPError(cdt).toXML.toString
		)
	}

	test("Test-Packet-contents") {
		// println(testPacket.packetExtensionsXML)
		// println(testPacket.propertiesXML)
		// println(testPacket.error.toXML)
		assert("""List(<testSub>test1</testSub>, <testSub>test2</testSub>, <testSub>test3</testSub>)""" == 
			testPacket.packetExtensionsXML.toString)
		assert("""<properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties>""" == 
			testPacket.propertiesXML.toString)
		assert("""<error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error>""" == 
			testPacket.error.toXML.toString)
	}

	test("Test-Packet") {
		println("""<testPacket to="to@gmail.com" from="from@gmail.com" packetId="NIib7-0"><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error></testPacket>""" ==
			testPacket.toXML.toString)
	}

	test("Test-Stream-toXML") {
//		val stream = new StanzasStream("jabber.org")
//		assert(stream.toXML.toString == """<stream:stream version="1.0" to="{to}" xmlns:stream="http://etherx.jabber.org/streams" xmlns="jabber:client"></stream:stream>""")
	}



}
