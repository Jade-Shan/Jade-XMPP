package jadeutils.common

import org.apache.commons.lang.StringUtils.isBlank

import org.slf4j.LoggerFactory
import org.slf4j.Logger

object StrUtils {

	def equalsIgnoreBlank(a: String, b: String): Boolean = {
			a == b || isBlank(a) == isBlank(b)
	}

}

trait Logging {
	lazy val logger = LoggerFactory.getLogger(this.getClass)
}

