package jadeutils.xmpp.utils

import javax.security.auth.callback.CallbackHandler

import scala.collection.immutable.Seq
import scala.collection.mutable.Map

import jadeutils.common.Logging
import jadeutils.xmpp.model.Packet


class SASLAuthentication(val conn: Connection) extends UserAuthentication 
	with Logging
{

	/* list of the name that mechanisms server support */
	private[this] var serverMechanisms: List[String] = Nil
	private[this] var currentMechanism: SASLMechanism = null

	/**
		* Boolean indicating if SASL negotiation has finished and was successful.
		*/
	private[this] var saslNegotiated: Boolean = false

	def authenticated() { saslNegotiated = true }

	/**
		* Boolean indication if SASL authentication has failed. When failed the server may end
		* the connection.
		*/
	private[this] var saslFailed: Boolean = false
	private[this] var resourceBinded: Boolean = false
	private[this] var sessionSupported: Boolean = false

	/**
		* The SASL related error condition if there was one provided by the server.
		*/
	private[this] var errorCondition: String = null

	def setServerMechNameList(names: Seq[String]) {
		this.serverMechanisms = names.toList
	}

	/**
		* Returns true if the server offered ANONYMOUS SASL as a way to authenticate users.
		*
		* @return true if the server offered ANONYMOUS SASL as a way to authenticate users.
		*/
	def hasAnonymousAuthentication() = serverMechanisms contains "ANONYMOUS"

	/**
		* Returns true if the server offered SASL authentication besides ANONYMOUS SASL.
		*
		* @return true if the server offered SASL authentication besides ANONYMOUS SASL.
		*/
	def hasNonAnonymousAuthentication() = !serverMechanisms.isEmpty && 
		(serverMechanisms.size != 1 || !hasAnonymousAuthentication)
	
	/**
		* get default SASL Mechanism
		*/
	private[this] def defaultMechanism = {
		// TODO: find both in selectedMechanism and mechanismPreferences
		//	String selectedMechanism = null;
		//	for (String mechanism : mechanismsPreferences) {
		//		if (implementedMechanisms.containsKey(mechanism)
		//				&& serverMechanisms.contains(mechanism)) {
		//			selectedMechanism = mechanism;
		//			break;
		//		}
		//	}
		//	retrun selectedMechanism
		"DIGEST-MD5"
	}

	@throws(classOf[XMPPException])
	def authenticate(username: String, password: String, resource: String): String = {
		val selectedMechanism = defaultMechanism
		logger.debug("default Sasl Mechainsm is: {}", selectedMechanism)
		if (null != selectedMechanism) {
			val mechanismClass = 
				SASLAuthentication.implementedMechanisms.get(selectedMechanism).get
			logger.debug("default Sasl Mechainsm class is: {}", mechanismClass)
			currentMechanism= mechanismClass.getConstructor(
				classOf[SASLAuthentication]).newInstance(this)
			currentMechanism.authenticate(username, conn.currHost, conn.serviceName,
				password)
		}
		// TODO: unfinished
		""
	}

	@throws(classOf[XMPPException])
	def authenticate(username: String, resource: String, cbh: CallbackHandler): String = {
		// TODO: next stage
		""
	}

	@throws(classOf[XMPPException])
	def authenticateAnonymously(): String = {
		// TODO: next stage
		""
	}

	def challengeReceived(challenge: String) {
		currentMechanism.challengeReceived(challenge)
	}

	def send(stanza: Packet) {
		conn.ioStream.sendPacket(stanza);
	}

}


object SASLAuthentication {

	/* Mechanisms we have implemented by code */
	val implementedMechanisms = 
	Map[String, Class[_ <: SASLMechanism]] (
		"EXTERNAL"  -> classOf[SASLExternalMechanism],
		"GSSAPI"    -> classOf[SASLGSSAPIMechanism],
		"DIGEST-MD5"-> classOf[SASLDigestMD5Mechanism],
		"CRAM-MD5"  -> classOf[SASLCramMD5Mechanism],
		"PLAIN"     -> classOf[SASLPlainMechanism],
		"ANONYMOUS" -> classOf[SASLAnonymous])

	/* Mechanisms preverences */
	var mechanismsPreferences = "GSSAPI" :: "DIGEST-MD5" :: "CRAM-MD5" ::
		"PLAIN" :: "ANONYMOUS" :: Nil;

}

