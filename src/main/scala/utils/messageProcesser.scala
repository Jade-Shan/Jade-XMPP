package jadeutils.xmpp.utils

import scala.actors._
import scala.actors.Actor._
import scala.xml.Elem

import jadeutils.common.Logging

trait MessageProcesser extends Logging {

	val msgHandlers: List[MsgHandler]

	def foreachHandler(elem: Elem) {
		logger.debug(" xml elem: {}", elem.toString)
		logger.debug("namespace: {}", elem.namespace)
		logger.debug("   prefix: {}", elem.prefix)
		logger.debug("    label: {}", elem.label)
		msgHandlers.foreach((handler) => handler.handle(elem))
	}

}


trait MsgHandler {

	def canProcess(elem: Elem): Boolean

	def process(elem: Elem)

	final def handle(elem: Elem) {
		if (canProcess(elem)) {
			process(elem)
		}
	}

}


