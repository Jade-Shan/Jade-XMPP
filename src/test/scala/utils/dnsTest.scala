package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class DnsTest extends FunSuite {

	test("Test-HostAddress-toString") {
		assert((new HostAddress("jabber.com",5222)).toString == "jabber.com:5222")
	}

	test("Test-SRVRecord-toString") {
		assert((new SRVRecord("jabber.com",5222,10,20)).toString == 
			"jabber.com:5222 prio:10 weight:20" )
	}

	test("Test-JavaxResolver") {
		println(JavaxResolver.lookupSRVRecords("_xmpp-client._tcp.jabber.com"))
	}

}
