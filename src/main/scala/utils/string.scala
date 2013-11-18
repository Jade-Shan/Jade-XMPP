package jadeutils.string

object StrUtils {

	def toEmptyAsNull(str: String): String = {
		if (str == null  || str.trim.isEmpty) null else str
	}

	def hashNull(str: String): Int = {
		if (str == null) 0 else str.hashCode
	}

}
