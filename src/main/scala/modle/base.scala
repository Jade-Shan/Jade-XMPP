package jadeutils.xmpp.model

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicLong

import scala.collection.mutable.HashMap
import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.NodeBuffer
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import org.apache.commons.lang.StringUtils.isBlank

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





class Roster(conn: Connection) {
	import Roster.Member
	
	val members = new HashMap[String,Member]()

	// TODO: implement Roster

	def reload() {
		// TODO: implenent reload Roster
	}
}



object Roster {

	object Subscription extends Enumeration {val BOTH, ONLY = Value}

	class Presence(val jid: Jid, var priority: Int, var status: String, 
		var show: String)
	{
	}

	class Member(val id: String, var name: String, var group: String, 
		var subscription: Subscription.Value) 
	{
		private[this] val jids = new HashMap[String, Presence] ()

		private[this] var priority: Int = 0
		private[this] var status: String = "online"
		private[this] var show: String = "online"

		def presences = jids.toList

		def updatePresence(presence: Presence) {
			if (presence.priority > this.priority) {
				this.priority = presence.priority
				this.status = presence.status
				this.show = presence.show
			}
			this.jids.put(presence.jid.toString, presence)
		}

	}

}
