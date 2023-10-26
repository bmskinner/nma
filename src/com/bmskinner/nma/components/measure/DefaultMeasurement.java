/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.components.measure;

import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;

/**
 * Allows for arbitrary measurements to be stored with dimensionality
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultMeasurement implements Measurement {

	private final String name;
	private final MeasurementDimension dim;

	public DefaultMeasurement(String s, MeasurementDimension d) {
		name = s.intern();
		dim = d;
	}

	public DefaultMeasurement(Element e) {
		this(e.getAttributeValue(XMLNames.XML_NAME),
				MeasurementDimension.valueOf(e.getAttributeValue(XMLNames.XML_DIMENSION)));
	}

	@Override
	public Element toXmlElement() {
		return new Element(XMLNames.XML_MEASUREMENT)
				.setAttribute(XMLNames.XML_NAME, name)
				.setAttribute(XMLNames.XML_DIMENSION, dim.toString());
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public boolean isDimensionless() {
		return MeasurementDimension.NONE.equals(dim);
	}

	@Override
	public boolean isAngle() {
		return MeasurementDimension.ANGLE.equals(dim);
	}

	@Override
	public MeasurementDimension getDimension() {
		return dim;
	}

	@Override
	public String label(MeasurementScale scale) {

		StringBuilder b = new StringBuilder(name);

		if (!dim.equals(MeasurementDimension.NONE))
			b.append(" (")
					.append(units(scale))
					.append(")");

		return b.toString();
	}

	@Override
	public double convert(double value, double factor, MeasurementScale scale) {
		return Measurement.convert(value, factor, scale, dim);
	}

	@Override
	public String units(MeasurementScale scale) {
		return Measurement.units(scale, dim);
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dim == null) ? 0 : dim.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultMeasurement other = (DefaultMeasurement) obj;
		if (dim != other.dim)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
