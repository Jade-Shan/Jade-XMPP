package jadeutils.xmpp.model

case class Jid(val local: String, val domain: String, val resource: String) {

	override def toString = {
//		if (null == domain || domain.trim.isEmpty) null
//		else if (null != local && !local.trim.isEmpty)
//			if (null != resource && !resource.trim.isEmpty)
//				"%s@%s/%s".format(local, domain, resource)
//			else
//				"%s@%s".format(local, domain)
//		else domain

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

class Stanzas;

