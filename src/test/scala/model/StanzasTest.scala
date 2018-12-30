package jadeutils.xmpp.model

import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeBuffer
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

class TestSub(val value: String) extends SubPacket {
	val elementName = "testSub"
	val namespace = "xml:ns:xmpp-stanzas"
	def toXML: Elem = <testSub>{value}</testSub>
}



class TestPacket(override val xmlns: String, override val packetId: String, 
	override val from: String, override val to: String, 
	override val error: XMPPError, private[this] var props: Map[String, Any],
	private[this] var subPkts: List[SubPacket]) extends Packet(
	xmlns, packetId, from, to, error, props, subPkts)
{

	def this(xmlns: String, from: String, to: String, error: XMPPError, 
		props: Map[String, Any], subPkts: List[SubPacket])
	{
		this(xmlns, Packet.nextId, from, to, error, props, subPkts)
	}

	def this(from: String, to: String, error: XMPPError, props: Map[String, Any],
		subPkts: List[SubPacket])
	{
		this(Packet.defaultXmlns, Packet.nextId, from, to, error, props, subPkts)
	}

	def this(xmlns: String, packetId: String, from: String, to: String) {
		this(xmlns, packetId, from, to, null, null, null)
	}

	def this(from: String, to: String) {
		this(from, to, null, null, null)
	}

	def this(p: Packet) {
		this(p.xmlns, p.packetId, p.from, p.to, p.error, p.properties, 
			p.subPackets)
	}

	def nodeXML(childElementXML: NodeBuffer): Elem = <testPacket>{
		childElementXML}</testPacket>

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
			"Oops", appExtList), pktProps, pkExtList)

	test("Test-Condition") {
		assert(<internal-server-error 
			xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/> == 
			XMPPError.Condition.toXML(XMPPError.Condition.interna_server_error))

		assert(<redirect xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/> == 
			XMPPError.Condition.toXML(XMPPError.Condition.redirect))

		assert(<request-timeout xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/> == 
			XMPPError.Condition.toXML(XMPPError.Condition.request_timeout))
	}

	test("Test-XMPPError") {
		// <?xml version="1.0" encoding="UTF-8"?>
		// <error code="500" type="WAIT">
		// ..<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />
		// ..<text xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" xml:lang="en">Oops</text>
		// ..<testSub>test1</testSub>
		// ..<testSub>test2</testSub>
		// ..<testSub>test3</testSub>
		// </error>
		assert(<error code="500" type="WAIT"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error> ==
			new XMPPError(cdt, "Oops", appExtList).toXML)
		
		// <?xml version="1.0" encoding="UTF-8"?>
		// <error type="WAIT" code="500">
		// ..<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />
		// ..<text xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" xml:lang="en">Oops</text>
		// </error>
		assert(<error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text></error> ==
			new XMPPError(cdt, "Oops").toXML)
		

		// <?xml version="1.0" encoding="UTF-8"?>
		// <error type="WAIT" code="500">
		// ..<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />
		// </error>
		assert(<error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/></error> ==
			new XMPPError(cdt).toXML)
	}

	test("Test-Packet-contents") {

		assert(List(<testSub>test1</testSub>, <testSub>test2</testSub>, <testSub>test3</testSub>) == 
			testPacket.subPacketsXML)

		// <?xml version="1.0" encoding="UTF-8"?>
		// <properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties">
		// ..<property>
		// ....<name>version</name>
		// ....<value code="50">integer</value>
		// ..</property>
		// ..<property>
		// ....<name>name</name>
		// ....<value code="account">string</value>
		// ..</property>
		// ..<property>
		// ....<name>balance</name>
		// ....<value code="55.35">double</value>
		// ..</property>
		// </properties>
		assert(<properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties> == 
			testPacket.propertiesXML)


		// <?xml version="1.0" encoding="UTF-8"?>
		// <error type="WAIT" code="500">
		// ..<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />
		// ..<text xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" xml:lang="en">Oops</text>
		// ..<testSub>test1</testSub>
		// ..<testSub>test2</testSub>
		// ..<testSub>test3</testSub>
		// </error>
		assert(<error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error> == 
			testPacket.error.toXML)
	}

	test("Test-Packet") {

		// <?xml version="1.0" encoding="UTF-8"?>
		// <testPacket xmlns="sor:tkow:xml" packetId="4B2Lx-0" from="from@gmail.com"
		// ....to="to@gmail.com">
		// ..<properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties">
		// ....<property>
		// ......<name>version</name>
		// ......<value code="50">integer</value>
		// ....</property>
		// ....<property>
		// ......<name>name</name>
		// ......<value code="account">string</value>
		// ....</property>
		// ....<property>
		// ......<name>balance</name>
		// ......<value code="55.35">double</value>
		// ....</property>
		// ..</properties>
		// ..<testSub>test1</testSub>
		// ..<testSub>test2</testSub>
		// ..<testSub>test3</testSub>
		// </testPacket>
		assert(<testPacket xmlns="sor:tkow:xml" packetId="4B2Lx-0" from="from@gmail.com" to="to@gmail.com"><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></testPacket> ==
			XML.loadString(testPacket.toXML.toString))
	}

}


@RunWith(classOf[JUnitRunner])
class StanzTest extends FunSuite {

	val pkExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val pktProps: Map[String, Any] = Map("version" -> 50, "name" -> "account",
		"balance" -> 55.35)

