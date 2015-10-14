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
package components.generic;

import utility.Utils;
import ij.IJ;

/**
 * Use to hold boolean results from a Profile
 * - for example, local minima or maxima. 
 *
 */
public class BooleanProfile {
	
	protected boolean[] array;
	
	/**
	 * Constructor based on an array of values.
	 * @param values the array to use
	 */
	public BooleanProfile(boolean[] values){

		this.array = new boolean[values.length];
		for(int i=0; i<this.array.length; i++){
			array[i] = values[i];
		}
	}
	
	/**
	 * Constructor based on an existing Profile. Makes a copy 
	 * of the existing Profile
	 * @param p the profile to copy
	 */
	public BooleanProfile(BooleanProfile p){
		
		this.array = new boolean[p.size()];
		for(int i=0; i<this.array.length; i++){
			array[i] = p.get(i);
		}
	}
	
	/**
	 * Get the length of the array in the profile
	 * @return the size of the profile
	 */
	public int size(){
		return array.length;
	}
	
	/**
	 * Get the value at the given index
	 * @param index the index
	 * @return the value at the index
	 */
	public boolean get(int index){
		boolean result = false;

		try {
			if(index>=array.length){
				throw new Exception("Requested value "+index+" is beyond profile end");
			}
			result = this.array[index];
		} catch(Exception e){
			IJ.log("Cannot get value from profile: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		}
		return result;
	}
	
	/**
	 * Get the array from the profile
	 * @return an array of values
	 */
	public boolean[] asArray(){
		return this.array;
	}
	
	/**
	 * Get an X-axis; get a position
	 * for each point on the scale 0-<length>
	 * @param length the length to scale to
	 * @return a profile with the positions as values
	 */
	public Profile getPositions(int length){
		double [] result = new double[array.length];
		for(int i=0;i<array.length;i++){
			result[i] = (double) i / (double) array.length * (double) length;
		}
		return new Profile(result);
	}
	
	/**
	 * Copy the profile and offset it to start from the given index
	 * @param j the index to start from
	 * @return a new offset BooleanProfile
	 * @throws Exception 
	 */
	public BooleanProfile offset(int j) throws Exception{
		boolean[] newArray = new boolean[this.size()];
		for(int i=0;i<this.size();i++){
			newArray[i] = this.array[ Utils.wrapIndex( i+j , this.size() ) ];
		}
		return new BooleanProfile(newArray);
	}
	
	  /**
	   * Returns true at each position if either profile is true at that position
	   * @param adder the profile to compare. Must be the same length as this profile
	   * @return the new profile
	   */
	  public BooleanProfile or(BooleanProfile profile){
		  if(this.size()!=profile.size()){
			  throw new IllegalArgumentException("Profile sizes do not match");
		  }
		  boolean[] result = new boolean[this.size()];

		  for (int i=0; i<array.length; i++) { 
			  result[i] = array[i] || profile.get(i);
		  }
		  return new BooleanProfile(result);
	  }
	  
	  /**
	   * Returns true at each position if both profiles are true at that position
	   * @param adder the profile to compare. Must be the same length as this profile
	   * @return the new profile
	   */
	  public BooleanProfile and(BooleanProfile profile){
		  if(this.size()!=profile.size()){
			  throw new IllegalArgumentException("Profile sizes do not match");
		  }
		  boolean[] result = new boolean[this.size()];

		  for (int i=0; i<array.length; i++) { 
			  result[i] = array[i] && profile.get(i);
		  }
		  return new BooleanProfile(result);
	  }
	
	
}
