package jadeutils.common

import java.util.Random

import scala.xml.Text

import org.apache.commons.lang.StringUtils.isBlank

import net.iharder.Base64

import org.slf4j.LoggerFactory
import org.slf4j.Logger

object ObjUtils {

	def hashField(field: Any) = if (null == field) 0 else field.hashCode

}

object StrUtils {

	def equalsIgnoreBlank(a: String, b: String): Boolean = {
		a == b || isBlank(a) == isBlank(b)
	}

	/* rand star tools */
	var randGen: Random = new Random();
	val numAndChar = "0123456789abcdefghijklmnopqrstuvwxyz" +
	"ABCDEFGHIJKLMNOPQRSTUVWXYZ"
	val numAndCharMaxIdx = numAndChar.length - 1 
	def randomNumLetterStr(len : Int): String = {
		if (len < 1) null;
		else {
			val buff = new Array[Char](len)
			for (i <- 0 until buff.length)
				buff(i) = numAndChar(randGen.nextInt(numAndCharMaxIdx))
			new String(buff);
		}
	}

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
		lineBreaks: Boolean): String = {
		Base64.encodeBytes(data, offset, len, 
			if (lineBreaks)  Base64.NO_OPTIONS else Base64.DONT_BREAK_LINES)
		}

	/**
		* Encodes a byte array into a bse64 String.
		*
		* @param data The byte arry to encode.
		* @param lineBreaks True if the encoding should contain line breaks and false if it should not.
		* @return A base64 encoded String.
		*/
	def encodeBase64(data: Array[Byte], lineBreaks: Boolean):String = 
	encodeBase64(data, 0, data.length, lineBreaks)

	/**
		* Encodes a byte array into a base64 String.
		*
		* @param data a byte array to encode.
		* @return a base64 encode String.
		*/
	def encodeBase64(data: Array[Byte]):String = encodeBase64(data, false)


	/**
		* Decodes a base64 String.
		* Unlike Base64.decode() this method does not try to detect and decompress a gzip-compressed input.
		*
		* @param data a base64 encoded String to decode.
		* @return the decoded String.
		*/
	def decodeBase64(data: String) = {
		var bytes: Array[Byte] = null
		try {
			bytes = data.getBytes("UTF-8");
		} catch {
			case e: java.io.UnsupportedEncodingException =>
			bytes = data.getBytes();
		}
		Base64.decode(bytes, 0, bytes.length, Base64.NO_OPTIONS);
	}
}



object XMLUtils {

	def newTextAttr(v: Any) = if (null == v) null else Text(v.toString)
}

trait Logging {
	lazy val logger = LoggerFactory.getLogger(this.getClass)
}

