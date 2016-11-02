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

package components.active.generic;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import analysis.profiles.ProfileException;
import components.AbstractCellularComponent;
import components.generic.BooleanProfile;
import components.generic.IProfile;
import components.generic.Profile;
import components.nuclear.IBorderSegment;

public class FloatProfile implements IProfile {

	private static final long serialVersionUID = 1L;

	protected final float[] array;


	/**
	 * Constructor for a new Profile, based on an array of values.
	 * @param values the array to use
	 */
	public FloatProfile(final float[] values){

		if(values.length==0){
			throw new IllegalArgumentException("Input array has zero length in profile constructor");
		}
		this.array = new float[values.length];
		for(int i=0; i<this.array.length; i++){
			array[i] = values[i];
		}
	}

	/**
	 * Constructor based on an existing Profile. Makes a copy 
	 * of the existing Profile
	 * @param p the profile to copy
	 */
	public FloatProfile(final IProfile p){
		if(p==null){
			throw new IllegalArgumentException("Input profile is null in profile constructor");
		}

		this.array = new float[p.size()];

		for(int i=0; i<p.size(); i++){
			array[i] = (float) p.get(i);
		}
	}

	/**
	 * Constructor based on an fixed value and the profile
	 * length
	 * @param value the value for the profile to hold at each index
	 * @param length the length of the profile 
	 */
	public FloatProfile(final float value, final int length){

		if(length<1){
			throw new IllegalArgumentException("Profile length cannot be less than 1");
		}

		this.array = new float[length];
		for(int i=0; i<this.array.length; i++){
			array[i] = value;
		}
	}



	/* (non-Javadoc)
	 * @see components.generic.IProfile#size()
	 */
	@Override
	public int size(){
		return array.length;
	}


	/* (non-Javadoc)
	 * @see components.generic.IProfile#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FloatProfile other = (FloatProfile) obj;
		if (!Arrays.equals(array, other.array))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#get(int)
	 */
	@Override
	public double get(int index) throws IndexOutOfBoundsException {

		if(index<0 || index >= array.length){
			throw new IndexOutOfBoundsException("Requested value "+index+" is beyond profile end ("+array.length+")");
		}
		return array[index];

	}


