/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package com.bmskinner.nuclear_morphology.analysis.detection;

import java.util.HashMap;
import java.util.Set;

/**
 * Holds single stats. Used in the Detector to 
 * transfer CoM, area etc.
 *
 */
public class StatsMap {
	
	private HashMap<String, Double> values = new HashMap<String, Double>(0);
	
	public StatsMap(){
		
	}
	
	public StatsMap(StatsMap s){
		for(String key : s.keys()){
			this.add(key, new Double(s.get(key)));
		}
	}
	
	public Set<String> keys(){
		return this.values.keySet();
	}
	
	public void add(String s, Double d){
		if(s==null || d==null){
			throw new IllegalArgumentException("Argument is null");
		}
		values.put(s, d);
	}
	
	public Double get(String s){
		if(s==null){
			throw new IllegalArgumentException("Argument is null");
		}
		if(!values.containsKey(s)){
			throw new IllegalArgumentException("Key is not present");
		}
		return values.get(s);
	}

}
