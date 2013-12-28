package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

import jadeutils.xmpp.model._

@RunWith(classOf[JUnitRunner])
class LoginTest extends FunSuite {
	val prop: Properties = new Properties()
	prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("connect.properties"))

	val server = prop.getProperty("conn.server")
	val port = Integer.valueOf(prop.getProperty("conn.port"))
	val username = prop.getProperty("conn.username")
	val password = prop.getProperty("conn.password")

	test("Test-login") {
		val conn = new XMPPConnection(server)
		conn.connect()
		// conn.login(username, password)
	}

}
