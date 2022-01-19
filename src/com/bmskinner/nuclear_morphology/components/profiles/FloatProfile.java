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
package com.bmskinner.nuclear_morphology.components.profiles;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * The default implementation of {@link IProfile}, which stores values in float
 * precision.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class FloatProfile implements IProfile {
	
	private static final String XML_PROFILE = "Profile";

    private static final long serialVersionUID = 1L;

    protected final float[] array;

    /**
     * Constructor for a new Profile, based on an array of values.
     * 
     * @param values the array to use
     */
    public FloatProfile(final float[] values) {

        if (values==null || values.length == 0)
            throw new IllegalArgumentException("Input array has zero length in profile constructor");
        this.array = values;
    }

    /**
     * Constructor based on an existing Profile. Makes a copy of the existing
     * Profile
     * 
     * @param p the profile to copy
     */
    public FloatProfile(@NonNull final IProfile p) {
    	if(p instanceof FloatProfile) {
    		FloatProfile other = (FloatProfile)p;
    		this.array = Arrays.copyOf(other.array,other.array.length);
    	} else {
    		this.array = p.toFloatArray();
    	}

    }
    
    /**
     * Constructor based on an fixed value and the profile length
     * 
     * @param value the value for the profile to hold at each index
     * @param length the length of the profile
     */
    public FloatProfile(final float value, final int length) {

        if (length < 1)
            throw new IllegalArgumentException("Profile length cannot be less than 1");

        this.array = new float[length];
        for (int i = 0; i < this.array.length; i++)
            array[i] = value;
    }
    
    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     */
    public FloatProfile(Element e) {
    	String[] s = e.getText()
    			.replace("[", "")
    			.replace("]", "")
    			.split(",");
    	array = new float[s.length];
    	for(int i=0; i<s.length; i++) {
    		array[i] = Float.parseFloat(s[i]);
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
        FloatProfile other = (FloatProfile) obj;
        if (!Arrays.equals(array, other.array))
            return false;
        return true;
    }

    @Override
    public double get(int index) throws IndexOutOfBoundsException {

        if (index < 0 || index >= array.length)
            throw new IndexOutOfBoundsException("Requested value " + index + " is beyond profile end (" + array.length + ")");
        return array[index];

    }

    @Override
    public double get(double prop) {

        if (prop < 0 || prop > 1)
            throw new IndexOutOfBoundsException("Value " + prop + " must be between 0-1");
        int index = getIndexOfFraction(prop);
        return array[index];

    }

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
    public int getIndexOfMax(@NonNull BooleanProfile limits) throws ProfileException {
        
        if(limits==null)
            throw new IllegalArgumentException("Limits are cannot be null");

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

        if (maxIndex == -1) {
            throw new ProfileException("No valid index for maximum value");
        }
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

        double desiredDistanceFromStart = (double) array.length * d;

        int target = (int) desiredDistanceFromStart;

        return target;
    }

    @Override
    public double getFractionOfIndex(int index) {
        if (index < 0 || index >= array.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }

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
    public int getIndexOfMin(@NonNull BooleanProfile limits) throws ProfileException {
        
        if(limits==null)
            throw new IllegalArgumentException("Limits are cannot be null");

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
        if (minIndex == -1) {
            throw new ProfileException("No valid index for minimum value");
        }
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

    @Override
    public double absoluteSquareDifference(@NonNull IProfile testProfile) throws ProfileException {

        double[] arr2 = testProfile.toDoubleArray();
        if (array.length == arr2.length) 
            return CellularComponent.squareDifference(this.toDoubleArray(), arr2);

        // Lengthen the shorter profile
        if (array.length > arr2.length) {
            arr2 = IProfile.interpolate(arr2, array.length);
            return CellularComponent.squareDifference(this.toDoubleArray(), arr2);
        } else {
        	double[] arr1 = IProfile.interpolate(this.toDoubleArray(), arr2.length);
            return CellularComponent.squareDifference(arr1, arr2);
        }
    }
    
    @Override
  	public double absoluteSquareDifference(@NonNull IProfile testProfile, int interpolationLength) throws ProfileException {
  		float[] arr1 = IProfile.interpolate(array, interpolationLength);
  		float[] arr2 = IProfile.interpolate(testProfile.toFloatArray(), interpolationLength);
  		return CellularComponent.squareDifference(arr1, arr2);
  	}

    @Override
    public IProfile copy() throws ProfileException {
        return new FloatProfile(this.array);
    }

    @Override
    public IProfile offset(int j) throws ProfileException {
        float[] newArray = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            newArray[i] = array[wrapIndex(i + j)];
        }
        return new FloatProfile(newArray);
    }

    @Override
    public IProfile smooth(int windowSize) {
        
        if(windowSize < 1)
            throw new IllegalArgumentException("Window size must be a positive integer");

        float[] result = new float[array.length];

        for (int i = 0; i < array.length; i++) { // for each position

            float[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE);
            float[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);

            float average = array[i];
            for (int k = 0; k < prevValues.length; k++) {
                average += prevValues[k] + nextValues[k];
            }

            result[i] = (float) (average / (windowSize*2 + 1));
        }
        return new FloatProfile(result);
    }
    
    
    /**
     * Wrap arrays. If an index falls of the end, it is returned to the start
     * and vice versa
     * 
     * @param i the index
     * @return the index within the array
     */
    protected int wrapIndex(int i){
        if (i < 0) {
            // if the inputs are (-336, 330), this will return -6. Recurse until
            // positive
            i = array.length + i;
            return wrapIndex(i);
        }

        if (i < array.length) { // if not wrapping
            return i;
        }

        return i % array.length;
    }
    
    protected static int wrapIndex(int i, int l){
        if (i < 0) {
            // if the inputs are (-336, 330), this will return -6. Recurse until
            // positive
            i = l + i;
            return wrapIndex(i, l);
        }

        if (i < l) { // if not wrapping
            return i;
        }

        return i % l;
    }
    
    /**
     * Get an array of the values before or after the current index
     * 
     * @param position the position in the array
     * @param windowSize the number of points to find
     * @param type find points before (-1) or after (1)
     * @return an array of values, the first being adjacent to the given position
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
		if(newLength == size())
			return this;
		return new FloatProfile(IProfile.interpolate(array, newLength));
	}

  
	@Override
	public int findBestFitOffset(@NonNull IProfile testProfile) throws ProfileException {
		return findBestFitOffset(testProfile, 0, array.length);
	}
	
	@Override
	public int findBestFitOffset(@NonNull IProfile testProfile, int minOffset, int maxOffset) throws ProfileException {
		float[] test = testProfile.toFloatArray();  
		if (array.length != test.length) 
			test = IProfile.interpolate(test, array.length);
		return CellularComponent.getBestFitOffset(array, test, minOffset, maxOffset);
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
        
        if (windowSize < 1)
            throw new IllegalArgumentException("Window size must be a positive integer greater than 0");
        
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

                int prev_i = wrapIndex(i - (j + 1)); // the
                                                                                    // index
                                                                                    // j+1
                                                                                    // before
                                                                                    // i
                int next_i = wrapIndex(i + (j + 1)); // the
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

        boolean[] values = new boolean[array.length];

        for (int i = 0; i < array.length; i++) {
            values[i] = minima.get(i) && array[i] < threshold;
        }
        return new BooleanProfile(values);
    }

    @Override
    public BooleanProfile getLocalMaxima(int windowSize) {
        
        if (windowSize < 1)
            throw new IllegalArgumentException("Window size must be a positive integer greater than 0");

        boolean[] result = new boolean[this.size()];

        for (int i = 0; i < array.length; i++) {

            float[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE);
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

    @Override
    public IProfile getWindow(int index, int windowSize) {

        float[] result = new float[windowSize * 2 + 1];

        float[] prevValues = getValues(index, windowSize, IProfile.ARRAY_BEFORE); // slots
                                                                                  // for
                                                                                  // previous
                                                                                  // angles
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
    public IProfile getSubregion(int indexStart, int indexEnd){

        if (indexStart >= array.length)
            throw new IllegalArgumentException(String.format("Start index (%d) is beyond array length (%d)", indexStart, array.length));
        if (indexEnd >= array.length)
            throw new IllegalArgumentException(String.format("End index (%d) is beyond array length (%d)", indexEnd, array.length));
        if(indexStart < 0 || indexEnd < 0)
            throw new IllegalArgumentException(String.format("Start (%d) or end index (%d) is below zero", indexStart, indexEnd));
        if (indexStart < indexEnd) {
            return new FloatProfile( Arrays.copyOfRange(array, indexStart, indexEnd+1) );

        } else { // case when array wraps

            float[] resultA = Arrays.copyOfRange(array, indexStart, array.length);
            float[] resultB = Arrays.copyOfRange(array, 0, indexEnd+1);
            float[] result = new float[resultA.length + resultB.length];
            int index = 0;
            for (float d : resultA) {
                result[index++] = d;
            }
            
            for (float d : resultB) {
                result[index++] = d;
            }

            return new FloatProfile(result);
        }
    }

    @Override
    public IProfile getSubregion(@NonNull IProfileSegment segment) throws ProfileException {

        if (segment == null)
            throw new IllegalArgumentException("Segment is null");

        if (segment.getProfileLength() != array.length)
            throw new IllegalArgumentException("Segment comes from a different length profile");
        
        return getSubregion(segment.getStartIndex(), segment.getEndIndex());
    }


    @Override
    public IProfile calculateDeltas(int windowSize) {
        
        if (windowSize<1)
            throw new IllegalArgumentException("Window size must be a positive integer");

        float[] deltas = new float[array.length];

        for (int i = 0; i < array.length; i++) {

            float[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE);
            float[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);

            float delta = 0;
            for (int k = 0; k < prevValues.length; k++) {

                if (k == 0) {
                    delta += (array[i] - prevValues[k]) + (nextValues[k] - array[i]);

                } else {
                    delta += (prevValues[k - 1] - prevValues[k]) + (nextValues[k] - nextValues[k - 1]);
                }

            }

            deltas[i] = delta;
        }
        return new FloatProfile(deltas);
    }

    @Override
    public IProfile power(double exponent) {
        float[] values = new float[this.size()];

        for (int i = 0; i < array.length; i++) {
            values[i] = (float) Math.pow(array[i], exponent);
        }
        return new FloatProfile(values);
    }

    @Override
    public IProfile absolute() {
        float[] values = new float[this.size()];

        for (int i = 0; i < array.length; i++) {
            values[i] = Math.abs(array[i]);
        }
        return new FloatProfile(values);
    }

    @Override
    public IProfile cumulativeSum() {
        float[] values = new float[this.size()];

        float total = 0;
        for (int i = 0; i < array.length; i++) {
            total += array[i];
            values[i] = total;
        }
        return new FloatProfile(values);
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
        return new FloatProfile(result);
    }

    @Override
    public IProfile multiply(IProfile multiplier) {
        if (this.size() != multiplier.size()) {
            throw new IllegalArgumentException("Profile sizes do not match");
        }
        float[] result = new float[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (float) (array[i] * multiplier.get(i));
        }
        return new FloatProfile(result);
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
        return new FloatProfile(result);
    }

    @Override
    public IProfile divide(IProfile divider) {
        if (this.size() != divider.size()) {
            throw new IllegalArgumentException("Profile sizes do not match");
        }
        float[] result = new float[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (float) (array[i] / divider.get(i));
        }
        return new FloatProfile(result);
    }

    @Override
    public IProfile add(IProfile adder) {
        if (this.size() != adder.size()) {
            throw new IllegalArgumentException("Profile sizes do not match");
        }
        float[] result = new float[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (float) (array[i] + adder.get(i));
        }
        return new FloatProfile(result);
    }

    @Override
    public IProfile add(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value))
            throw new IllegalArgumentException("Cannot add NaN or infinity");

        float[] result = new float[array.length];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (float) (array[i] + value);
        }
        return new FloatProfile(result);
    }

    @Override
    public IProfile subtract(IProfile sub) {
        if (this.size() != sub.size())
            throw new IllegalArgumentException("Profile sizes do not match");

        float[] result = new float[this.size()];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (float) (array[i] - sub.get(i));
        }
        return new FloatProfile(result);
    }
    
    @Override
    public IProfile subtract(double value) {

        if (Double.isNaN(value) || Double.isInfinite(value))
            throw new IllegalArgumentException("Cannot subtract NaN or infinity");

        float[] result = new float[array.length];

        for (int i = 0; i < array.length; i++) { // for each position in sperm
            result[i] = (float) (array[i] - value);
        }
        return new FloatProfile(result);
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
		float[] result = new float[array.length];
		
		for (int i = 0; i < array.length; i++) {
			result[i] = (float) (((array[i]/(oldMax-oldMin))*newRange)+min);
		}
		return new FloatProfile(result);
	}

    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < array.length; i++) {
            builder.append("Index " + i + "\t" + array[i] + "\r\n");
        }
        return builder.toString();
    }
    
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
    
    

	@Override
	public Element toXmlElement() {
		Element e = new Element(XML_PROFILE);
		e.setText(Arrays.toString(array));
		return e;
	}

	@Override
	public int wrap(int index) {
		if (index < 0)
            return wrap(size() + index);
        if (index < size())
            return index;
        return index % size();
	}

	@Override
	public Iterator<Integer> iterator() {
		return IntStream.range(0, array.length).iterator();
	}
}
