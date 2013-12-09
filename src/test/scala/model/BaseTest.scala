package jadeutils.xmpp.model

import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Node
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class JidTest extends FunSuite {

	test("Test-JID-equals") {
		assert(Jid("jade", "jade-dungeon.net", "cellphone") == 
			Jid("jade", "jade-dungeon.net", "cellphone"))
	}

	test("Test-JID-hashCode") {
		assert(Jid("jade", "jade-dungeon.net", "cellphone").hashCode == 
			Jid("jade", "jade-dungeon.net", "cellphone").hashCode)
		assert(Jid("jade", "jade-dungeon.net", "cellphone").hashCode != 
			Jid("jad3", "jade-dungeon.net", "cellphone").hashCode)
	}

	test("Test-JID-toString") {
		/*
		 * case as full
		 */
		assert(Jid("jade", "jade-dungeon.net", "cellphone").toString == 
			"jade@jade-dungeon.net/cellphone")

		/*
		 * case as no resource
		 */
		assert(Jid("jade","jade-dungeon.net", null).toString == 
			"jade@jade-dungeon.net")
		assert(Jid("jade","jade-dungeon.net", "").toString == 
			"jade@jade-dungeon.net")
		assert(Jid("jade","jade-dungeon.net", "   ").toString == 
			"jade@jade-dungeon.net")

		/*
		 * case as no local
		 */
		assert(Jid(null, "jade-dungeon.net", "cellphone").toString == 
			"jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "cellphone").toString == 
			"jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "cellphone").toString == 
			"jade-dungeon.net")
		assert(Jid(null, "jade-dungeon.net", null).toString == "jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "").toString == "jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "   ").toString == "jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "   ").toString == "jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "").toString == "jade-dungeon.net")

		/*
		 * case as domain is null
		 */
		assert(Jid("jade", null, "cellphone").toString == "")
		assert(Jid("jade", "", "cellphone").toString == "")
		assert(Jid("jade", "   ", "cellphone").toString == "")
		assert(Jid(null, null, "cellphone").toString == "")
		assert(Jid("", "", "cellphone").toString == "")
		assert(Jid("jade", null, null).toString == "")
		assert(Jid("jade", "", "   ").toString == "")
		assert(Jid("jade", "  ", "").toString == "")
		assert(Jid(null, null, null).toString == "")
		assert(Jid("", "", "  ").toString == "")
		assert(Jid("  ", "  ", "").toString == "")
	}

	test("Test-JID-fromString") {
		var c = Jid.fromString("aa@bb.com/cc")
		assert(c.toString == "Some(aa@bb.com/cc)")
		c = Jid.fromString("aa@bb.com")
		assert(c.toString == "Some(aa@bb.com)")
		c = Jid.fromString("aa@bb.com")
		assert(c.toString == "Some(aa@bb.com)")
		c = Jid.fromString("bb.com")
		assert(c.toString == "Some(bb.com)")
		c = Jid.fromString("!!!!!!!!!")
		assert(c.toString == "None")
	}

	test("Test-JID-apply") {
		var c = Jid("jade", "jade-dungeon.net", "cellphone")
		assert(c.toString == "jade@jade-dungeon.net/cellphone")
		c = Jid(null, "jade-dungeon.net", "cellphone")
		assert(c.toString == "jade-dungeon.net")
		c = Jid("jade", null, "cellphone")
		assert(c.toString == "")
		c = Jid("jade", "jade-dungeon.net", null)
		assert(c.toString == "jade@jade-dungeon.net")
		c = Jid(null, null, "cellphone")
		assert(c.toString == "")
		c = Jid(null, "jade-dungeon.net", null)
		assert(c.toString == "jade-dungeon.net")
		c = Jid("jade", null, null)
		assert(c.toString == "")
		c = Jid(null, null, null)
		assert(c.toString == "")
	}

	test("Test-JID-unapply") {
		var c = Jid("jade", "jade-dungeon.net", "cellphone")
		var d = Jid.unapply(c)
		assert(d.toString == "Some((jade,jade-dungeon.net,cellphone))")
		c = Jid(null, "jade-dungeon.net", "cellphone")
		d = Jid.unapply(c)
		assert(d.toString == "Some((null,jade-dungeon.net,cellphone))")
		c = Jid("jade", null, "cellphone")
		d = Jid.unapply(c)
		assert(d.toString == "Some((jade,null,cellphone))")
		c = Jid("jade", "jade-dungeon.net", null)
		d = Jid.unapply(c)
		assert(d.toString == "Some((jade,jade-dungeon.net,null))")
		c = Jid(null, null, "cellphone")
		d = Jid.unapply(c)
		assert(d.toString == "Some((null,null,cellphone))")
		c = Jid(null, "jade-dungeon.net", null)
		d = Jid.unapply(c)
		assert(d.toString == "Some((null,jade-dungeon.net,null))")
		c = Jid("jade", null, null)
		d = Jid.unapply(c)
		assert(d.toString == "Some((jade,null,null))")
		c = Jid(null, null, null)
		d = Jid.unapply(c)
		assert(d.toString == "Some((null,null,null))")
	}

}




