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

	def init() {
		this.writer = conn.writer
	}

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
	import MessageReader.MsgStat

	val logger = MessageReader.logger
	val reader: Reader
	val processer: Actor

	var keepReading = false

	// buffer
	val buffSize = 8 * 1024
	private[this] val buffer = new Array[Char](buffSize)
	private[this] var start = 0 
	private[this] var curr = 0 

	// message
	private[this] var status: MsgStat.Value = MsgStat.INIT
	private[this] val msg: StringBuffer = new StringBuffer

	def init() { logger.debug("MessageReader init ..."); processer.start() }

	def close() { keepReading = false }

	/* clean message ready for a new stanze */
	private[this] def resetMsg() { msg setLength 0; status = MsgStat.INIT }

	def act() {
		logger.debug("MessageReader start ...")
		keepReading = true
		var recSize = 0
		while (keepReading && recSize != -1) {
			recSize = reader.read(buffer, 0, buffSize)
			var recStr = (new String(buffer)).substring(0, recSize)
			processer ! recStr
		}
	}

}

object MessageReader  extends Logging {
	object MsgStat extends Enumeration {
		val INIT,  // 等待标签开始
		OPEN,  // 等待标签结束
		XML,  // 等待标签结束
		CLOSE = Value // 标签结束，已经是一条完整的消息
	}
}



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
