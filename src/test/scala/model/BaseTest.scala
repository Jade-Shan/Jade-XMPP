package jadeutils.xmpp.model

import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeBuffer
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith


import jadeutils.common.Logging
import jadeutils.xmpp.utils.Connection
import jadeutils.xmpp.utils.ProxyInfo

@RunWith(classOf[JUnitRunner])
class JidTest extends FunSuite {

	test("Test-JID-equals") {
		assert(Jid("jade", "jade-dungeon.net", "cellphone") == 
			Jid("jade", "jade-dungeon.net", "cellphone"))
		assert(Jid("jade", "jade-dungeon.net", "cellphone") != 
			Jid("jade", "jade-dungeon.net", ""))
	}

	test("Test-JID-sameUser") {
		assert(Jid("jade", "jade-dungeon.net", "cellphone")  isSameUser
			Jid("jade", "jade-dungeon.net", "cellphone"))
		assert(Jid("jade", "jade-dungeon.net", "cccccc")  isSameUser
			Jid("jade", "jade-dungeon.net", "cellphone"))
		assert(Jid("jade", "jade-dungeon.net", null)  isSameUser
			Jid("jade", "jade-dungeon.net", "cellphone"))
		assert(Jid("jade", "jade-dungeon.net", "cccccc")  isSameUser
			Jid("jade", "jade-dungeon.net", null))

		assert(!(Jid("jade", "dungeon.net", "cellphone")  isSameUser
			Jid("jade", "jade-dungeon.net", "cellphone")))
		assert(!(Jid("jade", "jade-dugeon.net", "cccccc")  isSameUser
			Jid("jade", "jade-dungeon.net", "cellphone")))
		assert(!(Jid("jade", "jade-dugeon.net", null)  isSameUser
			Jid("jade", "jade-dungeon.net", "cellphone")))
		assert(!(Jid("jade", "jade-dugeon.net", "cccccc")  isSameUser
			Jid("jade", "jade-dungeon.net", null)))
	}

	test("Test-JID-hashCode") {
		assert(Jid("jade", "jade-dungeon.net", "cellphone").hashCode == 
			Jid("jade", "jade-dungeon.net", "cellphone").hashCode)
		assert(Jid("jade", "jade-dungeon.net", "cellphone").hashCode != 
			Jid("jad3", "jade-dungeon.net", "cellphone").hashCode)
	}

