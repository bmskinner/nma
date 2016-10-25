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

package utility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This utility class handles conversions of arrays from
 * primitives to Objects
 * @author bms41
 *
 */
public class ArrayConverter {
	
	List<Object> data = new ArrayList<Object>();
	
	public ArrayConverter(int[] d){
		
		for(Object o : d){
			data.add(o);
		}
		
	}
	
	public ArrayConverter(float[] d){

		for(float o : d){
			data.add(o);
		}

	}
	
	public ArrayConverter(Integer[] d){

		for(Object o : d){
			data.add(o);
		}
	}

	public ArrayConverter(Double[] d){

		for(Object o : d){
			data.add(o);
		}
	}

	public ArrayConverter(double[] d){

		for(Object o : d){
			data.add(o);
		}
	}

	public ArrayConverter(Collection<? extends Object> d){
		
		for(Object o : d){
			data.add(o);
		}
		
	}
	
	/**
	 * Convert the given data to a Double[]
	 * @return
	 * @throws ArrayConversionException if the data are not Numbers
	 */
	public Double[] toDoubleObjectArray() throws ArrayConversionException {
		
		Object first = data.get(0);
		
		if(first instanceof Number){
			
			Double[] result = new Double[data.size()];
			
			for(int i=0; i<data.size(); i++){
				result[i] = ( (Number) data.get(i)).doubleValue();
			}
			return result;
			
		} else {
			throw new ArrayConversionException("Data is not a number");
		}
		
	}
	
	/**
	 * Convert the given data to a double[]
	 * @return
	 * @throws ArrayConversionException if the data are not Numbers
	 */
	public double[] toDoubleArray() throws ArrayConversionException {
		Object first = data.get(0);
		
		if(first instanceof Number){
			
			double[] result = new double[data.size()];
			
			for(int i=0; i<data.size(); i++){
				result[i] = ( (Number) data.get(i)).doubleValue();
			}
			return result;
			
		} else {
			throw new ArrayConversionException("Data is not a number");
		}
	}
	
	/**
	 * Convert the given data to a String[]
	 * @return
	 */
	public String[] toStringArray() {

		String[] result = new String[data.size()];

		for(int i=0; i<data.size(); i++){
			result[i] = data.get(i).toString();
		}
		return result;

	}

	
	/**
	 * Convert the given data to a Integer[]
	 * @return
	 * @throws ArrayConversionException if the data are not Numbers
	 */
	public Integer[] toIntegerObjectArray() throws ArrayConversionException {
		
		Object first = data.get(0);
		
		if(first instanceof Number){
			
			Integer[] result = new Integer[data.size()];
			
			for(int i=0; i<data.size(); i++){
				result[i] = ( (Number) data.get(i)).intValue();
			}
			return result;
			
		} else {
			throw new ArrayConversionException("Data is not a number");
		}
		
	}
	
	/**
	 * Convert the given data to a int[]
	 * @return
	 * @throws ArrayConversionException if the data are not Numbers
	 */
	public int[] toIntArray() throws ArrayConversionException {
		Object first = data.get(0);
		
		if(first instanceof Number){
			
			int[] result = new int[data.size()];
			
			for(int i=0; i<data.size(); i++){
				result[i] = ( (Number) data.get(i)).intValue();
			}
			return result;
			
		} else {
			throw new ArrayConversionException("Data is not a number");
		}
	}
	
	/**
	 * Convert the given data to a Integer[]
	 * @return
	 * @throws ArrayConversionException if the data are not Numbers
	 */
	public Float[] toFloatObjectArray() throws ArrayConversionException {
		
		Object first = data.get(0);
		
		if(first instanceof Number){
			
			Float[] result = new Float[data.size()];
			
			for(int i=0; i<data.size(); i++){
				result[i] = ( (Number) data.get(i)).floatValue();
			}
			return result;
			
		} else {
			throw new ArrayConversionException("Data is not a number");
		}
		
	}
	
	/**
	 * Convert the given data to a int[]
	 * @return
	 * @throws ArrayConversionException if the data are not Numbers
	 */
	public float[] toFloatArray() throws ArrayConversionException {
		Object first = data.get(0);
		
		if(first instanceof Number){
			
			float[] result = new float[data.size()];
			
			for(int i=0; i<data.size(); i++){
				result[i] = ( (Number) data.get(i)).intValue();
			}
			return result;
			
		} else {
			throw new ArrayConversionException("Data is not a number");
		}
	}
	
	public List<Double> toDoubleList() throws ArrayConversionException{
		
		List<Double> result = new ArrayList<Double>();
		
		Double[] arr = this.toDoubleObjectArray();
		
		for(Double d : arr){
			result.add(d);
		}
		return result;
		
	}

	public List<Integer> toIntegerList() throws ArrayConversionException{
		
		List<Integer> result = new ArrayList<Integer>();
		
		Integer[] arr = this.toIntegerObjectArray();
		
		for(Integer d : arr){
			result.add(d);
		}
		return result;
		
	}
	
	/**
	 * Convert the data in this converter to a tab delimited string
	 * @return
	 */
	public String toString(){
		return toString("\t");
	}
	
	/**
	 * Convert the data in this converter to a delimited string
	 * @param delimiter the separator between values
	 * @return
	 */
	public String toString(String delimiter){
		StringBuilder b = new StringBuilder();
		for(Object o : data){
			b.append(o.toString()+delimiter);
		}
		return b.toString();
	}

	 @SuppressWarnings("serial")
	 public class ArrayConversionException extends Exception {

		 public ArrayConversionException() { super(); }
		 public ArrayConversionException(String message) { super(message); }
		 public ArrayConversionException(String message, Throwable cause) { super(message, cause); }
		 public ArrayConversionException(Throwable cause) { super(cause); }
	 }

}
