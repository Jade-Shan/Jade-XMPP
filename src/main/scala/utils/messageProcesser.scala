package jadeutils.xmpp.utils

import scala.actors._
import scala.actors.Actor._
import scala.xml.Elem

import jadeutils.common.Logging

trait MessageProcesser extends Actor with Logging {

	this: XMPPConnection =>

	val msgHandlers: List[MsgHandler]

	def act() {
		logger.debug("MessageProcesser start ...")
		while (true) {
			receive {
				case elem: Elem => {
					logger.debug(" xml elem: {}", elem.toString)
					logger.debug("namespace: {}", elem.namespace)
					logger.debug("   prefix: {}", elem.label )
					logger.debug("    label: {}", elem.prefix)
				}
				case oth => logger.error("unexcept msg: {}", oth)
			}
		}
	}

}


trait MsgHandler {

	def canProcess(elem: Elem): Boolean

	def process(elem: Elem)

	final def handel(elem: Elem) {
		if (canProcess(elem)) {
			process(elem)
		}
	}

}


