package jadeutils.xmpp.utils

import java.io.Reader
import java.io.Writer
import java.io.InputStream
import java.io.OutputStream

abstract class XMPPInputOutputStream {
	var compressionMethod: String

	def isSupported(): Boolean

	def getInputStream(inputStream: InputStream): InputStream

	def getOutputStream(outputStream: OutputStream): OutputStream
}

