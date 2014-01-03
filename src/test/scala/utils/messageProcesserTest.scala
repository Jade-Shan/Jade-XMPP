package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

//import scala.actors._
//import scala.actors.Actor._
//import scala.xml.Elem

import jadeutils.common.Logging


@RunWith(classOf[JUnitRunner])
class MessageProcesserTest extends FunSuite with Logging{
	
	val conn = new TestConnection( "jabber.org", 25, null)
	val processer = new MessageProcesser(null)
	processer.start

	test("Test-steam") {
		logger.debug("hello?")
		processer ! <stream:stream version="1.0" xmlns="jabber:client" xmlns:stream="http://etherx.jabber.org/streams" to="jabber.org"></stream:stream>
		Thread.sleep(3 * 1000)
	}

}

class TestConnection(override val serviceName: String, override val port: Int, 
	override val proxyInfo: ProxyInfo) 
	extends Connection(serviceName, port, proxyInfo) with Logging
{
}
