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

import java.util.Arrays;
import java.util.List;

import utility.ArrayConverter;
import utility.ArrayConverter.ArrayConversionException;

@SuppressWarnings("serial")
public class Quartile extends DescriptiveStatistic {
	
	public static final int LOWER_QUARTILE = 25;
	public static final int UPPER_QUARTILE = 75;
	public static final int MEDIAN         = 50;
	
	public Quartile(List<Number> values, double lowerPercent) {
		  
		  Number[] array = values.toArray(new Number[0]);
		  value = new Quartile(array, lowerPercent);

	  }
	  
	  
	  /*
	    Calculate the <lowerPercent> quartile from a Double[] array
	  */
	  public Quartile(double[] values, double lowerPercent) {

		  Double[] temp2;
			try {
				temp2 = new ArrayConverter(values).toDoubleObjectArray();
			} catch (ArrayConversionException e) {
				temp2 = new Double[0]; 
			}
			
		  value = new Quartile(temp2, lowerPercent);

	  }
	  
	  /**
	   * Calculate the given quartile for an array of values
	   * @param values
	   * @param lowerPercent
	   * @return
	   */
	  public Quartile(Number[] values, double lowerPercent) {
		  if (values == null || values.length == 0) {
			  throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
		  }

		  if(values.length==1){
			  value =  values[0];
		  }

		  // Rank order the values
		  Number[] v = new Number[values.length];
		  System.arraycopy(values, 0, v, 0, values.length);
		  Arrays.sort(v);

		  int n = (int) Math.round(v.length * lowerPercent / 100);

		  value =  v[n];
	  }

}
