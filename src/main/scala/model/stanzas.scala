package jadeutils.xmpp.model

import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.util.concurrent.atomic.AtomicLong

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
import jadeutils.common.XMLUtils.newTextAttr

trait SubPacket {
	val elementName: String
	val namespace: String
	def toXML(): Node
}

/**
 * <p>XMPP error sub-packet.</p>
 * <table border=1>
 *      <hr><td><b>Code</b></td><td><b>XMPP Error</b></td><td><b>Type</b></td></hr>
 *      <tr><td>500</td><td>interna-server-error</td><td>WAIT</td></tr>
 *      <tr><td>403</td><td>forbidden</td><td>AUTH</td></tr>
 *      <tr><td>400</td><td>bad-request</td><td>MODIFY</td></tr>
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
class XMPPError (val condition: XMPPError.Condition.Value, val message: String, 
	private[this] var appExtList: List[SubPacket]) 
{
	private[this] val errSpec = {
		if (null != condition) XMPPError.ErrorSpecification.specFor(condition)
		else null
	}
	val code: Integer = if (null != errSpec) errSpec.code else null
	val errType = if (null != errSpec) errSpec.errType else null
	val conditionName = if (null != condition) condition.toString else null

	override def equals(that: Any) = {
		that match {
			case that: XMPPError => {
				if (this.condition == that.condition && this.message == that.message)
					true
				else false
			}
			case _ => false
		}
	}

	override def hashCode() = {
		var n = hashField(condition)
		n = 41 * n + hashField(message)
		n
	}

	def applicationExtensions() = appExtList

	def applicationExtensions_=(newList: List[SubPacket]) {
		if (null == newList) appExtList = Nil
		else appExtList = newList
	}

	def this(condition: XMPPError.Condition.Value, message: String) {
		this(condition, message, Nil)
	}

	def this(condition: XMPPError.Condition.Value) {
		this(condition, null)
	}

	def toXML: Node = {
		val x = <error>{ 
			if (null != condition) XMPPError.Condition.toXML(condition) 
		}{ 
			if (null != message) <text xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" 
				xml:lang="en">{message}</text> 
		}{ 
			if (null != applicationExtensions) applicationExtensions.map(_.toXML)
		}</error>
		x % Attribute(None, "code", newTextAttr(code.toString), 
			Attribute(None, "type", newTextAttr(errType.toString), Null))
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

		def toXML(condition: Condition.Value): Node = {
			XML.loadString("""<%s xmlns="urn:ietf:params:xml:ns:xmpp-stanzas" />""" 
				format condition.toString)
		}
 
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

abstract class Packet(val xmlns: String, val packetId: String, 
	val from: String, val to: String, val error: XMPPError, 
	private[this] var props: Map[String, Any],
	private[this] var subPkts: List[SubPacket])
{ 
	val logger = Packet.logger

	def properties() = props 
	def properties_= (newProps: Map[String, Any]) {
		if (null == newProps) props = scala.collection.immutable.Map()
		else props = newProps
	}

	def subPackets() = subPkts
	def subPackets_= (newList: List[SubPacket]) {
		if (null == newList) subPkts = Nil
		else subPkts = newList
	}

	override def equals(that: Any) = {
		that match {
			case that: Packet => {
				if (this. xmlns == that.xmlns && this.packetId == that.packetId && 
					this.from == that.from && this.to == that.to) true
				 else false
			}
			case _ => false
		}
	}

	override def hashCode() = {
		var n = hashField(xmlns)
		n = 41 * n + hashField(packetId)
		n = 41 * n + hashField(from)
		n = 41 * n + hashField(to)
		n = 41 * n + hashField(error)
		n
	}

	def this(xmlns: String, from: String, to: String, error: XMPPError, 
		props: Map[String, Any], subPkts: List[SubPacket])
	{
		this(xmlns, Packet.nextId, from, to, error, props, subPkts)
	}

	def this(from: String, to: String, error: XMPPError, props: Map[String, Any],
		subPkts: List[SubPacket])
	{
		this(Packet.defaultXmlns, Packet.nextId, from, to, error, props, subPkts)
	}

	def this(xmlns: String, packetId: String, from: String, to: String) {
		this(xmlns, packetId, from, to, null, null, null)
	}

	def this(from: String, to: String) {
		this(from, to, null, null, null)
	}

	def this(p: Packet) {
		this(p.xmlns, p.packetId, p.from, p.to, p.error, p.properties, 
			p.subPackets)
	}

	private[this] def propertyXML(item: Any): Node = {
		val rec: (String, String) = item match {
			case v if v.isInstanceOf[Boolean] => (v.toString, "boolean")
			case v if v.isInstanceOf[Int]     => (v.toString, "integer")
			case v if v.isInstanceOf[Float]   => (v.toString, "float")
			case v if v.isInstanceOf[Double]  => (v.toString, "double")
			case v if v.isInstanceOf[String]  => (v.toString, "string")
			case _ => {
				var byteStream: ByteArrayOutputStream = null
				var out: ObjectOutputStream = null
				try {
					byteStream = new ByteArrayOutputStream();
					out = new ObjectOutputStream(byteStream);
					out.writeObject(item);
					(encodeBase64(byteStream.toByteArray()), "java-object");
				} catch {
					case e: Exception => { e.printStackTrace(); null}
				} finally {
					if (out != null) {
						try {
							out.close();
						} catch {
							case e: Exception => { e.printStackTrace() }
						}
					}
					if (byteStream != null) {
						try {
							byteStream.close();
						} catch {
							case e: Exception => { e.printStackTrace() }
						}
					}
				}
			}
		}
		<value>{rec._2}</value> % Attribute(None, "code", newTextAttr(rec._1), Null)
	}

	def propertiesXML(): Node = {
		if (null != properties && !properties.isEmpty) {
			<properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties">{
				properties.map((p: (String, Any)) => {
						<property><name>{p._1}</name>{propertyXML(p._2)}</property>
				}) 
			}</properties >
		} else null
	}

	/**
		* get list of sub nodes
		*/
	def subPacketsXML(): List[Node] =  {
		if (null != subPackets) subPackets.map(_.toXML) else Nil
	}

	def childElementXML: NodeBuffer = {
		val nb = new NodeBuffer
		val p = propertiesXML
		val es = subPackets
		if (null != p) nb += p
		if (null != es) es.foreach(nb += _.toXML)
		nb
	}

	def addAttributeToXML(node: Elem): Elem = node % 
		Attribute(None, "to", newTextAttr(to), 
			Attribute(None, "from", newTextAttr(from), 
				Attribute(None, "packetId", newTextAttr(packetId), 
					Attribute(None, "xmlns", newTextAttr(xmlns), Null))))

	def nodeXML(childElementXML: NodeBuffer): Elem
	
	def toXML: Node = addAttributeToXML(nodeXML(childElementXML))

}

