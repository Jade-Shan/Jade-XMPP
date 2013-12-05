package jadeutils.xmpp.model

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

class TestSub(val value: String) extends PacketExtension {
	val elementName = "testSub"
	val namespace = "xml:ns:xmpp-stanzas"
	def toXML = <testSub>{value}</testSub>
}

@RunWith(classOf[JUnitRunner])
class StanzasTest extends FunSuite {

	test("Test-Condition") {
		assert( """<internal-server-error xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
			XMPPError.Condition.toXML(XMPPError.Condition.interna_server_error).toString)

		assert( """<redirect xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
			XMPPError.Condition.toXML(XMPPError.Condition.redirect).toString)

		assert( """<request-timeout xmlns="urn:ietf:params:xml:ns:xmpp-stanzas"/>""" == 
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

	test("Test-Stream-toXML") {
//		val stream = new StanzasStream("jabber.org")
//		assert(stream.toXML.toString == """<stream:stream version="1.0" to="{to}" xmlns:stream="http://etherx.jabber.org/streams" xmlns="jabber:client"></stream:stream>""")
	}



}
