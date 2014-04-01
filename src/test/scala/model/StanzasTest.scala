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

