package jadeutils.xmpp.utils

import java.lang.reflect.Constructor
import java.io.FileInputStream
import java.io.IOException
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.Principal
import java.security.PublicKey
import java.util.Date
import javax.net.ssl.X509TrustManager
import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.PasswordCallback

import javax.net.ssl.KeyManager
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager

import scala.collection.mutable.Map

import jadeutils.common.Logging
import jadeutils.xmpp.model.Packet


trait AuthModule extends Logging { this: XMPPConnection =>

	val saslAuthentication: SASLAuthentication = new SASLAuthentication(this)

	var anonymous = false
	var usingTLS = false
	var authenticated = false /* is auth now  */
	var wasAuth = false       /* has auth before */

	def wasAuthenticated: Boolean = wasAuth

	def wasAuthenticated_=(wasAuthenticated: Boolean) {
		if (!wasAuth)
			wasAuth = wasAuthenticated
	}

	def proceedTLSReceived() {
		var ks: KeyStore = null;
		var kms: Array[KeyManager] = null;

		// Secure the plain connection
		var context = SSLContext.getInstance("TLS")
		var sslSocket: SSLSocket = context.getSocketFactory.createSocket(
			ioStream.socket, ioStream.socket.getInetAddress().getHostAddress(), 
			ioStream.socket.getPort(), true).asInstanceOf[SSLSocket];
		ioStream.socket = sslSocket
		sslSocket.setSoTimeout(0);
		sslSocket.setKeepAlive(true)
		// Initialize the reader and writer with the new secured version
		ioStream.initReaderAndWriter();
		// Proceed to do the handshake
		sslSocket.startHandshake();
		usingTLS = true
	}

}




case class KeyStoreOptions ( val authType: String, val path: String, 
	val password: String) 
{

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

class ServerTrustManager (val serviceName: String, 
	val connCfg: ConnectionConfiguration) extends X509TrustManager with Logging {

	var trustStore: KeyStore = null

	def getAcceptedIssuers(): Array[X509Certificate] = 
	new Array[X509Certificate](0)

	def checkClientTrusted(chain: Array[X509Certificate], authType: String) {
		// do nothing
	} 

	@throws(classOf[CertificateException])
	def checkServerTrusted(chain: Array[X509Certificate], authType: String) {
		val nSize = chain.length
		val peerIdentities: List[String] = ServerTrustManager.getPeerIdentity(chain(0))

		if (connCfg.verifyChainEnabled) {
						// TODO: next phase
//			// Working down the chain, for every certificate in the chain,
//			// verify that the subject of the certificate is the issuer of the
//			// next certificate in the chain.
//			var principalLast: Principal = null
//			for (n <- 1 to nSize) {
//				val i = nSize - n
//				val x509certificate = chain(i)
//				val principalIssuer = x509certificate.getIssuerDN
//				val principalSubject = x509certificate.getSubjectDN
//				if (principalLast != null) {
//					if (principalIssuer.equals(principalLast)) {
//						try {
//							val publickey: PublicKey = chain(i + 1).getPublicKey()
//							chain(i).verify(publickey)
//						} catch {
//							case e: GeneralSecurityException =>
//							throw new CertificateException(
//								"signature verification failed of " + peerIdentities);
//						}
//					} else {
//						throw new CertificateException(
//							"subject/issuer verification failed of " + peerIdentities);
//					}
//				}
//				principalLast = principalSubject;
//			}
//
		}

		if (connCfg.verifyRootCAEnabled) {
						// TODO: next phase
//			// Verify that the the last certificate in the chain was issued
//			// by a third-party that the client trusts.
//			var trusted = false
//			try {
//				trusted = 
//					trustStore.getCertificateAlias(chain(nSize - 1)) != null
//				if (!trusted && nSize == 1 && connCfg.selfSignedCertificateEnabled)
//				{
//					logger.info("Accepting self-signed certificate of remote server: " +
//						peerIdentities)
//					trusted = true
//				}
//			} catch {
//				case e: KeyStoreException => e.printStackTrace()
//			}
//			if (!trusted) {
//				throw new CertificateException("root certificate not trusted of " + 
//					peerIdentities)
//			}
		}

		if (connCfg.notMatchingDomainCheckEnabled) {
						// TODO: next phase
//			// Verify that the first certificate in the chain corresponds to
//			// the server we desire to authenticate.
//			// Check if the certificate uses a wildcard indicating that subdomains are valid
//			if (peerIdentities.size == 1) {
//				val ns: String = peerIdentities.indexOf(0)
//				if (ns.startsWith("*.")) {
//					// Remove the wildcard
//					val peerIdentity = ns.replace("*.", "");
//					// Check if the requested subdomain matches the certified domain
//					if (!serviceName.endsWith(peerIdentity)) {
//						throw new CertificateException("target verification failed of " + 
//							peerIdentities)
//					}
//				} else if (!peerIdentities.contains(serviceName)) {
//					throw new CertificateException("target verification failed of " + 
//						peerIdentities)
//				}
//			}
		}

		if (connCfg.expiredCertificatesCheckEnabled) {
						// TODO: next phase
			// For every certificate in the chain, verify that the certificate
			// is valid at the current time.
//			val date = new Date();
//			for (i <- 0 until nSize) {
//				try {
//					chain(i).checkValidity(date)
//				} catch {
//					case e: GeneralSecurityException => throw new CertificateException(
//						"invalid date of " + serviceName)
//				}
//			}
		}
	}

}

object ServerTrustManager {

	val cnPattern = """(?i)(cn=)([^,]*)""".r

	val stores = Map.empty[KeyStoreOptions, KeyStore]

	def apply(serviceName: String, connCfg: ConnectionConfiguration) = {
		var trustStore: KeyStore = null;
		val options = new KeyStoreOptions(connCfg.truststoreType,
			connCfg.trustStorePath, connCfg.truststorePassword)
		if (stores contains options) {
			trustStore = stores.get(options).get
		} else {
			var inputStream: FileInputStream =  null
			try {
				trustStore = KeyStore.getInstance(options.authType)
				inputStream = new FileInputStream(options.path)
				trustStore.load(inputStream, options.password.toCharArray)
			} catch {
				case e: Exception => {
					trustStore = null
					e.printStackTrace
				}
			} finally {
				if (null != inputStream) {
					try {
						inputStream.close
					} catch {
						case e: Exception => // do nothin
					}
				}
			}
			stores put (options, trustStore)
		}
		connCfg.verifyRootCAEnabled = (trustStore != null)
		new ServerTrustManager(serviceName, connCfg)
	}

	def getPeerIdentity(certificate: X509Certificate): List[String] = {
		var names: List[String] = this.getSubjectAlternativeNames(certificate)
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
		val identities: List[String] = Nil
		try {
			val altNames = certificate.getSubjectAlternativeNames();
			// Check that the certificate includes the SubjectAltName extension
			if (altNames == null) return Nil
		} catch {
			case e: CertificateParsingException => e.printStackTrace
		}
		identities;
	}

}





trait UserAuthentication {

	@throws(classOf[XMPPException])
	def authenticate(username: String, resource: String, cbh: CallbackHandler): String

	@throws(classOf[XMPPException])
	def authenticate(username: String, password: String, resource: String): String

	@throws(classOf[XMPPException])
	def authenticateAnonymously(): String
}



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
