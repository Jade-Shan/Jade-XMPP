package jadeutils.xmpp.utils

// import org.slf4j.LoggerFactory
// import org.slf4j.Logger

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








// abstract class MessageReader extends Actor {
// 	val logger = MessageReader.logger
// 	val reader: Reader
// 	val processer: Actor
// 	private[this] var xmlLoader: XMLLoader = null
// 
// 	var keepReading = true
// 
// 	def init() {
// 		logger.debug("MessageReader init ..."); 
// 		xmlLoader = new XMLLoader(reader)
// 		processer.start() 
// 	}
// 
// 	def close() { keepReading = false }
// 
// 	def act() {
// 		logger.debug("MessageReader start ...")
// 		while (keepReading) processer ! xmlLoader.fillMsg()
// 	}
// 
// }
// 
// object MessageReader  extends Logging { }
// 
// class XMLLoader(val reader: Reader) {
// 	val buffSize = 8 * 1024
// 	private[this] var buffer = new Array[Char](buffSize)
// 	private[this] var start = 0 
// 	private[this] var curr = 0 
// 
// 	import XMLLoader.MsgStat
// 	// 当前消息读取的完整状态
// 	private[this] var status: MsgStat.Value = MsgStat.INIT
// 	private[this] val msg: StringBuffer = new StringBuffer
// 
// 	/* clean message ready for a new stanze */
// 	private[this] def resetMsg() { msg setLength 0; status = MsgStat.INIT }
// 
// 	def fillMsg(): String = {
// 		var len = 0
// 		while (len != -1 && status != MsgStat.CLOSE) {
// 			len = reader.read(buffer, 0, buffSize)
// 			for (i <- 0 until len) { msg.append(buffer(i)) }
// 			checkMsg(msg.toString)
// 		}
// 		msg.toString
// 	}
// 
// 	private[this] def checkMsg(str: String) {
// 		// TODO: check message complate
// 		status = MsgStat.CLOSE
// 	}
// 
// }
// 
// object XMLLoader {
// 	object MsgStat extends Enumeration {
// 		val INIT,  // 等待标签开始
// 		OPEN,  // 等待标签结束
// 		XML,  // 等待标签结束
// 		CLOSE = Value // 标签结束，已经是一条完整的消息
// 	}
// }
