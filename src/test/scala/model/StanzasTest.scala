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

//	test("Test-Stream-toXML") {
//		assert(
//			<stream:stream xmlns="jabber:client" to="to@google.com" version="1.0" xmlns:stream="http://etherx.jabber.org/streams"><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></stream:stream> ==
//			XML.loadString(new Stream("to@google.com", pkExtList).toXML.toString))
//		assert(
//			<stream:stream xmlns="jabber:client" to="to@google.com" version="1.0" xmlns:stream="http://etherx.jabber.org/streams"><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></stream:stream> ==
//			XML.loadString(new Stream("to@google.com", pktProps, pkExtList).toXML.toString))
//	}

	test("Test-IQ") {
		assert(<iq type="get" id="Os3j-5796" from="from@gmail.com" to="to@gmail.com"><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub><error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error></iq> ==
			XML.loadString(new IQ(IQ.Type.GET, "Os3j-5796", "from@gmail.com", "to@gmail.com", err, pktProps, pkExtList).toXML.toString))
//		println(new IQ(IQ.Type.GET, "Os3j-5796", "from@gmail.com", "to@gmail.com", err, pktProps, pkExtList).toXML.toString)
	}

	test("Test-IQ-result") {
		val req = new IQ(IQ.Type.GET, "Os3j-5796", null, "from@gmail.com", "to@gmail.com", err, pktProps, pkExtList)
		assert(<iq type="get" packetId="Os3j-5796" from="from@gmail.com" to="to@gmail.com"></iq> ==
			XML.loadString(IQ.createResultIQ(req).toXML.toString))
//		println(IQ.createResultIQ(req).toXML.toString)

		val badReq = new IQ(IQ.Type.ERROR, "Os3j-5796", null, "from@gmail.com", "to@gmail.com", err, pktProps, pkExtList)
		// intercept(classOf[IllegalArgumentException], "exception here") {
		// 	IQ.createResultIQ(badReq).asInstanceOf[scala.reflect.Manifest]
		// }
		val r = try {
			IQ.createResultIQ(badReq)
			false
		} catch {
			case e: IllegalArgumentException => true
			case _: Throwable => false
		}
		assert(true == r)
	}

	test("Test-IQ-Error") {
		val req = new IQ(IQ.Type.GET, "Os3j-5796", null, 
			"from@gmail.com", "to@gmail.com", err, pktProps, pkExtList)
		assert(<iq type="error" packetId="Os3j-5796" from="from@gmail.com" to="to@gmail.com"><error type="WAIT" code="500"><internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/><text xml:lang="en" xmlns="urn:ietf:params:xml:ns:xmpp-stanzas">Oops</text><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></error></iq> ==
			XML.loadString(IQ.createErrorResponse(req, err).toXML.toString))
//			println(IQ.createErrorResponse(req, err).toXML.toString)

		val badReq = new IQ(IQ.Type.ERROR, "Os3j-5796", null, "from@gmail.com", "to@gmail.com", err, pktProps, pkExtList)
		val r = try {
			IQ.createErrorResponse(badReq, err)
			false
		} catch {
			case e: IllegalArgumentException => true
			case _: Throwable => false
		}
		assert(true == r)
	}

}
