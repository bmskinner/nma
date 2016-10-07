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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("serial")
public class Mean extends DescriptiveStatistic {
	
	public Mean(Number[] array){
		if(array == null || array.length==0){
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		}
		List<? extends Number> list = Arrays.asList(array);
		compareList(list);
	}
	
	public Mean(double[] array){
		if(array == null || array.length==0){
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		}
		List<Number> list = new ArrayList<Number>();
		for(double d : array){
			list.add(d);
		}
		compareList(list);

	}
		
	
	/**
	 * Calculate the maximum value in the given list
	 * @param list
	 */
	public Mean(List<? extends Number> list){
		if(list == null || list.isEmpty()){
			throw new IllegalArgumentException(NULL_OR_EMPTY_LIST_ERROR);
		}
		
		compareList(list);
	}
		
	private void compareList(List<? extends Number> list){
			    
	    if(list.size()==1){
	    	value = list.get(0);
	    }
	    
	    double sum = 0;
	    for(Number d : list){
	    	sum += d.doubleValue();
	    }
	    value = sum / list.size();
		
	}
}
