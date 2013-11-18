package jadeutils.xmpp.model

import jadeutils.string.StrUtils

case class Jid(val local: String, val domain: String, val resource: String) {

	override def equals(that: Any) = that match {
		case that: Jid => {
			StrUtils.toEmptyAsNull(this.local) ==
			StrUtils.toEmptyAsNull(that.local) || 
			StrUtils.toEmptyAsNull(this.domain) == 
			StrUtils.toEmptyAsNull(that.domain) || 
			StrUtils.toEmptyAsNull(this.resource) == 
			StrUtils.toEmptyAsNull(that.resource)
		}
		case _ => false
	}

	override def hashCode = {
		var n = 41 
		n = 41 * (n + StrUtils.hashNull(local))
		n = 41 * (n + StrUtils.hashNull(domain))
		n = 41 * (n + StrUtils.hashNull(resource))
		n
	}

	override def toString = {
		this match {
			case Jid(l, d, r) if (null == d || d.trim.isEmpty) =>
				null
			case Jid(l, d, r) if (null == l || l.trim.isEmpty) =>
				d
			case Jid(l, d, r) if (null == r || r.trim.isEmpty) =>
				"%s@%s".format(l, d)
			case Jid(l, d, r) =>
				"%s@%s/%s".format(l, d, r)
		}
	}

}

object Jid {

	def unapply(str: String): Option[Jid] = {
		val jidReg = """(((\w+([-_\.]\w+)*)@)?)(\w+([-_\.]\w+)*)((/(\w+([-_\.]\w+)*))?)""".r
		val jidReg(a,b,lo,c,dom,d,e,f,rec,g) = str
		Some(Jid(lo,dom,rec))
	}

}


class Stanzas;

