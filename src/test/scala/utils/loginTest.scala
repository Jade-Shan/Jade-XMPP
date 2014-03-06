package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

import jadeutils.common.Logging

import jadeutils.xmpp.handler.StreamHandler
import jadeutils.xmpp.handler.StreamFeatureHandler



@RunWith(classOf[JUnitRunner])
class LoginTest extends FunSuite {
	import LoginTest.MockConnection

	val prop: Properties = new Properties()
	prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("connect.properties"))

	val server = prop.getProperty("conn.server")
	val port = Integer.valueOf(prop.getProperty("conn.port"))
	val username = prop.getProperty("conn.username")
	val password = prop.getProperty("conn.password")

	test("Test-login") {
		val conn = new MockConnection(server)
		conn.connect()
		conn.login(username, password)
		Thread.sleep(3 * 1000)
	}

}

object LoginTest {

	class MockConnection(override val serviceName: String, override val port: Int, 
		override val proxyInfo: ProxyInfo) 
		extends XMPPConnection(serviceName, port, proxyInfo) with Logging
		with MessageProcesser 
	{
		val msgHandlers = new StreamHandler(this) :: 
			new StreamFeatureHandler(this) :: Nil 

		def this(serviceName: String, port: Int) {
			this(serviceName, port, ProxyInfo.forNoProxy)
		}

		def this(serviceName: String) {
			this(serviceName, 5222, ProxyInfo.forNoProxy)
		}
	}

}
