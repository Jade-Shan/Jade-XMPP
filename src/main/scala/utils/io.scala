package jadeutils.xmpp.utils

import java.io.Reader
import java.io.Writer
import java.io.InputStream
import java.io.OutputStream

import scala.actors._
import scala.actors.Actor._

abstract class XMPPInputOutputStream {
	var compressionMethod: String

	def isSupported(): Boolean

	def getInputStream(inputStream: InputStream): InputStream

	def getOutputStream(outputStream: OutputStream): OutputStream
}

class PacketReader (val connection: XMPPConnection) extends Actor {

	var connectionID: String = null

	def init() { }

	def close() {}

	def act() {}
}

class PacketWriter (val connection: XMPPConnection) extends Actor {

	def init() { }

	def close() {}

	def act() {}
}