	/* (non-Javadoc)
	 * @see components.generic.IProfile#getMax()
	 */
	@Override
	public double getMax(){
		double max = 0;
		for(int i=0; i<array.length;i++){
			if(array[i]>max){
				max = array[i];
			}
		}
		return max;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getIndexOfMax(components.generic.BooleanProfile)
	 */
	@Override
	public int getIndexOfMax(BooleanProfile limits){
		double max = 0;
		int maxIndex = 0;
		for(int i=0; i<array.length;i++){
			if( limits.get(i) && array[i]>max){
				max = array[i];
				maxIndex = i;
			}
		}
		return maxIndex;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getIndexOfMax()
	 */
	@Override
	public int getIndexOfMax(){

		BooleanProfile b = new BooleanProfile(this, true);
		return getIndexOfMax(b);
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfile#getProportionalIndex(double)
	 */
	@Override
	public int getProportionalIndex(double d) {
		if(d<0 || d > 1){
			throw new IllegalArgumentException("Proportion must be between 0-1: "+d);
		}
		
		double desiredDistanceFromStart = (double) array.length * d;
		
		int target = (int) desiredDistanceFromStart;
		
		return target;		
	}
	
	/* (non-Javadoc)
	 * @see components.generic.IProfile#getIndexProportion(int)
	 */
	@Override
	public double getIndexProportion(int index){
		if(index < 0 || index >= this.size()){
			throw new IllegalArgumentException("Index out of bounds: "+index);
		}
		
		double fractionalDistance =  (double) index / (double) array.length;
		
		return fractionalDistance;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getMin()
	 */
	@Override
	public double getMin(){
		double min = this.getMax();
		for(int i=0; i<array.length;i++){
			if(array[i]<min){
				min = array[i];
			}
		}
		return min;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getIndexOfMin(components.generic.BooleanProfile)
	 */
	@Override
	public int getIndexOfMin(BooleanProfile limits){
		double min = this.getMax();

		int minIndex = 0;

		for(int i=0; i<array.length;i++){
			if( limits.get(i) && array[i]<min){
				min = array[i];
				minIndex = i;
			}
		}

		return minIndex;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getIndexOfMin()
	 */
	@Override
	public int getIndexOfMin(){

		BooleanProfile b = new BooleanProfile(this, true);
		return getIndexOfMin(b);
		//		double min = this.getMax();
		//		int minIndex = 0;
		//		for(int i=0; i<array.length;i++){
		//			if(array[i]<min){
		//				min = array[i];
		//				minIndex = i;
		//			}
		//		}
		//		return minIndex;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#asArray()
	 */
	@Override
	public double[] asArray(){
		double[] result = new double[this.size()];
		for(int i=0;i<result.length; i++){
			result[i] = this.array[i];
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see components.generic.IProfile#getPositions(int)
	 */
	@Override
	public IProfile getPositions(int length){
		float [] result = new float[array.length];
		for(int i=0;i<array.length;i++){
			result[i] = (float) getRescaledIndex(i, length);
		}
		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getRescaledIndex(int, int)
	 */
	@Override
	public double getRescaledIndex(int index, int newLength){
		return (float)index / (float) array.length * (float) newLength;
	}

	/**
	 * Check the lengths of the two profiles. Return the first profile
	 * interpolated to the length of the longer.
	 * @param profile1 the profile to return interpolated
	 * @param profile2 the profile to compare
	 * @return a new profile with the length of the longest input profile
	 */
	private IProfile equaliseLengths(IProfile profile1, IProfile profile2) {
		if(profile1==null || profile2==null){
			throw new IllegalArgumentException("Input profile is null when equilising lengths");
		}
		// profile 2 is smaller
		// return profile 1 unchanged
		if(profile2.size() < profile1.size() ){
			return profile1;
		} else {
			// profile 1 is smaller; interpolate to profile 2 length
			profile1 = profile1.interpolate(profile2.size());
		}

		return profile1;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#absoluteSquareDifference(components.generic.IProfile)
	 */
	@Override
	public double absoluteSquareDifference(IProfile testProfile) {

		if(testProfile==null){
			throw new IllegalArgumentException("Test profile is null");
		}
		// the test profile needs to be matched to this profile
		// whichever is smaller must be interpolated 
		IProfile profile1 = equaliseLengths(this.copy(), testProfile);
		IProfile profile2 = equaliseLengths(testProfile, this.copy());

		double difference = 0;

		for(int j=0; j<profile1.size(); j++){ // for each point round the array

			double thisValue = profile1.get(j);
			double testValue = profile2.get(j);
			difference += Math.pow(thisValue - testValue, 2); // square difference - highlights extremes
		}
		return difference;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#weightedSquareDifference(components.generic.IProfile)
	 */
	@Override
	public double weightedSquareDifference(IProfile testProfile) throws Exception {

		if(testProfile==null){
			throw new IllegalArgumentException("Test profile is null");
		}

		// Ensure both profiles have the same length, to allow
		// point by point comparisons. The shorter is interpolated.
		IProfile profile1 = equaliseLengths(this.copy(), testProfile);
		IProfile profile2 = equaliseLengths(testProfile, this.copy());

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

	/* (non-Javadoc)
	 * @see components.generic.IProfile#copy()
	 */
	@Override
	public IProfile copy(){
		return new FloatProfile(this.array);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#offset(int)
	 */
	@Override
	public IProfile offset(int j) throws ProfileException {
		float[] newArray = new float[this.size()];
		for(int i=0;i<this.size();i++){
			newArray[i] = this.array[ AbstractCellularComponent.wrapIndex( i+j , this.size() ) ];
		}
		return new FloatProfile(newArray);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#smooth(int)
	 */
	@Override
	public IProfile smooth(int windowSize){

		float[] result = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position

			float[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
			float[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

			float average = array[i];
			for(int k=0;k<prevValues.length;k++){ 
				average += prevValues[k] + nextValues[k];	        
			}

			result[i] = (float) (average / (windowSize*2 + 1));
		}
		return new FloatProfile(result);
	}

	/**
	 * Get an array of the values <windowSize> before or after the current point
	 * @param position the position in the array
	 * @param windowSize the number of points to find
	 * @param type find points before or after
	 * @return an array of values
	 */
	private float[] getValues(int position, int windowSize, int type){

		float[] values = new float[windowSize]; // slots for previous angles
		for(int j=0;j<values.length;j++){

			// If type was before, multiply by -1; if after, multiply by 1
			int index = AbstractCellularComponent.wrapIndex( position + ((j+1)*type)  , this.size() );
			values[j] = array[index];
		}
		return values;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#reverse()
	 */
	@Override
	public void reverse(){

		float tmp;
		for (int i = 0; i < this.array.length / 2; i++) {
			tmp = this.array[i];
			this.array[i] = this.array[this.array.length - 1 - i];
			this.array[this.array.length - 1 - i] = tmp;
		}
	}  

	/* (non-Javadoc)
	 * @see components.generic.IProfile#interpolate(int)
	 */
	@Override
	public IProfile interpolate(int newLength) {

		if(newLength < this.size()){
			//    	finer("Interpolating to a smaller array!");
		}

		float[] newArray = new float[newLength];

		// where in the old curve index is the new curve index?
		for (int i=0; i<newLength; i++) {
			// we have a point in the new curve.
			// we want to know which points it lay between in the old curve
			float oldIndex = ( (float)i / (float)newLength) * (float) array.length; // get the fractional index position needed

			// get the value in the old profile at the given fractional index position
			newArray[i] = interpolateValue(oldIndex);
		}
		return new FloatProfile(newArray);
	}

	/**
	 * Take an index position from a non-normalised profile. Normalise it
	 * Find the corresponding angle in the median curve.
	 * Interpolate as needed
	 * @param normIndex the fractional index position to find within this profile
	 * @return an interpolated value
	 */
	private float interpolateValue(float normIndex){

		// convert index to 1 window boundaries
		// This allows us to see the array indexes above and below the desired
		// fractional index. From these, we can interpolate the fractional component.
		// NOTE: this does not account for curves. Interpolation is linear.
		int index1 = (int) Math.round(normIndex);
		int index2 	= index1 > normIndex
				? index1 - 1
						: index1 + 1;


		// Decide which of the two indexes is the higher, and which is the lower
		int indexLower 	= index1 < index2
				? index1
						: index2;

		int indexHigher 	= index2 > index1
				? index2
						: index1;

		//	  System.out.println("Set indexes "+normIndex+": "+indexLower+"-"+indexHigher);

		// wrap the arrays
		indexLower  = AbstractCellularComponent.wrapIndex(indexLower , array.length);
		indexHigher = AbstractCellularComponent.wrapIndex(indexHigher, array.length);
		//	  System.out.println("Wrapped indexes "+normIndex+": "+indexLower+"-"+indexHigher);

		// get the values at these indexes
		float valueHigher = array[ indexHigher ];
		float valueLower  = array[ indexLower  ];
		//	  System.out.println("Wrapped values "+normIndex+": "+valueLower+" and "+valueHigher);

		// calculate the difference between values
		// this can be negative
		float valueDifference = valueHigher - valueLower;
		//	  System.out.println("Difference "+normIndex+": "+valueDifference);

		// calculate the distance into the region to go
		float offset = normIndex - indexLower;
		//	  System.out.println("Offset "+normIndex+": "+offset);


		// add the offset to the lower index
		float positionToFind = indexLower + offset;
		positionToFind = (float) AbstractCellularComponent.wrapIndex(positionToFind , array.length);
		//	  System.out.println("Position to find "+normIndex+": "+positionToFind);

		// calculate the value to be added to the lower index value
		float newValue = valueDifference * offset; // 0 for 0, full difference for 1
		//	  System.out.println("New value "+normIndex+": "+newValue);

		float linearInterpolatedValue = newValue + valueLower;
		//	  System.out.println("Interpolated "+normIndex+": "+linearInterpolatedValue);

		return linearInterpolatedValue;
	}

	/*
    Interpolate another profile to match this, and move this profile
    along it one index at a time. Find the point of least difference, 
    and return this offset. Returns the positive offset to this profile
	 */
	/* (non-Javadoc)
	 * @see components.generic.IProfile#getSlidingWindowOffset(components.generic.IProfile)
	 */
	@Override
	public int getSlidingWindowOffset(IProfile testProfile) throws ProfileException {

		double lowestScore = this.absoluteSquareDifference(testProfile);
		int index = 0;
		for(int i=0;i<this.size();i++){

			IProfile offsetProfile = this.offset(i);

			double score = offsetProfile.absoluteSquareDifference(testProfile);
			if(score<lowestScore){
				lowestScore=score;
				index=i;
			}

		}
		return index;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getConsistentRegionBounds(double, double, int)
	 */
	@Override
	public int[] getConsistentRegionBounds(double value, double tolerance, int points){

		int counter = 0;
		int start = -1;
		int end = -1;
		int[] result = {start, end};

		for(int index = 0; index<array.length; index++){ // go through each point TODO wrapping
			double d = array[index];
			if(d > value-tolerance && d < value+tolerance){ // if the point meets criteria

				if(start==-1){ // start a new region if needed
					counter = 0;
					start = index;
				}
				counter++; // start counting a new region or increase an existing region

			} else { // does not meet criteria

				end = index;

				if(counter>=points){ // if the region is large enough
					// return points
					result[0] = start; // use the saved start and end indexes
					result[1] = end;
					return result;

				} else { // otherwise, reset the counter 


					start=-1;
					end=-1;
				}


			}
		}
		return result;
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
	/* (non-Javadoc)
	 * @see components.generic.IProfile#getLocalMinima(int)
	 */
	@Override
	public BooleanProfile getLocalMinima(int windowSize){
		// go through angle array (with tip at start)
		// look at 1-2-3-4-5 points ahead and behind.
		// if all greater, local minimum
		double[] prevValues = new double[windowSize]; // slots for previous angles
		double[] nextValues = new double[windowSize]; // slots for next angles

		// int count = 0;
		// List<Integer> result = new ArrayList<Integer>(0);

		boolean[] minima = new boolean[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm

			// go through each lookup position and get the appropriate angles
			for(int j=0;j<prevValues.length;j++){

				int prev_i = AbstractCellularComponent.wrapIndex( i-(j+1)  , this.size() ); // the index j+1 before i
				int next_i = AbstractCellularComponent.wrapIndex( i+(j+1)  , this.size() ); // the index j+1 after i

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
				minima[i] = true;
			} else {
				minima[i] = false;
			}

			// result.add(i);

		}
		BooleanProfile minimaProfile = new BooleanProfile(minima);
		// this.minimaCalculated = true;
		// this.minimaCount =  count;
		return minimaProfile;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getLocalMinima(int, double)
	 */
	@Override
	public BooleanProfile getLocalMinima(int windowSize, double threshold){
		BooleanProfile minima = getLocalMinima(windowSize);

		boolean[] values = new boolean[this.size()];

		for (int i=0; i<array.length; i++) { 

			if(minima.get(i)==true && this.get(i)<threshold){
				values[i] = true;
			} else {
				values[i] = false;
			} 
		}
		return new BooleanProfile(values);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getLocalMinima(int, double, double)
	 */
	@Override
	public BooleanProfile getLocalMinima(int windowSize, double threshold, double fraction){
		BooleanProfile minima = getLocalMinima(windowSize, threshold);

		boolean[] values = new boolean[this.size()];

		double fractionThreshold = (this.getMax()-this.getMin()) * fraction;

		for (int i=0; i<array.length; i++) { 

			if(minima.get(i)==true && ( this.get(i)>fractionThreshold || this.get(i)<-fractionThreshold   )  ){
				values[i] = true;
			} else {
				values[i] = false;
			} 
		}
		return new BooleanProfile(values);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getLocalMaxima(int)
	 */
	@Override
	public BooleanProfile getLocalMaxima(int windowSize){
		// go through array
		// look at points ahead and behind.
		// if all lower, local maximum

		boolean[] result = new boolean[this.size()];

		for (int i=0; i<array.length; i++) { // for each position

			float[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
			float[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

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

			result[i] = isMaximum;
		}
		return new BooleanProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getLocalMaxima(int, double)
	 */
	@Override
	public BooleanProfile getLocalMaxima(int windowSize, double threshold){
		BooleanProfile maxima = getLocalMaxima(windowSize);

		boolean[] values = new boolean[this.size()];

		for (int i=0; i<array.length; i++) { 

			if(maxima.get(i)==true && this.get(i)>threshold){
				values[i] = true;
			} else {
				values[i] = false;
			} 
		}
		return new BooleanProfile(values);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getLocalMaxima(int, double, double)
	 */
	@Override
	public BooleanProfile getLocalMaxima(int windowSize, double threshold, double fraction){
		BooleanProfile minima = getLocalMaxima(windowSize, threshold);

		boolean[] values = new boolean[this.size()];

		double fractionThreshold = this.getMax()-this.getMin() * fraction;

		for (int i=0; i<array.length; i++) { 

			if(minima.get(i)==true && ( this.get(i)>fractionThreshold || this.get(i)<-fractionThreshold   )  ){
				values[i] = true;
			} else {
				values[i] = false;
			} 
		}
		return new BooleanProfile(values);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getWindow(int, int)
	 */
	@Override
	public IProfile getWindow(int index, int windowSize){

		float[] result = new float[windowSize*2 + 1];

		float[] prevValues = getValues(index, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
		float[] nextValues = getValues(index, windowSize, Profile.ARRAY_AFTER);

		// need to reverse the previous array
		for(int k=prevValues.length, i=0;k>0;k--, i++){ 
			result[i] = prevValues[k-1];        
		}
		result[windowSize] = array[index];
		for(int i=0; i<nextValues.length;i++){ 
			result[windowSize+i+1] = nextValues[i];        
		}

		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getSubregion(int, int)
	 */
	@Override
	public IProfile getSubregion(int indexStart, int indexEnd) {

		if(indexStart < indexEnd ){

			float[] result = Arrays.copyOfRange(array,indexStart, indexEnd);
			return new FloatProfile(result);

		} else { // case when array wraps

			float[] resultA = Arrays.copyOfRange(array,indexStart, array.length-1);
			float[] resultB = Arrays.copyOfRange(array,0, indexEnd);
			float[] result = new float[resultA.length+resultB.length];
			int index = 0;
			for(float d : resultA){
				result[index] = d;
				index++;
			}
			for(float d : resultB){
				result[index] = d;
				index++;
			}

			if(result.length==0){
				log(Level.SEVERE, "Subregion length zero: "+indexStart+" - "+indexEnd);
			}
			return new FloatProfile(result);
		}
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getSubregion(components.nuclear.NucleusBorderSegment)
	 */
	@Override
	public IProfile getSubregion(IBorderSegment segment) {

		if(segment==null){
			throw new IllegalArgumentException("Segment is null");
		}

		if(segment.getTotalLength()!=this.size()){
			throw new IllegalArgumentException("Segment comes from a different length profile");
		}
		return getSubregion(segment.getStartIndex(), segment.getEndIndex());
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#calculateDeltas(int)
	 */
	@Override
	public IProfile calculateDeltas(int windowSize){

		float[] deltas = new float[this.size()];


		for (int i=0; i<array.length; i++) { // for each position in sperm

			float[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
			float[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

			float delta = 0;
			for(int k=0;k<prevValues.length;k++){

				if(k==0){
					delta += (array[i] - prevValues[k]) + (nextValues[k] - array[i]);

				} else {
					delta += ( prevValues[k] - prevValues[k-1]) + (nextValues[k] - nextValues[k-1]);
				}

			}

			deltas[i] = delta;
		}
		return new FloatProfile(deltas);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#differentiate()
	 */
	@Override
	public IProfile differentiate(){

		float[] deltas = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm

			int prev_i = AbstractCellularComponent.wrapIndex( i-1  , this.size() ); // the index before
			int next_i = AbstractCellularComponent.wrapIndex( i+1  , this.size() ); // the index after


			float delta = 	array[i]	-  array[prev_i] +
					array[next_i]- array[i];


			deltas[i] = delta;
		}
		return new FloatProfile(deltas);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#log(double)
	 */
	@Override
	public IProfile log(double base){
		float[] values = new float[this.size()];

		for (int i=0; i<array.length; i++) { 
			values[i] = (float) (Math.log(array[i]) / Math.log(base));
		}
		return new FloatProfile(values);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#power(double)
	 */
	@Override
	public IProfile power(double exponent){
		float[] values = new float[this.size()];

		for (int i=0; i<array.length; i++) { 
			values[i] = (float) Math.pow(array[i],exponent);
		}
		return new FloatProfile(values);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#absolute()
	 */
	@Override
	public IProfile absolute(){
		float[] values = new float[this.size()];

		for (int i=0; i<array.length; i++) { 
			values[i] = Math.abs(array[i]);
		}
		return new FloatProfile(values);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#cumulativeSum()
	 */
	@Override
	public IProfile cumulativeSum(){
		float[] values = new float[this.size()];

		float total = 0;
		for (int i=0; i<array.length; i++) { 
			total += array[i];
			values[i] = total;
		}
		return new FloatProfile(values);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#multiply(double)
	 */
	@Override
	public IProfile multiply(double multiplier){
		float[] result = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm
			result[i] = (float) (array[i] * multiplier);
		}
		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#multiply(components.generic.IProfile)
	 */
	@Override
	public IProfile multiply(IProfile multiplier){
		if(this.size()!=multiplier.size()){
			throw new IllegalArgumentException("Profile sizes do not match");
		}
		float[] result = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm
			result[i] = (float) (array[i] * multiplier.get(i));
		}
		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#divide(double)
	 */
	@Override
	public IProfile divide(double divider){
		float[] result = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm
			result[i] = (float) (array[i] / divider);
		}
		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#divide(components.generic.IProfile)
	 */
	@Override
	public IProfile divide(IProfile divider){
		if(this.size()!=divider.size()){
			throw new IllegalArgumentException("Profile sizes do not match");
		}
		float[] result = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm
			result[i] = (float) (array[i] / divider.get(i));
		}
		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#add(components.generic.IProfile)
	 */
	@Override
	public IProfile add(IProfile adder){
		if(this.size()!=adder.size()){
			throw new IllegalArgumentException("Profile sizes do not match");
		}
		float[] result = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm
			result[i] = (float) (array[i] + adder.get(i));
		}
		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#add(double)
	 */
	@Override
	public IProfile add(double value){

		float[] result = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm
			result[i] = (float) (array[i] + value);
		}
		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#subtract(components.generic.IProfile)
	 */
	@Override
	public IProfile subtract(IProfile sub){
		if(this.size()!=sub.size()){
			throw new IllegalArgumentException("Profile sizes do not match");
		}
		float[] result = new float[this.size()];

		for (int i=0; i<array.length; i++) { // for each position in sperm
			result[i] = (float) (array[i] - sub.get(i));
		}
		return new FloatProfile(result);
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getRanks()
	 */
	@Override
	public IProfile getRanks(){

		int rank = 0;

		float[] sorted = Arrays.copyOf(array, array.length);
		Arrays.sort(sorted);

		float[]ranks = new float[this.size()];

		for(float sort :sorted ){

			for(int i=0; i<this.size(); i++){
				float value = array[i];
				if(value==sort){
					ranks[i] = rank;
					break;
				}
			}
			rank++; 
		}
		IProfile result  = new FloatProfile(ranks);
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.IProfile#getSortedIndexes()
	 */
	@Override
	public IProfile getSortedIndexes(){


		float[] sorted = Arrays.copyOf(array, array.length);
		Arrays.sort(sorted);

		float[]indexes = new float[sorted.length];

		// Go through each index in the sorted list
		for(int index=0; index<sorted.length; index++){

			float value = sorted[index];
			// Go through each index in the original array
			for(int originalIndex=0; originalIndex<sorted.length; originalIndex++){


				// If the value in the profile is the value at the original index,
				// save the original index
				if(value==this.get(originalIndex)){
					System.out.println("Found value "+value+" at original index "+originalIndex);
					indexes[index] = (float) originalIndex;
					break;
				}
			}

		}
		return new FloatProfile(indexes);
	}


	/* (non-Javadoc)
	 * @see components.generic.IProfile#toString()
	 */
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();

		for (int i=0; i<array.length; i++) {
			builder.append("Index "+i+"\t"+array[i]+"\r\n");
		}
		return builder.toString();
	}


	/**
	 * Given a list of ordered profiles, merge them into one 
	 * contiguous profile
	 * @param list the list of profiles to merge
	 * @return the merged profile
	 */
	public static FloatProfile merge(List<IProfile> list){
		if(list==null || list.size()==0){
			throw new IllegalArgumentException("Profile list is null or empty");
		}

		int totalLength = 0;
		for(IProfile p : list){
			totalLength += p.size();
		}

		float[] combinedArray = new float[totalLength];


		int i=0;

		for(IProfile p : list){

			for(int j=0; j<p.size(); j++){
				combinedArray[i++] = (float) p.get(j);
			}
		}

		return new FloatProfile(combinedArray);
	}



	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
