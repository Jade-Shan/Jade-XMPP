package jadeutils.xmpp.model

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicLong

import scala.collection.immutable.HashMap
import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.Node
import scala.xml.NodeBuffer
import scala.xml.Null
import scala.xml.Text
import scala.xml.XML

import jadeutils.common.Logging
import jadeutils.common.ObjUtils.hashField
import jadeutils.common.StrUtils.randomNumLetterStr
import jadeutils.common.StrUtils.encodeBase64
import jadeutils.common.XMLUtils.newTextAttr

/**
	* Represents XMPP presence packets. Every presence packet has a type, which is one of
	* the following values:
	* 
	*      {@link Presence.Type#available available} -- (Default) indicates the user is available to
	*          receive messages.
	*      {@link Presence.Type#unavailable unavailable} -- the user is unavailable to receive messages.
	*      {@link Presence.Type#subscribe subscribe} -- request subscription to recipient's presence.
	*      {@link Presence.Type#subscribed subscribed} -- grant subscription to sender's presence.
	*      {@link Presence.Type#unsubscribe unsubscribe} -- request removal of subscription to
	*          sender's presence.
	*      {@link Presence.Type#unsubscribed unsubscribed} -- grant removal of subscription to
	*          sender's presence.
	*      {@link Presence.Type#error error} -- the presence packet contains an error message.
	* A number of attributes are optional:
	* 
	*      Status -- free-form text describing a user's presence (i.e., gone to lunch).
	*      Priority -- non-negative numerical priority of a sender's resource. The
	*          highest resource priority is the default recipient of packets not addressed
	*          to a particular resource.
	*      Mode -- one of five presence modes: {@link Mode#available available} (the default),
	*          {@link Mode#chat chat}, {@link Mode#away away}, {@link Mode#xa xa} (extended away), and
	*          {@link Mode#dnd dnd} (do not disturb).
	* Presence packets are used for two purposes. First, to notify the server of our
	* the clients current presence status. Second, they are used to subscribe and
	* unsubscribe users from the roster.
	* 
	*@see RosterPacket
	*/
class Presence (val id: String, val language: String, 
	val prsType: Presence.Type.Value, val status: String, 
	val priority: Int, val mode: Presence.Mode.Value,
	private[this] var pktExts: List[PacketExtension]) extends Packet(
	null, null, null, null, null, null, pktExts)
{
    // TODO: priroty  The valid range is -128 through 128.

	//	this(prsType: Presence.Type.Value, status: String, priority: Int, 
	//		mode: Presence.Type.Value, pktExts: List[PacketExtension])
	//	{
	//		this(nextId, null, prsType, status, priority, mode, Nil)
	//	}

	//	this(prsType: Presence.Type.Value)
	//	{
	//		this(null, prsType, null, 0, null, Nil)
	//	}


    /*
     * Returns true if the {@link Type presence type} is available (online) and
     * false if the user is unavailable (offline), or if this is a presence packet
     * involved in a subscription operation. This is a convenience method
     * equivalent to getType() == Presence.Type.available. Note that even
     * when the user is available, their presence mode may be {@link Mode#away away},
     * {@link Mode#xa extended away} or {@link Mode#dnd do not disturb}. Use
     * {@link #isAway()} to determine if the user is away.
     * 
     *  @return true if the presence type is available.
     */
    def isAvailable = prsType == Presence.Type.available

    /*
     * Returns true if the presence type is {@link Type#available available} and the presence
     * mode is {@link Mode#away away}, {@link Mode#xa extended away}, or
     * {@link Mode#dnd do not disturb}. False will be returned when the type or mode
     * is any other value, including when the presence type is unavailable (offline).
     * This is a convenience method equivalent to
     * type == Type.available && (mode == Mode.away || mode == Mode.xa || mode == Mode.dnd).
    *  
    *   @return true if the presence type is available and the presence mode is away, xa, or dnd.
     */
    def isAway = {
        prsType == Presence.Type.available && (mode == Presence.Mode.away || 
					mode == Presence.Mode.xa || 
					mode == Presence.Mode.dnd); 
    }

		override def childElementXML: NodeBuffer = {
			val nb = super.childElementXML
			if (null != status) nb += <status>{status}</status>
			nb += <priority>{
				if (priority > 128) 128
				else if (priority < -128) -128
				else priority
			}</priority>
			if (null != mode) nb += <show>{mode.toString}</show>
			nb
		}

		override def addAttributeToXML(node: Elem): Elem = { 
			super.addAttributeToXML(node) % 
			Attribute(None, "id", newTextAttr(id), 
				Attribute(None, "type", newTextAttr(prsType.toString), Null))
		}

		def nodeXML(childElementXML: NodeBuffer): Elem = <presence>{
			childElementXML}</presence>

}

object Presence {

	/*
	 * A enum to represent the presecence type. Not that presence type is often confused
	 * with presence mode. Generally, if a user is signed into a server, they have a presence
	 * type of {@link #available available}, even if the mode is {@link Mode#away away},
	 * {@link Mode#dnd dnd}, etc. The presence type is only {@link #unavailable unavailable} when
	 * the user is signing out of the server.
	 *
	 * The user is available to receive messages (default).
	 * The user is unavailable to receive messages.
	 * Request subscription to recipient's presence.
	 * Grant subscription to sender's presence.
	 * Request removal of subscription to sender's presence.
	 * Grant removal of subscription to sender's presence.
	 * The presence packet contains an error message.
	 */
	object Type extends Enumeration { 
		val available, unavailable, subscribe, subscribed, unsubscribe, 
		unsubscribed, error = Value
	}

	/**
		* An enum to represent the presence mode.
		* Free to chat.
		* Available (the default).
		* Away.
		* Away for an extended period of time.
		* Do not disturb.
		*/
	object Mode extends Enumeration { 
		val chat, available, away, xa, dnd = Value
	}
}

