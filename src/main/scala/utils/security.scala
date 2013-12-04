package jadeutils.xmpp.utils

import java.lang.reflect.Constructor
import java.io.FileInputStream
import java.io.IOException
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.PasswordCallback




import scala.collection.mutable.HashMap

import jadeutils.common.Logging

case class KeyStoreOptions (
	val authType: String, val path: String, val password: String
) {

	override def equals(that: Any) = that match {
		case that: KeyStoreOptions => {
			this.authType == that.authType &&
			this.path == that.path &&
			this.password == that.password
		}
		case _ => false
	}

	override def hashCode = {
		var n = 41 
		if (null != authType) n = 41 * (n + authType.hashCode)
		if (null != path    ) n = 41 * (n + path.hashCode)
		if (null != password) n = 41 * (n + password.hashCode)
		n
	}

	override def toString = 
		"KeyStoreOptions=(%s, %s, %s)".format(authType, path, password)
}

class ServerTrustManager (
	val serviceName: String, val connCfg: ConnectionConfiguration, 
	val trustStore: KeyStore
) extends X509TrustManager with Logging {

	def getAcceptedIssuers(): Array[X509Certificate] = 
		new Array[X509Certificate](0)

	def checkClientTrusted(chain: Array[X509Certificate], authType: String) {
		// do nothing
	} 

	def checkServerTrusted(chain: Array[X509Certificate], authType: String) {
		val nSize = chain.length
		val peerIdentities = ServerTrustManager.getPeerIdentity(chain(0))

		if (connCfg.verifyChainEnabled) {
			// TODO: auth stuff
		}
		if (connCfg.verifyRootCAEnabled) {
			// TODO: auth stuff
		}
		if (connCfg.notMatchingDomainCheckEnabled) {
			// TODO: auth stuff
		}
		if (connCfg.expiredCertificatesCheckEnabled) {
			// TODO: auth stuff
		}
	} 

}

object ServerTrustManager extends Logging {

	val cnPattern = """(?i)(cn=)([^,]*)""".r

	val stores = new HashMap[KeyStoreOptions, KeyStore]()

	def apply(serviceName: String, connCfg: ConnectionConfiguration) = {
		var trustStore: KeyStore = null;
		val options = new KeyStoreOptions(connCfg.truststoreType,
			connCfg.trustStorePath, connCfg.truststorePassword)
		// val tr = stores.get(options)
		if (stores contains options) {
			trustStore = stores.get(options).get
		} else {
			trustStore = KeyStore.getInstance(options.authType)
			val inputStream = new FileInputStream(options.path)
			trustStore.load(inputStream, options.password.toCharArray)
			stores.put(options, trustStore)
		}
		connCfg.verifyRootCAEnabled = (trustStore != null)
		new ServerTrustManager(serviceName, connCfg, trustStore)
	}

	def getPeerIdentity(certificate: X509Certificate): List[String] = {
		var names = this.getSubjectAlternativeNames(certificate)
		if (names.isEmpty) {
			certificate.getSubjectDN().getName() match {
				case cnPattern(a, b, c) => names = b :: Nil
				case _ =>
			}
		}
		names
	}

	def getSubjectAlternativeNames(certificate: X509Certificate): List[String] = 
	{
		Nil
	}

}





/**
 * There are two ways to authenticate a user with a server. Using SASL or Non-SASL
 * authentication. This interface makes {@link SASLAuthentication} and
 * {@link NonSASLAuthentication} polyphormic.
 *
 * @author Gaston Dombiak
 * @author Jay Kline
 */
trait UserAuthentication {

    /**
     * Authenticates the user with the server.  This method will return the full JID provided by
     * the server.  The server may assign a full JID with a username and resource different than
     * requested by this method.
     *
     * Note that using callbacks is the prefered method of authenticating users since it allows
     * more flexability in the mechanisms used.
     *
     * @param username the requested username (authorization ID) for authenticating to the server
     * @param resource the requested resource.
     * @param cbh the CallbackHandler used to obtain authentication ID, password, or other
     * information
     * @return the full JID provided by the server while binding a resource for the connection.
     * @throws XMPPException if an error occurs while authenticating.
     */
		@throws(classOf[XMPPException])
		def authenticate(username: String, resource: String, cbh: CallbackHandler): String

    /**
     * Authenticates the user with the server. This method will return the full JID provided by
     * the server. The server may assign a full JID with a username and resource different than
     * the requested by this method.
     *
     * It is recommended that @{link #authenticate(String, String, CallbackHandler)} be used instead
     * since it provides greater flexability in authenticaiton and authorization.
     *
     * @param username the username that is authenticating with the server.
     * @param password the password to send to the server.
     * @param resource the desired resource.
     * @return the full JID provided by the server while binding a resource for the connection.
     * @throws XMPPException if an error occures while authenticating.
     */
		@throws(classOf[XMPPException])
    def authenticate(username: String, password: String, resource: String): String

    /**
     * Performs an anonymous authentication with the server. The server will created a new full JID
     * for this connection. An exception will be thrown if the server does not support anonymous
     * authentication.
     *
     * @return the full JID provided by the server while binding a resource for the connection.
     * @throws XMPPException if an error occures while authenticating.
     */
		@throws(classOf[XMPPException])
    def authenticateAnonymously(): String
}



/**
 * Implementation of JEP-0078: Non-SASL Authentication. Follow the following
 * <a href=http://www.jabber.org/jeps/jep-0078.html>link</a> to obtain more
 * information about the JEP.
 *
 * @author Gaston Dombiak
 */
class NonSASLAuthentication(val connection: XMPPConnection) 
	extends UserAuthentication 
{

		@throws(classOf[XMPPException])
		def authenticate(username: String, resource: String, cbh: CallbackHandler): String = {
			//Use the callback handler to determine the password, and continue on.
			val pcb = new PasswordCallback("Password: ",false);
			try {
				cbh.handle(Array(pcb));
				authenticate(username, String.valueOf(pcb.getPassword()),resource);
			} catch {
				case e: Exception => 
					throw new XMPPException("Unable to determine password.",e);
			}   
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

}


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
 * @see org.jivesoftware.smack.sasl.SASLMechanism
 *
 * @author Gaston Dombiak
 * @author Jay Kline
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

}
