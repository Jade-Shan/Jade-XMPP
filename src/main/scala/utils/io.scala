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





class PacketReader (val conn: XMPPConnection) extends Actor {

	val logger = PacketReader.logger

	val buffSize = 8 * 1024

	var keepReading = false

	var connectionID: String = null

	var reader: Reader = conn.reader

	def init() { 
		this.reader = conn.reader
	}

	def close() {
		keepReading = false
	}

	def act() {
		logger.debug("PacketReader start ...")
		keepReading = true
		reading()
	}

	def isSpecFlag(str: String): Boolean = {
		false
	}

	def isPacketComplete(str: String): (Boolean, String, xml.Node) = {
		if (isSpecFlag(str))
			(true, str, null)
		else
			try {
				val node = xml.XML.loadString(str)
				(true, str, node)
			} catch {
				case e: Exception => (false, str, null)
			}
	}

	private[this] def reading(): String = {
		val sb = new StringBuffer()
		var resp:(Boolean, String, xml.Node) = (false, null, null)
		var buffer = new Array[Char](buffSize)
		var recSize = 0
		while (keepReading && false == resp._1 && recSize != -1) {
			recSize = reader.read(buffer, 0, buffSize)
			var recStr = (new String(buffer)).substring(0, recSize)
			logger.debug("receive from Server: {}", recStr)
			sb.append(recStr)
			isPacketComplete(sb.toString)
		}
		sb.toString
	}

}



object PacketReader extends Logging {

}






class PacketWriter (val conn: XMPPConnection) extends Actor {

	val logger = PacketWriter.logger

	var writer: Writer = conn.writer

	def init() {
		this.writer = conn.writer
	}

	def close() {}

	def act() {
		logger.debug("PacketWriter start ...")

		receive {
			case str: String => {
				try {
					writer.write(str)
					writer.flush
				} catch {
					case e: Exception => 
						logger.error("Error writting data {}, \n because: {}", 
							str, e.toString)
				}
			}
			case node: xml.Node => {
				try {
					writer.write(node.toString)
					writer.flush
				} catch {
					case e: Exception => 
						logger.error("Error writting data {}, \n because: {}", 
							node.toString, e.toString)
				}
			}
		}
	}

}



object PacketWriter extends Logging {

}
