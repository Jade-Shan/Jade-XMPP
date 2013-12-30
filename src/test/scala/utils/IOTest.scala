package jadeutils.xmpp.utils

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

import java.io.Reader

import scala.actors._
import scala.actors.Actor._
import scala.xml.Elem

import jadeutils.common.Logging

class MockReader(lines: List[String]) extends Reader {
	val logger = MockReader.logger


	private[this] var lineIdx = 0

	def close() {}

	def read(buffer: Array[Char], idx: Int, len: Int): Int = {
		if (lineIdx < lines.length) {
			logger.debug("read line: {}", lines(lineIdx))
			val la = lines(lineIdx).toCharArray
			for (i <- 0 until la.length) {
				buffer(i) = la(i)
			}
			lineIdx = lineIdx + 1
			la.length
		} else {
			Thread.sleep(10 * 60 * 1000)
			0
		}
	}

}
object MockReader extends Logging

class MockMessageProcesser extends Actor {
	val logger = MockMessageProcesser.logger

	def act() {
		while (true) {
			receive {
				case elem: Elem => {
					logger.debug("xml elem: {}", elem.toString)
				}
				case oth => logger.error("unexcept msg: {}", oth)
			}
		}
	}

}
object MockMessageProcesser extends Logging

@RunWith(classOf[JUnitRunner])
class IOTest extends FunSuite {

	def load(data: List[String]) {
		var pkgReader = new PacketReader(new MockReader(data), new MockMessageProcesser())
		pkgReader.init()
		pkgReader.start()
		Thread.sleep(1 * 1000)
		println("\n\n\n")
	}

	test("Test-Read-XML-Head") {
		load(List( """<?xml version='1.0'?>"""))
		load(List( """<?x""", """ml version='1.0'?>"""))
	}

	test("Test-Slash") {
		load(List( """<stream:stream xmlns:stream='http://etherx.jabber.org/streams' """ +
			"""xmlns='jabber:client' from='jabber.org' id='fbe3166a9974bdc3' """ +
			"""version='1.0'>"""))
	}

	test("Test-Close-XML") {
		load(List( 
			"""<stream:features>""",
			"""<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>""",
			"""<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>""",
			"""<mechanism>SCRAM-SHA-1</mechanism>""",
			"""</mechanisms>""",
			"""</stream:features>""",
			"""<proceed/>"""))
	}

	test("Test-Read-XML-Example") {
		load(List("""<?xml version='1.0'?>""",
			"""<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' from='jabber.org' id='fbe3166a9974bdc3' version='1.0'>""",
			"""<stream:features>""",
			"""<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>""",
			"""<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>""",
			"""<mechanism>SCRAM-SHA-1</mechanism><mechanism>DIGEST-MD5</mechanism><mechanism>CRAM-MD5</mechanism><mechanism>PLAIN</mechanism><mechanism>LOGIN</mechanism>""",
			"""</mechanisms>""",
			"""</stream:features>""",
			"""<proceed xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>"""))
	}

}

