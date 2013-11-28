package jadeutils.xmpp.utils

import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

import scala.collection.mutable.HashMap

import jadeutils.common._

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
