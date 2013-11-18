package jadeutils.xmpp.model

import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class JidTest extends FunSuite {

	test("Test-JID-toString") {
		/*
		 * case as full
		 */
		assert(Jid("jade", "jade-dungeon.net", "cellphone").toString == "jade@jade-dungeon.net/cellphone")

		/*
		 * case as no resource
		 */
		assert(Jid("jade","jade-dungeon.net", null).toString == "jade@jade-dungeon.net")
		assert(Jid("jade","jade-dungeon.net", "").toString == "jade@jade-dungeon.net")
		assert(Jid("jade","jade-dungeon.net", "   ").toString == "jade@jade-dungeon.net")

		/*
		 * case as no local
		 */
		assert(Jid(null, "jade-dungeon.net", "cellphone").toString == "jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "cellphone").toString == "jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "cellphone").toString == "jade-dungeon.net")
		assert(Jid(null, "jade-dungeon.net", null).toString == "jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "").toString == "jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "   ").toString == "jade-dungeon.net")
		assert(Jid("", "jade-dungeon.net", "   ").toString == "jade-dungeon.net")
		assert(Jid("   ", "jade-dungeon.net", "").toString == "jade-dungeon.net")

		/*
		 * case as domain is null
		 */
		assert(Jid("jade", null, "cellphone").toString == null)
		assert(Jid("jade", "", "cellphone").toString == null)
		assert(Jid("jade", "   ", "cellphone").toString == null)
		assert(Jid(null, null, "cellphone").toString == null)
		assert(Jid("", "", "cellphone").toString == null)
		assert(Jid("jade", null, null).toString == null)
		assert(Jid("jade", "", "   ").toString == null)
		assert(Jid("jade", "  ", "").toString == null)
		assert(Jid(null, null, null).toString == null)
		assert(Jid("", "", "  ").toString == null)
		assert(Jid("  ", "  ", "").toString == null)
	}

}
