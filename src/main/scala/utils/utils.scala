package jadeutils.xmpp.utils


class XMPPException(val msg: String, val cause: Throwable) 
	extends Exception(msg, cause)
{

	def this(msg: String) { this(msg, null) }

	def this() { this("Unknow Exception", null) }

}

