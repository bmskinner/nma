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
package com.bmskinner.nuclear_morphology.components.rules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * An instruction for finding an index in a profile
 * 
 * @author bms41
 *
 */
public class Rule implements Serializable, XmlSerializable {

    private static final String XML_VALUE = "Value";

	private static final String XML_RULE = "Rule";

	private static final String XML_TYPE = "type";

	private static final long  serialVersionUID = 1L;
    
    private final RuleType     type;

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
    
    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     */
    public Rule(@NonNull Element e) {
    	RuleType rt = RuleType.valueOf(e.getAttributeValue(XML_TYPE));
		this.type = rt;
        
		for(Element c : e.getChildren(XML_VALUE)) {
			addValue(Double.parseDouble(c.getValue()));
		}
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
        return(values.get(index) == 1d);
    }

    public RuleType getType() {
        return type;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(type);
        for (Double d : values) {
            b.append(" : "+ d);
        }
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

		Element e = new Element(XML_RULE).setAttribute(XML_TYPE, getType().toString());
		
		for(int i=0; i<valueCount(); i++) {
			e.addContent(new Element(XML_VALUE).setText(String.valueOf(getValue(i))));
		}
		return e;
	}

	/**
     * A type of instruction to follow
     * 
     * @author bms41
     *
     */
    public enum RuleType {
    	
    	IS_ZERO_INDEX,

        IS_MINIMUM, IS_MAXIMUM,

        IS_LOCAL_MINIMUM, IS_LOCAL_MAXIMUM,

        VALUE_IS_LESS_THAN, VALUE_IS_MORE_THAN,

        INDEX_IS_LESS_THAN, INDEX_IS_MORE_THAN,

        IS_CONSTANT_REGION,

        FIRST_TRUE, LAST_TRUE,

        INDEX_IS_WITHIN_FRACTION_OF, INDEX_IS_OUTSIDE_FRACTION_OF,

        INVERT;
    	

    }
}
