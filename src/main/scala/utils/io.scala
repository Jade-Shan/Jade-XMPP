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

	var keepReading = false

	val maxSize = 8 * 1024
	val buffFac = 95 // 95%

	def currBuffSize() = {
		if (Runtime.getRuntime().freeMemory > 1000000L) maxSize
		else 256
	}

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

	def isXMLComplete(str: String): Boolean = false

	private[this] def reading(): String = {
		val sb = new StringBuffer()
		while (!isXMLComplete(sb.toString) && keepReading) {
			for (c <- loadBuffer()) {
				sb.append(c)
			}
		}
		sb.toString
	}

	private[this] def loadBuffer(): Array[Char] = {
		val buffSize = currBuffSize
		val buffSoftLimit = (buffSize * buffFac)
		var buffer = new Array[Char](buffSize)
		for (i <- 0 until buffSize) { // init buffer as empty char
			buffer(0) = ' '
		}

		var offset = 0
		var recSize = 0
		while (recSize > -1 && offset < buffSoftLimit && keepReading) {
			recSize = reader.read(buffer, offset, buffer.length - offset)
			logger.debug("read str from reader {}", buffer)
			offset = offset + recSize
		}
		buffer
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
	}

}



object PacketWriter extends Logging {

}
