package org.openhab.binding.edimax.internal.commands;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openhab.binding.edimax.internal.ConnectionInformation;
import org.openhab.binding.edimax.internal.HTTPSend;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public abstract class AbstractCommand<T extends Object> {

	/**
	 * GET constructor.
	 */
	public AbstractCommand() {
		// default
	}

	/**
	 * SET constructor.
	 * 
	 * @param newValue
	 */
	public AbstractCommand(T newValue) {
		setValue = newValue;
	}

	protected String getCommandString() {
		List<String> list = getPath();
		StringBuffer command = new StringBuffer();
		command.append(XML_HEADER);
		recurseSubList(list, command);
		return command.toString();
	}

	protected void recurseSubList(List<String> list, StringBuffer command) {
		String element = list.get(0);

		if (list.size() == 1) {
			// basket case
			command.append(createLeafTag(element));
			return;
		}

		// usually.
		command.append(createStartTag(element));
		List<String> subList = list.subList(1, list.size());
		recurseSubList(subList, command);
		command.append(createEndTag(element));
	}

	/**
	 * Overwrite and add your path entry.
	 * 
	 * @return
	 */
	protected List<String> getPath() {
		ArrayList<String> list = new ArrayList<String>();
		list.add("SMARTPLUG");
		return list;
	}

	/**
	 * Returns XPath expression to load response for a get request.
	 * 
	 * @return
	 */
	protected String getXPathString() {
		List<String> list = getPath();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < list.size(); i++) {
			sb.append("/" + list.get(i));
		}
		sb.append("/text()");
		return sb.toString();
	}

	/**
	 * Format usually used for dates.
	 */
	private SimpleDateFormat format = new SimpleDateFormat("yyyyMMddkkmmss");

	protected T unmarshal(String result) {

		Type tType = extractType();

		if (tType instanceof Class) {
			Class<?> tempClass = (Class<?>) tType;
			if (tempClass == Date.class) {
				try {
					return (T) format.parse(result);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			} else if (tempClass == Boolean.class) {
				if ("ON".equals(result)) {
					return (T) Boolean.TRUE;
				} else if ("OFF".equals(result)) {
					return (T) Boolean.FALSE;
				}
			} else if (tempClass == Integer.class) {
				return (T) new Integer(result);
			} else if (tempClass == BigDecimal.class) {
				return (T) new BigDecimal(result);
			} else if (tempClass == String.class) {
				return (T) result;
			}
		}

		throw new RuntimeException("Type unknown " + tType);
	}

	protected Type extractType() {
		Type mySuperclass = this.getClass().getGenericSuperclass();
		Type[] arr = ((ParameterizedType) mySuperclass).getActualTypeArguments();
		Type tType = arr[0];
		return tType;
	}

	protected T setValue;

	protected String marshal(T value) {
		return value.toString();
	}

	protected T getResultValue(String aResponse) {
		try {
			String result = extractValueFromXML(aResponse, getXPathString());
			return unmarshal(result);
		} catch (XPathExpressionException | ParserConfigurationException | SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Returns true if it is a SET command and false for GET.
	 * 
	 * @return
	 */
	protected boolean isSet() {
		return (setValue != null);
	}

	public T executeCommand(ConnectionInformation ci) throws IOException {
		String lastPart = "smartplug.cgi";
		String response = HTTPSend.executePost(ci.getUrl(), ci.getPort(), lastPart, getCommandString(),
				ci.getUsername(), ci.getPassword());

		return getResultValue(response);
	}

	protected String createStartTag(String aName) {
		StringBuffer res = new StringBuffer();
		res.append("<");
		res.append(aName);
		if ("SMARTPLUG".equals(aName)) {
			res.append(" id=\"edimax\"");
		} else if ("CMD".equals(aName)) {
			if (isSet()) {
				res.append(" id=\"setup\"");
			} else {
				res.append(" id=\"get\"");
			}
		}
		res.append(">");

		return res.toString();
	}

	protected String createEndTag(String aName) {
		return "</" + aName + ">";
	}

	protected String createLeafTag(String aName) {
		if (isSet()) {
			StringBuffer sb = new StringBuffer();
			sb.append(createStartTag(aName));
			sb.append(marshal(setValue));
			sb.append(createEndTag(aName));
			return sb.toString();
		} else {
			return "<" + aName + "/>";
		}
	}

	/**
	 * HELPER
	 */
	public static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF8\"?>\r\n";

	protected static final DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();

	protected String extractValueFromXML(String document, String xpathExpression) throws ParserConfigurationException,
			SAXException, IOException, XPathExpressionException {
		DocumentBuilder builder = domFactory.newDocumentBuilder();
		ByteArrayInputStream tempIS = new ByteArrayInputStream(document.getBytes());
		Document dDoc = builder.parse(tempIS);

		XPath xPath = XPathFactory.newInstance().newXPath();
		Node node = (Node) xPath.evaluate(xpathExpression, dDoc, XPathConstants.NODE);
		return node.getNodeValue();
	}

}
