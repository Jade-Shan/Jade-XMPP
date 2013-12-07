package jadeutils.common

import java.util.Random

import org.apache.commons.lang.StringUtils.isBlank

import net.iharder.Base64

import org.slf4j.LoggerFactory
import org.slf4j.Logger

object StrUtils {
	var randGen: Random = new Random();

	val numbersAndLetters = "0123456789abcdefghijklmnopqrstuvwxyz" +
	"ABCDEFGHIJKLMNOPQRSTUVWXYZ"

	/**
		* Encodes a byte array into a bse64 String.
		*
		* @param data The byte arry to encode.
		* @param offset the offset of the bytearray to begin encoding at.
		* @param len the length of bytes to encode.
		* @param lineBreaks True if the encoding should contain line breaks and false if it should not.
		* @return A base64 encoded String.
		*/
	def encodeBase64(data: Array[Byte], offset: Int, len: Int, 
		lineBreaks: Boolean)  = 
	{
		Base64.encodeBytes(data, offset, len, 
			if (lineBreaks)  Base64.NO_OPTIONS else Base64.DONT_BREAK_LINES);
	}

	def encodeBase64(buffer: Array[Byte]): Array[Byte] = {
		// TODO: imp base64
		null
	}

	def equalsIgnoreBlank(a: String, b: String): Boolean = {
		a == b || isBlank(a) == isBlank(b)
	}

	def randomNumLetterStr(length: Int): String = {
		if (length < 1) {
			null;
		} else {
			val randBuffer = new Array[Char](length)
			for (i <- 0 until randBuffer.length) {
				randBuffer(i) = this.numbersAndLetters(randGen.nextInt(61))
			}
			new String(randBuffer);
		}
	}

}

trait Logging {
	lazy val logger = LoggerFactory.getLogger(this.getClass)
}