object Packet extends Logging {

	val defaultLanguage = java.util.Locale.getDefault.getLanguage.toLowerCase;
	var defaultXmlns: String = null;

	val ID_NOT_AVAILABLE: String = "ID_NOT_AVAILABLE";
	var id= new AtomicLong(0)
	val prefix: String = randomNumLetterStr(5) + "-"
	def nextId() = this.prefix + this.id.getAndIncrement

}

class IQ(private[this] val mType: IQ.Type.Value, 
	override val packetId: String, val id: String, override val from: String, 
	override val to: String, override val error: XMPPError, 
	private[this] var props: Map[String, Any],
	private[this] var subPkts: List[SubPacket]) extends Packet( null, 
	packetId, from, to, error, props, subPkts)
{

	val msgType = if (null == mType) IQ.Type.GET else mType

	def this(mType: IQ.Type.Value, id: String, from: String, to: String, 
		error: XMPPError, props: Map[String, Any], subPkts: List[SubPacket])
	{
		this(mType, null, id, from, to, error, props, subPkts) 
	}

	def this(mType: IQ.Type.Value, from: String, to: String, error: XMPPError, 
		props: Map[String, Any], subPkts: List[SubPacket])
	{
		this(mType, null, Packet.nextId, from, to, error, props, subPkts) 
	}

	override def childElementXML: NodeBuffer = {
		val nb = super.childElementXML
		if (null != error) nb += error.toXML
		nb
	}

	override def addAttributeToXML(node: Elem): Elem = {
		super.addAttributeToXML(node) % Attribute(None, "id", newTextAttr(id), 
			Attribute(None, "type", newTextAttr(msgType.toString), Null))
	}

	def nodeXML(childElementXML: NodeBuffer): Elem = <iq>{childElementXML}</iq> 

}

object IQ {

	object Type extends Enumeration { 
		val GET = Value("get")
		val SET = Value("set")
		val RESULT = Value("result")
		val ERROR = Value("error") 
	}

	def createResultIQ(req: IQ): IQ = {
		if (req.msgType != Type.GET && req.msgType != Type.SET)
			throw new IllegalArgumentException("IQ type must be 'set' or 'get'. " +
			"Original IQ: " + req.toXML)
		else
			new IQ(req.msgType, req.packetId, null, req.from, req.to, 
			null, null, null)
	}


	def createErrorResponse(req: IQ, error: XMPPError): IQ = {
		if (req.msgType != Type.GET && req.msgType != Type.SET)
			throw new IllegalArgumentException("IQ type must be 'set' or 'get'. " +
			"Original IQ: " + req.toXML)
		else
			new IQ(IQ.Type.ERROR, req.packetId, null, req.from, req.to, 
			error, null, null)
	}

}


class Bind(val resource: String) extends SubPacket {
	val elementName = "bind"
	val namespace = "urn:ietf:params:xml:ns:xmpp-bind"
	def toXML: Elem = <bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"><resource>{
		resource
	}</resource></bind>
}

class Session extends SubPacket {
	val elementName = "session"
	val namespace = "urn:ietf:params:xml:ns:xmpp-session"
	def toXML: Elem = <session xmlns="urn:ietf:params:xml:ns:xmpp-session"/>
}

class Query extends SubPacket {
	val elementName = "query"
	val namespace =	"jabber:iq:roster"
	def toXML: Elem = <query xmlns="jabber:iq:roster"></query>
}