class TestSub(val value: String) extends PacketExtension {
	val elementName = "testSub"
	val namespace = "xml:ns:xmpp-stanzas"
	def toXML: Elem = <testSub>{value}</testSub>
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

	def this(from: String, to: String, error: XMPPError, props: Map[String, Any],
		pktExts: List[PacketExtension])
	{
		this(Packet.defaultXmlns, Packet.nextId, from, to, error, props, pktExts)
	}

	def this(xmlns: String, packetId: String, from: String, to: String) {
		this(xmlns, packetId, from, to, null, null, null)
	}

	def this(from: String, to: String) {
		this(from, to, null, null, null)
	}

	def this(p: Packet) {
		this(p.xmlns, p.packetId, p.from, p.to, p.error, p.properties, 
			p.packetExtensions)
	}

	def toXML: Node = addAttributeToXML(<testPacket>{contentXML}</testPacket>)

}



@RunWith(classOf[JUnitRunner])
class PacketTest extends FunSuite {

	val cdt = XMPPError.Condition.interna_server_error

	val appExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val pkExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val pktProps: Map[String, Any] = Map("version" -> 50, "name" -> "account",
		"balance" -> 55.35)

	val testPacket = new TestPacket("sor:tkow:xml", "4B2Lx-0", "from@gmail.com", 
		"to@gmail.com", new XMPPError( XMPPError.Condition.interna_server_error, 
			"Oops", appExtList),
		pktProps, pkExtList)

	test("Test-Condition") {
		assert(<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/> == 
			XMPPError.Condition.toXML(XMPPError.Condition.interna_server_error))

		assert(<redirect xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/> == 
			XMPPError.Condition.toXML(XMPPError.Condition.redirect))

		assert(<request-timeout xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/> == 
			XMPPError.Condition.toXML(XMPPError.Condition.request_timeout))
	}

	test("Test-XMPPError") {
		assert(<error code="500" type="WAIT"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error> ==
			new XMPPError(cdt, "Oops", appExtList).toXML)
		
		assert(<error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text></error> ==
			new XMPPError(cdt, "Oops").toXML)
		
		assert(<error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/></error> ==
			new XMPPError(cdt).toXML)
	}

	test("Test-Packet-contents") {
		assert(List(<testSub>test1</testSub>, <testSub>test2</testSub>, <testSub>test3</testSub>) == 
			testPacket.packetExtensionsXML)
		assert(<properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties> == 
			testPacket.propertiesXML)
		assert(<error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error> == 
			testPacket.error.toXML)
	}

	test("Test-Packet") {
		assert(<testPacket xmlns="sor:tkow:xml" packetId="4B2Lx-0" from="from@gmail.com" to="to@gmail.com"><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></testPacket> ==
			XML.loadString(testPacket.toXML.toString))
	}

	test("Test-Stream-toXML") {
//		val stream = new StanzasStream("jabber.org")
//		assert(stream.toXML.toString == """<stream:stream version="1.0" to="{to}" xmlns:stream="http://etherx.jabber.org/streams" xmlns="jabber:client"></stream:stream>""")
	}



}
