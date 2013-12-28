package jadeutils.xmpp.utils

import java.io.Reader
import java.io.Writer
import java.io.InputStream
import java.io.OutputStream

import scala.actors._
import scala.actors.Actor._

import org.apache.commons.lang.StringUtils.isBlank

import jadeutils.common.Logging
import jadeutils.common.StrUtils.isCharBlank

abstract class XMPPInputOutputStream {
	var compressionMethod: String

	def isSupported(): Boolean

	def getInputStream(inputStream: InputStream): InputStream

	def getOutputStream(outputStream: OutputStream): OutputStream
}



class PacketWriter (val conn: XMPPConnection) extends Actor {
	val logger = PacketWriter.logger

	var writer: Writer = conn.writer

	def init() { this.writer = conn.writer }

	def close() { this ! "stop-write" }

	def act() {
		logger.debug("PacketWriter start ...")
		var keepWritting = true
		while (keepWritting) {
			receive {
				case msg: String => {
					try {
						logger.debug("send MSG: " + msg)
						writer.write(msg)
						writer.flush
					} catch {
						case e: Exception => 
						logger.error("Error send data {}, because: {}", 
							msg, e.toString)
					}
				}
			}
		}
	}

}

object PacketWriter extends Logging { }



abstract class MessageReader extends Actor {
	val logger = MessageReader.logger

	val reader: Reader
	val processer: Actor
	var helper: ReaderStatusHelper = null

	var keepReading = false

	def init() { 
		logger.debug("MessageReader init ..."); 
		helper = new ReaderStatusHelper(reader, processer)
		processer.start() 
	}

	def close() { keepReading = false }

	def act() {
		logger.debug("MessageReader start ...")
		keepReading = true
		while (keepReading) { helper fillBuff }
	}

}

object MessageReader extends Logging



class PacketReader (val conn: XMPPConnection) extends MessageReader {
	val reader = conn.reader
	val processer = new MessageProcesser(conn)
}

object PacketReader extends Logging { }



class ReaderStatusHelper( val reader: Reader, val processer: Actor) {
	val logger = ReaderStatusHelper.logger
	val xmlProcessTracer = ReaderStatusHelper.xmlProcessTracer

	val buffSize = 8 * 1024

	// message
	import ReaderStatusHelper.MsgStat 
	private[this] var status: MsgStat.Value = MsgStat.Init
	private[this] val msg = new StringBuffer
	private[this] val errMsg = new StringBuffer

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
	private[this] def resetMsg() {
		msg.setLength(0)
		errMsg.setLength(0)
		label.setLength(0)
		tail.setLength(0)
		status = MsgStat.Init
	}

	private[this] var label =new StringBuffer // "<msg>"
	private[this] var tail =new StringBuffer  // "</msg>"

	/* process msg in buffer */
	private[this] def processBuffer(buffer: Array[Char], len: Int) {
		logger.debug("Start checking msg")
		for (i <- 0 until len) {
			var c = buffer(i)
			if (status == MsgStat.Init) { // """    """
				if (c == '<') {
					status = MsgStat.Start; label.setLength(0)
				} else {
					c = ' '
				}
			} else if (status == MsgStat.Start) { // """<""", """<ab"""
				if (c == '>') {
					status = MsgStat.Open
				} else if (isCharBlank(c)) {
					status = MsgStat.Label
				} else if (c == '/') {
					status = MsgStat.MustClose
				} else {
					label.append(c)
				}
			} else if (status == MsgStat.MustClose) { // """<ab/""", """<ab /""", """<ab id='5'/"""
				if (c == '>') {
					status = MsgStat.Close
				} else {
					status = MsgStat.Label
				}
			} else if (status == MsgStat.Label) { //  """<ab """, """<ab id='5'"""
				if (c == '>') {
					status = MsgStat.Open
				} else if (c == '/') {
					status = MsgStat.MustClose
				}
			} else if (status == MsgStat.Open) { // """<ab>""", """<ab>....."""
				if (c == '<') {
					status = MsgStat.WaitTail
				}
			} else if (status == MsgStat.WaitTail) { // """<ab><""", """<ab>.....<"""
				if (c == '/') {
					status = MsgStat.ReadTail
					tail.setLength(0)
				} else {
					status = MsgStat.Open
				}
			} else if (status == MsgStat.ReadTail) { // """<ab></""", """<ab/></a""", """<ab/></ef"""
				if (c == '>') {
					if (label.toString().trim() == tail.toString().trim()) {
						status = MsgStat.Close
						xmlProcessTracer.trace("""get '>' and match status is : {}""",status )
					} else {
						xmlProcessTracer.trace("""get '>' and not match """)
						status = MsgStat.Open
					}
				} else if (isCharBlank(c)) {
						xmlProcessTracer.trace("""get ' ' tail name finish """)
						status = MsgStat.Tail
				} else {
						xmlProcessTracer.trace("""get char tail name not finish """)
					tail.append(c)
				}
			} else if (status == MsgStat.Tail) { //  """<ab>...</ef """, """<ab/></ef"""
				if (c == '>') {
					if (label.toString().trim() == tail.toString().trim()) {
						xmlProcessTracer.trace("""get '>' and match """)
						status = MsgStat.Close
					} else {
						xmlProcessTracer.trace("""get '>' and not match """)
						status = MsgStat.Open
					}
				}
			} else if (status == MsgStat.Err) {
				errMsg.append(c)
				c = ' '
			}
			msg.append(c) 

			showDebugInfo()

			if (status == MsgStat.Open){
				if (label.toString.trim == "stream:stream") {
					logger.debug("auto close stream")
					status = MsgStat.Close
					msg.append("</stream:stream>")
				}
				if (label.toString.trim == "?xml") {
					logger.debug("auto ignore <?xml>")
					resetMsg()
				}
			}

			if (status == MsgStat.Close) {
				logger.debug("msg complate")
				processer ! msg.toString
				resetMsg()
			} else if (status == MsgStat.Err){
				logger.error("xml error")
				logger.error("   msg: {}", msg.toString)
				logger.error("errMsg: {}", errMsg.toString)
			} 

		}
	}

	private[this] def showDebugInfo() {
		xmlProcessTracer.trace("===========================")
		xmlProcessTracer.trace("   msg: {}", msg.toString)
		xmlProcessTracer.trace("status: {}", status)
		xmlProcessTracer.trace(" label: {}", label.toString)
		xmlProcessTracer.trace("  tail: {}", tail.toString)
		xmlProcessTracer.trace("===========================")
	}
}

object ReaderStatusHelper   extends Logging {
	val xmlProcessTracer = getLoggerByName("xmlProcessTracer")

	object MsgStat extends Enumeration {
		val Init,  // 等待标签开始 ""
		Start,  // XML开始"""<"""
		Label,  // 得到完整标签 """<msg """
		Open,  // 等待标签结束 """<msg ...>""", """<msg>"""
		WaitTail, // """<abc><"""
		ReadTail, // """<abc></""", """<abc></efg""", """<abc></abc"""
		Tail,   // """<abc></abc """, """<abc></dfg """, """<abc></abc"""
		MustClose, // 自关闭标签一定要关的状态 """<msg/""", """<msg /""", """<msg id='5' /"""
		Close, // 标签结束，已经是一条完整的消息 """<msg>.....</msg>""", """<msg/>""", """<msg />""", """<msg id='55' />"""
		Err = Value 
	}
}

