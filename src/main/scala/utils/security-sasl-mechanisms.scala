package jadeutils.xmpp.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicLong
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.UnsupportedCallbackException
import javax.security.auth.callback.Callback
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import javax.security.sasl.RealmCallback
import javax.security.sasl.RealmChoiceCallback
import javax.security.sasl.Sasl
import javax.security.sasl.SaslClient
import javax.security.sasl.SaslException

import scala.collection.mutable.Map
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
import jadeutils.common.StrUtils.decodeBase64
import jadeutils.common.XMLUtils.newTextAttr

import jadeutils.xmpp.model.Packet

abstract class SASLMechanism(val saslAuthentication: SASLAuthentication) 
	extends CallbackHandler with Logging
{
	var name: String        // mechanism name
	var sc: SaslClient = null

	var authenticationId: String = null
	var host: String = null
	var serviceName: String = null
	var password: String = null

	@throws(classOf[IOException])
	@throws(classOf[XMPPException])
	def authenticate(cbh: CallbackHandler) {
		val props = new java.util.HashMap[String, String]()
		sc = Sasl.createSaslClient(Array(this.name), authenticationId, "xmpp", 
			host, props, cbh)
		authenticate()
	}

	@throws(classOf[IOException])
	@throws(classOf[XMPPException])
	def authenticate(username: String, hstName: String, servName: String, 
		pwd: String) 
	{
		logger.debug("authenticate with:")
		logger.debug("User: {}", username)
		logger.debug("host: {}", hstName)
		logger.debug("serv: {}", servName)
		logger.debug("pwd:  {}", pwd)
		authenticationId = username
		host = hstName
		serviceName = servName
		password = pwd

		sc = Sasl.createSaslClient(Array(name), username, "xmpp", servName,
			new java.util.HashMap[String, String](), this)
		authenticate()
	}

	@throws(classOf[IOException])
	@throws(classOf[XMPPException])
	def authenticate() {
		var authenticationText: String = null
		try {
			if(sc.hasInitialResponse()) {
				val response: Array[Byte] = sc.evaluateChallenge(new Array[Byte](0))
				authenticationText = encodeBase64(response, false)
				logger.debug("Auth has init Resp, text is: {}", authenticationText)
			}
		} catch {
			case e: SaslException => throw new XMPPException(
				"SASL authentication failed", e)
		}

		// Send the authentication to the server
		logger.debug("Send SASL auth info, mechanism is: {}", this.name)
		this.saslAuthentication.send(new SASLMechanism.AuthMechanism(this.name, 
			authenticationText))
	}

	@throws(classOf[IOException])
	def challengeReceived(challenge: String) {
		var response: Array[Byte] = null
		if(challenge != null) {
			logger.debug("text is: {}", new String(decodeBase64(challenge)))
			response = sc.evaluateChallenge(decodeBase64(challenge))
		} else {
			response = sc.evaluateChallenge(new Array[Byte](0))
		}

		var responseStanza: Packet = null
		if (response == null) {
			responseStanza = new SASLMechanism.Response()
		}
		else {
			logger.debug("resp is: {}", new String(response))
			logger.debug("resp base64: {}", new String(encodeBase64(response)))
			responseStanza = new SASLMechanism.Response(encodeBase64(response, false))
		}

		// Send the authentication to the server
		this.saslAuthentication.send(responseStanza);
	}


	@throws(classOf[IOException])
	@throws(classOf[UnsupportedCallbackException ])
	def handle(callbacks: Array[Callback]) {
		for (callback <- callbacks) {
			callback match {
				case c: NameCallback => c.setName(authenticationId)
				case c: PasswordCallback => c.setPassword(password.toCharArray())
				case c: RealmCallback => {
					c.setText(c.getDefaultText())
				}
				case c: RealmChoiceCallback => // do nothing
				case _ => throw new UnsupportedCallbackException(callback)
			}
		}
	}

}

object SASLMechanism extends Logging {

	/**
		* Initiating SASL authentication by select a mechanism.
		*/
	class AuthMechanism(val name: String, val authenticationText: String) 
		extends Packet( "urn:ietf:params:xml:ns:xmpp-sasl", null, null, null, null,
		null, null)
	{

		override def addAttributeToXML(node: Elem): Elem = {
			super.addAttributeToXML(node) % Attribute(None, "mechanism", 
				newTextAttr(name), Null)
		}

		def nodeXML(childElementXML: NodeBuffer): Elem = <auth>{authenticationText}</auth> 

	}

