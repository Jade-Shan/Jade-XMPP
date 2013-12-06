package jadeutils.common

import java.util.Random

import org.apache.commons.lang.StringUtils.isBlank

import org.slf4j.LoggerFactory
import org.slf4j.Logger

object StrUtils {
	var randGen: Random = new Random();

	val numbersAndLetters = "0123456789abcdefghijklmnopqrstuvwxyz" +
	"ABCDEFGHIJKLMNOPQRSTUVWXYZ"

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

