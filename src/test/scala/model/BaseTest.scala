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
		var jid = new Jid("jade", "jade-dungeon.net", "cellphone")
		assert(jid.toString == "jade@jade-dungeon.net/cellphone")

		/*
		 * case as no resource
		 */
		jid = new Jid("jade","jade-dungeon.net", null)
		assert(jid.toString == "jade@jade-dungeon.net")

		/*
		 * case as no local
		 */
		jid = new Jid(null, "jade-dungeon.net", "cellphone")
		assert(jid.toString == "jade-dungeon.net")
		jid = new Jid(null, "jade-dungeon.net", null)
		assert(jid.toString == "jade-dungeon.net")

		/*
		 * case as domain is null
		 */
		jid = new Jid("jade", null, "cellphone")
		assert(jid.toString == null)
		jid = new Jid(null, null, "cellphone")
		assert(jid.toString == null)
		jid = new Jid("jade", null, null)
		assert(jid.toString == null)
		jid = new Jid(null, null, null)
		assert(jid.toString == null)
	}

}
