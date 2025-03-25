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
package com.bmskinner.nma.components.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * An instruction for finding an index in a profile
 * 
 * @author Ben Skinner
 *
 */
public class Rule implements XmlSerializable {

	private final RuleType type;

	private final List<Double> values = new ArrayList<>();

	public Rule(RuleType type, double value) {

		this.type = type;
		this.values.add(value);
	}

	public Rule(RuleType type, boolean value) {

		this.type = type;
		double v = value ? 1d : 0d;
		this.values.add(v);
	}

	protected Rule(Rule r) {
		type = r.type;
		for (Double d : r.values)
			values.add(d.doubleValue());
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 */
	public Rule(@NonNull Element e) {
		RuleType rt = RuleType.valueOf(e.getAttributeValue(XMLNames.XML_RULE_TYPE));
		this.type = rt;

		for (Element c : e.getChildren(XMLNames.XML_RULE_VALUE)) {
			addValue(Double.parseDouble(c.getValue()));
		}
	}

	public Rule duplicate() {
		return new Rule(this);
	}

	public void addValue(double d) {
		values.add(d);
	}

	/**
	 * Get the first value in the rule
	 * 
	 * @return
	 */
	public double getValue() {
		return values.get(0);
	}

	public double getValue(int index) {
		return values.get(index);
	}

	public int valueCount() {
		return values.size();
	}

	public boolean getBooleanValue() {
		return getBooleanValue(0);
	}

	public boolean getBooleanValue(int index) {
		return (values.get(index) == 1d);
	}

	public RuleType getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(type);
		b.append(" [parameters: ");

		String params = IntStream.range(0, values.size()).mapToObj(i -> {
			return type.isBoolean(i) ? String.valueOf(getBooleanValue(i))
					: String.valueOf(getValue(i));
		}).collect(Collectors.joining(", "));

		b.append(params + "]");
		return b.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, values);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Rule other = (Rule) obj;
		return type == other.type && Objects.equals(values, other.values);
	}

	@Override
	public Element toXmlElement() {

		Element e = new Element(XMLNames.XML_RULE).setAttribute(XMLNames.XML_RULE_TYPE,
				getType().toString());

		for (int i = 0; i < valueCount(); i++) {
			e.addContent(new Element(XMLNames.XML_RULE_VALUE).setText(String.valueOf(getValue(i))));
		}
		return e;
	}

	/**
	 * A type of instruction to follow
	 * 
	 * @author Ben Skinner
	 *
	 */
	public enum RuleType {

		/**
		 * If 1, finds the point at the first index of a profile. If 0, finds all points
		 * except the first index
		 */
		IS_ZERO_INDEX(true),

		/**
		 * If 1, finds the minimum point in a profile. If 0, finds all points except the
		 * minimum
		 */
		IS_MINIMUM(true),

		/**
		 * If 1, finds the maximum point in a profile. If 0, finds all points except the
		 * maximum
		 */
		IS_MAXIMUM(true),

		/**
		 * If the first value is 1, finds local minima in a profile. If 0, finds all
		 * points except the local minima . A window size is needed to find minima and
		 * maxima; this is provided in the second value as an integer
		 */
		IS_LOCAL_MINIMUM(true, false),

		/**
		 * If the first value is 1, finds local maxima in a profile. If 0, finds all
		 * points except the local maxima. A window size is needed to find minima and
		 * maxima; this is provided in the second value as an integer
		 */
		IS_LOCAL_MAXIMUM(true, false),

		/**
		 * Finds points that are less than the given absolute value
		 */
		VALUE_IS_LESS_THAN(false),

		/**
		 * Finds points that are more than the given absolute value
		 */
		VALUE_IS_MORE_THAN(false),

		/**
		 * Finds points that have an index in their profile lower than the value
		 */
		INDEX_IS_LESS_THAN(false),

		/**
		 * Finds points that have an index in their profile higher than the value
		 */
		INDEX_IS_MORE_THAN(false),

		/**
		 * Finds regions with a constant value over at least a given number of indexes.
		 * The stringency of the constant calue can be altered.
		 * 
		 */
		IS_CONSTANT_REGION(false, false, false),

		/**
		 * Designed to be applied after a previous rule. If 1, finds the first index
		 * matching the rule conditions. If 0, finds all except the first index matching
		 * the rule conditions
		 */
		FIRST_TRUE(true),

		/**
		 * Designed to be applied after a previous rule. If 1, finds the last index
		 * matching the rule conditions. If 0, finds all except the last index matching
		 * the rule conditions
		 */
		LAST_TRUE(true),

		/**
		 * Finds indexes that are within the given fraction of the profile
		 * 
		 */
		INDEX_IS_WITHIN_FRACTION_OF(false),

		/**
		 * Finds indexes that areoutside the given fraction of the profile
		 * 
		 */
		INDEX_IS_OUTSIDE_FRACTION_OF(false),

		/**
		 * Inverts the boolean profile state
		 */
		INVERT();

		private boolean[] isBoolean;

		/**
		 * Create with an array indicating whcih options are booleans disguised as
		 * doubles
		 * 
		 * @param options an array, each element of which is a boolean
		 */
		RuleType(boolean... options) {
			this.isBoolean = options;
		}

		public boolean isBoolean(int index) {
			return isBoolean[index];
		}

	}
}
