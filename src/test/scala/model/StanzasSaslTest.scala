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
class StanzSaslTest extends FunSuite {


	test("Test-SASLMechanism-node-toXML") {
		assert( <auth mechanism="testName" xmlns="urn:ietf:params:xml:ns:xmpp-sasl">XCVOSDFHPWOER2sdfwer==</auth> ==
			XML.loadString(new SASLMechanism.AuthMechanism("testName", "XCVOSDFHPWOER2sdfwer==").toXML.toString))

		assert(<challenge xmlns="urn:ietf:params:xml:ns:xmpp-sasl">XCVOSDFHPWOER2sdfwer==</challenge> ==
			XML.loadString(new SASLMechanism.Challenge( "XCVOSDFHPWOER2sdfwer==").toXML.toString))

		assert(<response xmlns="urn:ietf:params:xml:ns:xmpp-sasl">XCVOSDFHPWOER2sdfwer==</response> ==
			XML.loadString(new SASLMechanism.Response( "XCVOSDFHPWOER2sdfwer==").toXML.toString))


		assert(<success xmlns="urn:ietf:params:xml:ns:xmpp-sasl">XCVOSDFHPWOER2sdfwer==</success> ==
			XML.loadString(new SASLMechanism.Success( "XCVOSDFHPWOER2sdfwer==").toXML.toString))

		assert(<failure xmlns="urn:ietf:params:xml:ns:xmpp-sasl">XCVOSDFHPWOER2sdfwer==</failure> ==
			XML.loadString(new SASLMechanism.Failure( "XCVOSDFHPWOER2sdfwer==").toXML.toString))

//		println((new SASLMechanism.Response( "XCVOSDFHPWOER2sdfwer==").toXML.toString))
//		assert(  ==
//			XML.loadString(new SASLMechanism.Challenge( "XCVOSDFHPWOER2sdfwer==").toXML.toString))
	}

}
