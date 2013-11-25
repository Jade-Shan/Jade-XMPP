package jadeutils.nds

class HostAddress(val fqdn: String, val port: Int) {

	override def toString = fqdn + ":" + port

	override def hashCode = fqdn.hashCode * 37 + port

	override def equals(that: Any) = that match {
		case that: HostAddress => 
			this.fqdn == that.fqdn && this.port == that.port
		case _ => false
	}
	
}

class SRVRecord (override val fqdn: String, override val port: Int, 
	val priority: Int, val weight: Int) extends HostAddress(fqdn, port)
{
	override def toString = super.toString + " prio:" + priority + 
		" weight:" + weight
}


trait DNSResolver {
	def lookupSRVRecords(hostName: String): List[SRVRecord]
}

import java.util.Hashtable
import javax.naming.directory.Attribute
import javax.naming.directory.Attributes
import javax.naming.directory.DirContext
import javax.naming.directory.InitialDirContext

object JavaxResolver extends DNSResolver {

	val srvRegex= """^(\d+)\s(\d+)\s(\d+)\s(.+[^.])\.?$""".r

	val env = new Hashtable[String, String]()
  env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory")
  val dirContext = new InitialDirContext(env);

	def lookupSRVRecords(hostName: String): List[SRVRecord] = {
		var result: List[SRVRecord] = Nil;
		val dnsLookup = this.dirContext.getAttributes(hostName, Array("SRV"))
		val srvAttribute = dnsLookup.get("SRV")
		val srvRecords = srvAttribute.getAll()
		while (srvRecords.hasMore) {
			/*
			 * rec format is:
			 *   priority weight port host
			 */
			srvRecords.next match {
				case srvRegex(priority, weight, port, host) => {
					result = new SRVRecord(host, port.toInt, priority.toInt, 
						weight.toInt) :: result
				}
				case _ => println("other")
			}
		}
		result
	}

}










