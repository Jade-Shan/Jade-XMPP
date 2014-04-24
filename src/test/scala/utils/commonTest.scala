package jadeutils.common

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import net.iharder.Base64
import org.apache.commons.lang.StringUtils.isBlank



object ProxyType extends Enumeration { val NONE, HTTP, SOCKS4, SOCKS5 = Value }

@RunWith(classOf[JUnitRunner])
class CommonTest extends FunSuite {

	test("Test-isBlank") {
		assert(isBlank(null))
		assert(isBlank(""))
		assert(isBlank("       "))
		assert(!isBlank("aaaaa"))
	}

	test("Test-Equals-ignore-blank") {
		assert( StrUtils.equalsIgnoreBlank("a","a"))
		assert(!StrUtils.equalsIgnoreBlank("a","b"))
		assert(!StrUtils.equalsIgnoreBlank("","a"))
		assert(!StrUtils.equalsIgnoreBlank("a",""))
		assert( StrUtils.equalsIgnoreBlank("",null))
		assert(!StrUtils.equalsIgnoreBlank("a",null))
	}

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
