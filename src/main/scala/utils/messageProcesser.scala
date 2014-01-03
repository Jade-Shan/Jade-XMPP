package jadeutils.xmpp.utils

import scala.actors._
import scala.actors.Actor._
import scala.xml.Elem

import jadeutils.common.Logging

class MessageProcesser(val conn: Connection) extends Actor with Logging {

	def act() {
		logger.debug("MessageProcesser start ...")
		while (true) {
			receive {
				case elem: Elem => {
					logger.debug("xml elem: {}", elem.toString)
				}
				case oth => logger.error("unexcept msg: {}", oth)
			}
		}
	}

}

