package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import java.io.Reader

import scala.actors._
import scala.actors.Actor._

import jadeutils.common.Logging

class MockReader(lines: List[String]) extends Reader {
	private[this] var lineIdx = 0

	def close() {}

	def read(buffer: Array[Char], idx: Int, len: Int): Int = {
		if (lineIdx < lines.length) {
			val la = lines(lineIdx).toCharArray
			for (i <- 0 until la.length) {
				buffer(i) = la(i)
			}
			lineIdx = lineIdx + 1
			la.length
		} else {
			Thread.sleep(30 * 1000)
			0
		}
	}

}

class MockMessageProcesser extends Actor {
	val logger = MockMessageProcesser.logger

	def act() {
		while (true) {
			receive {
				case str: String => logger.debug("receive: {}", str)
			}
		}
	}

}
object MockMessageProcesser extends Logging {}

class MockPacketReader(pReader: MockReader) extends MessageReader {
	val reader = pReader
	val processer = new MockMessageProcesser
}

@RunWith(classOf[JUnitRunner])
class IOTest extends FunSuite {

	val lines = List("""<?xml version='1.0'?>""",
	"""<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' from='jabber.org' id='fbe3166a9974bdc3' version='1.0'>""",
	"""<stream:features>""",
	"""<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>""",
	"""<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>""",
	"""<mechanism>SCRAM-SHA-1</mechanism><mechanism>DIGEST-MD5</mechanism><mechanism>CRAM-MD5</mechanism><mechanism>PLAIN</mechanism><mechanism>LOGIN</mechanism>""",
	"""</mechanisms>""",
	"""</stream:features>""",
	"""<proceed xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>""");

	test("Test-Read-XML-Head") {
		var data = List( """<?xml version='1.0'?>""")
		var mr = new MockPacketReader(new MockReader(data))
		mr.init()
		mr.start()
		Thread.sleep(10 * 1000)
		println("\n\n\n")

		data = List( """<?xml version='1.0'?>""")
		mr = new MockPacketReader(new MockReader(data))
		mr.init()
		mr.start()
		Thread.sleep(10 * 1000)
		println("\n\n\n")
	}

}

