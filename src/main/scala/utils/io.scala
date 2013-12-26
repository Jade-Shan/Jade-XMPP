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

	def init() { logger.debug("MessageReader init ..."); processer.start() }

	def close() { keepReading = false }

	def act() {
		logger.debug("MessageReader start ...")
		keepReading = true
		while (keepReading){
			var len = 0
			while(len != -1 && status != MsgStat.CLOSE) {
				len = reader.read(buffer, 0, buffSize)
				logger.debug("read to buffer")
				for (i <- 0 until len) { msg.append(buffer(i)) }
				checkMsg(msg.toString)
			}
			//logger.debug("get msg: {}", msg.toString)
			// processer ! msg.toString 
		}
	}

	// message
	private[this] var status: MsgStat.Value = MsgStat.INIT
	private[this] val msg: StringBuffer = new StringBuffer

	/* clean message ready for a new stanze */
	private[this] def resetMsg() { msg.setLength(0); status = MsgStat.INIT }

//	private[this] def fillMsg(): String = {
//		var len = 0
//		while (len != -1 && status != MsgStat.CLOSE) {
//			len = reader.read(buffer, 0, buffSize)
//			for (i <- 0 until len) { msg.append(buffer(i)) }
//			checkMsg(msg.toString)
//		}
//		msg.toString
//	}

	// buffer
	val buffSize = 8 * 1024
	private[this] val buffer = new Array[Char](buffSize)
	private[this] var start = 0 
	private[this] var curr = 0 

	private[this] def checkMsg(str: String) {
		logger.debug("Start checking msg")
		// TODO: check message complate
		status = MsgStat.CLOSE

		if(status == MsgStat.CLOSE){
			logger.debug("msg complate")
			handleCompleteMsg(str)
		}
	}

	private[this] def handleCompleteMsg(str: String) {
		resetMsg
		processer ! str
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
