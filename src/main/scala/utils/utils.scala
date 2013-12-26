package jadeutils.xmpp.utils

import java.io.Reader

import scala.actors._
import scala.actors.Actor._

import jadeutils.common.Logging

class XMPPException(val msg: String, val cause: Throwable) 
	extends Exception(msg, cause)
{

	def this(msg: String) {
		this(msg, null)
	}

	def this() {
		this("Unknow Exception", null)
	}

}


class ReaderStatusHelper( val reader: Reader, val processer: Actor) {
	val logger = ReaderStatusHelper.logger
	import ReaderStatusHelper.MsgStat 

	// message
	private[this] var status: MsgStat.Value = MsgStat.INIT
	private[this] val msg: StringBuffer = new StringBuffer

	// buffer
	val buffSize = 8 * 1024
	private[this] val buffer = new Array[Char](buffSize)
	private[this] var start = 0 
	private[this] var curr = 0 

	def fillBuff() {
			var len = 0
			while(len != -1 && status != MsgStat.CLOSE) {
				len = reader.read(buffer, 0, buffSize)
				logger.debug("read to buffer")
				for (i <- 0 until len) { msg.append(buffer(i)) }
				checkMsg(msg.toString)
			}
	}

	/* clean message ready for a new stanze */
	private[this] def resetMsg() { msg.setLength(0); status = MsgStat.INIT }

	private[this] def checkMsg(str: String) {
		logger.debug("Start checking msg")
		// TODO: check message complate
		status = MsgStat.CLOSE

		if(status == MsgStat.CLOSE){
			logger.debug("msg complate")
			handleCompleteMsg(str)
		}
	}

	private[this] def handleCompleteMsg(str: String) {
		resetMsg
		processer ! str
	}
}

object ReaderStatusHelper   extends Logging {
	object MsgStat extends Enumeration {
		val INIT,  // 等待标签开始
		OPEN,  // 等待标签结束
		XML,  // 等待标签结束
		CLOSE = Value // 标签结束，已经是一条完整的消息
	}
}




