package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import jadeutils.xmpp.handler.StreamHandler

import jadeutils.common.Logging

/**
 * 消息处理器测试，消息处理器能接收到消息
 */


@RunWith(classOf[JUnitRunner])
class MessageProcesserTest extends FunSuite with Logging {
	import MessageProcesserTest.MockConnection
	
	val conn = new MockConnection("jabber.org", 25, ProxyInfo.forNoProxy)

	test("Test-steam") {
		logger.debug("hello?")
		conn.foreachHandler(
			<stream:stream version="1.0" xmlns="jabber:client" 
			xmlns:stream="http://etherx.jabber.org/streams" 
			to="jabber.org"></stream:stream>)
		Thread.sleep(3 * 1000)
	}

}

object MessageProcesserTest {
	class MockConnection(override val serviceName: String, override val port: Int, 
		override val proxyInfo: ProxyInfo) 
		extends XMPPConnection(serviceName, port, proxyInfo) with Logging
		with MessageProcesser 
	{
		val msgHandlers = new StreamHandler(this) :: Nil 
	}
}

/**
 * 消息处理器测试，通过反射动态调用类的构造方法
 */
class MyMock(val id: Int, val name: String)

@RunWith(classOf[JUnitRunner])
class MessageProcesserLoderTest extends FunSuite with Logging {
	
	test("Test-CreateClass") {
		val mm = classOf[MyMock].getConstructor(
			classOf[Int],classOf[String]).newInstance(new Integer(1), "Jade")
		assert(1 == mm.id && "Jade" == mm.name)
	}

	test("Test-CreateClass-By-Name") {
		val mm: MyMock = Class.forName("jadeutils.xmpp.utils.MyMock").getConstructor(
			classOf[Int],Class.forName("java.lang.String"))
			.newInstance(new Integer(1), "Jade").asInstanceOf[MyMock]
		assert(1 == mm.id && "Jade" == mm.name)
	}

}


@RunWith(classOf[JUnitRunner])
class MessageHandlerTest extends FunSuite with Logging {
	import MessageHandlerTest.TestMessageHandler

	test("Test-Receive-Message") {
		val handler = new TestMessageHandler(null)

		handler.handle(<message type="chat" id="jadexmppIBIDM-0" 
			from="aa@gmail.com" to="bb@gmail.com"><active 
			xmlns="http://jabber.org/protocol/chatstates"
			/><body>hello</body></message>)
	}

}

object MessageHandlerTest extends FunSuite with Logging {
	import jadeutils.xmpp.handler.MessageHandler
	import jadeutils.xmpp.model.Message

	class TestMessageHandler(conn: XMPPConnection) 
		extends MessageHandler(conn)
	{

		def onMessageReceive(msg: Message) {
			println(" xml is: " + msg.toXML)
			println("  id is: " + msg.id)
			println("from is: " + msg.from)
			println("  to is: " + msg.to)
			println("body is: " + msg.msgStr)

			assert("jadexmppIBIDM-0" == msg.id)
			assert("aa@gmail.com"    == msg.from)
			assert("bb@gmail.com"    == msg.to)
			assert("hello"           == msg.msgStr)
		}

	}

	class MockConnection(override val serviceName: String, override val port: Int, 
		override val proxyInfo: ProxyInfo) 
		extends XMPPConnection(serviceName, port, proxyInfo) with Logging
		with MessageProcesser 
	{
		val msgHandlers = new TestMessageHandler(this) :: Nil 

		def this(serviceName: String, port: Int) {
			this(serviceName, port, ProxyInfo.forNoProxy)
		}

		def this(serviceName: String) {
			this(serviceName, 5222, ProxyInfo.forNoProxy)
		}
	}

}
