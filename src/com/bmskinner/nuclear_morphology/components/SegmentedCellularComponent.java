///*******************************************************************************
// *      Copyright (C) 2016 Ben Skinner
// *   
// *     This file is part of Nuclear Morphology Analysis.
// *
// *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
// *     it under the terms of the GNU General Public License as published by
// *     the Free Software Foundation, either version 3 of the License, or
// *     (at your option) any later version.
// *
// *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
// *     but WITHOUT ANY WARRANTY; without even the implied warranty of
// *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// *     GNU General Public License for more details.
// *
// *     You should have received a copy of the GNU General Public License
// *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
// *******************************************************************************/
//
//package com.bmskinner.nuclear_morphology.components;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//
//import org.eclipse.jdt.annotation.NonNull;
//
//import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
//import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
//import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
//import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
//import com.bmskinner.nuclear_morphology.components.generic.IPoint;
//import com.bmskinner.nuclear_morphology.components.generic.IProfile;
//import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
//import com.bmskinner.nuclear_morphology.components.generic.Tag;
//import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
//import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
//import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
//import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
//
//import ij.gui.Roi;
//
///**
// * This experimental class incorporates segments as subclasses to prevent errors
// * in indexing.
// * @author bms41
// * @since 1.13.8
// *
// */
//public abstract class SegmentedCellularComponent extends ProfileableCellularComponent {
//
//	private static final long serialVersionUID = 1L;
//
//
//	/**
//     * Construct with an ROI, a source image and channel, and the original
//     * position in the source image
//     * 
//     * @param roi
//     * @param f
//     * @param channel
//     * @param position
//     * @param centreOfMass
//     */
//    public SegmentedCellularComponent(Roi roi, IPoint centreOfMass, File f, int channel, int[] position) {
//        super(roi, centreOfMass, f, channel, position);
//    }
//
//    /**
//     * Create a new component based on the given template object. If the object has segments,
//     * these will be copied to the new component.
//     * @param c
//     * @throws UnprofilableObjectException
//     */
//    public SegmentedCellularComponent(final CellularComponent c) throws UnprofilableObjectException {
//        super(c);
//    }
//
//    /**
//     * An implementation of a profile tied to an object
//     * @author ben
//     * @since 1.13.8
//     */
//    public class DefaultProfile implements IProfile {
//
//        private static final long serialVersionUID = 1L;
//
//        protected final float[] array;
//
//        /**
//         * Constructor for a new Profile, based on an array of values.
//         * 
//         * @param values the array to use
//         */
//        public DefaultProfile(final float[] values) {
//
//        	if(values==null)
//        		throw new IllegalArgumentException("Array is null");
//            if (values.length!=SegmentedCellularComponent.this.getBorderLength())
//                throw new IllegalArgumentException("Input array does not match object border length");
//            this.array = values;
//        }
//
//        /**
//         * Constructor based on an existing Profile. Makes a copy of the existing
//         * Profile
//         * 
//         * @param p the profile to copy
//         */
//        public DefaultProfile(@NonNull final IProfile p) {
//            if (p==null)
//                throw new IllegalArgumentException("Profile is null");
//            if(p.size()!=SegmentedCellularComponent.this.getBorderLength())
//                throw new IllegalArgumentException("Profile does not match object border length");
//
//            this.array = new float[p.size()];
//
//            for (int i = 0; i < p.size(); i++) {
//                array[i] = (float) p.get(i);
//            }
//        }
//
//        /**
//         * Constructor based on an fixed value and the profile length
//         * 
//         * @param value the value for the profile to hold at each index
//         */
//        public DefaultProfile(final float value) {
//
//            this.array = new float[SegmentedCellularComponent.this.getBorderLength()];
//            for (int i = 0; i < this.array.length; i++) {
//                array[i] = value;
//            }
//        }
//
//        @Override
//        public int size() {
//            return array.length;
//        }
//
//        @Override
//        public int hashCode() {
//            final int prime = 31;
//            int result = 1;
//            result = prime * result + Arrays.hashCode(array);
//            return result;
//        }
//
//
//        @Override
//        public boolean equals(Object obj) {
//            if (this == obj)
//                return true;
//            if (obj == null)
//                return false;
//            if (getClass() != obj.getClass())
//                return false;
//            DefaultProfile other = (DefaultProfile) obj;
//            if (!Arrays.equals(array, other.array))
//                return false;
//            return true;
//        }
//
//
//        @Override
//        public double get(int index) throws IndexOutOfBoundsException {
//
//            if (index < 0 || index >= array.length) {
//                throw new IndexOutOfBoundsException(
//                        "Requested value " + index + " is beyond profile end (" + array.length + ")");
//            }
//            return array[index];
//        }
//
//        @Override
//        public double get(double prop) {
//            if (prop < 0 || prop > 1) {
//                throw new IndexOutOfBoundsException("Value " + prop + " must be between 0-1");
//            }
//
//            int index = this.getIndexOfFraction(prop);
//
//            return array[index];
//
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getMax()
//         */
//        @Override
//        public double getMax() {
//            double max = 0;
//            for (int i = 0; i < array.length; i++) {
//                if (array[i] > max) {
//                    max = array[i];
//                }
//            }
//            return max;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getIndexOfMax(components.generic.
//         * BooleanProfile)
//         */
//        @Override
//        public int getIndexOfMax(@NonNull BooleanProfile limits) throws ProfileException {
//
//            if ( limits.size() != array.length) {
//                throw new IllegalArgumentException("Limits are wrong size for this profile");
//            }
//
//            double max = Double.MIN_VALUE;
//            if ( limits.size() != array.length)
//                throw new IllegalArgumentException("Limits are wrong size for this profile");
//
//            double max = -Double.MAX_VALUE;
//            int maxIndex = -1;
//            for (int i = 0; i < array.length; i++) {
//                if (limits.get(i) && array[i] > max) {
//                    max = array[i];
//                    maxIndex = i;
//                }
//            }
//
//            if (maxIndex == -1)
//                throw new ProfileException("No valid index for maximum value");
//            return maxIndex;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getIndexOfMax()
//         */
//        @Override
//        public int getIndexOfMax() throws ProfileException {
//            return getIndexOfMax( new BooleanProfile(this, true) );
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getProportionalIndex(double)
//         */
//        @Override
//        public int getIndexOfFraction(double d) {
//            if (d < 0 || d > 1) {
//                throw new IllegalArgumentException("Proportion must be between 0-1: " + d);
//            }
//
//            double desiredDistanceFromStart = (double) array.length * d;
//
//            int target = (int) desiredDistanceFromStart;
//
//            return target;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getIndexProportion(int)
//         */
//        @Override
//        public double getFractionOfIndex(int index) {
//            if (index < 0 || index >= array.length) {
//                throw new IllegalArgumentException("Index out of bounds: " + index);
//            }
//
//            return (double) index / (double) array.length;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getMin()
//         */
//        @Override
//        public double getMin() {
//            double min = this.getMax();
//            for (int i = 0; i < array.length; i++) {
//                if (array[i] < min) {
//                    min = array[i];
//                }
//            }
//            return min;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getIndexOfMin(components.generic.
//         * BooleanProfile)
//         */
//        @Override
//        public int getIndexOfMin(@NonNull BooleanProfile limits) throws ProfileException {
//
//            if (limits.size() != array.length) {
//                throw new IllegalArgumentException("Limits are wrong size for this profile");
//            }
//
//            double min = Double.MAX_VALUE;
//
//            int minIndex = -1;
//
//            for (int i = 0; i < array.length; i++) {
//                if (limits.get(i) && array[i] < min) {
//                    min = array[i];
//                    minIndex = i;
//                }
//            }
//            if (minIndex == -1) {
//                throw new ProfileException("No valid index for minimum value");
//            }
//            return minIndex;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getIndexOfMin()
//         */
//        @Override
//        public int getIndexOfMin() throws ProfileException {
//            return getIndexOfMin(new BooleanProfile(this, true));
//        }
//
//        @Override
//        public float[] toFloatArray() {
//            float[] result = new float[array.length];
//            System.arraycopy(array, 0, result, 0, array.length);
//            return result;
//        }
//
//        @Override
//        public double[] toDoubleArray() {
//            double[] result = new double[array.length];
//            for (int i = 0; i < result.length; i++) {
//                result[i] = array[i];
//            }
//            return result;
//        }
//
//
//        /**
//         * Check the lengths of the two profiles. Return the first profile
//         * interpolated to the length of the longer.
//         * 
//         * @param profile1
//         *            the profile to return interpolated
//         * @param profile2
//         *            the profile to compare
//         * @return a new profile with the length of the longest input profile
//         * @throws ProfileException
//         */
//        private IProfile equaliseLengths(@NonNull IProfile profile1, @NonNull IProfile profile2) throws ProfileException {
//
//            // profile 2 is smaller
//            // return profile 1 unchanged
//            if (profile2.size() < profile1.size()) {
//                return profile1;
//            } else {
//                // profile 1 is smaller; interpolate to profile 2 length
//                profile1 = profile1.interpolate(profile2.size());
//            }
//
//            return profile1;
//        }
//
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see
//         * components.generic.IProfile#absoluteSquareDifference(components.generic.
//         * IProfile)
//         */
//        @Override
//        public double absoluteSquareDifference(@NonNull IProfile testProfile) throws ProfileException {
//
//            float[] arr2 = testProfile.toFloatArray();
//
//            if (array.length == arr2.length) {
//                return squareDifference(array, arr2);
//            }
//
//            if (array.length > arr2.length) {
//                arr2 = interpolate(arr2, array.length);
//                return squareDifference(array, arr2);
//            } else {
//                float[] arr1 = this.toFloatArray();
//                arr1 = interpolate(arr1, arr2.length);
//                return squareDifference(arr1, arr2);
//            }
//        }
//
//        /**
//         * Calculate the absolute square difference between two arrays of equal
//         * length. Note - array lengths are not checked.
//         * 
//         * @param arr1
//         * @param arr2
//         * @return
//         */
//        private double squareDifference(float[] arr1, float[] arr2) {
//            double difference = 0;
//
//            for (int j = 0; j < arr1.length; j++) { // for each point round the
//
//                difference += Math.pow(arr1[j] - arr2[j], 2); // square difference -
//                // highlights extremes
//            }
//
//            return difference;
//        }
//
//        /*
//         * -------------------- Profile manipulation --------------------
//         */
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#copy()
//         */
//        @Override
//        public IProfile copy() {
//            return new DefaultProfile(array);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#offset(int)
//         */
//        @Override
//        public IProfile offset(int j) throws ProfileException {
//            float[] newArray = new float[array.length];
//            for (int i = 0; i < array.length; i++) {
//                newArray[i] = array[CellularComponent.wrapIndex(i + j, array.length)];
//            }
//            return new DefaultProfile(newArray);
//        }
//
//        /**
//         * Offset the array by the given amount
//         * 
//         * @param arr
//         * @param j
//         * @return
//         */
//        private float[] offset(float[] arr, int j) {
//            float[] newArray = new float[arr.length];
//            for (int i = 0; i < arr.length; i++) {
//                newArray[i] = arr[CellularComponent.wrapIndex(i + j, arr.length)];
//            }
//            return newArray;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#smooth(int)
//         */
//        @Override
//        public IProfile smooth(int windowSize) {
//        	 if(windowSize<2)
//             	throw new IllegalArgumentException(String.format("Window size %d must be >1",windowSize));
//
//
//            float[] result = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position
//
//                float[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE); // slots
//                // for
//                // previous
//                // angles
//                float[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);
//
//                float average = array[i];
//                for (int k = 0; k < prevValues.length; k++) {
//                    average += prevValues[k] + nextValues[k];
//                }
//
//                result[i] = (float) (average / (windowSize * 2 + 1));
//            }
//            return new DefaultProfile(result);
//        }
//
//        /**
//         * Get an array of the values <i>windowSize</i> before or after the current point
//         * 
//         * @param position
//         *            the position in the array
//         * @param windowSize
//         *            the number of points to find
//         * @param type
//         *            find points before or after
//         * @return an array of values
//         */
//        private float[] getValues(int position, int windowSize, int type) {
//
//            float[] values = new float[windowSize]; // slots for previous angles
//            for (int j = 0; j < values.length; j++) {
//
//                // If type was before, multiply by -1; if after, multiply by 1
//                int index = CellularComponent.wrapIndex(position + ((j + 1) * type), this.size());
//                values[j] = array[index];
//            }
//            return values;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#reverse()
//         */
//        @Override
//        public void reverse() {
//
//            float tmp;
//            for (int i = 0; i < this.array.length / 2; i++) {
//                tmp = this.array[i];
//                this.array[i] = this.array[this.array.length - 1 - i];
//                this.array[this.array.length - 1 - i] = tmp;
//            }
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#interpolate(int)
//         */
//        @Override
//        public IProfile interpolate(int newLength) throws ProfileException {
//
//            if(newLength<MINIMUM_PROFILE_LENGTH)
//            	throw new IllegalArgumentException(String.format("New length %d below minimum %d",newLength,MINIMUM_PROFILE_LENGTH));
//
//            float[] newArray = new float[newLength];
//
//            // where in the old curve index is the new curve index?
//            for (int i = 0; i < newLength; i++) {
//                // we have a point in the new curve.
//                // we want to know which points it lay between in the old curve
//                float oldIndex = ((float) i / (float) newLength) * (float) array.length; // get
//                // the
//                // fractional
//                // index
//                // position
//                // needed
//
//                // get the value in the old profile at the given fractional index
//                // position
//                newArray[i] = interpolateValue(oldIndex);
//            }
//            return new FloatProfile(newArray);
//        }
//
//        /**
//         * Interpolate the array to the given length, and return as a new array
//         * 
//         * @return
//         */
//        private float[] interpolate(float[] array, int length) {
//
//            float[] result = new float[length];
//
//            // where in the old curve index is the new curve index?
//            for (int i = 0; i < length; i++) {
//                // we have a point in the new array.
//                // we want to know which points it lies between in the old profile
//                float fraction = ((float) i / (float) length); // get the fractional
//                // index position
//                // needed
//
//                // get the value in the old profile at the given fractional index
//                // position
//                result[i] = getInterpolatedValue(array, fraction);
//            }
//            return result;
//
//        }
//
//        /**
//         * Get the interpolated value at the given fraction along the given array
//         * 
//         * @param array
//         * @param fraction
//         *            the fraction, from 0-1
//         * @return
//         */
//        private float getInterpolatedValue(float[] array, float fraction) {
//            // Get the equivalent index of the fraction in the array
//            double index = fraction * array.length;
//            double indexFloor = Math.floor(index);
//
//            // Get the integer portion and find the bounding indices
//            int indexLower = (int) indexFloor;
//            if (indexLower == array.length) { // only wrap possible if fraction is
//                // range 0-1
//                indexLower = 0;
//            }
//
//            int indexHigher = indexLower + 1;
//            if (indexHigher == array.length) { // only wrap possible if fraction is
//                // range 0-1
//                indexHigher = 0;
//            }
//
//            // Find the fraction between the indices
//            double diffFraction = index - indexFloor;
//
//            // Calculate the linear interpolation
//            double interpolate = array[indexLower] + ((array[indexHigher] - array[indexLower]) * diffFraction);
//
//            return (float) interpolate;
//
//        }
//
//        /**
//         * Take an index position from a non-normalised profile. Normalise it Find
//         * the corresponding angle in the median curve. Interpolate as needed
//         * 
//         * @param normIndex
//         *            the fractional index position to find within this profile
//         * @return an interpolated value
//         */
//        private float interpolateValue(float normIndex) {
//
//            // convert index to 1 window boundaries
//            // This allows us to see the array indexes above and below the desired
//            // fractional index. From these, we can interpolate the fractional
//            // component.
//            // NOTE: this does not account for curves. Interpolation is linear.
//            int index1 = (int) Math.round(normIndex);
//            int index2 = index1 > normIndex ? index1 - 1 : index1 + 1;
//
//            // Decide which of the two indexes is the higher, and which is the lower
//            int indexLower = index1 < index2 ? index1 : index2;
//
//            int indexHigher = index2 > index1 ? index2 : index1;
//
//            // System.out.println("Set indexes "+normIndex+":
//            // "+indexLower+"-"+indexHigher);
//
//            // wrap the arrays
//            indexLower = CellularComponent.wrapIndex(indexLower, array.length);
//            indexHigher = CellularComponent.wrapIndex(indexHigher, array.length);
//            // System.out.println("Wrapped indexes "+normIndex+":
//            // "+indexLower+"-"+indexHigher);
//
//            // get the values at these indexes
//            float valueHigher = array[indexHigher];
//            float valueLower = array[indexLower];
//            // System.out.println("Wrapped values "+normIndex+": "+valueLower+" and
//            // "+valueHigher);
//
//            // calculate the difference between values
//            // this can be negative
//            float valueDifference = valueHigher - valueLower;
//            // System.out.println("Difference "+normIndex+": "+valueDifference);
//
//            // calculate the distance into the region to go
//            float offset = normIndex - indexLower;
//            // System.out.println("Offset "+normIndex+": "+offset);
//
//            // add the offset to the lower index
//            float positionToFind = indexLower + offset;
//            positionToFind = (float) CellularComponent.wrapIndex(positionToFind, array.length);
//            // System.out.println("Position to find "+normIndex+":
//            // "+positionToFind);
//
//            // calculate the value to be added to the lower index value
//            float newValue = valueDifference * offset; // 0 for 0, full difference
//            // for 1
//            // System.out.println("New value "+normIndex+": "+newValue);
//
//            float linearInterpolatedValue = newValue + valueLower;
//            // System.out.println("Interpolated "+normIndex+":
//            // "+linearInterpolatedValue);
//
//            return linearInterpolatedValue;
//        }
//
//        /*
//         * Interpolate another profile to match this, and move this profile along it
//         * one index at a time. Find the point of least difference, and return this
//         * offset. Returns the positive offset to this profile
//         */
//        /*
//         * (non-Javadoc)
//         * 
//         * @see
//         * components.generic.IProfile#getSlidingWindowOffset(components.generic.
//         * IProfile)
//         */
//        @Override
//        public int getSlidingWindowOffset(@NonNull IProfile testProfile) throws ProfileException {
//
//            /*
//             * NEW CODE VERSION - FAIL. GIVES ERROR IN getSubRegion() during segment
//             * fitting
//             */
//
//            float[] test = testProfile.toFloatArray();
//
//            if (array.length == test.length) {
//                return getBestFitOffset(array, test);
//            } else {
//
//                // Change the test array to fit
//                test = interpolate(test, array.length);
//                return getBestFitOffset(array, test);
//
//            }
//
//            /*
//             * OLD CODE
//             */
//
//            // double lowestScore = this.absoluteSquareDifference(testProfile);
//            // int index = 0;
//            // for(int i=0;i<this.size();i++){
//            //
//            // IProfile offsetProfile = this.offset(i);
//            //
//            // double score = offsetProfile.absoluteSquareDifference(testProfile);
//            // if(score<lowestScore){
//            // lowestScore=score;
//            // index=i;
//            // }
//            //
//            // }
//            // return index;
//        }
//
//        /**
//         * Get the sliding window offset of array 1 that best matches array 2. The
//         * arrays must be the same length
//         * 
//         * @param arr1
//         * @param arr2
//         * @return
//         */
//        private int getBestFitOffset(float[] arr1, float[] arr2) {
//
//            double lowestScore = squareDifference(arr1, arr2);
//            int index = 0;
//
//            // Duplicate array 1
//            // float[] tmp = new float[arr1.length];
//            // System.arraycopy(arr1, 0, tmp, 0, arr1.length);
//
//            // Position by position
//            for (int i = 0; i < arr1.length; i++) {
//
//                float[] tmp = offset(arr1, i);
//
//                // float[] tmp2 = new float[arr1.length];
//                //
//                // // Offset the array by 1
//                // System.arraycopy(tmp, 0, tmp2, 1, tmp.length-1);
//                // tmp2[0] = tmp[tmp.length-1];
//                //
//                // // Compare to array 2
//                double score = squareDifference(tmp, arr2);
//                if (score < lowestScore) {
//                    lowestScore = score;
//                    index = i;
//                }
//                //
//                // // Fix the offset
//                // System.arraycopy(tmp2, 0, tmp, 0, tmp.length);
//                // tmp = tmp2;
//
//            }
//            return index;
//        }
//
//
//
//        /*
//         * --------------------
//         *  Detect minima within profiles
//         * --------------------
//         */
//
//        /*
//         * For each point in the array, test for a local minimum. The values of the
//         * points <minimaLookupDistance> ahead and behind are checked. Each should
//         * be greater than the value before. One exception is allowed, to account
//         * for noisy data. Returns the indexes of minima
//         */
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getLocalMinima(int)
//         */
//        @Override
//        public BooleanProfile getLocalMinima(int windowSize) {
//        	
//        	if(windowSize<1)
//             	throw new IllegalArgumentException(String.format("Window size %d must be >=1",windowSize));
//
//            // go through angle array (with tip at start)
//            // look at 1-2-3-4-5 points ahead and behind.
//            // if all greater, local minimum
//            double[] prevValues = new double[windowSize]; // slots for previous
//            // angles
//            double[] nextValues = new double[windowSize]; // slots for next angles
//
//            // int count = 0;
//            // List<Integer> result = new ArrayList<Integer>(0);
//
//            boolean[] minima = new boolean[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//
//                // go through each lookup position and get the appropriate angles
//                for (int j = 0; j < prevValues.length; j++) {
//
//                    int prev_i = CellularComponent.wrapIndex(i - (j + 1), this.size()); // the
//                    // index
//                    // j+1
//                    // before
//                    // i
//                    int next_i = CellularComponent.wrapIndex(i + (j + 1), this.size()); // the
//                    // index
//                    // j+1
//                    // after
//                    // i
//
//                    // fill the lookup array
//                    prevValues[j] = array[prev_i];
//                    nextValues[j] = array[next_i];
//                }
//
//                // with the lookup positions, see if minimum at i
//                // return a 1 if all higher than last, 0 if not
//                // prev_l = 0;
//                boolean ok = true;
//                for (int k = 0; k < prevValues.length; k++) {
//
//                    // for the first position in prevValues, compare to the current
//                    // index
//                    if (k == 0) {
//                        if (prevValues[k] <= array[i] || nextValues[k] <= array[i]) {
//                            ok = false;
//                        }
//                    } else { // for the remainder of the positions in prevValues,
//                        // compare to the prior prevAngle
//
//                        if (prevValues[k] <= prevValues[k - 1] || nextValues[k] <= nextValues[k - 1]) {
//                            ok = false;
//                        }
//                    }
//                }
//
//                if (ok) {
//                    // count++;
//                    minima[i] = true;
//                } else {
//                    minima[i] = false;
//                }
//
//                // result.add(i);
//
//            }
//            BooleanProfile minimaProfile = new BooleanProfile(minima);
//            // this.minimaCalculated = true;
//            // this.minimaCount = count;
//            return minimaProfile;
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getLocalMinima(int, double)
//         */
//        @Override
//        public BooleanProfile getLocalMinima(int windowSize, double threshold) {
//            BooleanProfile minima = getLocalMinima(windowSize);
//
//            boolean[] values = new boolean[this.size()];
//
//            for (int i = 0; i < array.length; i++) {
//
//                if (minima.get(i) == true && this.get(i) < threshold) {
//                    values[i] = true;
//                } else {
//                    values[i] = false;
//                }
//            }
//            return new BooleanProfile(values);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getLocalMaxima(int)
//         */
//        @Override
//        public BooleanProfile getLocalMaxima(int windowSize) {
//        	
//        	if(windowSize<1)
//             	throw new IllegalArgumentException(String.format("Window size %d must be >=1",windowSize));
//            // go through array
//            // look at points ahead and behind.
//            // if all lower, local maximum
//
//            boolean[] result = new boolean[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position
//
//                float[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE); // slots
//                // for
//                // previous
//                // angles
//                float[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);
//
//                // with the lookup positions, see if maximum at i
//                // return a 1 if all lower than last, 0 if not
//                boolean isMaximum = true;
//                for (int k = 0; k < prevValues.length; k++) {
//
//                    // for the first position in prevValues, compare to the current
//                    // index
//                    if (k == 0) {
//                        if (prevValues[k] >= array[i] || nextValues[k] >= array[i]) {
//                            isMaximum = false;
//                        }
//                    } else { // for the remainder of the positions in prevValues,
//                        // compare to the prior prevAngle
//
//                        if (prevValues[k] >= prevValues[k - 1] || nextValues[k] >= nextValues[k - 1]) {
//                            isMaximum = false;
//                        }
//                    }
//                }
//
//                result[i] = isMaximum;
//            }
//            return new BooleanProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getLocalMaxima(int, double)
//         */
//        @Override
//        public BooleanProfile getLocalMaxima(int windowSize, double threshold) {
//            BooleanProfile maxima = getLocalMaxima(windowSize);
//
//            boolean[] values = new boolean[this.size()];
//
//            for (int i = 0; i < array.length; i++) {
//
//                if (maxima.get(i) == true && this.get(i) > threshold) {
//                    values[i] = true;
//                } else {
//                    values[i] = false;
//                }
//            }
//            return new BooleanProfile(values);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getWindow(int, int)
//         */
//        @Override
//        public IProfile getWindow(int index, int windowSize) {
//
//            float[] result = new float[windowSize * 2 + 1];
//
//            float[] prevValues = getValues(index, windowSize, IProfile.ARRAY_BEFORE); // slots
//            // for
//            // previous
//            // angles
//            float[] nextValues = getValues(index, windowSize, IProfile.ARRAY_AFTER);
//
//            // need to reverse the previous array
//            for (int k = prevValues.length, i = 0; k > 0; k--, i++) {
//                result[i] = prevValues[k - 1];
//            }
//            result[windowSize] = array[index];
//            for (int i = 0; i < nextValues.length; i++) {
//                result[windowSize + i + 1] = nextValues[i];
//            }
//
//            return new FloatProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getSubregion(int, int)
//         */
//        @Override
//        public IProfile getSubregion(int indexStart, int indexEnd) {
//
//        	if(indexStart<0 || indexEnd < 0)
//        		throw new IllegalArgumentException("Start or end index is below zero");
//        	
//            if (indexStart >= array.length) {
//                throw new IllegalArgumentException(
//                        "Start index (" + indexStart + ") is beyond array length (" + array.length + ")");
//            }
//
//            if (indexEnd >= array.length) {
//                throw new IllegalArgumentException(
//                        "End index (" + indexEnd + ") is beyond array length (" + array.length + ")");
//            }
//
//            if (indexStart < indexEnd) {
//
//                float[] result = Arrays.copyOfRange(array, indexStart, indexEnd);
//                return new FloatProfile(result);
//
//            } else { // case when array wraps
//
//                float[] resultA = Arrays.copyOfRange(array, indexStart, array.length - 1);
//                float[] resultB = Arrays.copyOfRange(array, 0, indexEnd);
//                float[] result = new float[resultA.length + resultB.length];
//                int index = 0;
//                for (float d : resultA) {
//                    result[index] = d;
//                    index++;
//                }
//                for (float d : resultB) {
//                    result[index] = d;
//                    index++;
//                }
//
//                if (result.length == 0) {
//                    warn("Subregion length zero: " + indexStart + " - " + indexEnd);
//                }
//                return new FloatProfile(result);
//            }
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#getSubregion(components.nuclear.
//         * NucleusBorderSegment)
//         */
//        @Override
//        public IProfile getSubregion(@NonNull IBorderSegment segment) {
//
//            if (segment == null)
//                throw new IllegalArgumentException("Segment is null");
//
//            if (segment.getTotalLength() != array.length) {
//                throw new IllegalArgumentException("Segment comes from a different length profile");
//            }
//            return getSubregion(segment.getStartIndex(), segment.getEndIndex());
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#calculateDeltas(int)
//         */
//        @Override
//        public IProfile calculateDeltas(int windowSize) {
//
//            float[] deltas = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//
//                float[] prevValues = getValues(i, windowSize, IProfile.ARRAY_BEFORE); // slots
//                // for
//                // previous
//                // angles
//                float[] nextValues = getValues(i, windowSize, IProfile.ARRAY_AFTER);
//
//                float delta = 0;
//                for (int k = 0; k < prevValues.length; k++) {
//
//                    if (k == 0) {
//                        delta += (array[i] - prevValues[k]) + (nextValues[k] - array[i]);
//
//                    } else {
//                        delta += (prevValues[k] - prevValues[k - 1]) + (nextValues[k] - nextValues[k - 1]);
//                    }
//
//                }
//
//                deltas[i] = delta;
//            }
//            return new DefaultProfile(deltas);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#power(double)
//         */
//        @Override
//        public IProfile power(double exponent) {
//            float[] values = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) {
//                values[i] = (float) Math.pow(array[i], exponent);
//            }
//            return new DefaultProfile(values);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#absolute()
//         */
//        @Override
//        public IProfile absolute() {
//            float[] values = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) {
//                values[i] = Math.abs(array[i]);
//            }
//            return new DefaultProfile(values);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#cumulativeSum()
//         */
//        @Override
//        public IProfile cumulativeSum() {
//            float[] values = new float[array.length];
//
//            float total = 0;
//            for (int i = 0; i < array.length; i++) {
//                total += array[i];
//                values[i] = total;
//            }
//            return new DefaultProfile(values);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#multiply(double)
//         */
//        @Override
//        public IProfile multiply(double multiplier) {
//
//            if (Double.isNaN(multiplier) || Double.isInfinite(multiplier)) {
//                throw new IllegalArgumentException("Cannot add NaN or infinity");
//            }
//
//            float[] result = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//                result[i] = (float) (array[i] * multiplier);
//            }
//            return new DefaultProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#multiply(components.generic.IProfile)
//         */
//        @Override
//        public IProfile multiply(@NonNull IProfile multiplier) {
//            if (this.size() != multiplier.size()) {
//                throw new IllegalArgumentException("Profile sizes do not match");
//            }
//            float[] result = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//                result[i] = (float) (array[i] * multiplier.get(i));
//            }
//            return new DefaultProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#divide(double)
//         */
//        @Override
//        public IProfile divide(double divider) {
//
//            if (Double.isNaN(divider) || Double.isInfinite(divider)) {
//                throw new IllegalArgumentException("Cannot add NaN or infinity");
//            }
//
//            float[] result = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//                result[i] = (float) (array[i] / divider);
//            }
//            return new DefaultProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#divide(components.generic.IProfile)
//         */
//        @Override
//        public IProfile divide(@NonNull IProfile divider) {
//            if (this.size() != divider.size()) {
//                throw new IllegalArgumentException("Profile sizes do not match");
//            }
//            float[] result = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//                result[i] = (float) (array[i] / divider.get(i));
//            }
//            return new DefaultProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#add(components.generic.IProfile)
//         */
//        @Override
//        public IProfile add(@NonNull IProfile adder) {
//            if (this.size() != adder.size()) {
//                throw new IllegalArgumentException("Profile sizes do not match");
//            }
//            float[] result = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//                result[i] = (float) (array[i] + adder.get(i));
//            }
//            return new DefaultProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#add(double)
//         */
//        @Override
//        public IProfile add(double value) {
//
//            if (Double.isNaN(value) || Double.isInfinite(value)) {
//                throw new IllegalArgumentException("Cannot add NaN or infinity");
//            }
//
//            float[] result = new float[array.length];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//                result[i] = (float) (array[i] + value);
//            }
//            return new DefaultProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#subtract(components.generic.IProfile)
//         */
//        @Override
//        public IProfile subtract(@NonNull IProfile sub) {
//            if (this.size() != sub.size()) {
//                throw new IllegalArgumentException("Profile sizes do not match");
//            }
//            float[] result = new float[this.size()];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//                result[i] = (float) (array[i] - sub.get(i));
//            }
//            return new DefaultProfile(result);
//        }
//        
//        @Override
//        public IProfile subtract(double value) {
//            if (Double.isNaN(value) || Double.isInfinite(value))
//                throw new IllegalArgumentException("Cannot subtract NaN or infinity");
//
//            float[] result = new float[array.length];
//
//            for (int i = 0; i < array.length; i++) { // for each position in sperm
//                result[i] = (float) (array[i] - value);
//            }
//            return new DefaultProfile(result);
//        }
//
//        /*
//         * (non-Javadoc)
//         * 
//         * @see components.generic.IProfile#toString()
//         */
//        @Override
//        public String toString() {
//            StringBuilder builder = new StringBuilder();
//
//            for (int i = 0; i < array.length; i++) {
//                builder.append("Index " + i + "\t" + array[i] + "\r\n");
//            }
//            return builder.toString();
//        }
//
//        /**
//         * Given a list of ordered profiles, merge them into one contiguous profile
//         * 
//         * @param list
//         *            the list of profiles to merge
//         * @return the merged profile
//         */
//        public IProfile merge(List<IProfile> list) {
//            if (list == null || list.size() == 0) {
//                throw new IllegalArgumentException("Profile list is null or empty");
//            }
//
//            int totalLength = 0;
//            for (IProfile p : list) {
//                totalLength += p.size();
//            }
//
//            float[] combinedArray = new float[totalLength];
//
//            int i = 0;
//
//            for (IProfile p : list) {
//
//                for (int j = 0; j < p.size(); j++) {
//                    combinedArray[i++] = (float) p.get(j);
//                }
//            }
//
//            return new DefaultProfile(combinedArray);
//        }
//    }
//
//
//    /**
//     * An implementation of a segmented profile tied to an object
//     * @author ben
//     * @since 1.13.8
//     *
//     */
//    public class DefaultSegmentedProfile extends DefaultProfile implements ISegmentedProfile {
//
//		private static final long serialVersionUID = 1L;
//		private int[] segmentBounds = new int[1];
//        private UUID[] ids = new UUID[1];
//        private Map<UUID, List<IBorderSegment>> mergeSources = new HashMap<>();
//
//        /**
//         * Construct using a regular profile and a list of border segments
//         * 
//         * @param p the profile
//         * @param segments the list of segments to use
//         * @throws ProfileException
//         */
//        public DefaultSegmentedProfile(@NonNull final IProfile p, @NonNull final List<IBorderSegment> segments) throws ProfileException {
//            super(p);
//            segmentBounds = new int[segments.size()];
//            for(int i=0; i<segmentBounds.length; i++){
//                IBorderSegment s = segments.get(i);
//                segmentBounds[i] = s.getStartIndex();
//                ids[i] = s.getID();
//            }
//        }
//        
//          /**
//           * Construct using an existing profile. Copies the data and segments
//           * 
//           * @param profile the segmented profile to copy
//           * @throws ProfileException
//           * @throws IndexOutOfBoundsException
//           */
//          public DefaultSegmentedProfile(@NonNull final ISegmentedProfile profile) throws IndexOutOfBoundsException, ProfileException {
//              super(profile);
//              segmentBounds = new int[profile.getSegmentCount()];
//              for(int i=0; i<segmentBounds.length; i++){
//                  IBorderSegment s = profile.getSegmentAt(i);
//                  segmentBounds[i] = s.getStartIndex();
//                  ids[i] = s.getID();
//              }
//          }
//        
//          /**
//           * Construct using a basic profile. Two segments are created that span the
//           * entire profile, half each
//           * 
//           * @param profile
//           */
//          public DefaultSegmentedProfile(@NonNull final IProfile profile) {
//              this(profile.toFloatArray());
//          }
//        
//          /**
//           * Construct from an array of values with a single segment
//           * 
//           * @param values
//           * @throws Exception
//           */
//          public DefaultSegmentedProfile(float[] values) {
//              super(values);
//              segmentBounds[0] = 0;
//              ids[0] = UUID.randomUUID();
//          }
//          
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#hasSegments()
//           */
//          @Override
//          public boolean hasSegments() {
//              return segmentBounds!=null && segmentBounds.length>0;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegments()
//           */
//          @Override
//          public List<IBorderSegment> getSegments() {
//              List<IBorderSegment> temp = new ArrayList<>();
//              
//              for(int i=0; i<segmentBounds.length; i++){
//                  temp.add( new DefaultBorderSegment(ids[i]));
//              }
//
//              return temp;
//          }
//                
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegment(java.util.UUID)
//           */
//          @Override
//          public IBorderSegment getSegment(@NonNull final UUID id) throws UnavailableComponentException {
//              if (id == null)
//                  throw new IllegalArgumentException("Id is null");
//              
//              for(int i=0; i<ids.length; i++){
//                  if(ids[i].equals(id))
//                      return new DefaultBorderSegment(ids[i]);
//              }
//
//              throw new UnavailableComponentException("Segment with id " + id.toString() + " not found");
//        
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#hasSegment(java.util.UUID)
//           */
//          @Override
//          public boolean hasSegment(@NonNull final UUID id) {
//              for (UUID u : ids) {
//                  if (u.equals(id))
//                      return true;
//              }
//              return false;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegmentsFrom(java.util.UUID)
//           */
//          @Override
//          public List<IBorderSegment> getSegmentsFrom(@NonNull UUID id) throws Exception {
//              return getSegmentsFrom(getSegment(id));
//          }
//          
//          /**
//           * Get the index of the segment with the given id
//           * @param id
//           * @return
//           */
//          private int getIndexOfSegment(@NonNull final UUID id){
//              for(int i=0; i<ids.length; i++){
//                  if(ids[i].equals(id))
//                      return i;
//              }
//              return -1;
//          }
//        
//          /**
//           * Get the segments in order from the given segment
//           * 
//           * @param firstSeg the first segment in the profile
//           * @return
//           * @throws UnavailableComponentException 
//           */
//          private List<IBorderSegment> getSegmentsFrom(@NonNull IBorderSegment firstSeg) throws UnavailableComponentException {
//
//              if (firstSeg == null)
//                  throw new IllegalArgumentException("Requested first segment is null");
//
//              int index = getIndexOfSegment(firstSeg.getID());
//
//              if(index==-1)
//                  throw new UnavailableComponentException("No segment with first segment id ");
//
//              List<IBorderSegment> result = new ArrayList<>();
//              for(int j=0; j<ids.length; j++){
//                  int k = wrapSegmentIndex(index+j); 
//                  result.add(getSegment(ids[k]));
//              }
//
//              return result;
//          }
//
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getOrderedSegments()
//           */
//          @Override
//          public List<IBorderSegment> getOrderedSegments() {
//        
//              IBorderSegment firstSeg = null; // default to the first segment in the
//                                              // profile
//        
//              /*
//               * Choose the first segment of the profile to be the segment starting at
//               * the zero index
//               */
//              for (IBorderSegment seg : getSegments()) {
//        
//                  if (seg.getStartIndex() == ZERO_INDEX) {
//                      firstSeg = seg;
//                  }
//              }
//        
//              if (firstSeg == null) {
//        
//                  /*
//                   * A subset of nuclei do not produce segment boundaries
//                   */
////                  fine("Cannot get ordered segments");
////                  fine("Profile is " + this.toString());
////                  fine("Using the first segment in the profile");
//                  firstSeg = this.getSegments().get(0); // default to the first
//                                                        // segment in the profile
//              }
//        
//              List<IBorderSegment> result;
//              try {
//                  result = getSegmentsFrom(firstSeg);
//              } catch (UnavailableComponentException e) {
//                  warn("Profile error getting segments");
//                  fine("Profile error getting segments", e);
//                  result = new ArrayList<IBorderSegment>();
//              }
//        
//              return result;
//        
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegment(java.lang.String)
//           */
//          @Override
//          public IBorderSegment getSegment(@NonNull String name) throws UnavailableComponentException {
//              if (name == null) {
//                  throw new IllegalArgumentException("Requested segment name is null");
//              }
//        
//              for (IBorderSegment seg : this.getSegments()) {
//                  if (seg.getName().equals(name)) {
//                      return seg;
//                  }
//              }
//              throw new UnavailableComponentException("Requested segment name is not present");
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegment(components.nuclear.
//           * IBorderSegment)
//           */
//          @Override
//          public IBorderSegment getSegment(@NonNull IBorderSegment segment) {
//              if (!this.contains(segment))
//                  throw new IllegalArgumentException("Requested segment is not present");
//              
//              try {
//                  return getSegment(segment.getID());
//              } catch(UnavailableComponentException e){
//                  return null;
//              }
//              
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegmentAt(int)
//           */
//          @Override
//          public IBorderSegment getSegmentAt(int position) {
//        
//              if(position < 0 || position > ids.length-1)
//                  throw new IllegalArgumentException("Segment position is out of bounds");
//              
//              try {
//                return getSegment(ids[position]);
//            } catch (UnavailableComponentException e) {
//                return null;
//            }
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegmentContaining(int)
//           */
//          @Override
//          public IBorderSegment getSegmentContaining(int index) {
//        
//              if (index < 0 || index >= this.size())
//                  throw new IllegalArgumentException("Index is out of profile bounds");
//        
//              for (IBorderSegment seg : getSegments()) {
//                  if (seg.contains(index)) {
//                      return seg;
//                  }
//              }
//              throw new IllegalArgumentException("Index not in profile");
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#setSegments(java.util.List)
//           */
//          @Override
//          public void setSegments(@NonNull List<IBorderSegment> segments) {
//              if (segments == null || segments.isEmpty()) {
//                  throw new IllegalArgumentException("Segment list is null or empty");
//              }
//        
//              if (segments.get(0).getTotalLength() != this.size()) {
//                  throw new IllegalArgumentException("Segment list is from a different total length");
//              }
//              
//              segmentBounds = new int[segments.size()];
//              ids = new UUID[segments.size()];
//              
//        
//              for (int i=0; i<ids.length; i++) {
//                  segmentBounds[i] = segments.get(i).getStartIndex();
//                  ids[i] = segments.get(i).getID();
//              }
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#clearSegments()
//           */
//          @Override
//          public void clearSegments() {
//              segmentBounds = new int[0];
//              ids = new UUID[0];
//        
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegmentNames()
//           */
//          @Override
//          public List<String> getSegmentNames() {
//              List<String> result = new ArrayList<>();
//              for (int i=0; i<ids.length; i++) {
//                  result.add("Seg_"+i);
//              }
//              return result;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegmentIDs()
//           */
//          @Override
//          public List<UUID> getSegmentIDs() {
//              List<UUID> result = new ArrayList<UUID>();
//              for (UUID id : ids) {
//                  result.add(id);
//              }
//              return result;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#getSegmentCount()
//           */
//          @Override
//          public int getSegmentCount() {
//              return ids.length;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see
//           * components.generic.ISegmentedProfile#getDisplacement(components.nuclear.
//           * IBorderSegment)
//           */
//          @Override
//          public double getDisplacement(@NonNull final IBorderSegment segment) {
//              
//              if (!contains(segment)) {
//                  throw new IllegalArgumentException("Segment is not in profile");
//              }
//              double start = this.get(segment.getStartIndex());
//              double end = this.get(segment.getEndIndex());
//        
//              double min = Math.min(start, end);
//              double max = Math.max(start, end);
//        
//              return max - min;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#contains(components.nuclear.
//           * IBorderSegment)
//           */
//          @Override
//          public boolean contains(@NonNull final IBorderSegment segment) {
//              if (segment == null)
//                  return false;
//              
//              for(int i=0; i<ids.length; i++){
//                  if(equals(i, segment))
//                      return true;
//              }
//
//              return false;
//          }
//
//          /**
//           * Test if the given segment is at the given segment index
//           * @param i
//           * @param seg
//           * @return
//           */
//          private boolean equals(int i, @NonNull IBorderSegment seg){
//              int j = wrapSegmentIndex(i);
//              return segmentBounds[i]==seg.getStartIndex() && segmentBounds[j]==seg.getEndIndex() && ids[i].equals(seg.getID());
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#update(components.nuclear.
//           * IBorderSegment, int, int)
//           */
//          @Override
//          public boolean update(@NonNull IBorderSegment segment, int startIndex, int endIndex) throws SegmentUpdateException {
//        
//              if (!this.contains(segment))
//                  throw new IllegalArgumentException("Segment is not part of this profile");
//        
//              // test effect on all segments in list: the update should
//              // not allow the endpoints to move within a segment other than
//              // next or prev
//              IBorderSegment nextSegment = segment.nextSegment();
//              IBorderSegment prevSegment = segment.prevSegment();
//        
//              for (IBorderSegment testSeg : this.getSegments()) {
//        
//                  // if the proposed start or end index is found in another segment
//                  // that is not next or prev, do not proceed
//                  if (testSeg.contains(startIndex) || testSeg.contains(endIndex)) {
//        
//                      if (!testSeg.getName().equals(segment.getName()) && !testSeg.getName().equals(nextSegment.getName())
//                              && !testSeg.getName().equals(prevSegment.getName())) {
//                          return false;
//                      }
//                  }
//              }
//              
//              //TODO: check this works with the new indexing 
//        
//              // the basic checks have been passed; the update will not damage linkage
//              // Allow the segment to determine if the update is valid and apply it
//        
//              return segment.update(startIndex, endIndex);
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see
//           * components.generic.ISegmentedProfile#adjustSegmentStart(java.util.UUID,
//           * int)
//           */
//          @Override
//          public boolean adjustSegmentStart(@NonNull UUID id, int amount) throws SegmentUpdateException {
//              if (!hasSegment(id)) {
//                  throw new IllegalArgumentException("Segment is not part of this profile");
//              }
//        
//              // get the segment within this profile, not a copy
//              IBorderSegment segmentToUpdate;
//              try {
//                  segmentToUpdate = this.getSegment(id);
//              } catch (UnavailableComponentException e) {
//                  stack(e);
//                  throw new SegmentUpdateException("Error getting segment", e);
//              }
//        
//              int newValue = wrapIndex(segmentToUpdate.getStartIndex() + amount);
//              return this.update(segmentToUpdate, newValue, segmentToUpdate.getEndIndex());
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see
//           * components.generic.ISegmentedProfile#adjustSegmentEnd(java.util.UUID,
//           * int)
//           */
//          @Override
//          public boolean adjustSegmentEnd(@NonNull UUID id, int amount) throws SegmentUpdateException {
//              if (!hasSegment(id)) {
//                  throw new IllegalArgumentException("Segment is not part of this profile");
//              }
//        
//              // get the segment within this profile, not a copy
//              IBorderSegment segmentToUpdate;
//              try {
//                  segmentToUpdate = this.getSegment(id);
//              } catch (UnavailableComponentException e) {
//                  stack(e);
//                  throw new SegmentUpdateException("Error getting segment");
//              }
//        
//              int newValue = wrapIndex(segmentToUpdate.getEndIndex() + amount);
//              return this.update(segmentToUpdate, segmentToUpdate.getStartIndex(), newValue);
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#nudgeSegments(int)
//           */
//          @Override
//          public void nudgeSegments(int amount) {
//              for(int i=0; i<ids.length; i++){
//                  segmentBounds[i] = wrapIndex(segmentBounds[i]+amount);
//              }    
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#offset(int)
//           */
//          @Override
//          public ISegmentedProfile offset(int offset) throws ProfileException {
//        
//              // get the basic profile with the offset applied
//              IProfile offsetProfile = super.offset(offset);
//        
//              /*
//               * The segmented profile starts like this:
//               * 
//               * 0 5 15 35 |-----|----------|--------------------|
//               * 
//               * After applying offset=5, the profile looks like this:
//               * 
//               * 0 10 30 35 |----------|--------------------|-----|
//               * 
//               * The new profile starts at index 'offset' in the original profile This
//               * means that we must subtract 'offset' from the segment positions to
//               * make them line up.
//               * 
//               * The nudge function in IBorderSegment moves endpoints by a specified
//               * amount
//               * 
//               */
//              // fine("Offsetting segments in profile by "+offset );
//        
//              // fine("Profile length: "+size()+"; segment total:
//              // "+segments[0].getTotalLength());
//        
//              List<IBorderSegment> segments = IBorderSegment.nudge(getSegments(), -offset);
//        
//              /*
//               * Ensure that the first segment in the list is at index zero
//               */
//        
//              return new DefaultSegmentedProfile(offsetProfile, segments);
//          }
//        
//          @Override
//          public ISegmentedProfile interpolate(int length) throws ProfileException {
//              
//        
//              // get the proportions of the existing segments
//              double[] props = new double[segments.length];
//        
//              for (int i = 0; i < segments.length; i++) {
//                  props[i] = this.getFractionOfIndex(segments[i].getStartIndex());
//              }
//        
//              // get the target start indexes of the new segments
//        
//              int[] newStarts = new int[segments.length];
//        
//              for (int i = 0; i < segments.length; i++) {
//                  newStarts[i] = (int) (props[i] * (double) length);
//              }
//        
//              // Make the new segments
//              List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>(segments.length);
//              for (int i = 0; i < segments.length - 1; i++) {
//        
//                  int testStart = newStarts[i];
//                  int testEnd = newStarts[i + 1];
//        
//                  if (testEnd - testStart < IBorderSegment.MINIMUM_SEGMENT_LENGTH) {
//                      newStarts[i + 1] = newStarts[i + 1] + 1;
//                  }
//        
//                  IBorderSegment seg = new DefaultBorderSegment(newStarts[i], newStarts[i + 1], length, segments[i].getID());
//                  newSegs.add(seg);
//              }
//        
//              // Add final segment
//              // We need to correct start and end positions appropriately.
//              // Since the start position may already have been set, we need to adjust
//              // the end,
//              // i.e the start position of the first segment.
//        
//              try {
//                  int firstStart = newStarts[0];
//                  int lastStart = newStarts[segments.length - 1];
//                  if (newSegs.get(0).wraps(newStarts[segments.length - 1], newStarts[0])) {
//                      // wrapping final segment
//                      if (firstStart + (length - lastStart) < IBorderSegment.MINIMUM_SEGMENT_LENGTH) {
//                          newStarts[0] = firstStart + 1; // update the start in the
//                                                         // array
//                          newSegs.get(0).update(firstStart, newSegs.get(1).getStartIndex()); // update
//                                                                                             // the
//                                                                                             // new
//                                                                                             // segment
//                      }
//        
//                  } else {
//                      // non-wrapping final segment
//                      if (firstStart - lastStart < IBorderSegment.MINIMUM_SEGMENT_LENGTH) {
//                          newStarts[0] = firstStart + 1; // update the start in the
//                                                         // array
//                          newSegs.get(0).update(firstStart, newSegs.get(1).getStartIndex()); // update
//                                                                                             // the
//                                                                                             // new
//                                                                                             // segment
//        
//                      }
//                  }
//              } catch (SegmentUpdateException e) {
//                  throw new ProfileException("Could not update segment indexes");
//              }
//        
//              IBorderSegment lastSeg = new DefaultBorderSegment(segments[segments.length - 1].getID());
//              newSegs.add(lastSeg);
//        
//              if (newSegs.size() != segments.length) {
//                  throw new ProfileException("Error interpolating segments");
//              }
//        
//              // interpolate the IProfile
//        
//              IProfile newProfile = super.interpolate(length);
//        
//              // assign new segments
//              IBorderSegment.linkSegments(newSegs);
//        
//              // log("Segment interpolation complete "+newSegs.size());
//        
//              return new DefaultSegmentedProfile(newProfile, newSegs);
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see
//           * components.generic.ISegmentedProfile#frankenNormaliseToProfile(components
//           * .generic.ISegmentedProfile)
//           */
//          @Override
//          public ISegmentedProfile frankenNormaliseToProfile(@NonNull ISegmentedProfile template) throws ProfileException {
//              
//              if (template==null)
//                  throw new IllegalArgumentException("Template segment is null");
//        
//              if (this.getSegmentCount() != template.getSegmentCount())
//                  throw new IllegalArgumentException("Segment counts are different in profile and template");
//              
//              for(UUID id : template.getSegmentIDs()){
//                  if(!hasSegment(id))
//                      throw new IllegalArgumentException("Segment ids do not match between profile and template");
//              }
//        
//              /*
//               * The final frankenprofile is made of stitched together profiles from
//               * each segment
//               */
//              List<IProfile> finalSegmentProfiles = new ArrayList<>(ids.length);
//        
//              try {
//        
//                  for (UUID segID : template.getSegmentIDs()) {
//                      // Get the corresponding segment in this profile, by segment
//                      // position
//                      IBorderSegment testSeg = this.getSegment(segID);
//                      IBorderSegment templateSeg = template.getSegment(segID);
//        
//        
//                      // Interpolate the segment region to the new length
//                      IProfile revisedProfile = interpolateSegment(testSeg, templateSeg.length());
//                      finalSegmentProfiles.add(revisedProfile);
//                  }
//        
//              } catch (UnavailableComponentException e) {
//                  stack(e);
//                  throw new ProfileException("Error getting segment for normalising");
//              }
//        
//              // Recombine the segment profiles
//              IProfile mergedProfile = new DefaultProfile(IProfile.merge(finalSegmentProfiles));
//        
//              ISegmentedProfile result = new DefaultSegmentedProfile(mergedProfile, template.getSegments());
//              return result;
//          }
//        
//          /**
//           * The interpolation step of frankenprofile creation. The segment in this
//           * profile, with the same name as the template segment is interpolated to
//           * the length of the template, and returned as a new Profile.
//           * 
//           * @param templateSegment the segment to interpolate
//           * @param newLength the new length of the segment profile
//           * @return the interpolated profile
//           * @throws ProfileException
//           */
//          private IProfile interpolateSegment(IBorderSegment testSeg, int newLength) throws ProfileException {
//        
//              // get the region within the segment as a new profile
//              // Exclude the last index of each segment to avoid duplication
//              // the first index is kept, because the first index is used for border
//              // tags
//              int lastIndex = CellularComponent.wrapIndex(testSeg.getEndIndex() - 1, testSeg.getTotalLength());
//        
//              IProfile testSegProfile = this.getSubregion(testSeg.getStartIndex(), lastIndex);
//        
//              // interpolate the test segments to the length of the median segments
//              IProfile revisedProfile = testSegProfile.interpolate(newLength);
//              return revisedProfile;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#reverse()
//           */
//          @Override
//          public void reverse() {
//              super.reverse();
//        
//              // reverse the segments
//              // in a profile of 100
//              // if a segment began at 10 and ended at 20, it should begin at 80 and
//              // end at 90
//        
//              // if is begins at 90 and ends at 10, it should begin at 10 and end at
//              // 90
//              List<IBorderSegment> segments = new ArrayList<IBorderSegment>();
//              for (IBorderSegment seg : this.getSegments()) {
//        
//                  // invert the segment by swapping start and end
//                  int newStart = (this.size() - 1) - seg.getEndIndex();
//                  int newEnd = CellularComponent.wrapIndex(newStart + seg.length(), this.size());
//                  IBorderSegment newSeg = IBorderSegment.newSegment(newStart, newEnd, this.size(), seg.getID());
//                  // newSeg.setName(seg.getName());
//                  // since the order is reversed, add them to the top of the new list
//                  segments.add(0, newSeg);
//              }
//              try {
//                  IBorderSegment.linkSegments(segments);
//              } catch (ProfileException e) {
//                  warn("Error linking segments");
//                  stack("Cannot link segments in reversed profile", e);
//              }
//              this.setSegments(segments);
//        
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see
//           * components.generic.ISegmentedProfile#mergeSegments(components.nuclear.
//           * IBorderSegment, components.nuclear.IBorderSegment, java.util.UUID)
//           */
//          @Override
//          public void mergeSegments(@NonNull IBorderSegment segment1, @NonNull IBorderSegment segment2, @NonNull UUID id) throws ProfileException {
//        
//              if (segment1 == null)
//                  throw new IllegalArgumentException("Segment 1 cannot be null");
//        
//              if (segment2 == null)
//                  throw new IllegalArgumentException("Segment 2 cannot be null");
//        
//              if (id == null)
//                  throw new IllegalArgumentException("New segment UUID cannot be null");
//        
//              // Check the segments belong to the profile
//              if (!this.contains(segment1) || !this.contains(segment2))
//                  throw new IllegalArgumentException("An input segment is not part of this profile");
//        
//              if(!segment1.hasNextSegment() || !segment2.hasPrevSegment())
//                  throw new IllegalArgumentException("Input segments are not linked");
//              
//              // Check the segments are linked
//              if (!segment1.nextSegment().equals(segment2) && !segment1.prevSegment().equals(segment2))
//                  throw new IllegalArgumentException("Input segments are not linked");
//        
//              // Ensure we have the segments in the correct order
//              IBorderSegment firstSegment = segment1.nextSegment().equals(segment2) ? segment1 : segment2;
//              IBorderSegment secondSegment = segment2.nextSegment().equals(segment1) ? segment1 : segment2;
//        
//              // Create the new segment
//              int startIndex = firstSegment.getStartIndex();
//              int endIndex   = secondSegment.getEndIndex();
//              IBorderSegment mergedSegment = IBorderSegment.newSegment(startIndex, endIndex, this.size(), id);
//        
//              mergedSegment.addMergeSource(firstSegment);
//              mergedSegment.addMergeSource(secondSegment);
//        
//              // Replace the two segments in this profile
//              List<IBorderSegment> oldSegs = this.getSegments();
//              List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();
//        
//              int position = 0;
//              for (IBorderSegment oldSegment : oldSegs) {
//        
//                  if (oldSegment.equals(firstSegment)) {
//                      // add the merge instead
//                      mergedSegment.setPosition(position);
//                      newSegs.add(mergedSegment);
//                  } else if (oldSegment.equals(secondSegment)) {
//                      // do nothing
//                  } else {
//                      // add the original segments
//                      oldSegment.setPosition(position);
//                      newSegs.add(oldSegment);
//                  }
//                  position++;
//              }
//        
//              IBorderSegment.linkSegments(newSegs);
//        
//              this.setSegments(newSegs);
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see
//           * components.generic.ISegmentedProfile#unmergeSegment(components.nuclear.
//           * IBorderSegment)
//           */
//          @Override
//          public void unmergeSegment(@NonNull IBorderSegment segment) throws ProfileException {
//              // Check the segments belong to the profile
//              if (!this.contains(segment)) {
//                  throw new IllegalArgumentException("Input segment is not part of this profile");
//              }
//        
//              if (!segment.hasMergeSources()) {
//                  return;
//        //          throw new IllegalArgumentException("Segment does not have merge sources");
//              }
//        
//              // Replace the two segments in this profile
//              List<IBorderSegment> oldSegs = this.getSegments();
//              List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();
//        
//              int position = 0;
//              for (IBorderSegment oldSegment : oldSegs) {
//        
//                  if (oldSegment.equals(segment)) {
//        
//                      // add each of the old segments
//                      for (IBorderSegment mergedSegment : segment.getMergeSources()) {
//                          mergedSegment.setPosition(position);
//                          newSegs.add(mergedSegment);
//                          position++;
//                      }
//        
//                  } else {
//        
//                      // add the original segments
//                      oldSegment.setPosition(position);
//                      newSegs.add(oldSegment);
//                  }
//                  position++;
//              }
//              IBorderSegment.linkSegments(newSegs);
//              this.setSegments(newSegs);
//        
//          }
//        
//          @Override
//          public boolean isSplittable(@NonNull UUID id, int splitIndex) {
//              if (!this.hasSegment(id)) {
//                  throw new IllegalArgumentException("No segment with the given id");
//              }
//        
//              IBorderSegment segment;
//              try {
//                  segment = getSegment(id);
//              } catch (UnavailableComponentException e) {
//                  stack(e);
//                  return false;
//              }
//        
//              if (!segment.contains(splitIndex)) {
//                  throw new IllegalArgumentException("Splitting index is not within the segment");
//              }
//        
//              return IBorderSegment.isLongEnough(segment.getStartIndex(), splitIndex, this.size())
//                      && IBorderSegment.isLongEnough(splitIndex, segment.getEndIndex(), this.size());
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see
//           * components.generic.ISegmentedProfile#splitSegment(components.nuclear.
//           * IBorderSegment, int, java.util.UUID, java.util.UUID)
//           */
//          @Override
//          public void splitSegment(@NonNull IBorderSegment segment, int splitIndex, @NonNull UUID id1, @NonNull UUID id2) throws ProfileException {
//              // Check the segments belong to the profile
//              if (!this.contains(segment))
//                  throw new IllegalArgumentException("Input segment is not part of this profile");
//        
//              if (!segment.contains(splitIndex))
//                  throw new IllegalArgumentException("Splitting index is not within the segment");
//        
//              // Remove old merge sources from this segment
//              segment.clearMergeSources();
//        
//              /*
//               * Create two new segments, make them into merge sources for the segment
//               * to be split then use the existing unmerge method to put them into the
//               * full profile
//               */
//        
//              // Replace the two segments in this profile
//              List<IBorderSegment> oldSegs = this.getSegments();
//              List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();
//        
//              // Add the new segments to a list
//              List<IBorderSegment> splitSegments = new ArrayList<IBorderSegment>();
//              splitSegments
//                      .add(IBorderSegment.newSegment(segment.getStartIndex(), splitIndex, segment.getTotalLength(), id1));
//              splitSegments.add(IBorderSegment.newSegment(splitIndex, segment.getEndIndex(), segment.getTotalLength(), id2));
//        
//              segment.addMergeSource(splitSegments.get(0));
//              segment.addMergeSource(splitSegments.get(1));
//        
//              int position = 0;
//              for (IBorderSegment oldSegment : oldSegs) {
//        
//                  if (oldSegment.equals(segment)) {
//        
//                      // add each of the old segments
//                      // for(IBorderSegment mergedSegment : splitSegments){
//                      // mergedSegment.setPosition(position);
//                      newSegs.add(segment);
//                      // position++;
//                      // }
//        
//                  } else {
//        
//                      // add the original segments
//                      oldSegment.setPosition(position);
//                      newSegs.add(oldSegment);
//                  }
//                  position++;
//              }
//              IBorderSegment.linkSegments(newSegs);
//              this.setSegments(newSegs);
//              this.unmergeSegment(segment);
//        
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#toString()
//           */
//          @Override
//          public String toString() {
//              StringBuilder builder = new StringBuilder();
//              builder.append(super.toString()+System.getProperty("line.separator"));
//              for (IBorderSegment seg : this.getSegments()) {
//                  builder.append(seg.toString() + System.getProperty("line.separator"));
//              }
//              return builder.toString();
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#valueString()
//           */
//          @Override
//          public String valueString() {
//              return super.toString();
//          }
//          
//          @Override
//          public ISegmentedProfile copy() {
//              try {
//                  return new DefaultSegmentedProfile(this);
//              } catch (IndexOutOfBoundsException | ProfileException e) {
//                  stack(e);
//              }
//              return null;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#hashCode()
//           */
//          @Override
//          public int hashCode() {
//              final int prime = 31;
//              int result = super.hashCode();
//              result = prime * result + ((segments == null) ? 0 : Arrays.hashCode(segments));
//              return result;
//          }
//        
//          /*
//           * (non-Javadoc)
//           * 
//           * @see components.generic.ISegmentedProfile#equals(java.lang.Object)
//           */
//          @Override
//          public boolean equals(Object obj) {        	  
//              if (this == obj)
//                  return true;
//              if (!super.equals(obj))
//                  return false;
//              if (getClass() != obj.getClass())
//                  return false;
//              DefaultSegmentedProfile other = (DefaultSegmentedProfile) obj;
//              if (ids == null) {
//                  if (other.ids != null)
//                      return false;
//              } else if (!Arrays.equals(ids, other.ids))
//                  return false;
//              if (segmentBounds == null) {
//                  if (other.segmentBounds != null)
//                      return false;
//              } else if (!Arrays.equals(segmentBounds, other.segmentBounds))
//                  return false;
//              return true;
//          }
//          
//          /**
//           * Wrap the segment index to allow incrementing of segment indices in loops
//           * @param i the index to wrap
//           * @return the wrapped index
//           */
//          protected int wrapSegmentIndex(int i){
//              if (i < 0)
//                  return wrapSegmentIndex(segmentBounds.length + i);
//              if (i < segmentBounds.length)
//                  return i;
//              return i % segmentBounds.length;
//          }
//
//
//      /**
//       * An implementation of border segments that relies on the segment
//       * being contained within a profile.
//       * @author ben
//       * @since 1.13.8
//       *
//       */
//        public class DefaultBorderSegment implements IBorderSegment {
//              
//              private UUID id;
//              boolean isLocked = false;
//              
//              public DefaultBorderSegment(@NonNull UUID id){
//                  this.id = id;
//              }
//        
//              @Override
//              public UUID getID() {
//                  return id;
//              }
//        
//              @Override
//              public List<IBorderSegment> getMergeSources() {  
//                  return mergeSources.get(id);
//              }
//        
//              @Override
//              public void addMergeSource(IBorderSegment seg) {
//            	  mergeSources.get(id).add(seg);
//              }
//        
//              @Override
//              public void clearMergeSources() {
//            	  mergeSources.put(id, new ArrayList<IBorderSegment>());
//              }
//        
//              @Override
//              public boolean hasMergeSources() {
//            	  return !mergeSources.get(id).isEmpty();
//              }
//        
//              @Override
//              public int getStartIndex() {
//            	  int index = getIndexOfSegment(id);
//                  return DefaultSegmentedProfile.this.segmentBounds[index];
//              }
//        
//              @Override
//              public int getEndIndex() {
//            	  int index = DefaultSegmentedProfile.this.getIndexOfSegment(id);
//                  return segmentBounds[wrapSegmentIndex(index+1)];
//              }
//              
//              @Override
//              public int getProportionalIndex(double d) {
//                  if (d < 0 || d > 1)
//                      throw new IllegalArgumentException("Proportion must be between 0-1: " + d);
//
//                  double desiredDistanceFromStart = (double) length() * d;
//
//                  int target = (int) desiredDistanceFromStart;
//
//                  return wrapIndex(target+getStartIndex());
//              }
//
//              @Override
//              public double getIndexProportion(int index) {
//                  if (!contains(index)) {
//                      throw new IllegalArgumentException("Index out of segment bounds: " + index);
//                  }
//
//                  return (double) index-getStartIndex() / (double) length();
//              }
//        
//
//        
//              @Override
//              public String getName() {
//                  return "Seg_"+getPosition();
//              }
//        
//              @Override
//              public int getMidpointIndex() {
//                  return wrapIndex(  (length()/2) + getStartIndex()  );
//              }
//        
//              @Override
//              public int getDistanceToStart(int index) {
//                  // TODO Auto-generated method stub
//                  return 0;
//              }
//        
//              @Override
//              public int getDistanceToEnd(int index) {
//                  // TODO Auto-generated method stub
//                  return 0;
//              }
//        
//              @Override
//              public boolean isLocked() {
//                  return isLocked;
//              }
//        
//              @Override
//              public void setLocked(boolean b) {
//                  isLocked=b;
//              }
//        
//              @Override
//              public int getTotalLength() {
//                  return size();
//              }
//        
//              @Override
//              public IBorderSegment nextSegment() {
//                  int i = getIndexOfSegment(id);
//                  return new DefaultBorderSegment(ids[wrapSegmentIndex(i+1)]);
//              }
//        
//              @Override
//              public IBorderSegment prevSegment() {
//            	  int i = getIndexOfSegment(id);
//                  return new DefaultBorderSegment(ids[wrapSegmentIndex(i-1)]);
//              }
//        
//              @Override
//              public int length() {
//                  return wraps() ? getEndIndex() + (DefaultSegmentedProfile.this.size()-getStartIndex()) 
//                		  : getEndIndex()-getStartIndex();
//              }
//        
//              @Override
//              public int testLength(int start, int end) {
//                  return wraps(start, end) ? size()-start + end : end-start;
//              }
//        
//              @Override
//              public boolean wraps(int start, int end) {
//                  return end<start;
//              }
//        
//              @Override
//              public boolean wraps() {
//                  return getEndIndex()<getStartIndex();
//              }
//        
//              @Override
//              public boolean contains(int index) {
//            	  return wraps() ? index<=getEndIndex() || index>=getStartIndex() 
//            			  : index>=getStartIndex() && index <=getEndIndex();
//              }
//        
//              @Override
//              public boolean testContains(int start, int end, int index) {
//                  // TODO Auto-generated method stub
//                  return false;
//              }
//        
//              @Override
//              public boolean update(int startIndex, int endIndex) throws SegmentUpdateException {
//                  // TODO Auto-generated method stub
//                  return false;
//              }
//        
//              @Override
//              public void setNextSegment(IBorderSegment s) {
//                  // Does nothing in this implementation
//              }
//        
//              @Override
//              public void setPrevSegment(IBorderSegment s) {
//            	// Does nothing in this implementation
//              }
//        
//              @Override
//              public boolean hasNextSegment() {
//            	// By default in this implementation
//                  return true;
//              }
//        
//              @Override
//              public boolean hasPrevSegment() {
//            	// By default in this implementation
//                  return true;
//              }
//        
//              @Override
//              public void setPosition(int i) {
//                  // TODO Auto-generated method stub 
//              }
//        
//              @Override
//              public int getPosition() {
//            	  return getIndexOfSegment(id);
//              }
//        
//              @Override
//              public Iterator<Integer> iterator() {
//                  // TODO Auto-generated method stub
//                  return null;
//              }
//        
//              @Override
//              public String getDetail() {
//                  return getStartIndex()+"-"+getEndIndex();
//              }
//        
//              @Override
//              public boolean overlaps(@NonNull IBorderSegment seg) {
//                  // TODO Auto-generated method stub
//                  return false;
//              }
//              
//              
//              
//          }
//          
//        }
//
//    }
