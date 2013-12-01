package jadeutils.xmpp.model

import java.util.concurrent.atomic.AtomicLong

import jadeutils.common.StrUtils.randomNumLetterStr

trait PacketExtension {

	val elementName: String

	val namespace: String
}

abstract class Packet { 
	var xmlns: String = Packet.defaultXmlNs;

	var packetId: String = null
	var from: String = null
	var to: String = null

	var packetExtensions: List[PacketExtension] = Nil
	var properties: Map[String, Any] = null
}

object Packet { 
	val defaultLanguage = java.util.Locale.getDefault().getLanguage().toLowerCase();

	var defaultXmlNs: String = null;

	/**
		* Constant used as packetID to indicate that a packet has no id. To indicate that a packet
		* has no id set this constant as the packet's id. When the packet is asked for its id the
		* answer will be <tt>null</tt>.
		*/
	val ID_NOT_AVAILABLE: String = "ID_NOT_AVAILABLE";


	/**
		* Keeps track of the current increment, which is appended to the prefix to
		* forum a unique ID.
		*/
	var id= new AtomicLong(0)

	/**
		* A prefix helps to make sure that ID's are unique across mutliple instances.
		*/
	var prefix: String = randomNumLetterStr(5) + "-"

	/**
		* Returns the next unique id. Each id made up of a short alphanumeric
		* prefix along with a unique numeric value.
		*
		* @return the next id.
		*/
	def nextID() = this.prefix + this.id.getAndIncrement
}


