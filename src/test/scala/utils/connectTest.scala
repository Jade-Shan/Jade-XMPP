package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import jadeutils.xmpp.model._

@RunWith(classOf[JUnitRunner])
class ConnectionTest extends FunSuite {

	test("Test-resolveXmppClientDomain") {
		val addresses = XmppDNSService.resolveXmppClientDomain("jabber.org")
		// println(XmppDNSService.resolveXmppClientDomain("jabber.org"))
		assert(addresses != Nil  && addresses != Nil && addresses.size > 0)
	}

	test("Test-XmppConnection") {
		val conn = new XMPPConnection("jabber.org", 5222)
		conn.connect()
	}

	test("Test-ServerTrustManager") {
		val conn = new XMPPConnection("jabber.org", 5222)
		ServerTrustManager("jabber.org", conn.connCfg)
	}

}