	val appExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val err = new XMPPError( XMPPError.Condition.interna_server_error, 
		"Oops", appExtList)

	test("Test-IQ") {
		println(new IQ(IQ.Type.GET, "from@gmail.com", "to@gmail.com", null, null, 
			null).toXML)
	}

	test("Test-IQ-II") {

		// <iq type="get" id="Os3j-5796" from="from@gmail.com" to="to@gmail.com">
		// ..<properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties">
		// ....<property>
		// ....  <name>version</name>
		// ....  <value code="50">integer</value>
		// ....</property>
		// ....<property>
		// ......<name>name</name>
		// ......<value code="account">string</value>
		// ....</property>
		// ....<property>
		// ......<name>balance</name>
		// ......<value code="55.35">double</value>
		// ....</property>
		// ..</properties>
		// ..<testSub>test1</testSub>
		// ..<testSub>test2</testSub>
		// ..<testSub>test3</testSub>
		// ..<error type="WAIT" code="500">
		// ....<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />
		// ....<text xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" xml:lang="en">Oops</text>
		// ....<testSub>test1</testSub>
		// ....<testSub>test2</testSub>
		// ....<testSub>test3</testSub>
		// ..</error>
		// </iq>
		assert(<iq type="get" id="Os3j-5796" from="from@gmail.com" to="to@gmail.com"><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub><error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error></iq> ==
			XML.loadString(new IQ(IQ.Type.GET, "Os3j-5796", "from@gmail.com", //
				"to@gmail.com", err, pktProps, pkExtList).toXML.toString))
	}

	test("Test-IQ-result") {

		// <iq type="get" packetId="Os3j-5796" from="from@gmail.com" 
		// ..to="to@gmail.com"></iq>
		val req = new IQ(IQ.Type.GET, "Os3j-5796", null, "from@gmail.com", //
			"to@gmail.com", err, pktProps, pkExtList)
		assert(<iq type="get" packetId="Os3j-5796" from="from@gmail.com" to="to@gmail.com"></iq> ==
			XML.loadString(IQ.createResultIQ(req).toXML.toString))

		val badReq = new IQ(IQ.Type.ERROR, "Os3j-5796", null, "from@gmail.com", //
			"to@gmail.com", err, pktProps, pkExtList)
		withClue("Except IllegalArgumentException") {
			intercept[IllegalArgumentException] { IQ.createResultIQ(badReq) }
		}
		// // this is a hard way to test expect exception
		// val r = try {
		// 	IQ.createResultIQ(badReq)
		// 	false
		// } catch {
		// 	case e: IllegalArgumentException => true
		// 	case _: Throwable => false
		// }
		// assert(true == r)
	}

	test("Test-IQ-Error") {

		// <?xml version="1.0" encoding="UTF-8"?>
		// <iq type="error" packetId="Os3j-5796" from="from@gmail.com" to="to@gmail.com">
		// ..<error type="WAIT" code="500">
		// ....<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />
		// ....<text xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" xml:lang="en">Oops</text>
		// ....<testSub>test1</testSub>
		// ....<testSub>test2</testSub>
		// ....<testSub>test3</testSub>
		// ..</error>
		// </iq>
		val req = new IQ(IQ.Type.GET, "Os3j-5796", null, "from@gmail.com", 
			"to@gmail.com", err, pktProps, pkExtList)
		assert(<iq type="error" packetId="Os3j-5796" from="from@gmail.com" to="to@gmail.com"><error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error></iq> ==
			XML.loadString(IQ.createErrorResponse(req, err).toXML.toString))

		val badReq = new IQ(IQ.Type.ERROR, "Os3j-5796", null, "from@gmail.com", 
			"to@gmail.com", err, pktProps, pkExtList)
		withClue("Except IllegalArgumentException") {
			intercept[IllegalArgumentException] {
				IQ.createErrorResponse(badReq, err)
			}
		}
	}

	test("Test-Bind") {
		// <iq id="Os3j-5796" type="get">
		// ..<bind xmlns="urn:ietf:params:xml:ns:xmpp-bind">
		// ....<resource>jade-cellphone</resource>
		// ..</bind>
		// </iq>
		assert(<iq id="Os3j-5796" type="get"><bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"><resource>jade-cellphone</resource></bind></iq> ==
			XML.loadString(new IQ(IQ.Type.GET, "Os3j-5796", null, null, null, null, 
				new Bind("jade-cellphone") :: Nil).toXML.toString))
	}

	test("Test-Session") {
		assert(<iq id="Os3j-5796" type="get"><session xmlns="urn:ietf:params:xml:ns:xmpp-session"/></iq> ==
			XML.loadString(new IQ(IQ.Type.GET, "Os3j-5796", null, null, null, null, 
				new Session :: Nil).toXML.toString))
	}

}



@RunWith(classOf[JUnitRunner])
class MessageTest extends FunSuite {

	test("Test-Create-Message") {
		// val id = "jadexmpp" + Packet.nextId()
		val id = "jadexmppIBIDM-0"
		assert(<message type="chat" id="jadexmppIBIDM-0" from="aa@gmail.com" to="bb@gmail.com"><active xmlns="http://jabber.org/protocol/chatstates"/><body>hello</body></message> ==
			XML.loadString(new Message(id, "aa@gmail.com", "bb@gmail.com", "hello").toXML.toString))
	}

}

object MessageTest {

}
