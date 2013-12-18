package jadeutils.xmpp.utils



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
class SASLAuthentication(val connection: XMPPConnection) 
extends UserAuthentication 
{

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
		connection.sendPacket(stanza);
	}

}
