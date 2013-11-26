package jadeutils.xmpp.service

import scala.collection.mutable.HashMap

import jadeutils.nds._

object DNSService {

	val clientPrefix = "_xmpp-client._tcp"
	val serverPrefix = "_xmpp-server._tcp"

	val cache = new HashMap[String, List[HostAddress]]()

	def resolveXmppClientDomain(domain: String): List[HostAddress] = {
		val addressList = new HostAddress(domain, 5222) ::
			JavaxResolver.lookupSRVRecords(clientPrefix + "." + domain)
		this.cache.put("client-" + domain, addressList)
		addressList
	}

}






