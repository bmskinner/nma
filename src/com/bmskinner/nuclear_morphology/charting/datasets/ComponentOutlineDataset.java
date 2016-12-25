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

package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.HashMap;
import java.util.Map;

import org.jfree.data.xy.DefaultXYDataset;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

/**
 * Holds the outline of a cellular component
 * @author ben
 *
 * @param <E> the component class to be drawn
 */
@SuppressWarnings("serial")
public class ComponentOutlineDataset<E extends CellularComponent> 
	extends DefaultXYDataset {
	
	Map<Comparable, E> nuclei = new HashMap<Comparable, E>();
		
	/**
	 * Set the component for the given series
	 * @param i
	 * @param n
	 */
	public void setComponent(Comparable seriesKey, E n){
		nuclei.put(seriesKey, n);
	}
	
	/**
	 * Get the component for the given series
	 * @param i
	 * @return
	 */
	public E getComponent(Comparable seriesKey){
		return nuclei.get(seriesKey);
	}
	
	public boolean hasComponent(Comparable seriesKey){
		return nuclei.containsKey(seriesKey);
	}
	

}
