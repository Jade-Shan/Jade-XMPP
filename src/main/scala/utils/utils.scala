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

	val buffSize = 8 * 1024

	// message
	import ReaderStatusHelper.MsgStat 
	private[this] var status: MsgStat.Value = MsgStat.Init
	private[this] val msg = new StringBuffer

	// buffer
	private[this] val buffer = new Array[Char](buffSize)
	// private[this] var start = 0 
	// private[this] var curr = 0 

	/* load from server to buffer */
	def fillBuff() {
			var len = 0
			while(len != -1 && status != MsgStat.Close) {
				len = reader.read(buffer, 0, buffSize)
				processBuffer(buffer, len)
			}
	}

	/* clean message ready for a new stanze */
	private[this] def resetMsg() { msg.setLength(0); status = MsgStat.Init }

	private[this] var label =new StringBuffer // "<msg>"
	private[this] var tail =new StringBuffer  // "</msg>"

	/* process msg in buffer */
	private[this] def processBuffer(buffer: Array[Char], len: Int) {
		logger.debug("Start checking msg")
		for (i <- 0 until len) {
			var c = buffer(i)
			// if (status == MsgStat.Init) {
			// 	if (c == '<') {
			// 		msg.setLength(0)
			// 		label.setLength(0)
			// 		status = MsgStat.Start
			// 	} else {
			// 		c = ' '
			// 	}
			// } else if (status == MsgStat.Start) {
			// 	if (c == ' ' || c == '\t') {
			// 		status = MsgStat.Open
			// 	} else {
			// 		label.append(c)
			// 	}
			// } else {
			// 	// TODO: wait label end
			// 	logger.error("Error processing XML: {} + {}", msg.toString, c)
			// }
			msg.append(c) 
		}
		status = MsgStat.Close
		if(status == MsgStat.Close){
			logger.debug("msg complate")
			handleCompleteMsg(msg.toString)
		}
	}

	private[this] def handleCompleteMsg(str: String) {
		resetMsg
		processer ! str
	}
}

object ReaderStatusHelper   extends Logging {
	object MsgStat extends Enumeration {
		val Init,  // 等待标签开始 ""
		Start,  // XML开始"""<"""
		Label,  // 得到完整标签 """<msg """
		Open,  // 等待标签结束 """<msg ...>""", """<msg>"""
		WaitTail, // """<abc><"""
		ReadTail, // """<abc></""", """<abc></efg""", """<abc></abc"""
		Tail, // """<abc></abc>""", """<abc></efg>"""
		MustClose, // 自关闭标签一定要关的状态 """<msg/""", """<msg /""", """<msg id='5' /"""
		Close, // 标签结束，已经是一条完整的消息 """<msg>.....</msg>""", """<msg/>""", """<msg />""", """<msg id='55' />"""
		Err = Value 
	}
}




