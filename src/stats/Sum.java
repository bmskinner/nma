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

@SuppressWarnings("serial")
public class Sum extends DescriptiveStatistic {
	
	public Sum(int[] array){
		int c = 0;
		for(int i : array){
			c+=i;
		}
		value = c;
	}
	
	
	public Sum(double[] array){
		double c = 0;
		for(double i : array){
			c+=i;
		}
		value = c;
	}
	
	public Sum(float[] array){
		float c = 0;
		for(float i : array){
			c+=i;
		}
		value = c;
	}
	
	public Sum(long[] array){
		long c = 0;
		for(long i : array){
			c+=i;
		}
		value = c;
	}
	
	public Sum(List<? extends Number> list){
		
		Double d = 0d;
		for(Number n : list){
			d +=n.doubleValue();
		}
		value = d;
	}

}
