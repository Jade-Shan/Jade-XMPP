package jadeutils.xmpp.model

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class JidTest extends FunSuite {

	test("Test-JID-equals") {
		assert(Jid("jade", "jade-dungeon.net", "cellphone") == 
			Jid("jade", "jade-dungeon.net", "cellphone"))
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
