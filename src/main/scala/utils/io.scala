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



object PacketWriter extends Logging {

}





class PacketReader (val conn: XMPPConnection) extends Actor {

	import PacketReader.MsgStat

	val logger = PacketReader.logger

	var reader: Reader = conn.reader
	var processer: MessageProcesser = new MessageProcesser(conn)

	val buffSize = 8 * 1024
	var keepReading = false

	//var connectionID: String = null
	// 当前消息读取的完整状态
	private[this] var msgStat: MsgStat.Value = MsgStat.INIT
	private[this] var msg: StringBuffer = new StringBuffer


	def init() { 
		this.reader = conn.reader
		processer.start()
	}

	def close() {
		keepReading = false
	}

	def act() {
		logger.debug("PacketReader start ...")
		keepReading = true
		//val sb = new StringBuffer()
		//var resp:(Boolean, String, xml.Node) = (false, null, null)
		var buffer = new Array[Char](buffSize)
		var recSize = 0
		while (keepReading //	&& false == resp._1 
			&& recSize != -1) {
			recSize = reader.read(buffer, 0, buffSize)
			var recStr = (new String(buffer)).substring(0, recSize)
			logger.debug("receive from Server: {}", recStr)
			//sb.append(recStr)
			//isPacketComplete(sb.toString)
		}
		//sb.toString
	}

}



object PacketReader extends Logging {
	object MsgStat extends Enumeration {
		val INIT,  // 等待标签开始
		OPEN,  // 等待标签结束
		XML,  // 等待标签结束
		CLOSE = Value // 标签结束，已经是一条完整的消息
}
}





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



object MessageProcesser extends Logging {

}
