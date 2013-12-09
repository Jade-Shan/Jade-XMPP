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

@RunWith(classOf[JUnitRunner])
class StanzTest extends FunSuite {

	val cdt = XMPPError.Condition.interna_server_error

	val pkExtList = new TestSub("test1") :: new TestSub("test2") :: 
	new TestSub("test3") :: Nil

	val pktProps: Map[String, Any] = Map("version" -> 50, "name" -> "account",
		"balance" -> 55.35)

	test("Test-Stream-toXML") {
		assert(
			<stream:stream xmlns="jabber:client" to="to@google.com" version="1.0" xmlns:stream="http://etherx.jabber.org/streams"><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></stream:stream> ==
			XML.loadString(new Stream("to@google.com", pkExtList).toXML.toString))
		assert(
			<stream:stream xmlns="jabber:client" to="to@google.com" version="1.0" xmlns:stream="http://etherx.jabber.org/streams"><properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties"><property><name>version</name><value code="50">integer</value></property><property><name>name</name><value code="account">string</value></property><property><name>balance</name><value code="55.35">double</value></property></properties><testSub>test1</testSub><testSub>test2</testSub><testSub>test3</testSub></stream:stream> ==
			XML.loadString(new Stream("to@google.com", pktProps, pkExtList).toXML.toString))
	}

}