	/**
		* A SASL challenge stanza.
		*/
	class Challenge(val data: String) extends Packet( 
		"urn:ietf:params:xml:ns:xmpp-sasl", null, null, null, null, null, null)
	{
		def nodeXML(childElementXML: NodeBuffer): Elem = <challenge >{data}</challenge > 
	}

	/**
		* A SASL response stanza.
		*/
	class Response(val authenticationText: String) extends Packet(
		"urn:ietf:params:xml:ns:xmpp-sasl", null, null, null, null, null, null)
	{
		def this() {
			this(null)
		}

		def nodeXML(childElementXML: NodeBuffer): Elem = <response>{authenticationText}</response> 
	}

	/**
		* A SASL success stanza.
		*/
	class Success(val data: String) extends Packet(
		"urn:ietf:params:xml:ns:xmpp-sasl", null, null, null, null, null, null)
	{
		def nodeXML(childElementXML: NodeBuffer): Elem = <success>{data}</success> 
	}


	/**
		* A SASL failure stanza.
		*/
	class Failure(val condition: String) extends Packet(
		"urn:ietf:params:xml:ns:xmpp-sasl", null, null, null, null, null, null)
	{
		def nodeXML(childElementXML: NodeBuffer): Elem = <failure>{condition}</failure>
	}

}




/**
 * Implementation of the SASL PLAIN mechanism
 */
class SASLPlainMechanism (override val saslAuthentication: SASLAuthentication) 
	extends SASLMechanism(saslAuthentication)
{
	var name: String =    "PLAIN";
}


/**
 * Implementation of the SASL CRAM-MD5 mechanism
 */
class SASLCramMD5Mechanism (override val saslAuthentication: SASLAuthentication) 
	extends SASLMechanism(saslAuthentication)
{
	var name: String = "CRAM-MD5"
}



/**
 * Implementation of the SASL DIGEST-MD5 mechanism
 */
class SASLDigestMD5Mechanism (
	override val saslAuthentication: SASLAuthentication) 
	extends SASLMechanism(saslAuthentication)
{
	var name: String = "DIGEST-MD5"
}



/**
 * Implementation of the SASL EXTERNAL mechanism.
 */
class SASLExternalMechanism (
	override val saslAuthentication: SASLAuthentication) 
	extends SASLMechanism(saslAuthentication)
{
	var name: String = "EXTERNAL";
}



/**
 * Implementation of the SASL ANONYMOUS mechanism
 */
class SASLAnonymous (override val saslAuthentication: SASLAuthentication)
	extends SASLMechanism(saslAuthentication)
{
	var name: String = "ANONYMOUS"

	@throws(classOf[IOException])
	override def authenticate() {
		// Send the authentication to the server
		saslAuthentication.send(new SASLMechanism.AuthMechanism(name, null));
	}

	@throws(classOf[IOException])
	override def challengeReceived(challenge: String ) {
		// Build the challenge response stanza encoding the response text
		// and send the authentication to the server
		saslAuthentication.send(new SASLMechanism.Response());
	}
}



/**
 * Implementation of the SASL GSSAPI mechanism
 */
class SASLGSSAPIMechanism(override val saslAuthentication: SASLAuthentication) 
	extends SASLMechanism(saslAuthentication)
{
	var name: String = "GSSAPI"

	System.setProperty("javax.security.auth.useSubjectCredsOnly","false");
	System.setProperty("java.security.auth.login.config","gss.conf");

	@throws(classOf[IOException])
	@throws(classOf[XMPPException ])
	override def authenticate(cbh: CallbackHandler ) {
		val pops = new java.util.HashMap[String, String]()
		pops.put(Sasl.SERVER_AUTH, "TRUE")
		sc = Sasl.createSaslClient(Array(name), authenticationId, "xmpp", host, 
			pops, cbh);
		authenticate();
	}

	@throws(classOf[IOException])
	@throws(classOf[XMPPException ])
	override def authenticate() {
		val pops = new java.util.HashMap[String, String]()
		pops.put(Sasl.SERVER_AUTH, "TRUE")
		sc = Sasl.createSaslClient(Array(name), authenticationId, "xmpp", host, 
			pops, this);
		super.authenticate()
	}

}
