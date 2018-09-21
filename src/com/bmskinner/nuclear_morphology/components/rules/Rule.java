/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.rules;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An instruction for finding an index in a profile
 * 
 * @author bms41
 *
 */
public class Rule implements Serializable {

    private static final long  serialVersionUID = 1L;
    final private RuleType     type;
    final private List<Double> values           = new ArrayList<Double>(); // spare
                                                                           // field

    public Rule(RuleType type, double value) {

        this.type = type;
        this.values.add(value);
    }

    public Rule(RuleType type, boolean value) {

        this.type = type;
        double v = value ? 1d : 0d;
        this.values.add(v);
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

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(type + " : ");
        for (Double d : values) {
            b.append(d + " : ");
        }
        return b.toString();
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
