package jadeutils.common

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

class Building(val name: String)

class Library(override val name: String, val spec: String) 
	extends Building(name) with Logging
{
	this.logger.debug("Building library {} {}", name, spec)
}

object Library extends Logging
{
	def apply(name: String, spec: String) = {
		this.logger.debug("applying library {} {}", name, spec)
		new Library(name, spec)
	}
}

object ProxyType extends Enumeration { val NONE, HTTP, SOCKS4, SOCKS5 = Value }

@RunWith(classOf[JUnitRunner])
class commonTest extends FunSuite {

	test("Test-logging") {
		Library("Jade Library", "Computer Science")
	}

	test("Test-enum") {
	}
}
