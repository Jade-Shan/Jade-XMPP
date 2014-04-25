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
import jadeutils.xmpp.handler.ProceedTLSHandler
import jadeutils.xmpp.handler.SASLChallengeHandler
import jadeutils.xmpp.handler.SASLSuccessHandler
import jadeutils.xmpp.handler.IQHandler
import jadeutils.xmpp.handler.PresenceHandler



@RunWith(classOf[JUnitRunner])
class LoginTest extends FunSuite {
	import LoginTest.MockConnection

	val prop: Properties = new Properties()
	prop.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(
		"connect.properties"))

	val server = prop.getProperty("conn.server")
	val port = Integer.valueOf(prop.getProperty("conn.port"))
	val username = prop.getProperty("conn.username")
	val password = prop.getProperty("conn.password")

	test("Test-login") {
		val conn = new MockConnection(server)
		conn.connect()
		Thread.sleep(10 * 1000)
		conn.login(username, password)
		Thread.sleep(10 * 1000)
		conn write """<message id="8PH20-4" to="evokeralucard@gmail.com" from="jade-shan@jabber.org/Smack" type="chat"><body>Howdy!</body><thread>0VT250</thread></message>"""
		Thread.sleep(10 * 1000)
	}

}

object LoginTest {

	class MockConnection(override val serviceName: String, override val port: Int, 
		override val proxyInfo: ProxyInfo) 
		extends XMPPConnection(serviceName, port, proxyInfo) with Logging
		with MessageProcesser 
	{
		val msgHandlers = new StreamHandler(this) :: 
			new StreamFeatureHandler(this) :: new ProceedTLSHandler(this) :: 
			new SASLChallengeHandler(this) :: new SASLSuccessHandler(this) :: 
			new IQHandler(this) :: new PresenceHandler(this) :: Nil 

		def this(serviceName: String, port: Int) {
			this(serviceName, port, ProxyInfo.forNoProxy)
		}

		def this(serviceName: String) {
			this(serviceName, 5222, ProxyInfo.forNoProxy)
		}
	}

}
