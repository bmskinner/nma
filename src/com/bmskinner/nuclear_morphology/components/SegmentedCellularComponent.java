/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderSegment;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;

import ij.gui.Roi;

/**
 * This experimental class incorporates segments as subclasses to prevent errors
 * in indexing.
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class SegmentedCellularComponent extends ProfileableCellularComponent {

	private static final long serialVersionUID = 1L;


	/**
	 * Construct with an ROI, a source image and channel, and the original
	 * position in the source image
	 * 
	 * @param roi the region of interest defining the object
	 * @param centreOfMass the centre of mass of the object 
	 * @param f the image file the object was detected in
	 * @param channel the colour channel the object was detected in
	 * @param position the bounding box of the object in the format of {@link Imageable::getPosition }
	 */
	public SegmentedCellularComponent(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f, int channel, int[] position) {
		super(roi, centreOfMass, f, channel, position);
	}

	/**
	 * Create a new component based on the given template object. If the object has segments,
	 * these will be copied to the new component.
	 * @param c
	 * @throws UnprofilableObjectException
	 */
	public SegmentedCellularComponent(@NonNull final CellularComponent c) throws UnprofilableObjectException {
		super(c);
	}
	
	@Override
    public void calculateProfiles() throws ProfileException {

    	ProfileCreator creator = new ProfileCreator(this);

    	for (ProfileType type : ProfileType.values()) {
    		finest("Attempting to create profile "+type);
    		ISegmentedProfile profile = creator.createProfile(type);
    		finest("Assigning profile "+type);
    		setProfile(type, profile);
    	}
    }
	
	@Override
	public ISegmentedProfile getProfile(@NonNull ProfileType type) throws UnavailableProfileTypeException {

        if (!this.hasProfile(type))
            throw new UnavailableProfileTypeException("Cannot get profile type " + type);

        try {
        	ISegmentedProfile p = profileMap.get(type);
        	
        	if(! (p instanceof DefaultSegmentedProfile)) {
        		finer("Existing profile is not an internal profile class, is "+p.getClass().getSimpleName()+", converting");
        		// When reading old datasets, sometimes the profile length does not match the border list length.
        		// If this happens, the conversion will fail due to the new length constraints.
        		// As a stopgap, interpolate profiles as needed.
        		if(p.size()!=getBorderLength())
        			p = p.interpolate(getBorderLength());
        		assignProfile(type, new DefaultSegmentedProfile(p));
        	}
        	return profileMap.get(type);
        } catch (IndexOutOfBoundsException | ProfileException e) {
            throw new UnavailableProfileTypeException("Cannot get profile type " + type, e);
        }
    }
	
	@Override
	public void setProfile(@NonNull ProfileType type, @NonNull ISegmentedProfile profile) {
        if (segsLocked)
            return;

        if (this.getBorderLength() != profile.size())
            throw new IllegalArgumentException(String.format("Input profile length (%d) does not match border length (%d) for %s", profile.size(), getBorderLength(), type));
        
        try {
        	finest("Setting profile of type "+type);
    		assignProfile(type, new DefaultSegmentedProfile(profile));
		} catch (IndexOutOfBoundsException | ProfileException e) {
			stack("Unable to create copy of profile of type "+type+"; "+e.getMessage(), e);
		}
    }
	
	@Override
	public void setProfile(@NonNull ProfileType type, @NonNull Tag tag, @NonNull ISegmentedProfile p) throws UnavailableBorderTagException, UnavailableProfileTypeException {

        if (segsLocked)
            return;

        if (!this.hasBorderTag(tag))
            throw new UnavailableBorderTagException("Tag " + tag + " is not present");

        // fetch the index of the pointType (the zero of the input profile)
        int pointIndex = this.borderTags.get(tag);

        // remove the offset from the profile, by setting the profile to start from the pointIndex
        ISegmentedProfile oldProfile = getProfile(type);
        
        try {
            
            setProfile(type, (p).offset(-pointIndex));
        } catch (ProfileException e) { // restore the old profile
            stack("Error setting profile " + type + " at " + tag, e);
            setProfile(type, oldProfile);
        }

    }

	/**
	 * An implementation of a profile tied to an object
	 * @author ben
	 * @since 1.13.8
	 */
	public class DefaultProfile implements IProfile {

		private static final long serialVersionUID = 1L;
		protected final float[] array;

		/**
		 * Constructor for a new Profile, based on an array of values.
		 * 
		 * @param values the array to use
		 */
		public DefaultProfile(final float[] values) {
			if(values==null)
				throw new IllegalArgumentException("Array is null");
			if (values.length!=getBorderLength())
				throw new IllegalArgumentException(String.format("Input array length (%d) does not match border length (%d) in DefaultProfile constructor", values.length, getBorderLength()));
			this.array = values;
		}

		/**
		 * Constructor based on an existing Profile. Makes a copy of the existing
		 * Profile
		 * 
		 * @param p the profile to copy
		 */
		public DefaultProfile(@NonNull final IProfile p) {
			if (p==null)
				throw new IllegalArgumentException("Profile is null");
			
			if(p.size()!=getBorderLength())
				throw new IllegalArgumentException(String.format("Input profile length (%d) does not match border length (%d) in DefaultProfile constructor", p.size(), getBorderLength()));

			this.array = new float[p.size()];
			for (int i = 0; i < p.size(); i++) {
				array[i] = (float) p.get(i);
			}
		}

		/**
		 * Constructor based on an fixed value and the profile length
		 * 
		 * @param value the value for the profile to hold at each index
		 */
		public DefaultProfile(final float value) {
			this.array = new float[getBorderLength()];
			for (int i = 0; i < this.array.length; i++) {
				array[i] = value;
			}
		}

		@Override
		public int size() {
			return array.length;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(array);
			return result;
		}


		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			DefaultProfile other = (DefaultProfile) obj;
			if (!Arrays.equals(array, other.array))
				return false;
			return true;
		}


		@Override
		public double get(int index) throws IndexOutOfBoundsException {
			if (index < 0 || index >= array.length)
				throw new IndexOutOfBoundsException(
						"Requested value " + index + " is beyond profile end (" + array.length + ")");
			return array[index];
		}

		@Override
		public double get(double prop) {
			if (prop < 0 || prop > 1)
				throw new IndexOutOfBoundsException("Value " + prop + " must be between 0-1");
			int index = this.getIndexOfFraction(prop);
			return array[index];
		}

		@Override
		public double getMax() {
			double max = -Double.MAX_VALUE;
			for (int i = 0; i < array.length; i++) {
				if (array[i] > max)
					max = array[i];
			}
			return max;
		}

		@Override
		public int getIndexOfMax(@NonNull BooleanProfile limits) throws ProfileException {
			if ( limits.size() != array.length)
				throw new IllegalArgumentException("Limits are wrong size for this profile");

			double max = -Double.MAX_VALUE;
			int maxIndex = -1;
			for (int i = 0; i < array.length; i++) {
				if (limits.get(i) && array[i] > max) {
					max = array[i];
					maxIndex = i;
				}
			}

			if (maxIndex == -1)
				throw new ProfileException("No valid index for maximum value");
						return maxIndex;
		}

		@Override
		public int getIndexOfMax() throws ProfileException {
			return getIndexOfMax( new BooleanProfile(this, true) );
		}

		@Override
		public int getIndexOfFraction(double d) {
			if (d < 0 || d > 1)
				throw new IllegalArgumentException("Proportion must be between 0-1: " + d);

			double desiredDistanceFromStart = array.length * d;

			int target = (int) desiredDistanceFromStart;

			return target;
		}

		@Override
		public double getFractionOfIndex(int index) {
			if (index < 0 || index >= array.length)
				throw new IllegalArgumentException("Index out of bounds: " + index);
			return (double) index / (double) array.length;
		}

		@Override
		public double getMin() {
			double min = Double.MAX_VALUE;
			for (int i = 0; i < array.length; i++) {
				if (array[i] < min) {
					min = array[i];
				}
			}
			return min;
		}

		@Override
		public int getIndexOfMin(@NonNull BooleanProfile limits) throws ProfileException {

			if (limits.size() != array.length)
				throw new IllegalArgumentException("Limits are wrong size for this profile");
			double min = Double.MAX_VALUE;

			int minIndex = -1;

			for (int i = 0; i < array.length; i++) {
				if (limits.get(i) && array[i] < min) {
					min = array[i];
					minIndex = i;
				}
			}
			if (minIndex == -1)
				throw new ProfileException("No valid index for minimum value");
						return minIndex;
		}
		
		@Override
		public int getIndexOfMin() throws ProfileException {
			return getIndexOfMin(new BooleanProfile(this, true));
		}

		@Override
		public float[] toFloatArray() {
			float[] result = new float[array.length];
			System.arraycopy(array, 0, result, 0, array.length);
			return result;
		}

		@Override
		public double[] toDoubleArray() {
			double[] result = new double[array.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = array[i];
			}
			return result;
		}


		/**
		 * Check the lengths of the two profiles. Return the first profile
		 * interpolated to the length of the longer.
		 * 
		 * @param profile1 the profile to return interpolated
		 * @param profile2 the profile to compare
		 * @return a new profile with the length of the longest input profile
		 * @throws ProfileException
		 */
		private IProfile equaliseLengths(@NonNull IProfile profile1, @NonNull IProfile profile2) throws ProfileException {
			if (profile2.size() <= profile1.size())
				return profile1;
			return profile1.interpolate(profile2.size()); // profile 1 is smaller; interpolate to profile 2 length
		}

		@Override
		public double absoluteSquareDifference(@NonNull IProfile testProfile) throws ProfileException {

			float[] arr2 = testProfile.toFloatArray();

			if (array.length == arr2.length)
				return squareDifference(array, arr2);

			if (array.length > arr2.length) {
				arr2 = interpolate(arr2, array.length);
				return squareDifference(array, arr2);
			} 

			float[] arr1 = this.toFloatArray();
			arr1 = interpolate(arr1, arr2.length);
			return squareDifference(arr1, arr2);
		}

		/**
		 * Calculate the absolute square difference between two arrays of equal
		 * length. Note - array lengths are not checked.
		 * 
		 * @param arr1
		 * @param arr2
		 * @return
		 */
		private double squareDifference(float[] arr1, float[] arr2) {
			double difference = 0;
			for (int j = 0; j < arr1.length; j++) {
				difference += Math.pow(arr1[j] - arr2[j], 2);
			}
			return difference;
		}

		@Override
		public IProfile copy() throws ProfileException {
			return new DefaultProfile(array);
		}

		@Override
		public IProfile offset(int j) throws ProfileException {	
			return new DefaultProfile(offset(array, j));
		}

		/**
		 * Offset the array by the given amount
		 * 
		 * @param arr
		 * @param j
		 * @return
		 */
		private float[] offset(float[] arr, int j) {
			float[] newArray = new float[arr.length];
			for (int i = 0; i < arr.length; i++) {
				newArray[i] = arr[CellularComponent.wrapIndex(i + j, arr.length)];
			}
			return newArray;
		}

		@Override
		public IProfile smooth(int windowSize) {
			if(windowSize<2)
				throw new IllegalArgumentException(String.format("Window size is %d, must be >1",windowSize));

			float[] result = new float[array.length];

			for (int i = 0; i < array.length; i++) {
				float sum = 0f;
				for(int j=-windowSize; j<=windowSize; j++) {
					sum += array[wrapIndex(i+j)];
				}
				result[i] = (sum / (windowSize*2 + 1));
			}
			return new DefaultProfile(result);
		}

		/**
		 * Get an array of the values <i>windowSize</i> before or after the current point
		 * 
		 * @param position the position in the array
		 * @param windowSize the number of points to find
		 * @param type find points before or after
		 * @return an array of values
		 */
		private float[] getValues(int position, int windowSize, int type) {

			float[] values = new float[windowSize]; // slots for previous angles
			for (int j = 0; j < values.length; j++) {

				// If type was before, multiply by -1; if after, multiply by 1
				int index = wrapIndex(position + ((j + 1) * type));
				values[j] = array[index];
			}
			return values;
		}

		@Override
		public void reverse() {

			float tmp;
			for (int i = 0; i < this.array.length / 2; i++) {
				tmp = this.array[i];
				this.array[i] = this.array[this.array.length - 1 - i];
				this.array[this.array.length - 1 - i] = tmp;
			}
		}

		@Override
		public IProfile interpolate(int newLength) throws ProfileException {

			if(newLength<MINIMUM_PROFILE_LENGTH)
				throw new IllegalArgumentException(String.format("New length %d below minimum %d",newLength,MINIMUM_PROFILE_LENGTH));

			float[] newArray = new float[newLength];

			// where in the old curve index is the new curve index?
			for (int i = 0; i < newLength; i++) {
				// we have a point in the new curve.
				// we want to know which points it lay between in the old curve
				float oldIndex = ((float) i / (float) newLength) * array.length;

				// get the value in the old profile at the given fractional index
				// position
				newArray[i] = interpolateValue(oldIndex);
			}
			
			// We can't use a DefaultProfile for this because the length is tied to the component border
			return new FloatProfile(newArray);
		}

		/**
		 * Interpolate the array to the given length, and return as a new array
		 * 
		 * @param arr the array to interpolate
		 * @param length the new length
		 * @return
		 */
		private float[] interpolate(float[] arr, int length) {

			float[] result = new float[length];

			// where in the old curve index is the new curve index?
			for (int i = 0; i < length; i++) {
				// we have a point in the new array.
				// we want to know which points it lies between in the old profile
				float fraction = ((float) i / (float) length); // get the fractional
				// index position
				// needed

				// get the value in the old profile at the given fractional index
				// position
				result[i] = getInterpolatedValue(arr, fraction);
			}
			return result;

		}

		/**
		 * Get the interpolated value at the given fraction along the given array
		 * 
		 * @param arr
		 * @param fraction
		 *            the fraction, from 0-1
		 * @return
		 */
		private float getInterpolatedValue(float[] arr, float fraction) {
			// Get the equivalent index of the fraction in the array
			double index = fraction * arr.length;
			double indexFloor = Math.floor(index);

			// Get the integer portion and find the bounding indices
			int indexLower = (int) indexFloor;
			if (indexLower == arr.length) { // only wrap possible if fraction is
				// range 0-1
				indexLower = 0;
			}

			int indexHigher = indexLower + 1;
			if (indexHigher == arr.length) { // only wrap possible if fraction is
				// range 0-1
				indexHigher = 0;
			}

			// Find the fraction between the indices
			double diffFraction = index - indexFloor;

			// Calculate the linear interpolation
			double interpolate = arr[indexLower] + ((arr[indexHigher] - arr[indexLower]) * diffFraction);

			return (float) interpolate;

		}

		/**
		 * Find a value in the profile via linear interpolation.
		 * For example interpolateValue(12.4) will return the value 40%
		 * between the values at indexes 12 and 13.
		 * 
		 * @param normIndex the fractional index position to find within this profile.
		 * @return an interpolated value
		 */
		private float interpolateValue(float normIndex) {

			// Find the indexes to interpolate between
			int indexLower = wrapIndex( (int) Math.floor(normIndex));
			int indexUpper = wrapIndex( (int) Math.ceil(normIndex));

			// get the values at these indexes
			float valueLower = array[indexLower];
			float valueUpper = array[indexUpper];

			// calculate the difference between values; this can be negative
			float valueDifference = valueUpper - valueLower;

			// calculate the fractional distance
			float offset = normIndex - indexLower;

			// calculate the value to be added to the lower index value
			float newValue = valueDifference * offset;

			return valueLower+ newValue;
		}


		@Override
		public int getSlidingWindowOffset(@NonNull IProfile testProfile) throws ProfileException {
			float[] test = testProfile.toFloatArray();  
			if (array.length != test.length) 
				test = interpolate(test, array.length);      
			return getBestFitOffset(array, test);
		}

		/**
		 * Get the sliding window offset of array 1 that best matches array 2. The
		 * arrays must be the same length
		 * 
		 * @param arr1
		 * @param arr2
		 * @return
		 */
		private int getBestFitOffset(float[] arr1, float[] arr2) {

			double bestScore = Double.MAX_VALUE;
			int bestIndex = 0;

			for (int i = 0; i < arr1.length; i++) {
				double score = squareDifference(offset(arr1, i), arr2);
				if (score < bestScore) {
					bestScore = score;
					bestIndex = i;
				}
			}
			return bestIndex;
		}

		/*
		 * --------------------
		 *  Detect minima within profiles
		 * --------------------
		 */

		/*
		 * For each point in the array, test for a local minimum. The values of the
		 * points <minimaLookupDistance> ahead and behind are checked. Each should
		 * be greater than the value before. One exception is allowed, to account
		 * for noisy data. Returns the indexes of minima
		 */
		@Override
		public BooleanProfile getLocalMinima(int windowSize) {

			if(windowSize<1)
				throw new IllegalArgumentException(String.format("Window size %d must be >=1",windowSize));

			// go through angle array (with tip at start)
			// look at 1-2-3-4-5 points ahead and behind.
			// if all greater, local minimum
			double[] prevValues = new double[windowSize]; // slots for previous
			// angles
			double[] nextValues = new double[windowSize]; // slots for next angles

			// int count = 0;
			// List<Integer> result = new ArrayList<Integer>(0);

			boolean[] minima = new boolean[this.size()];

			for (int i = 0; i < array.length; i++) { // for each position in sperm

				// go through each lookup position and get the appropriate angles
				for (int j = 0; j < prevValues.length; j++) {

					int prev_i = CellularComponent.wrapIndex(i - (j + 1), this.size()); // the
					// index
					// j+1
					// before
					// i
					int next_i = CellularComponent.wrapIndex(i + (j + 1), this.size()); // the
					// index
					// j+1
					// after
					// i

					// fill the lookup array
					prevValues[j] = array[prev_i];
					nextValues[j] = array[next_i];
				}

				// with the lookup positions, see if minimum at i
				// return a 1 if all higher than last, 0 if not
				// prev_l = 0;
				boolean ok = true;
				for (int k = 0; k < prevValues.length; k++) {

					// for the first position in prevValues, compare to the current
					// index
					if (k == 0) {
						if (prevValues[k] <= array[i] || nextValues[k] <= array[i]) {
							ok = false;
						}
					} else { // for the remainder of the positions in prevValues,
						// compare to the prior prevAngle

						if (prevValues[k] <= prevValues[k - 1] || nextValues[k] <= nextValues[k - 1]) {
							ok = false;
						}
					}
				}

				if (ok) {
					// count++;
					minima[i] = true;
				} else {
					minima[i] = false;
				}

				// result.add(i);

			}
			BooleanProfile minimaProfile = new BooleanProfile(minima);
			// this.minimaCalculated = true;
			// this.minimaCount = count;
			return minimaProfile;
		}

		@Override
		public BooleanProfile getLocalMinima(int windowSize, double threshold) {
			BooleanProfile minima = getLocalMinima(windowSize);

			boolean[] values = new boolean[this.size()];

			for (int i = 0; i < array.length; i++) {

				if (minima.get(i) == true && this.get(i) < threshold) {
					values[i] = true;
				} else {
					values[i] = false;
				}
			}
			return new BooleanProfile(values);
		}

		@Override
		public BooleanProfile getLocalMaxima(int windowSize) {

			if(windowSize<1)
				throw new IllegalArgumentException(String.format("Window size %d must be >=1",windowSize));
			// go through array
			// look at points ahead and behind.
			// if all lower, local maximum

			boolean[] result = new boolean[this.size()];

			for (int i = 0; i < array.length; i++) { // for each position

				float[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE); // slots
				// for
				// previous
				// angles
				float[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);

				// with the lookup positions, see if maximum at i
				// return a 1 if all lower than last, 0 if not
				boolean isMaximum = true;
				for (int k = 0; k < prevValues.length; k++) {

					// for the first position in prevValues, compare to the current
					// index
					if (k == 0) {
						if (prevValues[k] >= array[i] || nextValues[k] >= array[i]) {
							isMaximum = false;
						}
					} else { // for the remainder of the positions in prevValues,
						// compare to the prior prevAngle

						if (prevValues[k] >= prevValues[k - 1] || nextValues[k] >= nextValues[k - 1]) {
							isMaximum = false;
						}
					}
				}

				result[i] = isMaximum;
			}
			return new BooleanProfile(result);
		}

		@Override
		public BooleanProfile getLocalMaxima(int windowSize, double threshold) {
			BooleanProfile maxima = getLocalMaxima(windowSize);

			boolean[] values = new boolean[this.size()];

			for (int i = 0; i < array.length; i++) {

				if (maxima.get(i) == true && this.get(i) > threshold) {
					values[i] = true;
				} else {
					values[i] = false;
				}
			}
			return new BooleanProfile(values);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see components.generic.IProfile#getWindow(int, int)
		 */
		@Override
		public IProfile getWindow(int index, int windowSize) {

			float[] result = new float[windowSize * 2 + 1];

			float[] prevValues = getValues(index, windowSize, IProfile.ARRAY_BEFORE);
			float[] nextValues = getValues(index, windowSize, IProfile.ARRAY_AFTER);

			// need to reverse the previous array
			for (int k = prevValues.length, i = 0; k > 0; k--, i++) {
				result[i] = prevValues[k - 1];
			}
			result[windowSize] = array[index];
			for (int i = 0; i < nextValues.length; i++) {
				result[windowSize + i + 1] = nextValues[i];
			}

			return new FloatProfile(result);
		}

		@Override
		public IProfile getSubregion(int indexStart, int indexEnd) {

	        if (indexStart >= array.length)
	            throw new IllegalArgumentException(String.format("Start index (%d) is beyond array length (%d)", indexStart, array.length));
	        if (indexEnd >= array.length)
	            throw new IllegalArgumentException(String.format("End index (%d) is beyond array length (%d)", indexEnd, array.length));
	        if(indexStart < 0 || indexEnd < 0)
	            throw new IllegalArgumentException(String.format("Start (%d) or end index (%d) is below zero", indexStart, indexEnd));

			if (indexStart < indexEnd) {
				float[] result = Arrays.copyOfRange(array, indexStart, indexEnd+1);
				return new FloatProfile(result);

			} 
			// case when array wraps

			float[] resultA = Arrays.copyOfRange(array, indexStart, array.length);
			float[] resultB = Arrays.copyOfRange(array, 0, indexEnd+1);

			float[] result = new float[resultA.length + resultB.length];
			int index = 0;
			for (float d : resultA) {
				result[index] = d;
				index++;
			}
			for (float d : resultB) {
				result[index] = d;
				index++;
			}

			if (result.length == 0) {
				warn("Subregion length zero: " + indexStart + " - " + indexEnd);
			}
			return new FloatProfile(result);

		}


		@Override
		public IProfile getSubregion(@NonNull IBorderSegment segment) {

			if (segment == null)
				throw new IllegalArgumentException("Segment is null");

			if (segment.getProfileLength() != array.length) {
				throw new IllegalArgumentException("Segment comes from a different length profile");
			}
			return getSubregion(segment.getStartIndex(), segment.getEndIndex());
		}

		@Override
		public IProfile calculateDeltas(int windowSize) {
			if(windowSize<=0)
				throw new IllegalArgumentException("Window size cannot be zero or less");

			float[] deltas = new float[this.size()];

			for (int i = 0; i < array.length; i++) {
				
				IProfile window = getWindow(i, windowSize);
				float delta = 0;
				for(int j=1; j<window.size(); j++) {
					delta += (window.get(j)-window.get(j-1));
				}
				deltas[i] = delta;
			}
			return new DefaultProfile(deltas);
		}

		@Override
		public IProfile power(double exponent) {
			float[] values = new float[this.size()];

			for (int i = 0; i < array.length; i++) {
				values[i] = (float) Math.pow(array[i], exponent);
			}
			return new DefaultProfile(values);
		}

		@Override
		public IProfile absolute() {
			float[] values = new float[this.size()];

			for (int i = 0; i < array.length; i++) {
				values[i] = Math.abs(array[i]);
			}
			return new DefaultProfile(values);
		}

		@Override
		public IProfile cumulativeSum() {
			float[] values = new float[array.length];

			float total = 0;
			for (int i = 0; i < array.length; i++) {
				total += array[i];
				values[i] = total;
			}
			return new DefaultProfile(values);
		}

		@Override
		public IProfile multiply(double multiplier) {

			if (Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
				throw new IllegalArgumentException("Cannot add NaN or infinity");
			}

			float[] result = new float[this.size()];

			for (int i = 0; i < array.length; i++) { // for each position in sperm
				result[i] = (float) (array[i] * multiplier);
			}
			return new DefaultProfile(result);
		}

		@Override
		public IProfile multiply(@NonNull IProfile multiplier) {
			if (this.size() != multiplier.size()) {
				throw new IllegalArgumentException("Profile sizes do not match");
			}
			float[] result = new float[this.size()];

			for (int i = 0; i < array.length; i++) { // for each position in sperm
				result[i] = (float) (array[i] * multiplier.get(i));
			}
			return new DefaultProfile(result);
		}

		@Override
		public IProfile divide(double divider) {

			if (Double.isNaN(divider) || Double.isInfinite(divider)) {
				throw new IllegalArgumentException("Cannot add NaN or infinity");
			}

			float[] result = new float[this.size()];

			for (int i = 0; i < array.length; i++) { // for each position in sperm
				result[i] = (float) (array[i] / divider);
			}
			return new DefaultProfile(result);
		}

		@Override
		public IProfile divide(@NonNull IProfile divider) {
			if (this.size() != divider.size()) {
				throw new IllegalArgumentException("Profile sizes do not match");
			}
			float[] result = new float[this.size()];

			for (int i = 0; i < array.length; i++) { // for each position in sperm
				result[i] = (float) (array[i] / divider.get(i));
			}
			return new DefaultProfile(result);
		}

		@Override
		public IProfile add(@NonNull IProfile adder) {
			if (this.size() != adder.size()) {
				throw new IllegalArgumentException("Profile sizes do not match");
			}
			float[] result = new float[this.size()];

			for (int i = 0; i < array.length; i++) { // for each position in sperm
				result[i] = (float) (array[i] + adder.get(i));
			}
			return new DefaultProfile(result);
		}

		@Override
		public IProfile add(double value) {

			if (Double.isNaN(value) || Double.isInfinite(value)) {
				throw new IllegalArgumentException("Cannot add NaN or infinity");
			}

			float[] result = new float[array.length];

			for (int i = 0; i < array.length; i++) { // for each position in sperm
				result[i] = (float) (array[i] + value);
			}
			return new DefaultProfile(result);
		}

		@Override
		public IProfile subtract(@NonNull IProfile sub) {
			if (this.size() != sub.size()) {
				throw new IllegalArgumentException("Profile sizes do not match");
			}
			float[] result = new float[this.size()];

			for (int i = 0; i < array.length; i++) { // for each position in sperm
				result[i] = (float) (array[i] - sub.get(i));
			}
			return new DefaultProfile(result);
		}

		@Override
		public IProfile subtract(double value) {
			if (Double.isNaN(value) || Double.isInfinite(value))
				throw new IllegalArgumentException("Cannot subtract NaN or infinity");

			float[] result = new float[array.length];

			for (int i = 0; i < array.length; i++) { // for each position in sperm
				result[i] = (float) (array[i] - value);
			}
			return new DefaultProfile(result);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			for (int i = 0; i < array.length; i++) {
				builder.append("Index " + i + "\t" + array[i] + "\r\n");
			}
			return builder.toString();
		}

		/**
		 * Given a list of ordered profiles, merge them into one contiguous profile
		 * 
		 * @param profiles the profiles to merge
		 * @return the merged profile
		 */
		public IProfile merge(List<IProfile> profiles) {
			if (profiles == null || profiles.size() == 0) {
				throw new IllegalArgumentException("Profile list is null or empty");
			}

			int totalLength = 0;
			for (IProfile p : profiles) {
				totalLength += p.size();
			}

			float[] combinedArray = new float[totalLength];

			int i = 0;
			
			for (IProfile p : profiles) {

				for (int j = 0; j < p.size(); j++) {
					combinedArray[i++] = (float) p.get(j);
				}
			}

			return new DefaultProfile(combinedArray);
		}
		
		public int wrap(int index) {
			if (index < 0)
	            return wrap(size() + index);
	        if (index < size())
	            return index;
	        return index % size();
		}
		
		
		
	}


	/**
	 * An implementation of a segmented profile tied to an object
	 * @author ben
	 * @since 1.13.8
	 *
	 */
	public class DefaultSegmentedProfile extends DefaultProfile implements ISegmentedProfile {

		private static final long serialVersionUID = 1L;
		private final BorderSegmentTree segments;

		/**
		 * Construct using a regular profile and a list of border segments
		 * 
		 * @param p the profile
		 * @param list the list of segments to use
		 * @throws ProfileException
		 */
		protected DefaultSegmentedProfile(@NonNull final IProfile p, @NonNull final List<IBorderSegment> list) throws ProfileException {
			super(p);
			// The root segment is one segment covering the whole profile. See single spotted pigs.
			segments = new BorderSegmentTree(IProfileCollection.DEFAULT_SEGMENT_ID);
			for(IBorderSegment s : list){
				segments.addMergeSource(s);
			}
		}

		/**
		 * Construct using an existing profile. Copies the data and segments
		 * 
		 * @param profile the segmented profile to copy
		 * @throws ProfileException
		 * @throws IndexOutOfBoundsException
		 */
		protected DefaultSegmentedProfile(@NonNull final ISegmentedProfile profile) throws IndexOutOfBoundsException, ProfileException {
			this(profile, profile.getSegments());
		}

		/**
		 * Construct using a basic profile. Two segments are created that span the
		 * entire profile, half each
		 * 
		 * @param profile
		 */
		protected DefaultSegmentedProfile(@NonNull final IProfile profile) {
			this(profile.toFloatArray());
		}

		/**
		 * Construct from an array of values with a single segment
		 * 
		 * @param values
		 * @throws Exception
		 */
		public DefaultSegmentedProfile(float[] values) {
			super(values);
			segments = new BorderSegmentTree(IProfileCollection.DEFAULT_SEGMENT_ID);
		}
		
		@Override
		public boolean hasSegments() {
			return true; // by design now
		}

		@Override
		public @NonNull List<IBorderSegment> getSegments() {
			if(segments.hasMergeSources())
				return segments.getMergeSources();
			List<IBorderSegment> l = new ArrayList<>();
			l.add(segments);
			return l;
		}

		@Override
		public @NonNull IBorderSegment getSegment(@NonNull final UUID id) throws UnavailableComponentException {
			
			if(segments.getID().equals(id))
				return segments;
			return segments.getMergeSource(id);
		}

		@Override
		public boolean hasSegment(@NonNull final UUID id) {
			return segments.hasMergeSource(id);
		}

		@Override
		public List<IBorderSegment> getSegmentsFrom(@NonNull final UUID id) throws UnavailableComponentException {
			return getSegmentsFrom(getSegment(id));
		}

		/**
		 * Get the index of the segment with the given id
		 * @param id
		 * @return
		 * @throws UnavailableComponentException if a segment with the id is not present
		 */
		private int getIndexOfSegment(@NonNull final UUID id) throws UnavailableComponentException {
			if(!segments.hasMergeSource(id))
				throw new UnavailableComponentException("No segment with requested id "+id);
			return segments.getChildIds().indexOf(id);
		}

		/**
		 * Get the segments in order from the given segment
		 * 
		 * @param firstSeg the first segment in the profile
		 * @return the segments ordered from the desired first segment
		 * @throws UnavailableComponentException if the desired first segment is not present in the profile
		 */
		private List<IBorderSegment> getSegmentsFrom(@NonNull IBorderSegment firstSeg) throws UnavailableComponentException {
			
			if(!hasSegment(firstSeg.getID()))
				throw new UnavailableComponentException("No segment with first segment id ");

			int index = getIndexOfSegment(firstSeg.getID());			
			
			List<IBorderSegment> result = new ArrayList<>();
			List<IBorderSegment> current = getSegments();
			for(int j=0; j<segments.nChildren(); j++){
				int k = wrapSegmentIndex(index+j); 
				result.add(current.get(k));
			}
			return result;
		}

		@Override
		public List<IBorderSegment> getOrderedSegments() {

			try {
				for (IBorderSegment seg : getSegments()) {
					if (seg.contains(ZERO_INDEX) && (getSegmentCount()==1 || seg.getEndIndex()!=ZERO_INDEX))
						return getSegmentsFrom(seg);
				}
			} catch (UnavailableComponentException e) {
				warn("Profile error getting segments");
				stack("Profile error getting segments", e);
				return new ArrayList<>();
			}
			return new ArrayList<>();
		}

		@Override
		public IBorderSegment getSegment(@NonNull String name) throws UnavailableComponentException {

			for (IBorderSegment seg : this.getSegments()) {
				if (seg.getName().equals(name)) {
					return seg;
				}
			}
			throw new UnavailableComponentException("Requested segment name is not present");
		}

		@Override
		public IBorderSegment getSegment(@NonNull IBorderSegment segment) {
			if (!this.contains(segment))
				throw new IllegalArgumentException("Requested segment is not present");

			try {
				return getSegment(segment.getID());
			} catch(UnavailableComponentException e){
				return null;
			}

		}

		@Override
		public IBorderSegment getSegmentAt(int position) {

			if(position < 0 || position > segments.nChildren()-1)
				throw new IllegalArgumentException("Segment position is out of bounds");
			return segments.getMergeSources().get(position);
		}

		@Override
		public IBorderSegment getSegmentContaining(int index) {

			if (index < 0 || index >= size())
				throw new IllegalArgumentException("Index is out of profile bounds");
			
			if(!segments.hasMergeSources())
				return segments;

			for (IBorderSegment seg : segments.getMergeSources()) {
				if (seg.contains(index))
					return seg;
			}
			throw new IllegalArgumentException("Index not in profile");
		}

		@Override
		public void setSegments(@NonNull List<IBorderSegment> list) {
			if (list == null || list.isEmpty())
				throw new IllegalArgumentException("Segment list is null or empty");

			if (list.get(0).getProfileLength() != size())
				throw new IllegalArgumentException("Segment list is from a different total length");
			
			segments.clearMergeSources();
			for(IBorderSegment s : list) {
				segments.addMergeSource(s);
			}
		}

		@Override
		public void clearSegments() {
			segments.clearMergeSources();
		}

		@Override
		public List<String> getSegmentNames() {
			return IntStream.range(0, segments.nChildren())
					.mapToObj(i->"Seg_"+i).collect(Collectors.toList());
		}

		@Override
		public List<UUID> getSegmentIDs() {
			if(segments.hasMergeSources())
				return segments.getChildIds();
			List<UUID> l = new ArrayList<>();
			l.add(segments.id);
			return l;
		}

		@Override
		public int getSegmentCount() {
			return segments.hasMergeSources() ? segments.nChildren() : 1;
		}

		@Override
		public double getDisplacement(@NonNull final IBorderSegment segment) {

			if (!contains(segment)) {
				throw new IllegalArgumentException("Segment is not in profile");
			}
			double start = this.get(segment.getStartIndex());
			double end = this.get(segment.getEndIndex());

			double min = Math.min(start, end);
			double max = Math.max(start, end);

			return max - min;
		}

		@Override
		public boolean contains(@NonNull final IBorderSegment segment) {
			return segments.hasMergeSource(segment.getID());
		}


		@Override
		public boolean update(@NonNull IBorderSegment segment, int startIndex, int endIndex) throws SegmentUpdateException {

			if (!this.contains(segment))
				throw new SegmentUpdateException(String.format("Segment %s is not part of this profile", segment.toString()));
			
			try {
				IBorderSegment testSeg = getSegment(segment.getID());
				
				return testSeg.update(startIndex, endIndex);
				
			} catch (UnavailableComponentException e) {
				throw new SegmentUpdateException(String.format("Segment %s is available in this profile", segment.toString()));
			}
		}

		@Override
		public void nudgeSegments(int amount) {
			segments.offset(amount);  
		}

		@Override
		public ISegmentedProfile offset(int offset) throws ProfileException {

			// get the basic profile with the offset applied
			IProfile offsetProfile = super.offset(offset);

			/*
			 * The segmented profile starts like this:
			 * 
			 * 0 5 15 35 |-----|----------|--------------------|
			 * 
			 * After applying offset=5, the profile looks like this:
			 * 
			 * 0 10 30 35 |----------|--------------------|-----|
			 * 
			 * The new profile starts at index 'offset' in the original profile This
			 * means that we must subtract 'offset' from the segment positions to
			 * make them line up.

			 */
			
			/*
			 * Apply the offset to each of the segments.
			 */

			DefaultSegmentedProfile result = new DefaultSegmentedProfile(offsetProfile);
			for(BorderSegmentTree s : segments.leaves) {
				result.segments.addMergeSource(new BorderSegmentTree(s, result.segments));
			}
			
			// root segment update
			result.segments.startIndex = wrap(result.segments.startIndex-offset);
			for(BorderSegmentTree s : result.segments.leaves) {		
				s.offset(-offset);
			}
			return result;
		}

		@Override
		public ISegmentedProfile interpolate(int length) throws ProfileException {
			
			// interpolate the IProfile
			IProfile newProfile = super.interpolate(length);
			
			List<IBorderSegment> newSegs = new ArrayList<>();
			
			// Interpolate the segments
			
			// single segment
			if(!segments.hasMergeSources()) {
				newSegs.add(new DefaultBorderSegment(0, length, length, segments.getID()));
				return new SegmentedFloatProfile(newProfile, newSegs);
			}
			
			// multiple child segments
			
			// Need to ensure the start and end indexes match for consecutive segments
			// after the interpolation
			
			List<Integer> putativeStartPoints = new ArrayList<>();
			
			for (BorderSegmentTree s : segments.leaves) {
				double prop = this.getFractionOfIndex(s.getStartIndex());
				int newStart  = (int) Math.round((prop * length));
				
				putativeStartPoints.add(newStart);
			}
			
			
			// With the new start points, create the segments
			
			for(int i=0; i<putativeStartPoints.size(); i++) {

				int newStart = putativeStartPoints.get(i);
				int newEnd   = i==putativeStartPoints.size()-1 ? putativeStartPoints.get(0) : putativeStartPoints.get(i+1);
				
				int newLength = newStart<newEnd ? newEnd-newStart : newEnd + length-newStart;
				
				if(newLength<IBorderSegment.MINIMUM_SEGMENT_LENGTH)
					throw new ProfileException(String.format("Cannot interpolate to %d: segment length %d would be too short", length, newLength));
				newSegs.add(new DefaultBorderSegment(newStart, newEnd, length, segments.leaves.get(i).getID()));
			}

			// assign new segments
			finer("Creating new SegmentedFloatProfile to pass interpolated profile out of component");
			return new SegmentedFloatProfile(newProfile, newSegs);
		}

		@Override
		public ISegmentedProfile frankenNormaliseToProfile(@NonNull ISegmentedProfile template) throws ProfileException {

			if (this.getSegmentCount() != template.getSegmentCount())
				throw new IllegalArgumentException("Segment counts are different in profile and template");

			for(UUID id : template.getSegmentIDs()){
				if(!hasSegment(id))
					throw new IllegalArgumentException("Segment ids do not match between profile and template");
			}
			
			/*
			 * The final frankenprofile is made of stitched together profiles from
			 * each segment
			 */
			List<IProfile> finalSegmentProfiles = new ArrayList<>(template.getSegmentCount());

			try {

				int counter = 0;
	            for (UUID segID : template.getSegmentIDs()) {
	                IBorderSegment thisSeg = this.getSegment(segID);
	                IBorderSegment templateSeg = template.getSegment(segID);
	                
	                // For each segment, 1 must be subtracted from the length because the
	                // segment lengths include the overlapping end and start indexes.
	                // The final segment has 2 subtracted to account for the overlapping start index of the profile.
	                int newLength = counter==template.getSegmentCount()-1 ? templateSeg.length()-2 : templateSeg.length()-1;

	                // Interpolate the segment region to the new length
	                IProfile revisedProfile = interpolateSegment(thisSeg, newLength);
	                finalSegmentProfiles.add(revisedProfile);
	                counter++;
	            }

			} catch (UnavailableComponentException e) {
				stack(e);
				throw new ProfileException("Error getting segment for normalising", e);
			}

			// Recombine the segment profiles
			IProfile mergedProfile = new DefaultProfile(IProfile.merge(finalSegmentProfiles));
	        if(mergedProfile.size()!=size())
	        	throw new ProfileException(String.format("Frankenprofile has a different length (%d) to source profile (%d)", mergedProfile.size(), size()));
	        
			ISegmentedProfile result = new DefaultSegmentedProfile(mergedProfile, template.getSegments());
			return result;
		}

		/**
		 * The interpolation step of frankenprofile creation. The segment in this
		 * profile, with the same name as the template segment is interpolated to
		 * the length of the template, and returned as a new Profile.
		 * 
		 * @param templateSegment the segment to interpolate
		 * @param newLength the new length of the segment profile
		 * @return the interpolated profile
		 * @throws ProfileException
		 */
		private IProfile interpolateSegment(IBorderSegment testSeg, int newLength) throws ProfileException {
			
			
			

			// get the region within the segment as a new profile
			// Exclude the last index of each segment to avoid duplication
			// the first index is kept, because the first index is used for border
			// tags
			int lastIndex = CellularComponent.wrapIndex(testSeg.getEndIndex() - 1, testSeg.getProfileLength());

			IProfile testSegProfile = this.getSubregion(testSeg.getStartIndex(), lastIndex);

			// interpolate the test segments to the length of the median segments
			IProfile revisedProfile = testSegProfile.interpolate(newLength);
			return revisedProfile;
		}


		@Override
		public void reverse() {
			super.reverse();
			
			// Update the root segment
			int oldRootSegmentStart = segments.startIndex;
			segments.startIndex = size()-1-oldRootSegmentStart;
			
			if(!segments.hasMergeSources())
				return;
			
			/*
			 * A profile is shown with indexes 0-5, and values a-f.
			 * There are 3 segments, 2-4; 4-5; 5-2.
			 * 
			 *  s1         s2         
			 *   s2        a      s2
			 *      f      0    b
			 *       5        \
			 *                 1
			 *       |          | 
			 *       4          2
			 *      e      3     c
			 *    s1       d      s2
			 *   s0                  s0
			 *            s0          
			 *                      
			 *                      
			 */

			List<BorderSegmentTree> newSegs = new ArrayList<>();
			for (BorderSegmentTree seg : segments.leaves) {
				newSegs.add(seg.reverse(segments));
			}
			
			segments.clearMergeSources();
			Collections.reverse(newSegs);
			for (BorderSegmentTree seg : newSegs) {
				segments.addMergeSource(seg);
			}
		}

		@Override
		public void mergeSegments(@NonNull UUID segment1, @NonNull UUID segment2, @NonNull UUID id) throws ProfileException {

			// Check the segments belong to the profile
			if (!this.hasSegment(segment1) || !this.hasSegment(segment2))
				throw new IllegalArgumentException("An input segment is not part of this profile");

			IBorderSegment seg1;
			IBorderSegment seg2;
			try {
				seg1 = getSegment(segment1);
				seg2 = getSegment(segment2);
			} catch (UnavailableComponentException e) {
				throw new IllegalArgumentException("An input segment is not part of this profile");
			}
			
			// Check the segments are linked
			if(! (seg1.hasNextSegment() && seg1.hasPrevSegment() && seg2.hasNextSegment() && seg2.hasPrevSegment()))
				throw new IllegalArgumentException("Input segments do not have next and previous segments set; cannot validate merge");
			
			if (!seg1.nextSegment().equals(seg2) && !seg1.prevSegment().equals(seg2))
				throw new IllegalArgumentException(String.format("Input segment 1 (%s) is not linked to segment 2 (%s)",seg1.toString(), seg2.toString() ));

			
			// Ensure we have the segments in the correct order
			IBorderSegment firstSegment  = seg1.nextSegment().equals(seg2) ? seg1 : seg2;
			IBorderSegment secondSegment = seg2.nextSegment().equals(seg1) ? seg1 : seg2;
			
			
//			System.out.println(String.format("Atttempting to merge segment 1 (%s) with segment 2 (%s)",seg1.toString(), seg2.toString() ));
			

			// Create the new segment
			int startIndex = firstSegment.getStartIndex();
			int endIndex   = secondSegment.getEndIndex();
			
			int newLength = firstSegment.testLength(startIndex, endIndex);
			BorderSegmentTree mergedSegment = new BorderSegmentTree(id, startIndex, newLength, segments);
			
			mergedSegment.addMergeSource(firstSegment);
			mergedSegment.addMergeSource(secondSegment);
			
			// Clear the old segments
			List<BorderSegmentTree> newSegs = new ArrayList<>();
			// Replace the two segments in this profile
			for (BorderSegmentTree oldSegment : segments.leaves) {

				if (oldSegment.equals(firstSegment)) {
					newSegs.add(mergedSegment);
				} else if (oldSegment.equals(secondSegment)) {
					// do nothing
				} else {
					// add the original segments
					newSegs.add(oldSegment);
				}
			}
			segments.clearMergeSources();
			for (BorderSegmentTree seg : newSegs) {
				segments.addMergeSource(seg);
			}
		}
		
		@Override
		public void mergeSegments(@NonNull IBorderSegment segment1, @NonNull IBorderSegment segment2, @NonNull UUID id) throws ProfileException {
			mergeSegments(segment1.getID(), segment2.getID(), id);
		}
		
		@Override
		public void unmergeSegment(@NonNull IBorderSegment segment) throws ProfileException {
			// Check the segments belong to the profile
			if (!this.contains(segment))
				throw new IllegalArgumentException("Input segment is not part of this profile");

			if (!segment.hasMergeSources())
				return;

			List<BorderSegmentTree> newSegs = new ArrayList<>();
			
			for (BorderSegmentTree oldSegment : segments.leaves) {

				if (oldSegment.equals(segment)) { // segment to unmerge

					for (BorderSegmentTree mergedSegment : oldSegment.leaves) {
						newSegs.add(mergedSegment);
					}

				} else { // all other segments
					newSegs.add(oldSegment);
				}
			}

			segments.clearMergeSources();
			for (BorderSegmentTree seg : newSegs) {
				segments.addMergeSource(seg);
			}

		}

		@Override
		public boolean isSplittable(@NonNull UUID id, int splitIndex) {
			if (!this.hasSegment(id)) {
				throw new IllegalArgumentException("No segment with the given id");
			}

			IBorderSegment segment;
			try {
				segment = getSegment(id);
			} catch (UnavailableComponentException e) {
				stack(e);
				return false;
			}

			if (!segment.contains(splitIndex)) {
				throw new IllegalArgumentException("Splitting index is not within the segment");
			}

			return IBorderSegment.isLongEnough(segment.getStartIndex(), splitIndex, this.size())
					&& IBorderSegment.isLongEnough(splitIndex, segment.getEndIndex(), this.size());
		}

		@Override
		public void splitSegment(@NonNull IBorderSegment segment, int splitIndex, @NonNull UUID id1, @NonNull UUID id2) throws ProfileException {

			if (!this.contains(segment))
				throw new IllegalArgumentException("Input segment is not part of this profile");

			if (!segment.contains(splitIndex))
				throw new IllegalArgumentException("Splitting index is not within the segment");
						
			if(segments.id.equals(segment.getID())) {
				// splitting the single root segment, clean the slate
				segments.clearMergeSources();
				
				int length1 = segments.testLength(segments.startIndex, splitIndex);
				int length2 = segments.testLength(splitIndex, segments.getEndIndex());
				
				segments.addMergeSource(new BorderSegmentTree(id1, segments.startIndex, length1, segments));
				segments.addMergeSource(new BorderSegmentTree(id2, splitIndex, length2, segments));	
				return;
			}
			
			// Otherwise we are splitting one of the child segments
			
			List<BorderSegmentTree> newSegs = new ArrayList<>();

			// Replace the two segments in this profile
			for (BorderSegmentTree oldSegment : segments.leaves) {
				if (oldSegment.equals(segment)) {
					
					int length1 = segments.testLength(oldSegment.startIndex, splitIndex);
					int length2 = segments.testLength(splitIndex, oldSegment.getEndIndex());
					
					newSegs.add(new BorderSegmentTree(id1, oldSegment.getStartIndex(), length1, segments));
					newSegs.add(new BorderSegmentTree(id2, splitIndex, length2, segments));	
				} else {
					newSegs.add(oldSegment);
				}
			}
			
			// Clear the old segments and replace with the split segments
			segments.clearMergeSources();
			for (BorderSegmentTree seg : newSegs) {
				segments.addMergeSource(seg);
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			for (IBorderSegment seg : getSegments()) {
				builder.append(seg.toString() + " | ");
			}
			return builder.toString();
		}

		@Override
		public String valueString() {
			return super.toString();
		}

		@Override
		public ISegmentedProfile copy() throws ProfileException{
			finer("Copying profile "+toString());
			return new DefaultSegmentedProfile(this);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((segments == null) ? 0 : segments.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {        	  
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (getClass() != obj.getClass())
				return false;
			DefaultSegmentedProfile other = (DefaultSegmentedProfile) obj;
			if (segments == null) {
				if (other.segments != null)
					return false;
			} else if (!segments.equals(other.segments))
				return false;
			return true;
		}

		/**
		 * Wrap the segment index to allow incrementing of segment indices in loops
		 * @param i the segment index to wrap
		 * @return the wrapped index
		 */
		protected int wrapSegmentIndex(int i){
			if (i < 0)
				return wrapSegmentIndex(segments.nChildren() + i);
			if (i < segments.nChildren())
				return i;
			return i % segments.nChildren();
		}

		/**
		 * Store segments of the profile in a hierarchical tree. When segments are split,
		 * new nodes are created. When segments are merged, a new node is created, with child nodes
		 * for the original segment.
		 * 
		 * 		Root                          Root                         Root
		 * 		/  \     -> split a          / |  \    -> merge a1 & a2    /  \
		 *     a    b                       a1 a2  b                      a3   b
		 *                                                               /  \
		 *                                                             a1    a2
		 *                                                             
		 * Splitting a segment is destructive: the original segment ID will not be recovered
		 * upon merging, even if the segment bounds are identical.
		 * 
		 * Requesting the list of segments will return the segments which are direct children of the 
		 * root node. 
		 * 
		 * The root node is a single segment covering the profile.
		 * @author bms41
		 *
		 */
		public class BorderSegmentTree implements IBorderSegment {
			private static final long serialVersionUID = 1L;
			protected final BorderSegmentTree parent;
			private final List<BorderSegmentTree> leaves = new LinkedList<>();
			private final UUID id;
			private int startIndex = 0;
			private int length = 0;
			private boolean isLocked = false;


			/**
			 * Construct a segment covering the entire profile
			 * @param id
			 */
			protected BorderSegmentTree(@NonNull UUID id){
				this(id, 0, size()+1, null);
			}
			
			/**
			 * Construct with a desired start and length. The segment will not have a parent segment
			 * @param id the segment id
			 * @param start the start index
			 * @param length the segment length
			 */
			protected BorderSegmentTree(@NonNull UUID id, int start, int length){
				this(id, start, length, null);
			}
			
			/**
			 * Construct with a desired start and length and parent segment
			 * @param id the segment id
			 * @param start the start index
			 * @param length the segment length
			 * @param parent the parent segment (can be null)
			 */
			protected BorderSegmentTree(@NonNull UUID id, int start, int length, @Nullable BorderSegmentTree parent){
				finer(String.format("Creating new segment at %d of length %d with parent %s", start, length, parent));
				if(start<0 || start > size())
					throw new IllegalArgumentException(String.format("Start index %d is outside profile bounds", start));
				if(length<0 || length > size()+1)
					throw new IllegalArgumentException(String.format("Segment length %d is outside profile bounds", length));
				if(length<IBorderSegment.MINIMUM_SEGMENT_LENGTH)
					throw new IllegalArgumentException(String.format("Segment length %d is below minimum %d", length, MINIMUM_SEGMENT_LENGTH));
				
				this.id = id;
				this.startIndex = start;
				this.length = length;
				this.parent = parent;
				leaves.clear();
			}
			
			
			/**
			 * Construct by copying existing segments
			 * @param seg
			 * @param parent
			 */
			protected BorderSegmentTree(@NonNull IBorderSegment seg, @Nullable BorderSegmentTree parent) {
				this(seg.getID(), seg.getStartIndex(), seg.wraps()?seg.length()-1:seg.length(), parent);
			}	
			
			@Override
			public IBorderSegment copy() {
				return new BorderSegmentTree(this, parent);
			}

			@Override
			public @NonNull UUID getID() {
				return id;
			}
			
			public int nChildren() {
				return leaves.size();
			}
			
			/**
			 * Reverse the segment within the profile. This returns a new segment with 
			 * the start and end indexes in the appropriate positions for profile reversal
			 * @return
			 */
			private BorderSegmentTree reverse(BorderSegmentTree parent) {
				int oldEnd = getEndIndex();
				int newStart = size()-1-oldEnd;
				BorderSegmentTree reversed = new BorderSegmentTree(id, newStart, length, parent);
				for(BorderSegmentTree s : leaves) {
					reversed.addMergeSource(s.reverse(reversed));
				}
				return reversed;
			}
			
			public List<UUID> getChildIds(){
				return leaves.stream().map(s->s.getID()).collect(Collectors.toList());
			}

			@Override
			public @NonNull List<IBorderSegment> getMergeSources() {
				if(leaves.isEmpty())
					return new ArrayList<>();
				return leaves.stream().collect(Collectors.toList());
			}
			
			public void splitAt(int index, @NonNull UUID a, @NonNull UUID b) throws ProfileException {
				if(!contains(index))
					throw new IllegalArgumentException("Index is outside segment bounds");
				
				splitSegment(this, index, a, b);
			}
			
			private void addMergeSource(@NonNull BorderSegmentTree mergeSource) {
				leaves.add(mergeSource);
			}

			@Override
			public void addMergeSource(@NonNull IBorderSegment seg) {
				if(seg==null)
					throw new IllegalArgumentException("Segment is null");
				if(seg.getProfileLength()!=size())
					throw new IllegalArgumentException("Segment does not come from the same profile");
				if(!contains(seg.getStartIndex()) || !contains(seg.getEndIndex()))
					throw new IllegalArgumentException(String.format("Potential merge source (%s) is not contained within this segment (%s)", seg.getDetail(), getDetail()));
				
				if(!leaves.isEmpty()) {
					BorderSegmentTree prevSegment = leaves.get(leaves.size()-1);

					if(prevSegment.getEndIndex()!=seg.getStartIndex())
						throw new IllegalArgumentException(String.format("Potential merge source start (%d) does not overlap previous merge source end (%d)", seg.getStartIndex(), prevSegment.getEndIndex()));
					
				}
				leaves.add(new BorderSegmentTree(seg, this));
			}

			@Override
			public void clearMergeSources() {
				leaves.clear();
			}

			@Override
			public boolean hasMergeSources() {
				return !leaves.isEmpty();
			}
			
			@Override
			public boolean hasMergeSource(@NonNull UUID uuid) {
				if(this.id.equals(uuid))
					return true;
				for(BorderSegmentTree s : leaves) {
					if(s.hasMergeSource(uuid))
						return true;
				}
				return false;
			}
			
			@Override
			public IBorderSegment getMergeSource(@NonNull UUID uuid) throws UnavailableComponentException {
				
				if(this.id.equals(uuid))
					return this;
				for(BorderSegmentTree s : leaves) {
					if(s.hasMergeSource(uuid))
						return s.getMergeSource(uuid);
				}
				throw new UnavailableComponentException("Merge source not present: "+uuid);
			}

			@Override
			public int getStartIndex() {
				return startIndex;
			}

			@Override
			public int getEndIndex() {
				return wrap(startIndex+length-1);
			}

			@Override
			public int getProportionalIndex(double d) {
				if (d < 0 || d > 1)
					throw new IllegalArgumentException("Proportion must be between 0-1: " + d);

				double targetLength = length * d;
				int absLength = (int) Math.round(targetLength);
				return wrap(startIndex + absLength);
			}

			@Override
			public double getIndexProportion(int index) {
				if (!contains(index))
					throw new IllegalArgumentException("Index out of segment bounds: " + index);
				return (double) getShortestDistanceToStart(index) / (double) length;
			}



			@Override
			public String getName() {
				return "Seg_"+getPosition();
			}

			@Override
			public int getMidpointIndex() {
				// If a segment is length 3 covering 0-1-2, the midpoint is 1
				// If a segment is length 4 covering 0-1-2-3, the midpoint is 1 (lower of the two possible indices)
				
				int mid = length%2==0 ? ((length-1)/2) : length/2;
				return wrap(startIndex+mid); 
			}

			@Override
			public int getShortestDistanceToStart(int index) {
				int d1 = wrap(index-startIndex);
				int d2 = wrap(startIndex-index);
				return d1<d2?d1:d2;
			}
			
			@Override
			public int getInternalDistanceToStart(int index) {
				if(wraps() && startIndex>index)
					return index + (getBorderLength()-startIndex);
				return index-startIndex;
			}

			@Override
			public int getShortestDistanceToEnd(int index) {
				
				int d1 = wrap(index-getEndIndex());
				int d2 = wrap(getEndIndex()-index);
				return d1<d2?d1:d2;
			}
			
			@Override
			public int getInternalDistanceToEnd(int index) {
				if(wraps() && getEndIndex()<index)
					return getEndIndex() + (getBorderLength()-index);
				return index-getEndIndex();
			}

			@Override
			public boolean isLocked() {
				return isLocked;
			}

			@Override
			public void setLocked(boolean b) {
				isLocked=b;
			}
			
			@Override
			public void offset(int amount) {
				this.startIndex = wrapIndex(startIndex+amount);
				for(BorderSegmentTree s : leaves) {
					s.offset(amount);
				}
			}

			@Override
			public int getProfileLength() {
				return size();
			}

			@Override
			public IBorderSegment nextSegment() {
				if(parent==null)
					return this;
				int thisIndex = parent.leaves.indexOf(this);
				return thisIndex==parent.leaves.size()-1
						? parent.leaves.get(0)
						: parent.leaves.get(thisIndex+1);
			}

			@Override
			public IBorderSegment prevSegment() {
				if(parent==null)
					return this;
				int thisIndex = parent.leaves.indexOf(this);
				return thisIndex==0
						? parent.leaves.get(parent.leaves.size()-1)
						: parent.leaves.get(thisIndex-1);
			}

			@Override
			public int length() {
				return testLength(startIndex, getEndIndex());
			}

			@Override
			public int testLength(int start, int end) {
				if(wraps(start, end))
					return end+size()+1-start+1; // add total of 2; one for index 0 and one for segment end
				return end-start+1; // add one for segment end
			}

			@Override
			public boolean wraps() {
				return getEndIndex()<=startIndex;
			}

			@Override
			public boolean contains(int index) {
				if(startIndex==getEndIndex())
					return true;
				return wraps() ? index<=getEndIndex() || index>=getStartIndex() 
						: index>=getStartIndex() && index <=getEndIndex();
			}

			@Override
			public boolean update(int startIndex, int endIndex) throws SegmentUpdateException {
				if(isLocked())
					throw new SegmentUpdateException("Segment is locked");
								
				// Check the incoming data
		        if (startIndex < 0 || startIndex > size()-1)
		            throw new SegmentUpdateException("Start index is outside the profile range: " + startIndex);
		        if (endIndex < 0 || endIndex > size()-1)
		            throw new SegmentUpdateException("End index is outside the profile range: " + endIndex);

				// Ensure next and prev segments cannot be 'jumped over'
				if( startIndex < getStartIndex() && !prevSegment().contains(startIndex))
					throw new SegmentUpdateException(String.format("Previous segment %s does not contain start index %d when decreasing", prevSegment().getDetail(), startIndex));
				if( endIndex > getEndIndex() && !nextSegment().contains(endIndex))
					throw new SegmentUpdateException(String.format("Next segment %s does not contain end index %d when increasing", nextSegment().getDetail(), endIndex));

				 // Ensure previous and next segments are not locked
		        if(startIndex!=this.startIndex && prevSegment().isLocked())
		        	throw new SegmentUpdateException(String.format("Update from %s to %d-%d affects previous segment (%s), which is locked", getDetail(), startIndex, endIndex, prevSegment().getDetail()));
		        if(endIndex!=this.getEndIndex() && nextSegment().isLocked())
		        	throw new SegmentUpdateException(String.format("Update from %s to %d-%d affects next segment (%s), which is locked", getDetail(), startIndex, endIndex, nextSegment().getDetail()));
				
				// Ensure next and previous segments will not become too short
				if(prevSegment().getShortestDistanceToStart(startIndex)<MINIMUM_SEGMENT_LENGTH)
					throw new SegmentUpdateException(String.format("Cannot update start to %d; previous segment (%s) will become too short", startIndex, prevSegment().getDetail()));
				if(nextSegment().getShortestDistanceToEnd(endIndex)<MINIMUM_SEGMENT_LENGTH)
					throw new SegmentUpdateException(String.format("Cannot update end to %d in profile of length %d; next segment (%s) will become too short", endIndex, size(), nextSegment().getDetail()));
				
				// Ensure this segment will not become too short
				int newLength = testLength(startIndex, endIndex);
				if(newLength<MINIMUM_SEGMENT_LENGTH)
					throw new SegmentUpdateException(String.format("Segment will become too short (%d)", newLength));
				
				// All checks passed
				
				fine(String.format("Updating segment to %d to %d", startIndex, endIndex));
				this.startIndex = startIndex;
				this.length = newLength;
				
				// Pass on the updated positions to the surrounding segments
				int segIndex = segments.leaves.indexOf(this);
				
				BorderSegmentTree nextSeg = segments.leaves.get(wrapSegmentIndex(segIndex+1));
				nextSeg.startIndex = getEndIndex();
				nextSeg.length = testLength(endIndex, nextSeg.getEndIndex());
				
				BorderSegmentTree prevSeg = segments.leaves.get(wrapSegmentIndex(segIndex-1));
				prevSeg.length = testLength(prevSeg.getStartIndex(), endIndex);
				
				return true;
			}

			@Override
			public void setNextSegment(@NonNull IBorderSegment s) {
				// Does nothing in this implementation
			}

			@Override
			public void setPrevSegment(@NonNull IBorderSegment s) {
				// Does nothing in this implementation
			}

			@Override
			public boolean hasNextSegment() {
				// By default in this implementation
				return true;
			}

			@Override
			public boolean hasPrevSegment() {
				// By default in this implementation
				return true;
			}

			@Override
			public void setPosition(int i) {
				// Does nothing in this implementation
			}

			@Override
			public int getPosition() {
				if(parent==null)
					return 0;
				return parent.leaves.indexOf(this);
			}

			@Override
			public Iterator<Integer> iterator() {
				if(wraps())
					return IntStream.concat(IntStream.range(startIndex, size()), IntStream.range(0, getEndIndex())).iterator();
				return IntStream.range(startIndex, wrap(startIndex+length)).iterator();
			}

			@Override
			public String getDetail() {
				return String.format("Segment %s | %s | %s | %s - %s | %s of %s | %s ", 
						getName(), getID(), getPosition(), getStartIndex(), getEndIndex(), length(), getProfileLength(), wraps());
			}

		    @Override
		    public boolean overlapsBeyondEndpoints(@NonNull IBorderSegment seg){
		    	if(seg==null)
		    		return false;
		    	if(seg.getProfileLength()!=getProfileLength())
					return false;
		    	
		    	Iterator<Integer> it = iterator();
		    	while(it.hasNext()) {
		    		int index = it.next();
		    		if(index==startIndex || index==getEndIndex())
		    			continue;
		    		if(seg.contains(index))
		    			return true;
		    	}
		    	return false;
		    }
		    
		    @Override
		    public boolean overlaps(@NonNull IBorderSegment seg){
		    	if(seg==null)
		    		return false;
		    	if(seg.getProfileLength()!=getProfileLength())
					return false;
				return seg.contains(startIndex) 
						|| seg.contains(getEndIndex()) 
						|| contains(seg.getStartIndex()) 
						|| contains(seg.getEndIndex());
		    }
			
			@Override
			public String toString() {
				return String.format("%d - %d of %d", startIndex, getEndIndex(), size());
			}
		}


	}



}
