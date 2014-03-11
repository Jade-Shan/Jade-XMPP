package jadeutils.xmpp.utils

import javax.security.auth.callback.CallbackHandler

import scala.collection.mutable.Map

import jadeutils.common.Logging
import jadeutils.xmpp.model.Packet


/**
	* <p>This class is responsible authenticating the user using SASL, binding the resource
	* to the connection and establishing a session with the server.</p>
	*
	* <p>Once TLS has been negotiated (i.e. the connection has been secured) it is possible to
	* register with the server, authenticate using Non-SASL or authenticate using SASL. If the
	* server supports SASL then Smack will first try to authenticate using SASL. But if that
	* fails then Non-SASL will be tried.</p>
	*
	* <p>The server may support many SASL mechanisms to use for authenticating. Out of the box
	* Smack provides several SASL mechanisms, but it is possible to register new SASL Mechanisms. Use
	* {@link #registerSASLMechanism(String, Class)} to register a new mechanisms. A registered
	* mechanism wont be used until {@link #supportSASLMechanism(String, int)} is called. By default,
	* the list of supported SASL mechanisms is determined from the {@link SmackConfiguration}. </p>
	*
	* <p>Once the user has been authenticated with SASL, it is necessary to bind a resource for
	* the connection. If no resource is passed in {@link #authenticate(String, String, String)}
	* then the server will assign a resource for the connection. In case a resource is passed
	* then the server will receive the desired resource but may assign a modified resource for
	* the connection.</p>
	*
	* <p>Once a resource has been binded and if the server supports sessions then Smack will establish
	* a session so that instant messaging and presence functionalities may be used.</p>
	*
	*/
class SASLAuthentication(val ioStream: IOStream) extends UserAuthentication {

	/* list of the name that mechanisms server support */
	private[this] var serverMechanisms: List[String] = Nil
	private[this] var currentMechanism: SASLMechanism = null

	/**
		* Boolean indicating if SASL negotiation has finished and was successful.
		*/
	private[this] var saslNegotiated: Boolean = false

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

	def setServerMechNameList(names: List[String]) {
		this.serverMechanisms = names
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

	@throws(classOf[XMPPException])
	def authenticate(username: String, resource: String, cbh: CallbackHandler): String = {
		// TODO: unfinished
		""
	}

	@throws(classOf[XMPPException])
	def authenticate(username: String, password: String, resource: String): String = {
		// TODO: unfinished
		""
	}

	@throws(classOf[XMPPException])
	def authenticateAnonymously(): String = {
		// TODO: unfinished
		""
	}

	def send(stanza: Packet) {
		ioStream.sendPacket(stanza);
	}

}


object SASLAuthentication {

	/* implementedMechanisms */
	val implementedMechanisms = 
	Map[String, Class[_ <: SASLMechanism]] (
		"EXTERNAL"  -> classOf[SASLExternalMechanism],
		"GSSAPI"    -> classOf[SASLGSSAPIMechanism],
		"DIGEST-MD5"-> classOf[SASLDigestMD5Mechanism],
		"CRAM-MD5"  -> classOf[SASLCramMD5Mechanism],
		"PLAIN"     -> classOf[SASLPlainMechanism],
		"ANONYMOUS" -> classOf[SASLAnonymous])

	var mechanismsPreferences = "GSSAPI" :: "DIGEST-MD5" :: "CRAM-MD5" ::
	"PLAIN" :: "ANONYMOUS" :: Nil;

}

