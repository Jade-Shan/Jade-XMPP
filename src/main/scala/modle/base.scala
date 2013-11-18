package jadeutils.xmpp.model

case class Jid(val local: String, val domain: String, val resource: String) {

	override def toString = {
		if (null == domain || domain.trim.isEmpty) null
		else if (null != local && !local.trim.isEmpty)
			if (null != resource && !resource.trim.isEmpty)
				"%s@%s/%s".format(local, domain, resource)
			else
				"%s@%s".format(local, domain)
		else domain
	}

}

class Stanzas;

