package jadeutils.xmpp.utils

import java.io.Reader
import java.io.Writer
import java.io.InputStream
import java.io.OutputStream

import scala.actors._
import scala.actors.Actor._

import jadeutils.common.Logging

abstract class XMPPInputOutputStream {
	var compressionMethod: String

	def isSupported(): Boolean

	def getInputStream(inputStream: InputStream): InputStream

	def getOutputStream(outputStream: OutputStream): OutputStream
}



class PacketWriter (val conn: XMPPConnection) extends Actor {
	val logger = PacketWriter.logger

	var writer: Writer = conn.writer

	def init() { this.writer = conn.writer }

	def close() { this ! "stop-write" }

	def act() {
		logger.debug("PacketWriter start ...")
		var keepWritting = true
		while (keepWritting) {
			receive {
				case str: String if "stop-write" == str => keepWritting = false
				case str: String => {
					try {
						writer.write(str)
						writer.flush
					} catch {
						case e: Exception => 
						logger.error("Error receive data {}, because: {}", 
							str, e.toString)
					}
				}
			}
		}
	}

}

object PacketWriter extends Logging { }



abstract class MessageReader extends Actor {

	val logger = MessageReader.logger
	val reader: Reader
	val processer: Actor
	var helper: ReaderStatusHelper = null

	var keepReading = false

	def init() { 
		logger.debug("MessageReader init ..."); 
		helper = new ReaderStatusHelper(reader, processer)
		processer.start() 
	}

	def close() { keepReading = false }

	def act() {
		logger.debug("MessageReader start ...")
		keepReading = true
		while (keepReading) { helper fillBuff }
	}

}

object MessageReader extends Logging



class PacketReader (val conn: XMPPConnection) extends MessageReader {
	val reader = conn.reader
	val processer = new MessageProcesser(conn)
}

object PacketReader extends Logging { }



class MessageProcesser (val conn: XMPPConnection) extends Actor {

	val logger = MessageProcesser.logger

	def act() {
		logger.debug("MessageProcesser start ...")
		while (true) {
			receive {
				case str: String => {
					try {
					} catch {
						case e: Exception => 
						logger.error("Error process data {}, because: {}", 
							str, e.toString)
					}
				}
			}
		}
	}

}

object MessageProcesser extends Logging { }
