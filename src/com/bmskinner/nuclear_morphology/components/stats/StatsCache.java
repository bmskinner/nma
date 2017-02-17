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

package com.bmskinner.nuclear_morphology.components.stats;

import java.util.HashMap;
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;

/**
 * Store plottable statistics for the collection
 * @author bms41
 * @since 1.13.4
 *
 */
public class StatsCache {
	
	
	// Need to be able to store the same stat for different components of the cell.
	// This requires keys on component and stat
	public class Key {

	    private final PlottableStatistic stat;
	    private final String component;
	    private final MeasurementScale scale;

	    public Key(PlottableStatistic stat, String component, MeasurementScale scale) {
			this.stat = stat;
			this.component = component;
			this.scale = scale;
		}

	    @Override
	    public boolean equals(Object o) {
	        if (this == o) return true;
	        if (!(o instanceof Key)) return false;
	        Key key = (Key) o;
	        
	        if(! stat.equals(key.stat)) return false;
	        
	        if(! component.equals(key.component)) return false;
	        
	        if(! scale.equals(key.scale)) return false;
	        
	        return true;
	    }

	    @Override
	    public int hashCode() {
	    	
	    	final int prime = 31;
			int result = 1;
			result = prime * result
					+ stat.hashCode();
			
			result = prime * result
					+ component.hashCode();
			
			result = prime * result
					+ scale.hashCode();

	        return result;
	    }

	}
	
	
	private Map<Key, Double> cache = new HashMap<Key, Double>();

	public StatsCache(){}

	/**
	 * Store the given statistic
	 * @param stat
	 * @param scale
	 * @param d
	 */
	public void setStatistic(PlottableStatistic stat, String component, MeasurementScale scale, double d){

		
		Key key = new Key(stat, component, scale);
		
		cache.put(key, d);

	}

	public double getStatistic(PlottableStatistic stat, String component, MeasurementScale scale){

		if(this.hasStatistic(stat, component, scale)){
			Key key = new Key(stat, component, scale);
			return cache.get(key);
		} else {
			return 0;
		}
		
	}

	public boolean hasStatistic(PlottableStatistic stat, String component, MeasurementScale scale){
		
		Key key = new Key(stat, component, scale);
		return cache.containsKey(key);

	}
}
