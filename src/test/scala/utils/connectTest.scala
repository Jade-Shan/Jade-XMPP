package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import jadeutils.xmpp.model._

@RunWith(classOf[JUnitRunner])
class ConnectionTest extends FunSuite {

	test("Test-resolveXmppClientDomain") {
		println(XmppDNSService.resolveXmppClientDomain("jabber.org"))
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
