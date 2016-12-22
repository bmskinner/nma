package com.bmskinner.nuclear_morphology.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * Calculate the standard deviation of a list of numbers
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class StDev extends DescriptiveStatistic {
	
	public StDev(Number[] array){
		if(array == null || array.length==0){
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		}
		List<? extends Number> list = Arrays.asList(array);
		compareList(list);
	}
	
	public StDev(double[] array){
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
	 * Calculate the mean value in the given list
	 * @param list
	 */
	public StDev(List<? extends Number> list){
		if(list == null || list.isEmpty()){
			throw new IllegalArgumentException(NULL_OR_EMPTY_LIST_ERROR);
		}
		
		compareList(list);
	}
		
	private void compareList(List<? extends Number> list){
			 
		if(list.size()==0){
			throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		}
		
	    if(list.size()<2){
	    	value = 0;
	    	return;
	    }
	    
	   
	  value =  Math.sqrt( new Variance(list).doubleValue());
		
	}

}
