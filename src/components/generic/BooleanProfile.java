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