	test("Test-JID-toString") {
		/*
		 * case as full
		 */
		assert(Jid("jade", "jade-dungeon.net", "cellphone").toString == 
			"jade@jade-dungeon.net/cellphone")

		/*
		 * case as no resource
		 */
		assert(Jid("jade","jade-dungeon.net", null).toString == 
			"jade@jade-dungeon.net")
		assert(Jid("jade","jade-dungeon.net", "").toString == 
			"jade@jade-dungeon.net")
		assert(Jid("jade","jade-dungeon.net", "   ").toString == 
			"jade@jade-dungeon.net")

		/*
		 * case as no local
		 */
		assert(Jid(null, "jade-dungeon.net", "cellphone").toString == 
			"jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "cellphone").toString == 
			"jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "cellphone").toString == 
			"jade-dungeon.net")
		assert(Jid(null, "jade-dungeon.net", null).toString == "jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "").toString == "jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "   ").toString == "jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "   ").toString == "jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "").toString == "jade-dungeon.net")

		/*
		 * case as domain is null
		 */
		assert(Jid("jade", null, "cellphone").toString == "")
		assert(Jid("jade", "", "cellphone").toString == "")
		assert(Jid("jade", "   ", "cellphone").toString == "")
		assert(Jid(null, null, "cellphone").toString == "")
		assert(Jid("", "", "cellphone").toString == "")
		assert(Jid("jade", null, null).toString == "")
		assert(Jid("jade", "", "   ").toString == "")
		assert(Jid("jade", "  ", "").toString == "")
		assert(Jid(null, null, null).toString == "")
		assert(Jid("", "", "  ").toString == "")
		assert(Jid("  ", "  ", "").toString == "")
	}

	test("Test-JID-userString") {
		println(Jid("aaa", "bb.com", "cellphone").userString)
		println(Jid("aaa", "bb.com", "").userString)
		println(Jid("aaa", "bb.com", null).userString)
	}

	test("Test-JID-fromString") {
		var c = Jid.fromString("aa@bb.com/cc")
		assert(c.toString == "Some(aa@bb.com/cc)")
		c = Jid.fromString("aa@bb.com")
		assert(c.toString == "Some(aa@bb.com)")
		c = Jid.fromString("aa@bb.com")
		assert(c.toString == "Some(aa@bb.com)")
		c = Jid.fromString("bb.com")
		assert(c.toString == "Some(bb.com)")
		c = Jid.fromString("!!!!!!!!!")
		assert(c.toString == "None")
	}

	test("Test-JID-apply") {
		var c = Jid("jade", "jade-dungeon.net", "cellphone")
		assert(c.toString == "jade@jade-dungeon.net/cellphone")
		c = Jid(null, "jade-dungeon.net", "cellphone")
		assert(c.toString == "jade-dungeon.net")
		c = Jid("jade", null, "cellphone")
		assert(c.toString == "")
		c = Jid("jade", "jade-dungeon.net", null)
		assert(c.toString == "jade@jade-dungeon.net")
		c = Jid(null, null, "cellphone")
		assert(c.toString == "")
		c = Jid(null, "jade-dungeon.net", null)
		assert(c.toString == "jade-dungeon.net")
		c = Jid("jade", null, null)
		assert(c.toString == "")
		c = Jid(null, null, null)
		assert(c.toString == "")
	}

	test("Test-JID-unapply") {
		var c = Jid("jade", "jade-dungeon.net", "cellphone")
		var d = Jid.unapply(c)
		assert(d.toString == "Some((jade,jade-dungeon.net,cellphone))")
		c = Jid(null, "jade-dungeon.net", "cellphone")
		d = Jid.unapply(c)
		assert(d.toString == "Some((null,jade-dungeon.net,cellphone))")
		c = Jid("jade", null, "cellphone")
		d = Jid.unapply(c)
		assert(d.toString == "Some((jade,null,cellphone))")
		c = Jid("jade", "jade-dungeon.net", null)
		d = Jid.unapply(c)
		assert(d.toString == "Some((jade,jade-dungeon.net,null))")
		c = Jid(null, null, "cellphone")
		d = Jid.unapply(c)
		assert(d.toString == "Some((null,null,cellphone))")
		c = Jid(null, "jade-dungeon.net", null)
		d = Jid.unapply(c)
		assert(d.toString == "Some((null,jade-dungeon.net,null))")
		c = Jid("jade", null, null)
		d = Jid.unapply(c)
		assert(d.toString == "Some((jade,null,null))")
		c = Jid(null, null, null)
		d = Jid.unapply(c)
		assert(d.toString == "Some((null,null,null))")
	}

}

@RunWith(classOf[JUnitRunner])
class RosterTest extends FunSuite {
	import RosterTest.MockConnection

	val conn = new MockConnection("jabber.org", 5222, ProxyInfo.forNoProxy)
	val roster = new Roster(conn)
	roster.init
	roster.start

	test("Test-create-by-presence") {
		val elem = <presence id="99jn5-513" to="jade-shan@jabber.org" 
			from="evokeralucard@gmail.com/androidcHg66345792"><status/><priority>0</priority>
			<c ver="xYEd+1ZdePfGl3AaJ23FB7rizRg=" node="http://www.igniterealtime.org/projects/smack/" 
			hash="sha-1" xmlns="http://jabber.org/protocol/caps"/><x xmlns="vcard-temp:x:update"><photo>fe309c077ae79f9c75d24673295fe2b36c74b47c</photo></x></presence>

		val jid: Jid = try {
			Jid.fromString((elem \ "@from").toString).getOrElse(null)
		} catch {
			case _ : Throwable => null
		}
		val priority: Int = try {
			Integer.parseInt((elem \ "priority").text.toString)
		} catch {
			case _ : Throwable => 5
		}
		val status: String = try {
			(elem \ "status").text.toString
		} catch {
			case _ : Throwable => null
		}
		val show: String = try {
			(elem \ "show").text.toString
		} catch {
			case _ : Throwable => null
		}

		val presence = new Roster.Presence(jid, priority, status, show)
		assert(Jid("evokeralucard", "gmail.com", "androidcHg66345792") == presence.jid)
		assert(0 == presence.priority)
		assert("" == presence.status)
		assert("" == presence.show)
	}

	test("Test-Process-Present") {
		val presence = new Roster.Presence(
			Jid("evokeralucard", "gmail.com", "androidcHg66345792"), 0, "", "")
		roster ! presence
		Thread.sleep(10 * 1000)
	}

}

object RosterTest {

	class MockConnection(override val serviceName: String, override val port: Int, 
		override val proxyInfo: ProxyInfo) 
		extends Connection(serviceName, port, proxyInfo) with Logging
	{
	}

}



