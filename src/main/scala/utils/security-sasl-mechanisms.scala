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
//import jadeutils.xmpp.utils.SASLAuthentication
//import jadeutils.xmpp.utils.XMPPException

import jadeutils.xmpp.model.Packet

/**
	* Base class for SASL mechanisms. Subclasses must implement these methods:
	* <ul>
	*  <li>{@link #getName()} -- returns the common name of the SASL mechanism.</li>
	* </ul>
	* Subclasses will likely want to implement their own versions of these mthods:
	*  <li>{@link #authenticate(String, String, String)} -- Initiate authentication stanza using the
	*  deprecated method.</li>
	*  <li>{@link #authenticate(String, String, CallbackHandler)} -- Initiate authentication stanza
	*  using the CallbackHandler method.</li>
	*  <li>{@link #challengeReceived(String)} -- Handle a challenge from the server.</li>
	* </ul>
	* 
	* Basic XMPP SASL authentication steps:
	* 1. Client authentication initialization, stanza sent to the server (Base64 encoded): 
	*    <auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism='DIGEST-MD5'/>
	* 2. Server sends back to the client the challenge response (Base64 encoded)
	*    sample: 
	*    realm=<sasl server realm>,nonce="OA6MG9tEQGm2hh",qop="auth",charset=utf-8,algorithm=md5-sess
	* 3. The client responds back to the server (Base 64 encoded):
	*    sample:
	*    username=<userid>,realm=<sasl server realm from above>,nonce="OA6MG9tEQGm2hh",
	*    cnonce="OA6MHXh6VqTrRk",nc=00000001,qop=auth,
	*    digest-uri=<digesturi>,
	*    response=d388dad90d4bbd760a152321f2143af7,
	*    charset=utf-8,
	*    authzid=<id>
	* 4. The server evaluates if the user is present and contained in the REALM
	*    if successful it sends: <response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/> (Base64 encoded)
	*    if not successful it sends:
	*    sample:
	*    <challenge xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>
	*        cnNwYXV0aD1lYTQwZjYwMzM1YzQyN2I1NTI3Yjg0ZGJhYmNkZmZmZA==
	*    </challenge>       
	* 
	*
	*/

abstract class SASLMechanism(val saslAuthentication: SASLAuthentication,
	authenticationId: String, host: String, serviceName: String, password: String) 
extends CallbackHandler 
{
	val logger = SASLMechanism.logger

	var name: String
	var sc: SaslClient = null

	/**
		* Builds and sends the <tt>auth</tt> stanza to the server. The callback handler will handle
		* any additional information, such as the authentication ID or realm, if it is needed.
		*
		* @param username the username of the user being authenticated.
		* @param host     the hostname where the user account resides.
		* @param cbh      the CallbackHandler to obtain user information.
		* @throws IOException If a network error occures while authenticating.
		* @throws XMPPException If a protocol error occurs or the user is not authenticated.
		*/
	@throws(classOf[IOException])
	@throws(classOf[XMPPException])
	def authenticate(cbh: CallbackHandler) {
		val props = new java.util.HashMap[String, String]()
		sc = Sasl.createSaslClient(Array(this.name), authenticationId, "xmpp", 
			host, props, cbh)
		authenticate()
	}

	/**
		* Builds and sends the <tt>auth</tt> stanza to the server. Note that this method of
		* authentication is not recommended, since it is very inflexable. Use
		* {@link #authenticate(String, String, CallbackHandler)} whenever possible.
		* 
		* Explanation of auth stanza:
		* 
		* The client authentication stanza needs to include the digest-uri of the form: xmpp/serverName 
		* From RFC-2831: 
		* digest-uri = "digest-uri" "=" digest-uri-value
		* digest-uri-value = serv-type "/" host [ "/" serv-name ]
		* 
		* digest-uri: 
		* Indicates the principal name of the service with which the client 
		* wishes to connect, formed from the serv-type, host, and serv-name. 
		* For example, the FTP service
		* on "ftp.example.com" would have a "digest-uri" value of "ftp/ftp.example.com"; the SMTP
		* server from the example above would have a "digest-uri" value of
		* "smtp/mail3.example.com/example.com".
		* 
		* host:
		* The DNS host name or IP address for the service requested. The DNS host name
		* must be the fully-qualified canonical name of the host. The DNS host name is the
		* preferred form; see notes on server processing of the digest-uri.
		* 
		* serv-name: 
		* Indicates the name of the service if it is replicated. The service is
		* considered to be replicated if the client's service-location process involves resolution
		* using standard DNS lookup operations, and if these operations involve DNS records (such
			* as SRV, or MX) which resolve one DNS name into a set of other DNS names. In this case,
		* the initial name used by the client is the "serv-name", and the final name is the "host"
		* component. For example, the incoming mail service for "example.com" may be replicated
		* through the use of MX records stored in the DNS, one of which points at an SMTP server
		* called "mail3.example.com"; it's "serv-name" would be "example.com", it's "host" would be
		* "mail3.example.com". If the service is not replicated, or the serv-name is identical to
		* the host, then the serv-name component MUST be omitted
		* 
		* digest-uri verification is needed for ejabberd 2.0.3 and higher   
		* 
		* @param username the username of the user being authenticated.
		* @param host the hostname where the user account resides.
		* @param serviceName the xmpp service location - used by the SASL client in digest-uri creation
		* serviceName format is: host [ "/" serv-name ] as per RFC-2831
		* @param password the password for this account.
		* @throws IOException If a network error occurs while authenticating.
		* @throws XMPPException If a protocol error occurs or the user is not authenticated.
		*/
	@throws(classOf[IOException])
	@throws(classOf[XMPPException])
	def authenticate() {
		val props = new java.util.HashMap[String, String]()
		this.sc = Sasl.createSaslClient(Array(this.name), authenticationId, 
			"xmpp", serviceName, props, this)
		var authenticationText: String = null
		try {
			if(sc.hasInitialResponse()) {
				val response: Array[Byte] = sc.evaluateChallenge(new Array[Byte](0))
				authenticationText = encodeBase64(response, false)
			}
		} catch {
			case e: SaslException => throw new XMPPException(
				"SASL authentication failed", e)
		}

		// Send the authentication to the server
		logger.debug("Send SASL auth info")
		this.saslAuthentication.send(new SASLMechanism.AuthMechanism(this.name, 
			authenticationText))
	}

	/**
		* The server is challenging the SASL mechanism for the stanza he just sent. Send a
		* response to the server's challenge.
		*
		* @param challenge a base64 encoded string representing the challenge.
		* @throws IOException if an exception sending the response occurs.
		*/
	@throws(classOf[IOException])
	def challengeReceived(challenge: String) {
		var response: Array[Byte] = null
		if(challenge != null) {
			response = sc.evaluateChallenge(decodeBase64(challenge))
		} else {
			response = sc.evaluateChallenge(new Array[Byte](0))
		}

		var responseStanza: Packet = null
		if (response == null) {
			responseStanza = new SASLMechanism.Response(null)
		}
		else {
			responseStanza = new SASLMechanism.Response(encodeBase64(response, false))
		}

		// Send the authentication to the server
		this.saslAuthentication.send(responseStanza);
	}


	/**
		* 
		*/
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

