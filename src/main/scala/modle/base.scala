package jadeutils.xmpp.model

class Jid(val local: String, val domain: String, val resource: String) {

	override def toString = {
		if (null == domain || domain.isEmpty) null
		else if (null != local && !local.isEmpty)
			if (null != resource && !resource.isEmpty)
				"%s@%s/%s".format(local, domain, resource)
			else
				"%s@%s".format(local, domain)
		else domain
	}

}

class Stanzas;

