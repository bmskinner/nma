/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package stats;

import java.util.List;

/**
 * This is an OO replacement for the static  Stats.max(list) method
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class Max extends Number {
	
	private Number value;
	
	/**
	 * Calculate the maximum value of the two integers
	 * @param list
	 */
	public Max(int a, int b){
		value = a > b ? a : b;
	}
	
	/**
	 * Calculate the maximum value in the given list
	 * @param list
	 */
	public Max(List<? extends Number> list){
		
		if(list == null || list.isEmpty()){
			throw new IllegalArgumentException("List is empty in max");
		}
		
		Object firstEntry = list.get(0);
		
		if(firstEntry instanceof Double){
			value = compareDouble(list);
		}
		
		if(firstEntry instanceof Float){
			value = compareFloat(list);
		}
		
		if(firstEntry instanceof Integer){
			value = compareInt(list);
		}
		
		if(firstEntry instanceof Long){
			value = compareLong(list);
		}
	}

	@Override
	public double doubleValue() {
		return value.doubleValue();
	}

	@Override
	public float floatValue() {
		return value.floatValue();
	}

	@Override
	public int intValue() {
		return value.intValue();
	}

	@Override
	public long longValue() {
		return value.longValue();
	}
	
	private Number compareDouble(List<? extends Number> list){
		Number result = Double.MIN_VALUE; 
		for(Number n : list){
			if(n.doubleValue()>result.doubleValue()){
				result=n;
			}
		}
		return result;
	}

	private Number compareFloat(List<? extends Number> list){
		Number result = Float.MIN_VALUE; 
		for(Number n : list){
			if(n.floatValue()>result.floatValue()){
				result=n;
			}
		}
		return result;
	}
	
	private Number compareInt(List<? extends Number> list){
		Number result = Integer.MIN_VALUE; 
		for(Number n : list){
			if(n.intValue()>result.intValue()){
				result=n;
			}
		}
		return result;
	}
	
	private Number compareLong(List<? extends Number> list){
		Number result = Long.MIN_VALUE; 
		for(Number n : list){
			if(n.longValue()>result.longValue()){
				result=n;
			}
		}
		return result;
	}
}
