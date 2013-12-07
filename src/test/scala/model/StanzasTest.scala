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
	override val from: String, override val to: String, override val error: XMPPError, 
	private[this] var pktExts: List[PacketExtension]) extends Packet(
	xmlns, packetId, from, to, error, pktExts)
{

	def this(xmlns: String, from: String, to: String, error: XMPPError, 
		pktExts: List[PacketExtension])
	{
		this(xmlns, Packet.nextId, from, to, error, pktExts)
	}

 	def this(xmlns: String, from: String, to: String, error: XMPPError) {
 		this(xmlns, Packet.nextId, from, to, error, Nil)
 	}
 
 	def this(from: String, to: String, error: XMPPError, 
 		pktExts: List[PacketExtension])
 	{
 		this(Packet.defaultXmlns, Packet.nextId, from, to, null, pktExts)
 	}
 
 	def this(from: String, to: String) {
 		this(Packet.defaultXmlns, Packet.nextId, from, to, null, Nil)
 	}

	def this(p: Packet) {
		this(p.xmlns, p.packetId, p.from, p.to, p.error, p.packetExtensions)
	}

	def toXML = addAttributeToXML(<testPacket>{ packetExtensionsXML }{ 
			propertiesXML }</testPacket>)
}




@RunWith(classOf[JUnitRunner])
class StanzasTest extends FunSuite {

	test("Test-Condition") {
		assert("""<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
			XMPPError.Condition.toXML(XMPPError.Condition.interna_server_error).toString)

		assert("""<redirect xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
			XMPPError.Condition.toXML(XMPPError.Condition.redirect).toString)

		assert("""<request-timeout xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
			XMPPError.Condition.toXML(XMPPError.Condition.request_timeout).toString)
	}

	test("Test-XMPPError") {
		val subs = new TestSub("test1") :: new TestSub("test1") :: 
			new TestSub("test1") :: Nil
		val cdt = XMPPError.Condition.interna_server_error
		
		assert("""<error type="WAIT" code="500">""" + 
			"""<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" + 
			"""<text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text>""" + 
			"""<testSub>test1</testSub><testSub>test1</testSub><testSub>test1</testSub>""" + 
			"""</error>""" ==
			new XMPPError(cdt, "Oops", subs).toXML.toString
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

	test("Test-Packet") {
		println(new TestPacket("sor:tkow:xml","from@gmail.com", "to@gmail.com", 
			new XMPPError( XMPPError.Condition.interna_server_error, "Oops", 
				new TestSub("test1") :: new TestSub("test2") :: new TestSub("test3") :: 
				Nil),
			new TestSub("test1") :: new TestSub("test2") :: new TestSub("test3") :: 
			Nil).toXML)
	}

	test("Test-Stream-toXML") {
//		val stream = new StanzasStream("jabber.org")
//		assert(stream.toXML.toString == """<stream:stream version="1.0" to="{to}" xmlns:stream="http://etherx.jabber.org/streams" xmlns="jabber:client"></stream:stream>""")
	}



}
