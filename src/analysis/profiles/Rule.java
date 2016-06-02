/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.profiles;

import java.util.ArrayList;
import java.util.List;


/**
 * An instruction for finding an index in a profile
 * @author bms41
 *
 */
public class Rule {

	final private RuleType type;
	final private List<Double> values = new ArrayList<Double>(); // spare field

	public Rule(RuleType type, double value){

		this.type = type;
		this.values.add(value);
	}

	public Rule(RuleType type, boolean value){

		this.type = type;
		double v = value ? 1d : 0d;
		this.values.add(v);
	} 
	
	public void addValue(double d){
		values.add(d);
	}

	/**
	 * Get the first value in the rule
	 * @return
	 */
	public double getValue(){
		return values.get(0);
	}
	
	public double getValue(int index){
		return values.get(index);
	}

	public boolean getBooleanValue(){
		return getBooleanValue(0);
	}
	
	public boolean getBooleanValue(int index){
		if(values.get(index)==1d){
			return true;
		} else {
			return false;
		}
	}
	
	public RuleType getType(){
		return type;
	}
	
	public String toString(){
		StringBuilder b = new StringBuilder();
		b.append(type + " : ");
		for(Double d : values){
			b.append(d+" : ");
		}
		return b.toString();
	}
	
	/**
	 * A type of instruction to follow
	 * @author bms41
	 *
	 */
	public enum RuleType{

		IS_MINIMUM,
		IS_MAXIMUM,
		
		IS_LOCAL_MINIMUM,
		IS_LOCAL_MAXIMUM,

		VALUE_IS_LESS_THAN,
		VALUE_IS_MORE_THAN,
		
		INDEX_IS_LESS_THAN,
		INDEX_IS_MORE_THAN,
		
		IS_CONSTANT_REGION,
		
		FIRST_TRUE,
		LAST_TRUE;

	}
}