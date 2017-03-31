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

package com.bmskinner.nuclear_morphology.components.generic;

import java.io.Serializable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter.ArrayConversionException;

/**
 * A profile contains an array of values, and this interface details
 * both access to those values, and the transformations that can be applied
 * to the profile.
 * @author ben
 * @since 1.13.3
 *
 */
public interface IProfile 
	extends Serializable, Loggable {

	static final int ARRAY_BEFORE = -1;
	static final int ARRAY_AFTER  = 1;
	static final int ZERO_INDEX   = 0;

	
	static IProfile makeNew(float[] array){
		return new FloatProfile(array);
	}
	
	static IProfile makeNew(double[] array){
		try {
			return new FloatProfile(new ArrayConverter(array).toFloatArray());
		} catch (ArrayConversionException e) {
			return null;
		}
	}
	
	/**
	 * Get the length of the array in the profile
	 * @return the size of the profile
	 */
	int size();

	/**
	 * Get the value at the given index
	 * @param index the index
	 * @return the value at the index
	 * @throws Exception 
	 */
	double get(int index) throws IndexOutOfBoundsException;
	
	
	/**
	 * Get the value at the given proportion along the profile
	 * @param proportion the proportion from 0-1
	 * @return
	 */
	double get(double proportion);

	/**
	 * Get the maximum value in the profile
	 * @return the maximum value
	 */
	double getMax();

	/**
	 * Get the index of the maximum value in the profile
	 * If there are multiple values at maximum, this returns the
	 * first only
	 * @param limits indexes that should be included or excluded from the search
	 * @return the index
	 */
	int getIndexOfMax(BooleanProfile limits);

	/**
	 * Get the index of the maximum value in the profile
	 * If there are multiple values at maximum, this returns the
	 * first only
	 * @return the index
	 */
	int getIndexOfMax();

	
	/**
	 * Get the index closest to the fraction
	 * of the way through the profile
	 * @param d a fraction between 0 (start) and 1 (end)
	 * @return the nearest index
	 */
	int getIndexOfFraction(double d);

	/**
	 * Get the fractional distance of the given index along the profile
	 * from zero to one.
	 * @param index the index to test
	 * @return
	 */
	double getFractionOfIndex(int index);
	
	/**
	 * Get the minimum value in the profile.If there are multiple values 
	 * at minimum, this returns the first only
	 * @return the minimum value
	 */
	double getMin();

	/**
	 * Get the index of the minimum value in the profile
	 * @param limits indexes that should be included or excluded from the search
	 * @return the index
	 */
	int getIndexOfMin(BooleanProfile limits);

	/**
	 * Get the index of the minimum value in the profile
	 * @return the index
	 */
	int getIndexOfMin();

	/**
	 * Get the array from the profile
	 * @return an array of values
	 */
//	double[] asArray();

	/**
	 * Get an X-axis; get a position
	 * for each point on the scale 0-length
	 * @param length the length to scale to
	 * @return a profile with the positions as values
	 */
	IProfile getPositions(int length);

	/**
	 * Get the position of an index on an X-axis, rescaled to the 
	 * new length 
	 * @param index
	 * @param newLength
	 * @return
	 */
	double getRescaledIndex(int index, int newLength);

	/**
	 * Calculate the square differences between this profile and
	 * a given profile. The shorter profile is interpolated.
	 * The testProfile must have been offset appropriately to avoid 
	 * spurious differences.
	 * @param testProfile the profile to compare to 
	 * @return the sum-of-squares difference
	 * @throws ProfileException 
	 */
	double absoluteSquareDifference(IProfile testProfile) throws ProfileException;

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
	double weightedSquareDifference(IProfile testProfile) throws Exception;

	/**
	 * Alternative to the constructor from profile
	 * @return a new profile with the same values as this
	 */
	IProfile copy();

	/**
	 * Copy the profile and offset it to start from the given index
	 * @param j the index to start from
	 * @return a new offset Profile
	 * @throws ProfileException 
	 * @throws Exception 
	 */
	IProfile offset(int j) throws ProfileException;

	/**
	 * Perform a window-averaging smooth of the profile with the given window size
	 * @param windowSize the size of the window
	 * @return
	 */
	IProfile smooth(int windowSize);

	/**
	 * Reverse the profile. Does not copy.
	 * @throws Exception 
	 */
	void reverse();

	/**
	 * Make this profile the length specified.
	 * @param newLength the new array length
	 * @return an interpolated profile
	 * @throws ProfileException 
	 */
	IProfile interpolate(int newLength) throws ProfileException;

	/*
	    Interpolate another profile to match this, and move this profile
	    along it one index at a time. Find the point of least difference, 
	    and return this offset. Returns the positive offset to this profile
	 */
	int getSlidingWindowOffset(IProfile testProfile) throws ProfileException;

	/**
	 * Detect regions with a consistent value in a profile
	 * @param value the profile value that is to be maintained
	 * @param tolerance the variation allow plus or minus
	 * @points the number of points the value must be sustained over
	 * @return the first and last index in the profile covering the detected region
	 */
	int[] getConsistentRegionBounds(double value, double tolerance, int points);

	/*
	    For each point in the array, test for a local minimum.
	    The values of the points <minimaLookupDistance> ahead and behind are checked.
	    Each should be greater than the value before.
	    One exception is allowed, to account for noisy data. Returns the indexes of minima
	 */
	BooleanProfile getLocalMinima(int windowSize);

	/**
	 * Get the local maxima that are above a threshold
	 * value
	 * @param windowSize the maxima window size
	 * @param threshold the threshold
	 * @return
	 */
	BooleanProfile getLocalMinima(int windowSize, double threshold);

	/**
	 * Get the local minima that are above a threshold
	 * value and have an absolute value greater than the given 
	 * fraction of the total value range in the profile
	 * @param windowSize the maxima window size
	 * @param threshold the threshold
	 * @param fraction the fraction threshold
	 * @return
	 */
	BooleanProfile getLocalMinima(int windowSize, double threshold,
			double fraction);

	/**
	 * Get the points considered local maxima for the given window
	 * size as a Profile. Maxima are 1, other points are 0
	 * @param windowSize the window size to use
	 * @return
	 */
	BooleanProfile getLocalMaxima(int windowSize);

	/**
	 * Get the local maxima that are above a threshold
	 * value
	 * @param windowSize the maxima window size
	 * @param threshold the threshold
	 * @return
	 */
	BooleanProfile getLocalMaxima(int windowSize, double threshold);

	/**
	 * Get the local maxima that are above a threshold
	 * value and have an absolute value greater than the given 
	 * fraction of the total value range in the profile
	 * @param windowSize the maxima window size
	 * @param threshold the threshold
	 * @param fraction the fraction threshold
	 * @return
	 */
	BooleanProfile getLocalMaxima(int windowSize, double threshold,
			double fraction);

	/**
	 * Get the windowSize points around a point of interest
	 * @param index the index position to centre on
	 * @param windowSize the number of points either side
	 * @return a profile with the window
	 */
	IProfile getWindow(int index, int windowSize);

	/**
	 * Fetch a sub-region of the profile as a new profile
	 * @param indexStart the index to begin
	 * @param indexEnd the index to end
	 * @return a Profile
	 */
	IProfile getSubregion(int indexStart, int indexEnd);

	/**
	 * Fetch a sub-region of the profile defined by the given segment. The segment
	 * must originate from an equivalent profile (i.e. have the same totalLength
	 * as the profile)
	 * @param segment the segment to find
	 * @return a Profile
	 * @throws ProfileException 
	 */
	IProfile getSubregion(IBorderSegment segment) throws ProfileException;

	/**
	 * Calculate the differences between the previous and next indexes
	 * across a given window size around this point
	 * @param windowSize
	 * @return
	 */
	IProfile calculateDeltas(int windowSize);

	/**
	 * Calculate the difference between each value and the previous value, 
	 * and the difference between each value and the next value, and returns the sums
	 * as a newP rofile 
	 * @return
	 */
	IProfile differentiate();

	/**
	 * Log transform the profile to the given base
	 * @param base
	 * @return
	 */
	IProfile log(double base);

	/**
	 * Raise the values in the profile to the given exponent
	 * @param base
	 * @return
	 */
	IProfile power(double exponent);

	/**
	 * Get the absolute values from a profile
	 * @return
	 */
	IProfile absolute();

	/**
	 * Get the cumulative sum of the values in the
	 * profile
	 * @return
	 */
	IProfile cumulativeSum();

	/**
	 * Multiply all values within the profile by a given value
	 * @param multiplier the value to multiply by
	 * @return the new profile
	 */
	IProfile multiply(double multiplier);

	/**
	 * Multiply all values within the profile by the value within the given Profile
	 * @param multiplier the profile to multiply by. Must be the same length as this profile
	 * @return the new profile
	 */
	IProfile multiply(IProfile multiplier);

	/**
	 * Divide all values within the profile by a given value
	 * @param divider the value to divide by
	 * @return the new profile
	 */
	IProfile divide(double divider);

	/**
	 * Divide all values within the profile by the values within the given Profile
	 * @param divider the profile to divide by. Must be the same length as this profile
	 * @return the new profile
	 */
	IProfile divide(IProfile divider);

	/**
	 * Add all values within the profile by the value within the given Profile
	 * @param adder the profile to add. Must be the same length as this profile
	 * @return the new profile
	 */
	IProfile add(IProfile adder);

	/**
	 * Add the given value to all points within the profile
	 * @param adder the value to add.
	 * @return the new profile
	 */
	IProfile add(double value);

	/**
	 * Subtract all values within the profile by the value within the given Profile
	 * @param adder the profile to subtract. Must be the same length as this profile
	 * @return the new profile
	 */
	IProfile subtract(IProfile sub);

	/**
	 * Get the rank of each value in the profile
	 * after sorting ascending
	 * @return a profile of rank values
	 */
	IProfile getRanks();

	/**
	 * Get the indexes of sorted values in the profile.
	 * Example: A 4 element profile has the values { 10, 5, 1, 2 }
	 * This function will return the indexes { 3, 2, 0, 1 },
	 * corresponding to the order of the values after sorting.
	 * @return a profile containing index values
	 */
	IProfile getSortedIndexes();

	String toString();

	/**
	 * Get the underlying array as a float array.
	 * This may involve loss of precision.
	 * @return
	 */
	float[] toFloatArray();
	
	/**
	 * Get the underlying array as a double array
	 * @return
	 */
	double[] toDoubleArray();

}