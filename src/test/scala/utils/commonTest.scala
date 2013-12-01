package jadeutils.common

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

object ProxyType extends Enumeration { val NONE, HTTP, SOCKS4, SOCKS5 = Value }

@RunWith(classOf[JUnitRunner])
class commonTest extends FunSuite {

}
