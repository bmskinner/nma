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

	/**
	 * Create an XML representation of this object
	 * 
	 * @return the XML element
	 */
	Element toXmlElement();
}
