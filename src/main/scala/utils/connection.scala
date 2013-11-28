package jadeutils.xmpp.utils

import java.net.Socket

class ConnectionConfiguration(val serviceName: String, val port: Int) {
	var hostAddresses: List[HostAddress] = null

	var compressionEnabled = false

	val trustStorePath = System.getProperty("java.home") + 
		java.io.File.separator + "lib" +
		java.io.File.separator + "security" +
		java.io.File.separator + "cacerts"
  val truststoreType = "jks"; // Set the default store type
  // Set the default password of the cacert file that is "changeit"
  val truststorePassword = "changeit"
  val keystorePath = System.getProperty("javax.net.ssl.keyStore")
  val keystoreType = "jks"
  val pkcs11Library = "pkcs11.config"

  var verifyChainEnabled = false;
  var verifyRootCAEnabled = false;
  var selfSignedCertificateEnabled = false;
  var expiredCertificatesCheckEnabled = false;
  var notMatchingDomainCheckEnabled = false;

	var saslAuthenticationEnabled = true
}

class XMPPConnection(val serviceName: String, val port: Int) {
	var connCfg = new ConnectionConfiguration(serviceName, port)
	connCfg.hostAddresses = XmppDNSService.resolveXmppClientDomain("jabber.org")

	var socket: Socket = null

	def connect() {
		for (host <- this.connCfg.hostAddresses) {
			println("curr conn: " + host.toString)
		}
	}
}

