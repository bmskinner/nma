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

package charting.datasets;

import java.util.HashMap;
import java.util.Map;

import org.jfree.data.xy.DefaultXYDataset;

import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class NucleusOutlineDataset 
	extends DefaultXYDataset {
	
	Map<Integer, Nucleus> nuclei = new HashMap<Integer, Nucleus>();
		
	/**
	 * Set the nucleus for the given series
	 * @param i
	 * @param n
	 */
	public void setNucleus(int i, Nucleus n){
		nuclei.put(i, n);
	}
	
	/**
	 * Get the nucleus for the given series
	 * @param i
	 * @return
	 */
	public Nucleus getNucleus(int i){
		return nuclei.get(i);
	}
	

}
