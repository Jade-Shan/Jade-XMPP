package jadeutils.xmpp.model

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicLong

import scala.actors._
import scala.actors.Actor._

import scala.collection.mutable.HashMap
import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.NodeBuffer
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import org.apache.commons.lang.StringUtils.isBlank


import jadeutils.common.Logging
import jadeutils.common.ObjUtils.hashField
import jadeutils.common.StrUtils.encodeBase64
import jadeutils.common.StrUtils.equalsIgnoreBlank
import jadeutils.common.StrUtils.randomNumLetterStr
import jadeutils.common.XMLUtils.newTextAttr
import jadeutils.xmpp.utils.Connection



case class Jid(val local: String, val domain: String, val resource: String) {

	override def equals(that: Any) = that match {
		case that: Jid => {
			equalsIgnoreBlank(this.local, that.local) &&
			equalsIgnoreBlank(this.domain, that.domain) &&
			equalsIgnoreBlank(this.resource, that.resource)
		}
		case _ => false
	}

	/**
	 * Same as equals function, but ignore the 
	 * resource part
	 */
	def isSameUser(that: Any) = that match {
		case that: Jid => {
			equalsIgnoreBlank(this.local, that.local) && 
			equalsIgnoreBlank(this.domain, that.domain)
		}
		case _ => false
	}

	override def hashCode = {
		var n = 41 
		if (null != local   ) n = 41 * (n + local.hashCode)
		if (null != domain  ) n = 41 * (n + domain.hashCode)
		if (null != resource) n = 41 * (n + resource.hashCode)
		n
	}

	override def toString = {
		this match {
			case Jid(l, d, r) if (isBlank(d)) => ""
			case Jid(l, d, r) if (isBlank(l)) => d
			case Jid(l, d, r) if (isBlank(r)) => "%s@%s".format(l, d)
			case Jid(l, d, r) => "%s@%s/%s".format(l, d, r)
		}
	}

	/**
	 * Same as toString function, but ignore the 
	 * resources part
	 */
	def userString = {
		this match {
			case Jid(l, d, r) if (isBlank(d)) => ""
			case Jid(l, d, r) if (isBlank(l)) => d
			case Jid(l, d, r) => "%s@%s".format(l, d)
		}
	}

}



object Jid {

	val jidPattern = ("""(((\w+([-_\.]\w+)*)@)?)(\w+([-_\.]\w+)*)""" +
	"""((/(\w+([-_\.]\w+)*))?)""").r

	def unapply(obj: Any): Option[(String, String, String)] = {
		obj match {
			case Jid(l, d, r) if (isBlank(d)) => None
			case Jid(l, d, r) if (isBlank(l)) => Some((null, d, null))
			case Jid(l, d, r) if (isBlank(r)) => Some((l, d, null))
			case Jid(l, d, r) => Some((l, d, r))
			case _ => None
		}
	}

	def fromString(str: String): Option[Jid] = {
		str match {
			case jidPattern(a,b,lo,c,dom,d,e,f,rec,g) => Some(Jid(lo,dom,rec))
			case _ => None
		}
	}

}





class Roster(conn: Connection) extends Actor with Logging {
	import Roster.Member
	import Roster.Subscription
	import Roster.Presence
	
	val members = new HashMap[String,Member]()

	// TODO: implement Roster


	var keepWritting: Boolean = false
	def init() { keepWritting = true }
	def close() { this ! false }

	def act() {
		logger.debug("Roster start...")
		while (keepWritting) {
			receive {
				case isStop: Boolean => keepWritting = isStop
				case presence: Roster.Presence => updatePresence(presence)
			}
		logger.debug("roster after update: {}", this)
		}
	}

	def updatePresence(presence: Roster.Presence) {
		var member = try {
			members(presence.jid.userString)
		} catch {
			case _ : Throwable => null
		}
		if (null == member) {
			member = new Member(presence.jid.userString, presence.jid.userString,
				null, Subscription.BOTH)
			members.put(member.id, member)
		}
		member.updatePresence(presence)
	}

	override def toString = "{class=Roster, members=%s}".format(members.toString)

}



object Roster {

	object Subscription extends Enumeration {val BOTH, ONLY = Value}

	class Presence(val jid: Jid, var priority: Int, var status: String, 
		var show: String)
	{
		override def toString = ("{class=Roster.Presence, jid=%s, priority=%s, " + 
			"status=%s, show=%s}").format(jid, priority, status, show)
		
	}

	class Member(val id: String, var name: String, var group: String, 
		var subscription: Subscription.Value) 
	{
		private[this] val jids = new HashMap[String, Roster.Presence] ()

		private[this] var priority: Int = 0
		private[this] var status: String = "online"
		private[this] var show: String = "online"

		override def toString = ("{class=Roster.Member, id=%s, name=%s, " + 
			"group=%s, subscription=%s, priority=%s, status=%s, show=%s, " +
			"jids=%s}").format(id, name, group, subscription, priority, status, show,
			jids.toString)

		def presences = jids.toList

		def updatePresence(presence: Roster.Presence) {
			if (presence.priority > this.priority) {
				this.priority = presence.priority
				this.status = presence.status
				this.show = presence.show
			}
			this.jids.put(presence.jid.toString, presence)
		}

	}

}
