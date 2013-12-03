package jadeutils.xmpp.model

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class StanzasTest extends FunSuite {

	test("Test-Stream-toXML") {
		val stream = new StanzasStream("jabber.org")
		assert(stream.toXML.toString == """<stream:stream version="1.0" to="{to}" xmlns:stream="http://etherx.jabber.org/streams" xmlns="jabber:client"></stream:stream>""")
	}



}
