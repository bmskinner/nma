/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.generic;

import java.io.Serializable;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * A profile contains an array of values, and this interface details both access
 * to those values, and the transformations that can be applied to the profile.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface IProfile extends Serializable, Loggable {

    static final int ARRAY_BEFORE = -1;
    static final int ARRAY_AFTER  = 1;
    static final int ZERO_INDEX   = 0;
    static final int MINIMUM_PROFILE_LENGTH = 3;

    /**
     * Create a new profile of the default type
     * @param array the array of values
     * @return a profile
     */
    static IProfile makeNew(float[] array) {
        return new FloatProfile(array);
    }

    /**
     * Create a new profile of the default type
     * @param array the array of values
     * @return a profile
     */
    static IProfile makeNew(double[] array) {
        float[] d = new float[array.length];
        for(int i=0; i<array.length; i++){
            d[i] = (float) array[i];
        }
        return new FloatProfile(d);
    }
    
    static IProfile merge(@NonNull List<IProfile> list){
        if (list == null || list.size() == 0)
            throw new IllegalArgumentException("Profile list is null or empty");

        int totalLength = 0;
        for (IProfile p : list) {
            totalLength += p.size();
        }

        float[] combinedArray = new float[totalLength];

        int i = 0;

        for (IProfile p : list) {

            for (int j = 0; j < p.size(); j++) {
                combinedArray[i++] = (float) p.get(j);
            }
        }

        return new FloatProfile(combinedArray);
    }

    /**
     * Get the length of the array in the profile
     * 
     * @return the size of the profile
     */
    int size();

    /**
     * Get the value at the given index
     * 
     * @param index the index
     * @return the value at the index
     * @throws IndexOutOfBoundsException if the index is not in the profile
     */
    double get(int index) throws IndexOutOfBoundsException;

    /**
     * Get the value at the given proportion along the profile
     * 
     * @param proportion the proportion from 0-1
     * @return the value at the index closest to the given proportion
     */
    double get(double proportion);

    /**
     * Get the maximum value in the profile
     * 
     * @return the maximum value
     */
    double getMax();

    /**
     * Get the index of the maximum value in the profile If there are multiple
     * values at maximum, this returns the first only
     * 
     * @param limits indexes that should be included or excluded from the search
     * @return the index of the first maximum value
     * @throws ProfileException
     */
    int getIndexOfMax(@NonNull BooleanProfile limits) throws ProfileException;

    /**
     * Get the index of the maximum value in the profile If there are multiple
     * values at maximum, this returns the first only
     * 
     * @return the index of the first maximum value
     * @throws ProfileException
     */
    int getIndexOfMax() throws ProfileException;

    /**
     * Get the index closest to the fraction of the way through the profile
     * 
     * @param d a fraction between 0 (start) and 1 (end)
     * @return the nearest index
     */
    int getIndexOfFraction(double d);

    /**
     * Get the fractional distance of the given index along the profile from
     * zero to one.
     * 
     * @param index the index to test
     * @return the proportional position of the index in the profile
     */
    double getFractionOfIndex(int index);

    /**
     * Get the minimum value in the profile.If there are multiple values at
     * minimum, this returns the first only
     * 
     * @return the minimum value in the profile
     */
    double getMin();

    /**
     * Get the index of the minimum value in the profile
     * 
     * @param limits indexes that should be included or excluded from the search
     * @return the index
     * @throws ProfileException
     */
    int getIndexOfMin(@NonNull BooleanProfile limits) throws ProfileException;

    /**
     * Get the index of the minimum value in the profile
     * 
     * @return the index
     * @throws ProfileException
     */
    int getIndexOfMin() throws ProfileException;

    /**
     * Calculate the sum-of-squares difference between this profile and a given
     * profile. The shorter profile is interpolated. The testProfile is assumed to
     * be offset appropriately to avoid spurious differences.
     * 
     * For example:
     * 
     * <pre>Profile A    compared to    Profile B</pre>
     * <pre>111222111                   111242110</pre>
     * <pre>    *   *                       *   *</pre>
     * 
     * has a difference at one index of 2, and at another of 1. The sum of squares difference
     * is 2^2 + 1^2 = 5.
     * <br>
     * The order of comparisons does not matter when profiles are different lengths, since the
     * shorter profile is interpolated. That is, {@code shortProfile.absoluteSquareDifference(longProfile)}
     * will yield the same result as {@code longProfile.absoluteSquareDifference(shortProfile)}.
     * <p>
     * However, when comparing multiple profiles it is advisable to normalise their lengths <b>before</b>
     * running the square difference calculation. 
     * 
     * @param testProfile the profile to compare to this profile
     * @return the sum-of-squares difference
     * @throws ProfileException if interpolation cannot be performed
     */
    double absoluteSquareDifference(@NonNull IProfile testProfile) throws ProfileException;

    /**
     * Alternative to the constructor from profile
     * 
     * @return a new profile of the same class with the same values as this profile
     */
    IProfile copy();

    /**
     * Create a profile offset to start from the given index. For example, a
     * profile { 1, 2, 3, 4} offset by 1 will become { 2, 3, 4, 1 }

     * @param j the index to set as index zero
     * @return a new offset profile
     * @throws ProfileException
     */
    IProfile offset(int j) throws ProfileException;

    /**
     * Perform a window-averaging smooth of the profile with the given window
     * size. E.g. smoothing with window size 2 will average across 5 points:
     * 2 behind, the index being smoothed, and 2 ahead.
     * 
     * @param windowSize the size of the window
     * @return a new smoothed profile
     */
    IProfile smooth(int windowSize);

    /**
     * Reverse the profile. Does not copy.
     * 
     * @throws Exception
     */
    void reverse();

    /**
     * Make this profile the length specified.
     * 
     * @param newLength
     *            the new array length
     * @return an interpolated profile
     * @throws ProfileException
     */
    IProfile interpolate(int newLength) throws ProfileException;

    /**
     * Interpolate another profile to match this, and move this profile along it
     * one index at a time. Find the point of least difference, and return the
     * offset.  It will always return a positive value.
     * 
     * @param testProfile
     * @return the offset to this profile that must be applied to match the test
     *         profile
     * @throws ProfileException
     */
    int getSlidingWindowOffset(@NonNull IProfile testProfile) throws ProfileException;

    /**
     * For each point in the array, test for a local minimum. The values of the
     * points <i>windowSize</i> ahead and behind are checked. Each should be
     * greater than the value before. One exception is allowed, to account for
     * noisy data.
     * 
     * @param windowSize the number of indices ahead and behind to check
     * @return a boolean profile with the indexes of local minima as true and
     *         all other indexes false
     */
    BooleanProfile getLocalMinima(int windowSize);

    /**
     * Get the local minima that are beloe a threshold value
     * 
     * @param windowSize the minima window size
     * @param threshold the threshold value which minima must be below
     * @return a profile describing the locations of minima
     */
    BooleanProfile getLocalMinima(int windowSize, double threshold);

    /**
     * Get the points considered local maxima for the given window size.
     * 
     * @param windowSize the window size to use
     * @return
     */
    BooleanProfile getLocalMaxima(int windowSize);

    /**
     * Get the local maxima that are above a threshold value
     * 
     * @param windowSize the maxima window size
     * @param threshold the threshold
     * @return
     */
    BooleanProfile getLocalMaxima(int windowSize, double threshold);

    /**
     * Get the windowSize points around a point of interest.
     * 
     * For example, requesting getWindow(5, 2) would return the 
     * indexes up to 2 away from 5 : 3, 4, 5, 6, 7
     * 
     * @param index the index position to centre on
     * @param windowSize the number of points either side
     * @return a profile with the window
     */
    IProfile getWindow(int index, int windowSize);

    /**
     * Fetch a sub-region of the profile as a new profile
     * 
     * @param indexStart the index to begin (inclusive)
     * @param indexEnd the index to end (inclusive)
     * @return a profile with the selected region
     */
    IProfile getSubregion(int indexStart, int indexEnd);

    /**
     * Fetch a sub-region of the profile defined by the given segment. The
     * segment must originate from an equivalent profile (i.e. have the same
     * totalLength as the profile)
     * 
     * @param segment the segment to find
     * @return a profile with the selected region
     * @throws ProfileException
     */
    IProfile getSubregion(@NonNull IBorderSegment segment) throws ProfileException;

    /**
     * Sum the difference between between each pair of values across a
     * given window size around each point. E.g. the profile 1-2-3-4-5 with 
     * a window size of 1 will give (1-5=-4) + (2-1=1) = -3 for index 0
     * 
     * @param windowSize the window size
     * @return a profile with the delta values
     */
    IProfile calculateDeltas(int windowSize);


    /**
     * Raise the values in the profile to the given exponent
     * 
     * @param base
     * @return
     */
    IProfile power(double exponent);

    /**
     * Get the absolute values from a profile
     * 
     * @return
     */
    IProfile absolute();

    /**
     * Get the cumulative sum of the values in the profile
     * 
     * @return
     */
    IProfile cumulativeSum();


    /**
     * Multiply all values within the profile by the value within the given
     * Profile
     * 
     * @param multiplier
     *            the profile to multiply by. Must be the same length as this
     *            profile
     * @return the new profile
     */
    IProfile multiply(@NonNull IProfile multiplier);
    
    /**
     * Multiply all values within the profile by a given value
     * 
     * @param multiplier
     *            the value to multiply by
     * @return the new profile
     */
    IProfile multiply(double multiplier);
    
    /**
     * Divide all values within the profile by the values within the given
     * Profile
     * 
     * @param divider the profile to divide by. Must be the same length as this profile
     * @return the new profile
     */
    IProfile divide(@NonNull IProfile divider);

    /**
     * Divide all values within the profile by a given value
     * 
     * @param divider the value to divide by
     * @return the new profile
     */
    IProfile divide(double divider);


    /**
     * Add all values within the profile by the value within the given Profile
     * 
     * @param adder
     *            the profile to add. Must be the same length as this profile
     * @return the new profile
     */
    IProfile add(@NonNull IProfile adder);

    /**
     * Add the given value to all points within the profile
     * 
     * @param adder the value to add.
     * @return the new profile
     */
    IProfile add(double value);

    /**
     * Subtract all values within the profile by the value within the given
     * Profile
     * 
     * @param adder the profile to subtract. Must be the same length as this
     *            profile
     * @return the new profile
     */
    IProfile subtract(@NonNull IProfile sub);
    
    /**
     * Subtract all values within the profile by the given constant
     * 
     * @param value the value to subtract
     * @return the new profile
     */
    IProfile subtract(double value);

    
    /**
     * Get the underlying array as a float array. This may involve loss of
     * precision.
     * 
     * @return
     */
    float[] toFloatArray();

    /**
     * Get the underlying array as a double array
     * 
     * @return
     */
    double[] toDoubleArray();
    
    /**
     * Wrap the given index into the current profile
     * For example, here is a profile of length 10 (indexes 0-9),
     * with some indexes to be accessed beyond each end of the profile:
     * <p>
     * <pre>     0        9  <br> ----|--------|----<br>-4                13</pre>
     * <p>
     * These indexes need to be wrapped to their appropriate index in the profile, preserving the number of
     * index steps.
     * <p>
     * Starting with index -4, the wrapped index is 6:
     * <p>
     * <pre> 67890     6789  <br> ----|--------|<br>     |     ----</pre>
     * For with index 13, the wrapped index is 3:
     * <p>
     * <pre>     0123     90123  <br>     |--------|----<br>     ----     |</pre>
     * 
     * 
     * @param index the index to wrap
     * @return
     */
    int wrap(int index);
    
}
