package com.bmskinner.nma.io;

import org.jdom2.Element;

/**
 * Interface implemented by all objects that can be serialized to XML
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public interface XmlSerializable {

	// HashOptions object keys
	String XML_OPTIONS = "Options";
	String XML_INTEGER_KEY = "Integer";
	String XML_FLOAT_KEY = "Float";
	String XML_DOUBLE_KEY = "Double";
	String XML_STRING_KEY = "String";
	String XML_BOOLEAN_KEY = "Boolean";
	String XML_SUBOPTION_KEY = "Suboption";

	// Core attribute keys
	String XML_NAME = "name";
	String XML_VALUE = "value";

	/**
	 * Create an XML representation of this object
	 * 
	 * @return the XML element
	 */
	Element toXmlElement();
}
