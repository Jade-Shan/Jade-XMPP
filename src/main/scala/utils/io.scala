package jadeutils.xmpp.utils

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Reader
import java.io.Writer
import java.io.InputStream
import java.io.InputStreamReader
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter

import java.net.Socket
import java.net.UnknownHostException

import scala.actors._
import scala.actors.Actor._
import scala.xml.XML

import org.apache.commons.lang.StringUtils.isBlank

import jadeutils.common.Logging
import jadeutils.common.StrUtils.isCharBlank
import jadeutils.xmpp.model.Packet




class PacketWriter (val writer: Writer) extends Actor with Logging {

	var keepWritting: Boolean = false

	def init() { keepWritting = true }

	def close() { this ! false }

	def act() {
		logger.debug("PacketWriter start ...")
		while (keepWritting) {
			receive {
				case isStop: Boolean => keepWritting = isStop
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



class PacketReader(val helper: ReaderStatusHelper) extends Actor with Logging {
	
	var keepReading = false

	def init() { 
		logger.debug("MessageReader init ..."); 
		keepReading = true
		helper.startProcesser() 
	}

	def close() { keepReading = false }

	def act() {
		logger.debug("MessageReader start ...")
		while (keepReading) { helper.fillBuff() }
	}

}



class ReaderStatusHelper (val reader: Reader, val processer: Actor) 
	extends Logging
{
	val traceLogger = getLoggerByName("xmlProcessTracer")

	def startProcesser() { processer.start() }

	val buffSize = 8 * 1024

	// message
	import ReaderStatusHelper.MsgStat 
	private[this] var status: MsgStat.Value = MsgStat.Init
	private[this] val msg = new StringBuffer
	private[this] val errMsg = new StringBuffer

	// buffer
	private[this] val buffer = new Array[Char](buffSize)

	/* load from server to buffer */
	def fillBuff() {
		var len = 0
		while(len != -1) {
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
					} else {
						status = MsgStat.Open
					}
				} else if (isCharBlank(c)) {
						status = MsgStat.Tail
				} else {
					tail.append(c)
				}
			} else if (status == MsgStat.Tail) { //  """<ab>...</ef """, """<ab/></ef"""
				if (c == '>') {
					if (label.toString().trim() == tail.toString().trim()) {
						status = MsgStat.Close
					} else {
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
				processer ! XML.loadString(msg.toString)
				resetMsg()
			} else if (status == MsgStat.Err){
				logger.error("xml error")
				logger.error("   msg: {}", msg.toString)
				logger.error("errMsg: {}", errMsg.toString)
			} 

		}
	}

	private[this] def showDebugInfo() {
		traceLogger.trace("===========================")
		traceLogger.trace("   msg: {}", msg.toString)
		traceLogger.trace("status: {}", status)
		traceLogger.trace(" label: {}", label.toString)
		traceLogger.trace("  tail: {}", tail.toString)
		traceLogger.trace("===========================")
	}

}

object ReaderStatusHelper {

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





trait CompressHandler {
	var compressionMethod: String

	def isSupported(): Boolean

	def getInputStream(inputStream: InputStream): InputStream

	def getOutputStream(outputStream: OutputStream): OutputStream
}





class IOStream(val conn: XMPPConnection) extends Logging{

	var connected = false
	var socketClosed = true

	var socket: Socket = null
	var reader: Reader = null
	var writer: Writer = null
	var packetReader: PacketReader = null
	var packetWriter: PacketWriter = null
	var compressHandler: CompressHandler = null

	def initReaderAndWriter() {
		try {
			if (compressHandler == null) {
				logger.debug("try create no compress reader & writer from socket")
				reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), conn.connCfg.charEncoding))
				writer = new BufferedWriter( new OutputStreamWriter(
					socket.getOutputStream(), conn.connCfg.charEncoding))
			} else {
				logger.debug("try create compress reader & writer from socket")
				writer = new BufferedWriter(new OutputStreamWriter(
					compressHandler.getOutputStream(
						socket.getOutputStream()), conn.connCfg.charEncoding));
				reader = new BufferedReader(new InputStreamReader(
					compressHandler.getInputStream(
						socket.getInputStream()), conn.connCfg.charEncoding));
			}
		} catch {
			case e: Exception => 
				logger.error("fail create reader & writer from socket")
		}
		if (null == writer) {
			logger.error("error create writer from socket")
			throw new XMPPException("fail create writer from socket")
		} else if (null == reader) {
			logger.error("error create reader from socket")
			throw new XMPPException("fail create reader from socket")
		} else {
			logger.debug("Success create reader/writer from socket")
		}
	}

	private[this] def initConnection() {
		val isFirstInit = (null == packetWriter) || (null == packetReader)
		initReaderAndWriter()
		try {
			if (isFirstInit) {
				logger.debug("1st time init Connection, create new Reader and Writer")
				packetReader = new PacketReader(new ReaderStatusHelper(reader, conn))
				packetWriter = new PacketWriter(writer)
			}
			packetReader.init
			packetReader.start
			packetWriter.init
			packetWriter.start

			openStream()
			Thread.sleep(10 * 1000)

			connected = true
		} catch {
			case e: Exception => {
				logger.error("fail init Reader Writer")
				/* close reader writer IO Socket */
				if (null != packetReader)
					packetReader.close
				packetReader = null
				if (null != packetWriter)
					packetWriter.close
				packetWriter = null
				if (null != reader) {
					try {
						reader.close
					} catch {
						case t: Throwable => /* do nothing */
					}
				}
				reader = null
				if (null != writer) {
					try {
						writer.close
					} catch {
						case t: Throwable => /* do nothing */
					}
				}
				writer = null
				if (null != socket) {
					try {
						socket.close
					} catch {
						case e: Exception => /* do nothing */
					}
				}
				/* throw exeption */
				throw e
			}
		}
	}

	def connectUsingConfiguration() {
		for (host <- conn.connCfg.hostAddresses) if (null == socket) {
			try {
				if (null == conn.connCfg.socketFactory) {
					logger.debug("No SocketFacoty, create new Socket({}:{})", host.fqdn, conn.port)
					this.socket = new Socket(host.fqdn, conn.port)
				} else {
					logger.debug("get Socket({}:{}) from SocketFactory", host.fqdn, conn.port)
					this.socket = conn.connCfg.socketFactory.createSocket(host.fqdn, conn.port)
				}
				conn.connCfg.currAddress = host
				logger.debug("Success get Socket({}:{})", host.fqdn, conn.port)
			} catch {
				case ex: Exception => {
					ex match {
						case e: UnknownHostException => 
							logger.error("Socket({}:{}) Connect time out", host.fqdn, conn.port)
						case e: IOException => 
							logger.error("Socket({}:{}) Remote Server error", host.fqdn, conn.port)
						case _ => 
							logger.error("Socket({}:{}) unknow error", host.fqdn, conn.port)
					}
					// add this host to bad host list
					conn.connCfg.badHostAddresses == host :: conn.connCfg.badHostAddresses
				}
			}
		}
		if (null == this.socket) {
			throw new XMPPException("None of Address list can create Socket")
		}
		socketClosed = false
		initConnection()
	}


	def openStream() {
		packetWriter ! """<stream:stream version="1.0" xmlns="jabber:client" """ + 
		"""xmlns:stream="http://etherx.jabber.org/streams" to="jabber.org">"""	
	}

	def closeStream() { packetWriter ! """</stream:stream>""" }

	def sendPacket(stanza: Packet) { packetWriter ! stanza.toXML.toString }

}
