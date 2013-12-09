package jadeutils.xmpp.model

import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeBuffer
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith


class TestIQ(private[this] val mType: IQ.Type.Value, override val id: String, 
	override val from: String, override val to: String, 
	override val error: XMPPError, private[this] var props: Map[String, Any],
	private[this] var pktExts: List[PacketExtension]) extends IQ ( mType, id, 
	from, to, error, props, pktExts)
{
}

@RunWith(classOf[JUnitRunner])
class StanzTest extends FunSuite {

	val cdt = XMPPError.Condition.interna_server_error

	val pkExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val pktProps: Map[String, Any] = Map("version" -> 50, "name" -> "account",
		"balance" -> 55.35)

	val appExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val err = new XMPPError( XMPPError.Condition.interna_server_error, 
			"Oops", appExtList)

	test("Test-Stream-toXML") {
		assert(
			<stream:stream xmlns="jabber:client" to="to@google.com" version="1.0" xmlns:stream="http://etherx.jabber.org/streams"><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></stream:stream> ==
			XML.loadString(new Stream("to@google.com", pkExtList).toXML.toString))
		assert(
			<stream:stream xmlns="jabber:client" to="to@google.com" version="1.0" xmlns:stream="http://etherx.jabber.org/streams"><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></stream:stream> ==
			XML.loadString(new Stream("to@google.com", pktProps, pkExtList).toXML.toString))
	}

	test("Test-IQ") {
		assert(<iq type="get" id="Os3j-5796" from="from@gmail.com" to="to@gmail.com"><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></iq> ==
			XML.loadString(new TestIQ(IQ.Type.GET, "Os3j-5796", "from@gmail.com", 
				"to@gmail.com", err, pktProps, pkExtList).toXML.toString))
		}

}
