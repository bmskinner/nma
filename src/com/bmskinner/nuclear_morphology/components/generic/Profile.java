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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.AbstractCellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;

/**
 * Holds arrays of values with wrapping and provides methods to manipulate them.
 * Used for distance and angle profiles.
 * 
 * @author bms41
 *
 */
@Deprecated
public class Profile implements IProfile {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    protected final double[] array;

    /**
     * Constructor for a new Profile, based on an array of values.
     * 
     * @param values
     *            the array to use
     */
    public Profile(final double[] values) {

        if (values.length == 0) {
            throw new IllegalArgumentException("Input array has zero length in profile constructor");
        }
        this.array = new double[values.length];
        for (int i = 0; i < this.array.length; i++) {
            array[i] = values[i];
        }
    }

    /**
     * Constructor based on an existing Profile. Makes a copy of the existing
     * Profile
     * 
     * @param p
     *            the profile to copy
     */
    public Profile(final IProfile p) {
        if (p == null) {
            throw new IllegalArgumentException("Input profile is null in profile constructor");
        }

        this.array = new double[p.size()];

        for (int i = 0; i < p.size(); i++) {
            array[i] = p.get(i);
        }
    }

    /**
     * Constructor based on an fixed value and the profile length
     * 
     * @param value
     *            the value for the profile to hold at each index
     * @param length
     *            the length of the profile
     */
    public Profile(final double value, final int length) {

        if (length < 1) {
            throw new IllegalArgumentException("Profile length cannot be less than 1");
        }

        this.array = new double[length];
        for (int i = 0; i < this.array.length; i++) {
            array[i] = value;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#size()
     */
    @Override
    public int size() {
        return array.length;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(array);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
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
        Profile other = (Profile) obj;
        if (!Arrays.equals(array, other.array))
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#get(int)
     */
    @Override
    public double get(int index) throws IndexOutOfBoundsException {

        if (index < 0 || index >= array.length) {
            throw new IndexOutOfBoundsException(
                    "Requested value " + index + " is beyond profile end of " + array.length);
        }
        return array[index];

    }

    @Override
    public double get(double prop) {

        if (prop < 0 || prop > 1) {
            throw new IndexOutOfBoundsException("Value " + prop + " must be between 0-1");
        }

        int index = this.getIndexOfFraction(prop);

        return array[index];

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getMax()
     */
    @Override
    public double getMax() {
        double max = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getIndexOfMax(components.generic.
     * BooleanProfile)
     */
    @Override
    public int getIndexOfMax(BooleanProfile limits) {
        double max = 0;
        int maxIndex = 0;
        for (int i = 0; i < array.length; i++) {
            if (limits.get(i) && array[i] > max) {
                max = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getIndexOfMax()
     */
    @Override
    public int getIndexOfMax() {

        BooleanProfile b = new BooleanProfile(this, true);
        return getIndexOfMax(b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getProportionalIndex(double)
     */
    @Override
    public int getIndexOfFraction(double d) {
        if (d < 0 || d > 1) {
            throw new IllegalArgumentException("Proportion must be between 0-1: " + d);
        }

        double desiredDistanceFromStart = (double) array.length * d;

        int target = (int) desiredDistanceFromStart;

        return target;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getIndexProportion(int)
     */
    @Override
    public double getFractionOfIndex(int index) {
        if (index < 0 || index >= this.size()) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }

        double fractionalDistance = (double) array.length * (double) index;

        return fractionalDistance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getMin()
     */
    @Override
    public double getMin() {
        double min = this.getMax();
        for (int i = 0; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getIndexOfMin(components.generic.
     * BooleanProfile)
     */
    @Override
    public int getIndexOfMin(BooleanProfile limits) {
        double min = this.getMax();

        int minIndex = 0;

        for (int i = 0; i < array.length; i++) {
            if (limits.get(i) && array[i] < min) {
                min = array[i];
                minIndex = i;
            }
        }

        return minIndex;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getIndexOfMin()
     */
    @Override
    public int getIndexOfMin() {

        BooleanProfile b = new BooleanProfile(this, true);
        return getIndexOfMin(b);
    }


    /**
     * Check the lengths of the two profiles. Return the first profile
     * interpolated to the length of the longer.
     * 
     * @param profile1
     *            the profile to return interpolated
     * @param profile2
     *            the profile to compare
     * @return a new profile with the length of the longest input profile
     * @throws ProfileException
     */
    private IProfile equaliseLengths(IProfile profile1, IProfile profile2) throws ProfileException {
        if (profile1 == null || profile2 == null) {
            throw new IllegalArgumentException("Input profile is null when equilising lengths");
        }
        // profile 2 is smaller
        // return profile 1 unchanged
        if (profile2.size() < profile1.size()) {
            return profile1;
        } else {
            // profile 1 is smaller; interpolate to profile 2 length
            profile1 = profile1.interpolate(profile2.size());
        }

        return profile1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfile#absoluteSquareDifference(components.generic.
     * IProfile)
     */
    @Override
    public double absoluteSquareDifference(IProfile testProfile) throws ProfileException {

        if (testProfile == null) {
            throw new IllegalArgumentException("Test profile is null");
        }
        // the test profile needs to be matched to this profile
        // whichever is smaller must be interpolated
        IProfile profile1 = equaliseLengths(this.copy(), testProfile);
        IProfile profile2 = equaliseLengths(testProfile, this.copy());

        double difference = 0;

        for (int j = 0; j < profile1.size(); j++) { // for each point round the
                                                    // array

            double thisValue = profile1.get(j);
            double testValue = profile2.get(j);
            difference += Math.pow(thisValue - testValue, 2); // square
                                                              // difference -
                                                              // highlights
                                                              // extremes
        }
        return difference;
    }

    /*
     * -------------------- Profile manipulation --------------------
     */

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#copy()
     */
    @Override
    public IProfile copy() {
        return new Profile(this.array);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#offset(int)
     */
    @Override
    public Profile offset(int j) throws ProfileException {
        double[] newArray = new double[this.size()];
        for (int i = 0; i < this.size(); i++) {
            newArray[i] = this.array[AbstractCellularComponent.wrapIndex(i + j, this.size())];
        }
        return new Profile(newArray);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#smooth(int)
     */
    @Override
    public IProfile smooth(int windowSize) {

        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position

            double[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots
                                                                                  // for
                                                                                  // previous
                                                                                  // angles
            double[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

            double average = array[i];
            for (int k = 0; k < prevValues.length; k++) {
                average += prevValues[k] + nextValues[k];
            }

            result[i] = average / (windowSize * 2 + 1);
        }
        return new Profile(result);
    }

    /**
     * Get an array of the values <i>windowSize</i> before or after the current point
     * 
     * @param position
     *            the position in the array
     * @param windowSize
     *            the number of points to find
     * @param type
     *            find points before or after
     * @return an array of values
     */
    private double[] getValues(int position, int windowSize, int type) {

        double[] values = new double[windowSize]; // slots for previous angles
        for (int j = 0; j < values.length; j++) {

            // If type was before, multiply by -1; if after, multiply by 1
            int index = AbstractCellularComponent.wrapIndex(position + ((j + 1) * type), this.size());
            values[j] = array[index];
        }
        return values;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#reverse()
     */
    @Override
    public void reverse() {

        double tmp;
        for (int i = 0; i < this.array.length / 2; i++) {
            tmp = this.array[i];
            this.array[i] = this.array[this.array.length - 1 - i];
            this.array[this.array.length - 1 - i] = tmp;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#interpolate(int)
     */
    @Override
    public IProfile interpolate(int newLength) {

        if (newLength < this.size()) {
            // finer("Interpolating to a smaller array!");
        }

        double[] newArray = new double[newLength];

        // where in the old curve index is the new curve index?
        for (int i = 0; i < newLength; i++) {
            // we have a point in the new curve.
            // we want to know which points it lay between in the old curve
            double oldIndex = ((double) i / (double) newLength) * (double) array.length; // get
                                                                                         // the
                                                                                         // fractional
                                                                                         // index
                                                                                         // position
                                                                                         // needed

            // get the value in the old profile at the given fractional index
            // position
            newArray[i] = interpolateValue(oldIndex);
        }
        return new Profile(newArray);
    }

    /**
     * Take an index position from a non-normalised profile. Normalise it Find
     * the corresponding angle in the median curve. Interpolate as needed
     * 
     * @param normIndex
     *            the fractional index position to find within this profile
     * @return an interpolated value
     */
    private double interpolateValue(double normIndex) {

        // convert index to 1 window boundaries
        // This allows us to see the array indexes above and below the desired
        // fractional index. From these, we can interpolate the fractional
        // component.
        // NOTE: this does not account for curves. Interpolation is linear.
        int index1 = (int) Math.round(normIndex);
        int index2 = index1 > normIndex ? index1 - 1 : index1 + 1;

        // Decide which of the two indexes is the higher, and which is the lower
        int indexLower = index1 < index2 ? index1 : index2;

        int indexHigher = index2 > index1 ? index2 : index1;

        // System.out.println("Set indexes "+normIndex+":
        // "+indexLower+"-"+indexHigher);

        // wrap the arrays
        indexLower = AbstractCellularComponent.wrapIndex(indexLower, array.length);
        indexHigher = AbstractCellularComponent.wrapIndex(indexHigher, array.length);
        // System.out.println("Wrapped indexes "+normIndex+":
        // "+indexLower+"-"+indexHigher);

        // get the values at these indexes
        double valueHigher = array[indexHigher];
        double valueLower = array[indexLower];
        // System.out.println("Wrapped values "+normIndex+": "+valueLower+" and
        // "+valueHigher);

        // calculate the difference between values
        // this can be negative
        double valueDifference = valueHigher - valueLower;
        // System.out.println("Difference "+normIndex+": "+valueDifference);

        // calculate the distance into the region to go
        double offset = normIndex - indexLower;
        // System.out.println("Offset "+normIndex+": "+offset);

        // add the offset to the lower index
        double positionToFind = indexLower + offset;
        positionToFind = AbstractCellularComponent.wrapIndex(positionToFind, array.length);
        // System.out.println("Position to find "+normIndex+":
        // "+positionToFind);

        // calculate the value to be added to the lower index value
        double newValue = valueDifference * offset; // 0 for 0, full difference
                                                    // for 1
        // System.out.println("New value "+normIndex+": "+newValue);

        double linearInterpolatedValue = newValue + valueLower;
        // System.out.println("Interpolated "+normIndex+":
        // "+linearInterpolatedValue);

        return linearInterpolatedValue;
    }

    /*
     * Interpolate another profile to match this, and move this profile along it
     * one index at a time. Find the point of least difference, and return this
     * offset. Returns the positive offset to this profile
     */
    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfile#getSlidingWindowOffset(components.generic.
     * IProfile)
     */
    @Override
    public int getSlidingWindowOffset(IProfile testProfile) {

        int index = 0;
        try {
            double lowestScore = this.absoluteSquareDifference(testProfile);

            for (int i = 0; i < this.size(); i++) {

                IProfile offsetProfile;

                offsetProfile = this.offset(i);

                double score = offsetProfile.absoluteSquareDifference(testProfile);
                if (score < lowestScore) {
                    lowestScore = score;
                    index = i;
                }
            }

        } catch (ProfileException e) {
            warn("Cannot offset profile");
            stack("Error getting offset profile", e);
        }

        return index;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getConsistentRegionBounds(double,
     * double, int)
     */
    @Override
    public int[] getConsistentRegionBounds(double value, double tolerance, int points) {

        int counter = 0;
        int start = -1;
        int end = -1;
        int[] result = { start, end };

        for (int index = 0; index < array.length; index++) { // go through each
                                                             // point TODO
                                                             // wrapping
            double d = array[index];
            if (d > value - tolerance && d < value + tolerance) { // if the
                                                                  // point meets
                                                                  // criteria

                if (start == -1) { // start a new region if needed
                    counter = 0;
                    start = index;
                }
                counter++; // start counting a new region or increase an
                           // existing region

            } else { // does not meet criteria

                end = index;

                if (counter >= points) { // if the region is large enough
                    // return points
                    result[0] = start; // use the saved start and end indexes
                    result[1] = end;
                    return result;

                } else { // otherwise, reset the counter

                    start = -1;
                    end = -1;
                }

            }
        }
        return result;
    }

    /*
     * -------------------- Detect minima within the profiles
     * --------------------
     */

    /*
     * For each point in the array, test for a local minimum. The values of the
     * points <minimaLookupDistance> ahead and behind are checked. Each should
     * be greater than the value before. One exception is allowed, to account
     * for noisy data. Returns the indexes of minima
     */
    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getLocalMinima(int)
     */
    @Override
    public BooleanProfile getLocalMinima(int windowSize) {
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

                int prev_i = AbstractCellularComponent.wrapIndex(i - (j + 1), this.size()); // the
                                                                                            // index
                                                                                            // j+1
                                                                                            // before
                                                                                            // i
                int next_i = AbstractCellularComponent.wrapIndex(i + (j + 1), this.size()); // the
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

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getLocalMinima(int, double)
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getLocalMinima(int, double, double)
     */
    @Override
    public BooleanProfile getLocalMinima(int windowSize, double threshold, double fraction) {
        BooleanProfile minima = getLocalMinima(windowSize, threshold);

        boolean[] values = new boolean[this.size()];

        double fractionThreshold = (this.getMax() - this.getMin()) * fraction;

        for (int i = 0; i < array.length; i++) {

            if (minima.get(i) == true && (this.get(i) > fractionThreshold || this.get(i) < -fractionThreshold)) {
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
     * @see components.generic.IProfile#getLocalMaxima(int)
     */
    @Override
    public BooleanProfile getLocalMaxima(int windowSize) {
        // go through array
        // look at points ahead and behind.
        // if all lower, local maximum

        boolean[] result = new boolean[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position

            double[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots
                                                                                  // for
                                                                                  // previous
                                                                                  // angles
            double[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

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

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getLocalMaxima(int, double)
     */
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
     * @see components.generic.IProfile#getLocalMaxima(int, double, double)
     */
    @Override
    public BooleanProfile getLocalMaxima(int windowSize, double threshold, double fraction) {
        BooleanProfile minima = getLocalMaxima(windowSize, threshold);

        boolean[] values = new boolean[this.size()];

        double fractionThreshold = this.getMax() - this.getMin() * fraction;

        for (int i = 0; i < array.length; i++) {

            if (minima.get(i) == true && (this.get(i) > fractionThreshold || this.get(i) < -fractionThreshold)) {
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

        double[] result = new double[windowSize * 2 + 1];

        double[] prevValues = getValues(index, windowSize, Profile.ARRAY_BEFORE); // slots
                                                                                  // for
                                                                                  // previous
                                                                                  // angles
        double[] nextValues = getValues(index, windowSize, Profile.ARRAY_AFTER);

        // need to reverse the previous array
        for (int k = prevValues.length, i = 0; k > 0; k--, i++) {
            result[i] = prevValues[k - 1];
        }
        result[windowSize] = array[index];
        for (int i = 0; i < nextValues.length; i++) {
            result[windowSize + i + 1] = nextValues[i];
        }

        return new Profile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getSubregion(int, int)
     */
    @Override
    public IProfile getSubregion(int indexStart, int indexEnd) {

        if (indexStart < indexEnd) {

            double[] result = Arrays.copyOfRange(array, indexStart, indexEnd);
            return new Profile(result);

        } else { // case when array wraps

            double[] resultA = Arrays.copyOfRange(array, indexStart, array.length - 1);
            double[] resultB = Arrays.copyOfRange(array, 0, indexEnd);
            double[] result = new double[resultA.length + resultB.length];
            int index = 0;
            for (double d : resultA) {
                result[index] = d;
                index++;
            }
            for (double d : resultB) {
                result[index] = d;
                index++;
            }

            if (result.length == 0) {
                log(Level.SEVERE, "Subregion length zero: " + indexStart + " - " + indexEnd);
            }
            return new Profile(result);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#getSubregion(components.nuclear.
     * NucleusBorderSegment)
     */
    @Override
    public IProfile getSubregion(IBorderSegment segment) {

        if (segment == null) {
            throw new IllegalArgumentException("Segment is null");
        }

        if (segment.getTotalLength() != this.size()) {
            throw new IllegalArgumentException("Segment comes from a different length profile");
        }
        return getSubregion(segment.getStartIndex(), segment.getEndIndex());
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#calculateDeltas(int)
     */
    @Override
    public IProfile calculateDeltas(int windowSize) {

        double[] deltas = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm

            double[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots
                                                                                  // for
                                                                                  // previous
                                                                                  // angles
            double[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

            double delta = 0;
            for (int k = 0; k < prevValues.length; k++) {

                if (k == 0) {
                    delta += (array[i] - prevValues[k]) + (nextValues[k] - array[i]);

                } else {
                    delta += (prevValues[k] - prevValues[k - 1]) + (nextValues[k] - nextValues[k - 1]);
                }

            }

            deltas[i] = delta;
        }
        IProfile result = new Profile(deltas);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#power(double)
     */
    @Override
    public IProfile power(double exponent) {
        double[] values = new double[this.size()];

        for (int i = 0; i < array.length; i++) {
            values[i] = Math.pow(array[i], exponent);
        }
        return new Profile(values);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#absolute()
     */
    @Override
    public IProfile absolute() {
        double[] values = new double[this.size()];

        for (int i = 0; i < array.length; i++) {
            values[i] = Math.abs(array[i]);
        }
        return new Profile(values);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#cumulativeSum()
     */
    @Override
    public IProfile cumulativeSum() {
        double[] values = new double[this.size()];

        double total = 0;
        for (int i = 0; i < array.length; i++) {
            total += array[i];
            values[i] = total;
        }
        return new Profile(values);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#multiply(double)
     */
    @Override
    public IProfile multiply(double multiplier) {
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = array[i] * multiplier;
        }
        return new Profile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#multiply(components.generic.IProfile)
     */
    @Override
    public IProfile multiply(IProfile multiplier) {
        if (this.size() != multiplier.size()) {
            throw new IllegalArgumentException("Profile sizes do not match");
        }
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = array[i] * multiplier.get(i);
        }
        return new Profile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#divide(double)
     */
    @Override
    public IProfile divide(double divider) {
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = array[i] / divider;
        }
        return new Profile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#divide(components.generic.IProfile)
     */
    @Override
    public IProfile divide(IProfile divider) {
        if (this.size() != divider.size()) {
            throw new IllegalArgumentException("Profile sizes do not match");
        }
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = array[i] / divider.get(i);
        }
        return new Profile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#add(components.generic.IProfile)
     */
    @Override
    public IProfile add(IProfile adder) {
        if (this.size() != adder.size()) {
            throw new IllegalArgumentException("Profile sizes do not match");
        }
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = array[i] + adder.get(i);
        }
        return new Profile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#add(double)
     */
    @Override
    public IProfile add(double value) {

        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = array[i] + value;
        }
        return new Profile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#subtract(components.generic.IProfile)
     */
    @Override
    public IProfile subtract(IProfile sub) {
        if (this.size() != sub.size()) {
            throw new IllegalArgumentException("Profile sizes do not match");
        }
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = array[i] - sub.get(i);
        }
        return new Profile(result);
    }
    
    @Override
    public IProfile subtract(double value) {

        if (Double.isNaN(value) || Double.isInfinite(value))
            throw new IllegalArgumentException("Cannot subtract NaN or infinity");

        double[] result = new double[array.length];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (double) (array[i] - value);
        }
        return new DoubleProfile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#toString()
     */
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
     * @param list
     *            the list of profiles to merge
     * @return the merged profile
     */
    public static Profile merge(List<IProfile> list) {
        if (list == null || list.size() == 0) {
            throw new IllegalArgumentException("Profile list is null or empty");
        }
        Profile result;
        // List<Double> combinedList = new ArrayList<Double>(0);
        int totalLength = 0;
        for (IProfile p : list) {
            totalLength += p.size();
        }

        double[] combinedArray = new double[totalLength];
        int i = 0;
        for (IProfile p : list) {

            for (int j = 0; j < p.size(); j++) {
                combinedArray[i++] = p.get(j);
            }
        }

        result = new Profile(combinedArray);
        return result;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // finest("\tWriting profile");
        out.defaultWriteObject();
        // finest("\tWrote profile");
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // finest("\tReading profile");
        in.defaultReadObject();
        // finest("\tRead profile");
    }

    @Override
    public float[] toFloatArray() {
        float[] result = new float[array.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (float) array[i];
        }
        return result;
    }

    @Override
    public double[] toDoubleArray() {
        return array;
    }

}
