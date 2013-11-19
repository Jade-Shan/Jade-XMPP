package jadeutils.string

import org.apache.commons.lang.StringUtils._

object StrUtils {

	def equalsIgnoreBlank(a: String, b: String): Boolean = {
			a == b || isBlank(a) == isBlank(b)
	}

}
