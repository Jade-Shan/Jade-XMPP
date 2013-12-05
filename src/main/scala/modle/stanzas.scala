package jadeutils.xmpp.model

import java.util.concurrent.atomic.AtomicLong

import jadeutils.common.Logging
import jadeutils.common.StrUtils.randomNumLetterStr

/**
 * Represents a XMPP error sub-packet. Typically, a server responds to a request that has
 * problems by sending the packet back and including an error packet. Each error has a code, type, 
 * error condition as well as as an optional text explanation. Typical errors are:<p>
 *<table border=1>
 *      <hr><td><b>Code</b></td><td><b>XMPP Error</b></td><td><b>Type</b></td></hr>
 *      <tr><td>500</td><td>interna-server-error</td><td>WAIT</td></tr>
 *      <tr><td>403</td><td>forbidden</td><td>AUTH</td></tr>
 *      <tr><td>400</td<td>bad-request</td><td>MODIFY</td>></tr>
 *      <tr><td>404</td><td>item-not-found</td><td>CANCEL</td></tr>
 *      <tr><td>409</td><td>conflict</td><td>CANCEL</td></tr>
 *      <tr><td>501</td><td>feature-not-implemented</td><td>CANCEL</td></tr>
 *      <tr><td>302</td><td>gone</td><td>MODIFY</td></tr>
 *      <tr><td>400</td><td>jid-malformed</td><td>MODIFY</td></tr>
 *      <tr><td>406</td><td>no-acceptable</td><td> MODIFY</td></tr>
 *      <tr><td>405</td><td>not-allowed</td><td>CANCEL</td></tr>
 *      <tr><td>401</td><td>not-authorized</td><td>AUTH</td></tr>
 *      <tr><td>402</td><td>payment-required</td><td>AUTH</td></tr>
 *      <tr><td>404</td><td>recipient-unavailable</td><td>WAIT</td></tr>
 *      <tr><td>302</td><td>redirect</td><td>MODIFY</td></tr>
 *      <tr><td>407</td><td>registration-required</td><td>AUTH</td></tr>
 *      <tr><td>404</td><td>remote-server-not-found</td><td>CANCEL</td></tr>
 *      <tr><td>504</td><td>remote-server-timeout</td><td>WAIT</td></tr>
 *      <tr><td>502</td><td>remote-server-error</td><td>CANCEL</td></tr>
 *      <tr><td>500</td><td>resource-constraint</td><td>WAIT</td></tr>
 *      <tr><td>503</td><td>service-unavailable</td><td>CANCEL</td></tr>
 *      <tr><td>407</td><td>subscription-required</td><td>AUTH</td></tr>
 *      <tr><td>500</td><td>undefined-condition</td><td>WAIT</td></tr>
 *      <tr><td>400</td><td>unexpected-condition</td><td>WAIT</td></tr>
 *      <tr><td>408</td><td>request-timeout</td><td>CANCEL</td></tr>
 * </table>
 * 
 */
class XMPPError (val condition: XMPPError.Condition.Value, val message: String) 
{
	private[this] val errSpec = {
		if (null != condition) XMPPError.ErrorSpecification.specFor(condition)
		else null
	}
	val code: Integer = if (null != errSpec) errSpec.code else null
	val errType = if (null != errSpec) errSpec.errType else null
	val conditionName = condition.toString

	def this(condition: XMPPError.Condition.Value) {
		this(condition, null)
	}

}

object XMPPError {

	object Type extends Enumeration {
		val WAIT, CANCEL, MODIFY, AUTH, CONTINUE = Value
	}

	object Condition extends Enumeration {
		val interna_server_error = Value("internal-server-error")
		val forbidden = Value("forbidden")
		val bad_request = Value("bad-request")
		val conflict = Value("conflict")
		val feature_not_implemented = Value("feature-not-implemented")
		val gone = Value("gone")
		val item_not_found = Value("item-not-found")
		val jid_malformed = Value("jid-malformed")
		val no_acceptable = Value("not-acceptable")
		val not_allowed = Value("not-allowed")
		val not_authorized = Value("not-authorized")
		val payment_required = Value("payment-required")
		val recipient_unavailable = Value("recipient-unavailable")
		val redirect = Value("redirect")
		val registration_required = Value("registration-required")
		val remote_server_error = Value("remote-server-error")
		val remote_server_not_found = Value("remote-server-not-found")
		val remote_server_timeout = Value("remote-server-timeout")
		val resource_constraint = Value("resource-constraint")
		val service_unavailable = Value("service-unavailable")
		val subscription_required = Value("subscription-required")
		val undefined_condition = Value("undefined-condition")
		val unexpected_request = Value("unexpected-request")
		val request_timeout = Value("request-timeout")
	}

