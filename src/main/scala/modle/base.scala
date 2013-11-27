package jadeutils.xmpp.model

import jadeutils.string.StrUtils._
import org.apache.commons.lang.StringUtils._

case class Jid(val local: String, val domain: String, val resource: String) {

	override def equals(that: Any) = that match {
		case that: Jid => {
			equalsIgnoreBlank(this.local, that.local)
			equalsIgnoreBlank(this.domain, that.domain)
			equalsIgnoreBlank(this.resource, that.resource)
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

}

object Jid {

	val jidPattern = """(((\w+([-_\.]\w+)*)@)?)(\w+([-_\.]\w+)*)((/(\w+([-_\.]\w+)*))?)""".r

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

