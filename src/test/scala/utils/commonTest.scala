package jadeutils.common

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import net.iharder.Base64



object ProxyType extends Enumeration { val NONE, HTTP, SOCKS4, SOCKS5 = Value }

@RunWith(classOf[JUnitRunner])
class CommonTest extends FunSuite {

	test("Test-base64") {
		assert("rO0ABXQAC2hlbGxvIHdvcmxk" == Base64.encodeObject("hello world"))
	}

	/**
		* test except exception
		*/
	test("Test-Exception") {
		val s = "hi"
		intercept[IndexOutOfBoundsException] {
			s.charAt(-1)
		}
	}

}