	class ErrorSpecification (val condition: XMPPError.Condition.Value, 
		val errType: XMPPError.Type.Value, val code: Int) { }

	object ErrorSpecification {
		import Condition._
		import Type._

		private[this] val instances =  Map(
			interna_server_error ->
				new ErrorSpecification(interna_server_error, WAIT, 500),
			forbidden ->
				new ErrorSpecification(forbidden, AUTH, 403),
			bad_request ->
				new XMPPError.ErrorSpecification(bad_request, MODIFY, 400),
			item_not_found ->
				new XMPPError.ErrorSpecification(item_not_found, CANCEL, 404),
			conflict ->
				new XMPPError.ErrorSpecification(conflict, CANCEL, 409),
			feature_not_implemented ->
				new XMPPError.ErrorSpecification(feature_not_implemented, CANCEL, 501),
			gone ->
				new XMPPError.ErrorSpecification(gone, MODIFY, 302),
			jid_malformed ->
				new XMPPError.ErrorSpecification(jid_malformed, MODIFY, 400),
			no_acceptable ->
				new XMPPError.ErrorSpecification(no_acceptable, MODIFY, 406),
			not_allowed ->
				new XMPPError.ErrorSpecification(not_allowed, CANCEL, 405),
			not_authorized ->
				new XMPPError.ErrorSpecification(not_authorized, AUTH, 401),
			payment_required ->
				new XMPPError.ErrorSpecification(payment_required, AUTH, 402),
			recipient_unavailable ->
				new XMPPError.ErrorSpecification(recipient_unavailable, WAIT, 404),
			redirect ->
				new XMPPError.ErrorSpecification(redirect, MODIFY, 302),
			registration_required ->
				new XMPPError.ErrorSpecification(registration_required, AUTH, 407),
			remote_server_not_found ->
				new XMPPError.ErrorSpecification(remote_server_not_found, CANCEL, 404),
			remote_server_timeout ->
				new XMPPError.ErrorSpecification(remote_server_timeout, WAIT, 504),
			remote_server_error ->
				new XMPPError.ErrorSpecification(remote_server_error, CANCEL, 502),
			resource_constraint ->
				new XMPPError.ErrorSpecification(resource_constraint, WAIT, 500),
			service_unavailable ->
				new XMPPError.ErrorSpecification(service_unavailable, CANCEL, 503),
			subscription_required ->
				new XMPPError.ErrorSpecification(subscription_required, AUTH, 407),
			undefined_condition ->
				new XMPPError.ErrorSpecification(undefined_condition, WAIT, 500),
			unexpected_request ->
				new XMPPError.ErrorSpecification(unexpected_request, WAIT, 400),
			request_timeout ->
				new XMPPError.ErrorSpecification(request_timeout, CANCEL, 408)
			)


		def specFor(condition: Condition.Value) = {
			instances(condition)
		}

	}
	
}

trait PacketExtension {
	val elementName: String
	val namespace: String
}

abstract class Packet(val p: Packet) { 
	val xmlns: String = if (null != p && null != p.xmlns) p.xmlns else Packet.defaultXmlNs 

	val packetId: String = Packet.nextID

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


// 
// 
// 
// class StanzasStream(val to: String) extends Packet {
// 
// 	def toXML = <stream:stream version="1.0" xmlns="jabber:client" 
// 		xmlns:stream="http://etherx.jabber.org/streams"
// 		to="{to}"></stream:stream>
// 
// }
// 
// abstract class IQ extends Packet {
// 
// 	private[this] var t = IQ.Type.GET
// 	def msgType = if (null == t) IQ.Type.GET else t
// 	def msgType_=(newType: IQ.Type.Value) {
// 		if (null == newType) t = IQ.Type.GET else t = newType
// 	}
// 
// }
// 
// object IQ extends Logging {
// 
// 	object Type extends Enumeration {
// 		val GET = Value("get")
// 		val SET = Value("set")
// 		val RESULT = Value("result")
// 		val ERROR = Value("error")
// 	}
// }
