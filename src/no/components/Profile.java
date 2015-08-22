package no.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utility.Utils;
import ij.IJ;

/**
 * Holds arrays of values with wrapping and provides
 * methods to manipulate them. Used for distance and angle
 * profiles.
 */
/**
 * @author bms41
 *
 */
public class Profile implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected double[] array;
	private static final int ARRAY_BEFORE = -1;
	private static final int ARRAY_AFTER = 1;


	
	/**
	 * Constructor for a new Profile, based on an array of values.
	 * @param values the array to use
	 */
	public Profile(double[] values){

		this.array = new double[values.length];
		for(int i=0; i<this.array.length; i++){
			array[i] = values[i];
		}
	}

	/**
	 * Constructor based on an existing Profile. Makes a copy 
	 * of the existing Profile
	 * @param p the profile to copy
	 */
	public Profile(Profile p){

		this.array = new double[p.size()];
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
	 * Test if the values in this profile are the same 
	 * as in the test profile (and have the same position)
	 * @param test the profile to test
	 * @return
	 */
	public boolean equals(Profile test){
		if(test==null){
			return false;
		}
		if(test.size()!=this.size()){
			return false;
		}
		
		for(int i=0;i<this.size();i++){
			if(this.get(i)!=test.get(i)){
				return false;
			}
		}
		return true;
	}

	
	/**
	 * Get the value at the given index
	 * @param index the index
	 * @return the value at the index
	 */
	public double get(int index){
		double result = 0;

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
	 * Get the maximum value in the profile
	 * @return the maximum value
	 */
	public double getMax(){
		double max = 0;
		for(int i=0; i<array.length;i++){
			if(array[i]>max){
				max = array[i];
			}
		}
		return max;
	}

	/**
	 * Get the index of the maximum value in the profile
	 * If there are multiple values at maximum, this returns the
	 * first only
	 * @return the index
	 */
	public int getIndexOfMax(){
		double max = 0;
		int maxIndex = 0;
		for(int i=0; i<array.length;i++){
			if(array[i]>max){
				max = array[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	/**
	 * Get the minimum value in the profile.If there are multiple values 
	 * at minimum, this returns the first only
	 * @return the minimum value
	 */
	public double getMin(){
		double min = this.getMax();
		for(int i=0; i<array.length;i++){
			if(array[i]<min){
				min = array[i];
			}
		}
		return min;
	}

	/**
	 * Get the index of the minimum value in the profile
	 * @return the index
	 */
	public int getIndexOfMin(){
		double min = this.getMax();
		int minIndex = 0;
		for(int i=0; i<array.length;i++){
			if(array[i]<min){
				min = array[i];
				minIndex = i;
			}
		}
		return minIndex;
	}

	/**
	 * Get the array from the profile
	 * @return an array of values
	 */
	public double[] asArray(){
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
	 * Check the lengths of the two profiles. Return the first profile
	 * interpolated to the length of the longer.
	 * @param profile1 the profile to return interpolated
	 * @param profile2 the profile to compare
	 * @return a new profile with the length of the longest input profile
	 */
	private Profile equaliseLengths(Profile profile1, Profile profile2){

		try{
			// profile 2 is smaller
			// return profile 1 unchanged
			if(profile2.size() < profile1.size() ){
				return profile1;
			} else {
				// profile 1 is smaller; interpolate to profile 2 length
				profile1 = profile1.interpolate(profile2.size());
			}
		} catch(Exception e){
			IJ.log("Error interpolating profiles: "+e.getMessage());
			IJ.log("Profile 1: ");
			profile1.print();
			IJ.log("Profile 2: ");
			profile2.print();
		}
		return profile1;
	}

	/**
	 * Calculate the square differences between this profile and
	 * a given profile. The shorter profile is interpolated.
	 * The testProfile must have been offset appropriately to avoid 
	 * spurious differences.
	 * @param testProfile the profile to compare to 
	 * @return the sum-of-squares difference
	 */
	public double absoluteSquareDifference(Profile testProfile){

		// the test profile needs to be matched to this profile
		// whichever is smaller must be interpolated 
		Profile profile1 = equaliseLengths(this.copy(), testProfile);
		Profile profile2 = equaliseLengths(testProfile, this.copy());

		double difference = 0;

		for(int j=0; j<profile1.size(); j++){ // for each point round the array

			double thisValue = profile1.get(j);
			double testValue = profile2.get(j);
			difference += Math.pow(thisValue - testValue, 2); // square difference - highlights extremes
		}
		return difference;
	}
	
	/**
	 * Calculate the sum of squares difference between this profile and
	 * a given profile. Unlike the absolute difference, this value is weighted
	 * to the difference from 180 degrees. That is, a difference of 5 degrees at
	 * 170 (to 175) will count less than a difference of 5 degrees at 30 (to 35).
	 * This promotes differences at regions of profile maxima, and minimises them at
	 * constant straight regions.
	 * @param testProfile the profile to compare
	 * @return
	 */
	public double weightedSquareDifference(Profile testProfile){
		
		if(testProfile==null){
			throw new IllegalArgumentException("Test profile is null");
		}
		
		// Ensure both profiles have the same length, to allow
		// point by point comparisons. The shorter is interpolated.
		Profile profile1 = equaliseLengths(this.copy(), testProfile);
		Profile profile2 = equaliseLengths(testProfile, this.copy());
		
		double result = 0;
		
		for(int j=0; j<profile1.size(); j++){ // for each point round the array

			double value1 = profile1.get(j);
			double value2 = profile2.get(j);
						
			// get the difference away from 180 degrees for the test profile
			double normalised2 = Math.abs(  value2 - 180  );
			
			/*
				Set the weighting to 1/180 multiplied by the difference	of the 
				test profile to 180. Hence, a difference of 180 degrees from
				the 180 degree baseline will get a weighting of 1, and a difference 
				of 0 degrees from the 180 degree baseline will get a weighting of 0
				(i.e. does not count if it is perfectly straight)
			*/
			double weight = (double) ( 1.0/180.0) * normalised2;
			
			// the difference between the two profiles at this point
			double difference = value1 - value2;
			
			// apply the weighting
			double weightedDifference = difference * weight;
			
			// add the square difference - highlights extremes
			result += Math.pow(weightedDifference, 2); 
		}
		
		return result;
	}

  /*
    --------------------
    Profile manipulation
    --------------------
  */

	/**
	 * Alternative to the constructor from profile
	 * @return a new profile with the same values as this
	 */
	public Profile copy(){
		return new Profile(this.array);
	}

	/**
	 * Copy the profile and offset it to start from the given index
	 * @param j the index to start from
	 * @return a new offset Profile
	 * @throws Exception 
	 */
	public Profile offset(int j) throws Exception{
		double[] newArray = new double[this.size()];
		for(int i=0;i<this.size();i++){
			newArray[i] = this.array[ Utils.wrapIndex( i+j , this.size() ) ];
		}
		return new Profile(newArray);
	}
  
	/**
	 * Perform a window-averaging smooth of the profile with the given window size
	 * @param windowSize the size of the window
	 * @return
	 */
	public Profile smooth(int windowSize){

		double[] result = new double[this.size()];

		for (int i=0; i<array.length; i++) { // for each position

			double[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
			double[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

			double average = array[i];
			for(int k=0;k<prevValues.length;k++){ 
				average += prevValues[k] + nextValues[k];	        
			}

			result[i] = average / (windowSize*2 + 1);
		}
		return new Profile(result);
	}
  
	/**
	 * Get an array of the values <windowSize> before or after the current point
	 * @param position the position in the array
	 * @param windowSize the number of points to find
	 * @param type find points before or after
	 * @return an array of values
	 */
	private double[] getValues(int position, int windowSize, int type){

		double[] values = new double[windowSize]; // slots for previous angles
		for(int j=0;j<values.length;j++){

			// If type was before, multiply by -1; if after, multiply by 1
			int index = Utils.wrapIndex( position + ((j+1)*type)  , this.size() );
			values[j] = array[index];
		}
		return values;
	}

	/**
	 * Reverse the profile. Does not copy.
	 */
	public void reverse(){

		double tmp;
		for (int i = 0; i < this.array.length / 2; i++) {
			tmp = this.array[i];
			this.array[i] = this.array[this.array.length - 1 - i];
			this.array[this.array.length - 1 - i] = tmp;
		}
	}  

  /**
   * Make this profile the length specified.
   * @param newLength the new array length
   * @return an interpolated profile
   */
  public Profile interpolate(int newLength) {

    if(newLength < this.size()){
    	System.out.println("Interpolating to a smaller array!");
//       throw new Exception("Cannot interpolate to a smaller array!");
    }
    
    double[] newArray = new double[newLength];
    
    // where in the old curve index is the new curve index?
    for (int i=0; i<newLength; i++) {
      // we have a point in the new curve.
      // we want to know which points it lay between in the old curve
      double oldIndex = ( (double)i / (double)newLength) * (double) array.length; // get the fractional index position needed
      
      // get the value in the old profile at the given fractional index position
      newArray[i] = interpolateValue(oldIndex);
    }
    return new Profile(newArray);
  }

  /**
   * Take an index position from a non-normalised profile. Normalise it
   * Find the corresponding angle in the median curve.
   * Interpolate as needed
   * @param normIndex the fractional index position to find within this profile
   * @return an interpolated value
   */
  private double interpolateValue(double normIndex){

	  // convert index to 1 window boundaries
	  // This allows us to see the array indexes above and below the desired
	  // fractional index. From these, we can interpolate the fractional component.
	  // NOTE: this does not account for curves. Interpolation is linear.
	  int index1 = (int) Math.round(normIndex);
	  int index2 	= index1 > normIndex
			  		? index1 - 1
					: index1 + 1;
	  
//	  System.out.println("Index selection "+normIndex+": "+index1+" and "+index2);

	  // Decide which of the two indexes is the higher, and which is the lower
	  int indexLower 	= index1 < index2
			  			? index1
			  			: index2;

	  int indexHigher 	= index2 > index1
			  			? index2
			  			: index1;
	  
//	  System.out.println("Set indexes "+normIndex+": "+indexLower+"-"+indexHigher);

	  // wrap the arrays
	  indexLower  = Utils.wrapIndex(indexLower , this.size());
	  indexHigher = Utils.wrapIndex(indexHigher, this.size());
//	  System.out.println("Wrapped indexes "+normIndex+": "+indexLower+"-"+indexHigher);

	  // get the values at these indexes
	  double valueHigher = array[ indexHigher ];
	  double valueLower  = array[ indexLower  ];
//	  System.out.println("Wrapped values "+normIndex+": "+valueLower+" and "+valueHigher);

	  // calculate the difference between values
	  // this can be negative
	  double valueDifference = valueHigher - valueLower;
//	  System.out.println("Difference "+normIndex+": "+valueDifference);
	  
	  // calculate the distance into the region to go
	  double offset = normIndex - indexLower;
//	  System.out.println("Offset "+normIndex+": "+offset);
	  

	  // add the offset to the lower index
	  double positionToFind = indexLower + offset;
	  positionToFind = Utils.wrapIndex(positionToFind , this.size());
//	  System.out.println("Position to find "+normIndex+": "+positionToFind);
	  
	  // calculate the value to be added to the lower index value
	  double newValue = valueDifference * offset; // 0 for 0, full difference for 1
//	  System.out.println("New value "+normIndex+": "+newValue);
	  
	  double linearInterpolatedValue = newValue + valueLower;
//	  System.out.println("Interpolated "+normIndex+": "+linearInterpolatedValue);

	  return linearInterpolatedValue;
  }

  /*
    Interpolate another profile to match this, and move this profile
    along it one index at a time. Find the point of least difference, 
    and return this offset. Returns the positive offset to this profile
   */
  public int getSlidingWindowOffset(Profile testProfile) throws Exception {

	  double lowestScore = this.absoluteSquareDifference(testProfile);
	  int index = 0;
	  for(int i=0;i<this.size();i++){

		  Profile offsetProfile = this.offset(i);

		  double score = offsetProfile.absoluteSquareDifference(testProfile);
		  if(score<lowestScore){
			  lowestScore=score;
			  index=i;
		  }

	  }
	  return index;
  }


  /*
    --------------------
    Detect minima within the profiles
    --------------------
  */

  /*
    For each point in the array, test for a local minimum.
    The values of the points <minimaLookupDistance> ahead and behind are checked.
    Each should be greater than the value before.
    One exception is allowed, to account for noisy data. Returns the indexes of minima
  */
  public Profile getLocalMinima(int windowSize){
    // go through angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all greater, local minimum
    double[] prevValues = new double[windowSize]; // slots for previous angles
    double[] nextValues = new double[windowSize]; // slots for next angles

    // int count = 0;
    // List<Integer> result = new ArrayList<Integer>(0);

    double[] minima = new double[this.size()];

    for (int i=0; i<array.length; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevValues.length;j++){

        int prev_i = Utils.wrapIndex( i-(j+1)  , this.size() ); // the index j+1 before i
        int next_i = Utils.wrapIndex( i+(j+1)  , this.size() ); // the index j+1 after i

        // fill the lookup array
        prevValues[j] = array[prev_i];
        nextValues[j] = array[next_i];
      }
      
      // with the lookup positions, see if minimum at i
      // return a 1 if all higher than last, 0 if not
      // prev_l = 0;
      boolean ok = true;
      for(int k=0;k<prevValues.length;k++){

        // for the first position in prevValues, compare to the current index
        if(k==0){
          if( prevValues[k] <= array[i] || 
              nextValues[k] <= array[i] ){
            ok = false;
          }
        } else { // for the remainder of the positions in prevValues, compare to the prior prevAngle
          
          if( prevValues[k] <= prevValues[k-1] || 
              nextValues[k] <= nextValues[k-1]){
            ok = false;
          }
        }
      }

      if(ok){
        // count++;
        minima[i] = 1;
      } else {
        minima[i] = 0;
      }

      // result.add(i);

    }
    Profile minimaProfile = new Profile(minima);
    // this.minimaCalculated = true;
    // this.minimaCount =  count;
    return minimaProfile;
  }

  /**
   * Get the points considered local maxima for the given window
   * size as a Profile. Maxima are 1, other points are 0
   * @param windowSize the window size to use
   * @return
   */
  public Profile getLocalMaxima(int windowSize){
	  // go through array
	  // look at points ahead and behind.
	  // if all lower, local maximum

	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position

		  double[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
		  double[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

		  // with the lookup positions, see if maximum at i
		  // return a 1 if all lower than last, 0 if not
		  boolean isMaximum = true;
		  for(int k=0;k<prevValues.length;k++){

			  // for the first position in prevValues, compare to the current index
			  if(k==0){
				  if( prevValues[k] >= array[i] || 
						  nextValues[k] >= array[i] ){
					  isMaximum = false;
				  }
			  } else { // for the remainder of the positions in prevValues, compare to the prior prevAngle

				  if( prevValues[k] >= prevValues[k-1] || 
						  nextValues[k] >= nextValues[k-1]){
					  isMaximum = false;
				  }
			  }
		  }

		  result[i] = isMaximum ? 1 : 0;
	  }
	  return new Profile(result);
  }
  
  /**
   * Get the windowSize points around a point of interest
   * @param index the index position to centre on
   * @param windowSize the number of points either side
   * @return a profile with the window
   */
  public Profile getWindow(int index, int windowSize){

	  double[] result = new double[windowSize*2 + 1];

	  double[] prevValues = getValues(index, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
	  double[] nextValues = getValues(index, windowSize, Profile.ARRAY_AFTER);

	  // need to reverse the previous array
	  for(int k=prevValues.length, i=0;k>0;k--, i++){ 
		  result[i] = prevValues[k-1];        
	  }
	  result[windowSize] = array[index];
	  for(int i=0; i<nextValues.length;i++){ 
		  result[windowSize+i+1] = nextValues[i];        
	  }

	  return new Profile(result);
  }

  /**
   * Fetch a sub-region of the profile
   * @param indexStart the index to begin
   * @param indexEnd the index to end
   * @return a Profile
   */
  public Profile getSubregion(int indexStart, int indexEnd){
	  try{
		  if(indexStart <= indexEnd ){
			  double[] result = Arrays.copyOfRange(array,indexStart, indexEnd);
			  return new Profile(result);
		  } else { // case when array wraps
			  if(indexStart > indexEnd){
				  double[] resultA = Arrays.copyOfRange(array,indexStart, this.size()-1);
				  double[] resultB = Arrays.copyOfRange(array,0, indexEnd);
				  double[] result = new double[resultA.length+resultB.length];
				  int index = 0;
				  for(double d : resultA){
					  result[index] = d;
					  index++;
				  }
				  for(double d : resultB){
					  result[index] = d;
					  index++;
				  }
				  return new Profile(result);
			  } else{
				  return null; // should never be reached
			  }
		  }
	  } catch (Exception e){
		  IJ.log("Error getting profile subregion: "+indexStart+" - "+indexEnd+" in size "+this.size());
		  return null;
	  }

  }
  
  /**
   * Fetch a sub-region of the profile defined by the given segment. The segment
   * must originate from an equivalent profile (i.e. have the same totalLength
   * as the profile)
   * @param segment the segment to find
   * @return a Profile
   */
  public Profile getSubregion(NucleusBorderSegment segment){
	  
	  if(segment==null){
		  throw new IllegalArgumentException("Segment is null");
	  }
	  
	  if(segment.getTotalLength()!=this.size()){
		  throw new IllegalArgumentException("Segment comes from a different length profile");
	  }
	  return this.getSubregion(segment.getStartIndex(), segment.getEndIndex());
  }

  public Profile calculateDeltas(int windowSize){

    double[] deltas = new double[this.size()];

    double[] prevValues = new double[windowSize]; // slots for previous angles
    double[] nextValues = new double[windowSize]; // slots for next angles

    for (int i=0; i<array.length; i++) { // for each position in sperm

      for(int j=0;j<prevValues.length;j++){

        int prev_i = Utils.wrapIndex( i-(j+1)  , this.size() ); // the index j+1 before i
        int next_i = Utils.wrapIndex( i+(j+1)  , this.size() ); // the index j+1 after i

        // fill the lookup array
        prevValues[j] = array[prev_i];
        nextValues[j] = array[next_i];
      }

      double delta = 0;
      for(int k=0;k<prevValues.length;k++){

        if(k==0){
          delta += (array[i] - prevValues[k]) + (nextValues[k] - array[i]);
          
        } else {
          delta += ( prevValues[k] - prevValues[k-1]) + (nextValues[k] - nextValues[k-1]);
        }
        
      }

      deltas[i] = delta;
    }
    Profile result = new Profile(deltas);
    return result;
  }
  
  public Profile differentiate(){

	  double[] deltas = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm

		  int prev_i = Utils.wrapIndex( i-1  , this.size() ); // the index before
		  int next_i = Utils.wrapIndex( i+1  , this.size() ); // the index after


		  double delta = 	array[i]	-  array[prev_i] +
				  array[next_i]- array[i];


		  deltas[i] = delta;
	  }
	  Profile result = new Profile(deltas);
	  return result;
  }

  /**
   * Multiply all values within the profile by a given value
   * @param multiplier the value to multiply by
   * @return the new profile
   */
  public Profile multiply(double multiplier){
	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm
		  result[i] = array[i] * multiplier;
	  }
	  return new Profile(result);
  }

  /**
   * Multiply all values within the profile by the value within the given Profile
   * @param multiplier the profile to multiply by. Must be the same length as this profile
   * @return the new profile
   */
  public Profile multiply(Profile multiplier){
	  if(this.size()!=multiplier.size()){
		  throw new IllegalArgumentException("Profile sizes do not match");
	  }
	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm
		  result[i] = array[i] * multiplier.get(i);
	  }
	  return new Profile(result);
  }

  /**
   * Divide all values within the profile by a given value
   * @param divider the value to divide by
   * @return the new profile
   */
  public Profile divide(double divider){
	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm
		  result[i] = array[i] / divider;
	  }
	  return new Profile(result);
  }

  /**
   * Divide all values within the profile by the values within the given Profile
   * @param divider the profile to divide by. Must be the same length as this profile
   * @return the new profile
   */
  public Profile divide(Profile divider){
	  if(this.size()!=divider.size()){
		  throw new IllegalArgumentException("Profile sizes do not match");
	  }
	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm
		  result[i] = array[i] / divider.get(i);
	  }
	  return new Profile(result);
  }

  /**
   * Add all values within the profile by the value within the given Profile
   * @param adder the profile to add. Must be the same length as this profile
   * @return the new profile
   */
  public Profile add(Profile adder){
	  if(this.size()!=adder.size()){
		  throw new IllegalArgumentException("Profile sizes do not match");
	  }
	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm
		  result[i] = array[i] + adder.get(i);
	  }
	  return new Profile(result);
  }

  /**
   * Add the given value to all points within the profile
   * @param adder the value to add.
   * @return the new profile
   */
  public Profile add(double value){

	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm
		  result[i] = array[i] + value;
	  }
	  return new Profile(result);
  }

  /**
   * Subtract all values within the profile by the value within the given Profile
   * @param adder the profile to subtract. Must be the same length as this profile
   * @return the new profile
   */
  public Profile subtract(Profile sub){
	  if(this.size()!=sub.size()){
		  throw new IllegalArgumentException("Profile sizes do not match");
	  }
	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm
		  result[i] = array[i] - sub.get(i);
	  }
	  return new Profile(result);
  }

  // use for debugging
  public void print(){
	  for (int i=0; i<array.length; i++) {
		  IJ.log("Point "+i+": "+array[i]);
	  }
  }


  /**
   * Given a list of ordered profiles, merge them into one 
   * contiguous profile
   * @param list the list of profiles to merge
   * @return the merged profile
   */
  public static Profile merge(List<Profile> list){
	  if(list==null || list.size()==0){
		  throw new IllegalArgumentException("Profile list is null or empty");
	  }
	  Profile result = new Profile(new double[0]);
	  List<Double> combinedList = new ArrayList<Double>(0);

	  for(Profile p : list){
		  double[] values = p.asArray();
		  List<Double> valueList = Arrays.asList(Utils.getDoubleFromdouble(values));
		  combinedList.addAll(valueList);
	  }

	  Double[] combinedArray = (Double[]) combinedList.toArray(new Double[0]);
	  result = new Profile(Utils.getdoubleFromDouble(combinedArray));
	  return result;
  }
}