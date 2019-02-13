/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.generic;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;

/**
 * A profile with double precision
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class DoubleProfile extends AbstractProfile implements IProfile {

    private static final long serialVersionUID = 1L;
    protected final double[]  array;

    /**
     * Constructor for a new Profile, based on an array of values.
     * 
     * @param values
     *            the array to use
     */
    public DoubleProfile(final double[] values) {

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
    public DoubleProfile(final IProfile p) {
        if (p == null) {
            throw new IllegalArgumentException("Input profile is null in profile constructor");
        }

        this.array = new double[p.size()];

        for (int i = 0; i < p.size(); i++) {
            array[i] = (double) p.get(i);
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
    public DoubleProfile(final double value, final int length) {

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
        DoubleProfile other = (DoubleProfile) obj;
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
                    "Requested value " + index + " is beyond profile end (" + array.length + ")");
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

    @Override
    public int getIndexOfMax(BooleanProfile limits) throws ProfileException {
    	if(limits==null)
            throw new IllegalArgumentException("Limits are cannot be null");
        if ( limits.size() != array.length)
            throw new IllegalArgumentException("Limits are wrong size for this profile");
        double max = 0;
        int maxIndex = -1;
        for (int i = 0; i < array.length; i++) {
            if (limits.get(i) && array[i] > max) {
                max = array[i];
                maxIndex = i;
            }
        }
        if (maxIndex == -1) {
            throw new ProfileException("No valid index for maximum value");
        }
        return maxIndex;
    }

    @Override
    public int getIndexOfFraction(double d) {
        if (d < 0 || d > 1)
            throw new IllegalArgumentException("Proportion must be between 0-1: " + d);
        double desiredDistanceFromStart = (double) array.length * d;

        return (int) desiredDistanceFromStart;
    }

    @Override
    public double getFractionOfIndex(int index) {
        if (index < 0 || index >= this.size())
            throw new IllegalArgumentException("Index out of bounds: " + index);
        return (double) index / (double) array.length;
    }

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


    @Override
    public int getIndexOfMin(BooleanProfile limits) throws ProfileException {
    	if(limits==null)
            throw new IllegalArgumentException("Limits are cannot be null");
        if ( limits.size() != array.length)
            throw new IllegalArgumentException("Limits are wrong size for this profile");
        double min = this.getMax();

        int minIndex = -1;

        for (int i = 0; i < array.length; i++) {
            if (limits.get(i) && array[i] < min) {
                min = array[i];
                minIndex = i;
            }
        }
        if (minIndex == -1) {
            throw new ProfileException("No valid index for min value");
        }
        return minIndex;
    }


    @Override
    public double absoluteSquareDifference(IProfile testProfile) throws ProfileException {

        
    	double[] arr2 = testProfile.toDoubleArray();


		if (array.length == arr2.length)
			return CellularComponent.squareDifference(array, arr2);

		if (array.length > arr2.length) {
			arr2 = IProfile.interpolate(testProfile.toDoubleArray(), array.length);
			return CellularComponent.squareDifference(array, arr2);
		} 

		
		double[] arr1 = IProfile.interpolate(array, arr2.length);
		return CellularComponent.squareDifference(arr1, arr2);
    }
    
    @Override
  	public double absoluteSquareDifference(@NonNull IProfile testProfile, int interpolationLength) throws ProfileException {
  		float[] arr1 = IProfile.interpolate(this.toFloatArray(), interpolationLength);
  		float[] arr2 = IProfile.interpolate(testProfile.toFloatArray(), interpolationLength);
  		return CellularComponent.squareDifference(arr1, arr2);
  	}

    @Override
    public IProfile copy() {
        return new DoubleProfile(this.array);
    }

    @Override
    public IProfile offset(int j) throws ProfileException {
        double[] newArray = new double[this.size()];
        for (int i = 0; i < this.size(); i++) {
            newArray[i] = this.array[CellularComponent.wrapIndex(i + j, this.size())];
        }
        return new DoubleProfile(newArray);
    }

    @Override
    public IProfile smooth(int windowSize) {
        if(windowSize < 1)
            throw new IllegalArgumentException("Window size must be a positive integer");
        
        double[] result = new double[size()];

        for (int i = 0; i < array.length; i++) { // for each position

            double[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE); // slots
                                                                                   // for
                                                                                   // previous
                                                                                   // angles
            double[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);

            double average = array[i];
            for (int k = 0; k < prevValues.length; k++) {
                average += prevValues[k] + nextValues[k];
            }

            result[i] = (double) (average / (windowSize * 2 + 1));
        }
        return new DoubleProfile(result);
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
            int index = CellularComponent.wrapIndex(position + ((j + 1) * type), this.size());
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
    public IProfile interpolate(int newLength) throws ProfileException {
        if(newLength < 1)
            throw new IllegalArgumentException("New length must be longer than 1");

        double[] newArray = new double[newLength];

        // where in the old curve index is the new curve index?
        for (int i = 0; i < newLength; i++) {
            // we have a point in the new curve.
            // we want to know which points it lay between in the old curve
            double oldIndex = ((double) i / (double) newLength) * (double) array.length;

            // get the value in the old profile at the given fractional index
            // position
            newArray[i] = interpolateValue(oldIndex);
        }
        return new DoubleProfile(newArray);
    }

    /**
     * Take an index position from a non-normalised profile. Normalise it Find
     * the corresponding angle in the median curve. Interpolate as needed
     * 
     * @param normIndex the fractional index position to find within this profile
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


        // wrap the arrays
        indexLower = CellularComponent.wrapIndex(indexLower, array.length);
        indexHigher = CellularComponent.wrapIndex(indexHigher, array.length);
        // System.out.println("Wrapped indexes "+normIndex+":
        // "+indexLower+"-"+indexHigher);

        // get the values at these indexes
        double valueHigher = array[indexHigher];
        double valueLower = array[indexLower];
        // calculate the difference between values
        // this can be negative
        double valueDifference = valueHigher - valueLower;

        // calculate the distance into the region to go
        double offset = normIndex - indexLower;

        // add the offset to the lower index
        double positionToFind = indexLower + offset;
        positionToFind = (double) CellularComponent.wrapIndex(positionToFind, array.length);

        // calculate the value to be added to the lower index value
        double newValue = valueDifference * offset; // 0 for 0, full difference
                                                    // for 1

        double linearInterpolatedValue = newValue + valueLower;


        return linearInterpolatedValue;
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
        if (windowSize < 1)
            throw new IllegalArgumentException("Window size must be a positive integer greater than 0");
        // go through angle array (with tip at start)
        // look at 1-2-3-4-5 points ahead and behind.
        // if all greater, local minimum
        double[] prevValues = new double[windowSize];
        double[] nextValues = new double[windowSize];


        boolean[] minima = new boolean[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm

            // go through each lookup position and get the appropriate angles
            for (int j = 0; j < prevValues.length; j++) {

                int prev_i = CellularComponent.wrapIndex(i - (j + 1), this.size());
                int next_i = CellularComponent.wrapIndex(i + (j + 1), this.size());

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

    @Override
    public BooleanProfile getLocalMaxima(int windowSize) {
        if (windowSize < 1)
            throw new IllegalArgumentException("Window size must be a positive integer greater than 0");

        boolean[] result = new boolean[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position

            double[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE); // slots
                                                                                   // for
                                                                                   // previous
                                                                                   // angles
            double[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);

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

    @Override
    public IProfile getWindow(int index, int windowSize) {

        double[] result = new double[windowSize * 2 + 1];

        double[] prevValues = getValues(index, windowSize, IProfile.ARRAY_BEFORE);
        double[] nextValues = getValues(index, windowSize, IProfile.ARRAY_AFTER);

        // need to reverse the previous array
        for (int k = prevValues.length, i = 0; k > 0; k--, i++) {
            result[i] = prevValues[k - 1];
        }
        result[windowSize] = array[index];
        for (int i = 0; i < nextValues.length; i++) {
            result[windowSize + i + 1] = nextValues[i];
        }

        return new DoubleProfile(result);
    }

    @Override
    public IProfile getSubregion(int indexStart, int indexEnd) {
    	 if (indexStart >= array.length)
             throw new IllegalArgumentException(String.format("Start index (%d) is beyond array length (%d)", indexStart, array.length));

         if (indexEnd >= array.length)
             throw new IllegalArgumentException(String.format("End index (%d) is beyond array length (%d)", indexEnd, array.length));
         
         if(indexStart < 0 || indexEnd < 0)
             throw new IllegalArgumentException(String.format("Start (%d) or end index (%d) is below zero", indexStart, indexEnd));

        if (indexStart < indexEnd) 
            return new DoubleProfile(Arrays.copyOfRange(array, indexStart, indexEnd+1));
        
		double[] resultA = Arrays.copyOfRange(array, indexStart, array.length);
		double[] resultB = Arrays.copyOfRange(array, 0, indexEnd+1);
		double[] result = new double[resultA.length + resultB.length];
		int index = 0;
		for (double d : resultA) {
		    result[index++] = d;
		}
		
		for (double d : resultB) {
		    result[index++] = d;
		}
		return new DoubleProfile(result);
    }

    @Override
    public IProfile calculateDeltas(int windowSize) {
    	
        if (windowSize<1)
            throw new IllegalArgumentException("Window size must be a positive integer");   	

        double[] deltas = new double[array.length];

        for (int i = 0; i < array.length; i++) {

            double[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE);
            double[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);

            double delta = 0;
            for (int k = 0; k < prevValues.length; k++) {
                if (k == 0) {
                    delta += (array[i] - prevValues[k]) + (nextValues[k] - array[i]);
                } else {
                    delta += (prevValues[k - 1] - prevValues[k]) + (nextValues[k] - nextValues[k - 1]);
                }
            }

            deltas[i] = delta;
        }
        return new DoubleProfile(deltas);
    }

    @Override
    public IProfile power(double exponent) {
        double[] values = new double[this.size()];

        for (int i = 0; i < array.length; i++) {
            values[i] = (double) Math.pow(array[i], exponent);
        }
        return new DoubleProfile(values);
    }

    @Override
    public IProfile absolute() {
        double[] values = new double[this.size()];

        for (int i = 0; i < array.length; i++) {
            values[i] = Math.abs(array[i]);
        }
        return new DoubleProfile(values);
    }

    @Override
    public IProfile cumulativeSum() {
    	
    	double[] values = new double[size()];

    	double total = 0;
        for (int i = 0; i < array.length; i++) {
            total += array[i];
            values[i] = total;
        }
        return new DoubleProfile(values);
    }

    @Override
    public IProfile multiply(double multiplier) {
    	if (Double.isNaN(multiplier) || Double.isInfinite(multiplier))
            throw new IllegalArgumentException("Cannot add NaN or infinity");
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (double) (array[i] * multiplier);
        }
        return new DoubleProfile(result);
    }

    @Override
    public IProfile multiply(IProfile multiplier) {
        if (this.size() != multiplier.size())
            throw new IllegalArgumentException("Profile sizes do not match");
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (double) (array[i] * multiplier.get(i));
        }
        return new DoubleProfile(result);
    }

    @Override
    public IProfile divide(double divider) {
    	if (Double.isNaN(divider) || Double.isInfinite(divider))
            throw new IllegalArgumentException("Cannot add NaN or infinity");
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (double) (array[i] / divider);
        }
        return new DoubleProfile(result);
    }

    @Override
    public IProfile divide(IProfile divider) {
        if (this.size() != divider.size())
            throw new IllegalArgumentException("Profile sizes do not match");
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (double) (array[i] / divider.get(i));
        }
        return new DoubleProfile(result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.IProfile#add(components.generic.IProfile)
     */
    @Override
    public IProfile add(IProfile adder) {
        if (this.size() != adder.size())
            throw new IllegalArgumentException("Profile sizes do not match");
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (double) (array[i] + adder.get(i));
        }
        return new DoubleProfile(result);
    }

    @Override
    public IProfile add(double value) {
    	if (Double.isNaN(value) || Double.isInfinite(value))
            throw new IllegalArgumentException("Cannot add NaN or infinity");
        double[] result = new double[size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (double) (array[i] + value);
        }
        return new DoubleProfile(result);
    }

    @Override
    public IProfile subtract(IProfile sub) {
        if (this.size() != sub.size())
            throw new IllegalArgumentException("Profile sizes do not match");
        double[] result = new double[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (double) (array[i] - sub.get(i));
        }
        return new DoubleProfile(result);
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
    
	@Override
	public IProfile normaliseAmplitude(double min, double max) {
		if(Double.isNaN(min) || Double.isNaN(max) || Double.isInfinite(min) || Double.isInfinite(max))
			throw new IllegalArgumentException("New range cannot be NaN or infinite");
		if(min>=max)
			throw new IllegalArgumentException("Min must be less than max in new amplitude");
		
		double oldMin = getMin();
		double oldMax = getMax();
		double newRange = max-min;
		double[] result = new double[array.length];
		
		for (int i = 0; i < array.length; i++) {
			result[i] = (((array[i]/(oldMax-oldMin))*newRange)+min);
		}
		return new DoubleProfile(result);
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
     * @param list the list of profiles to merge
     * @return the merged profile
     */
    public static DoubleProfile merge(List<IProfile> list) {
        if (list == null || list.size() == 0)
            throw new IllegalArgumentException("Profile list is null or empty");

        int totalLength = 0;
        for (IProfile p : list) {
            totalLength += p.size();
        }

        double[] combinedArray = new double[totalLength];

        int i = 0;

        for (IProfile p : list) {

            for (int j = 0; j < p.size(); j++) {
                combinedArray[i++] = (double) p.get(j);
            }
        }

        return new DoubleProfile(combinedArray);
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
        double[] result = new double[array.length];
        System.arraycopy(array, 0, result, 0, array.length);
        return result;
    }

    @Override
	public int wrap(int index) {
		if (index < 0)
            return wrap(size() + index);
        if (index < size())
            return index;
        return index % size();
	}
}
