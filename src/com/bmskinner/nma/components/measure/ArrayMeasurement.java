package com.bmskinner.nma.components.measure;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;

/**
 * A measurement that is associated with an array of values instead of a single
 * value. e.g. image histograms
 * 
 */
public class ArrayMeasurement extends DefaultMeasurement {

	public ArrayMeasurement(String s, MeasurementDimension d) {
		super(s, d);
	}

	public ArrayMeasurement(Element e) {
		this(e.getAttributeValue(XMLNames.XML_NAME),
				MeasurementDimension.valueOf(e.getAttributeValue(XMLNames.XML_DIMENSION)));
	}

	@Override
	@NonNull
	public Element toXmlElement() {
		return new Element(XMLNames.XML_ARRAY_MEASUREMENT)
				.setAttribute(XMLNames.XML_NAME, this.name())
				.setAttribute(XMLNames.XML_DIMENSION, this.getDimension().toString());
	}

	@Override
	public boolean isArrayMeasurement() {
		return true;
	}
}
