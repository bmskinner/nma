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
package com.bmskinner.nma.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.profiles.BooleanProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.stats.ModalityTest.BinnedData.Bin;

/**
 * Test a set of values for multimodality and return the values of the modes.
 * This uses the mvalue approach.
 *
 */
public class ModalityTest implements SignificanceTest {
    private BinnedData binData;

    public static final int DEFAULT_SMOOTHING_WINDOW = 3;
    
    private class DoubleProfile {
    	protected final double[]  array;
    	public DoubleProfile(final double[] values) {

            if (values.length == 0) {
                throw new IllegalArgumentException("Input array has zero length in profile constructor");
            }
            this.array = Arrays.copyOf(values, values.length);
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
                int index = CellularComponent.wrapIndex(position + ((j + 1) * type), array.length);
                values[j] = array[index];
            }
            return values;
        }
    	
    	public DoubleProfile smooth(int windowSize) {
            if(windowSize < 1)
                throw new IllegalArgumentException("Window size must be a positive integer");
            
            double[] result = new double[array.length];

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
    	
    	public BooleanProfile getLocalMaxima(int windowSize) {
            if (windowSize < 1)
                throw new IllegalArgumentException("Window size must be a positive integer greater than 0");

            boolean[] result = new boolean[array.length];

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
    }

    /**
     * 
     * @param data
     */
    public ModalityTest(double[] data, double minBinWidth, double maxBinWidth, double stepSize) {
        double[] cleanedData = trimOutliers(data);
        // http://www.brendangregg.com/FrequencyTrails/modes.html

        // Remove outliers from data.
        // Select the smallest bin size.
        // Group data into equally sized bins for the range.
        // Step through bins, summing the absolute difference in bin counts,
        // adding terminator bins of zero.
        // Calculate mvalue: divide sum by largest bin count.
        // Select a larger bin size.
        // Goto 3, and repeat until largest bin size tried.
        // Use the largest mvalue found.
        double binWidth = minBinWidth;

        double mValue = 0;

        while (binWidth <= maxBinWidth) {

            BinnedData bins = calculateBins(cleanedData, binWidth);

            int sum = sumDifferencesInBins(bins);

            int largestBinCount = getLargestBinCount(bins);

            bins.mValue = calculateMValue(sum, largestBinCount);

            if (bins.mValue > mValue) {
                mValue = bins.mValue;
                this.binData = bins;
            }

            binWidth += stepSize;

        }

    }

    public double getMValue() {
        return binData.mValue;
    }

    public BinnedData getBinnedData() {
        return this.binData;
    }

    /**
     * Remove the outliers of data. Only include data within the range: Q1 - 1.5
     * x IQR to Q3 + 1.5 x IQR
     * 
     * @param data
     *            the array of input data
     * @return the data without outliers
     */
    private double[] trimOutliers(double[] data) {

        double q1 = Stats.quartile(data, Stats.LOWER_QUARTILE);
        double q3 = Stats.quartile(data, Stats.UPPER_QUARTILE);
        double iqr = q3 - q1;
        double minValue = q1 - (1.5 * iqr);
        double maxValue = q3 + (1.5 * iqr);

        List<Double> result = new ArrayList<Double>();
        for (double d : data) {
            if (d >= minValue && d <= maxValue) {
                result.add(d);
            }
        }
        
        return result.stream().mapToDouble(d->d.doubleValue()).toArray();
    }

    private BinnedData calculateBins(double[] data, double binWidth) {

        Arrays.sort(data);

        BinnedData result = new BinnedData(binWidth);

        double binStart = data[0];
        double binEnd = binStart + binWidth;

        // assign each value to a bin
        int count = 0;
        for (double d : data) {

            if (d >= binStart && d < binEnd) {
                count++;
            } else {
                // new bin
                result.addBin(binStart, count);
                binStart = binEnd;
                binEnd += binWidth;
                count = 0;
            }
        }
        return result;
    }

    /**
     * Calculate the sum of differences in a dataset
     * 
     * @param data
     * @return
     */
    private int sumDifferencesInBins(BinnedData data) {
        int sum = 0;

        int prevValue = 0;
        for (Bin b : data.getBins()) {
            sum += Math.abs(b.value - prevValue);
            prevValue = b.value;
        }
        sum += Math.abs(0 - prevValue); // last value
        return sum;
    }

    private int getLargestBinCount(BinnedData bins) {
        return Arrays.stream(bins.toArray()).max().orElse(0);
    }

    /**
     * Calculate mvalue: divide sum of differences by largest bin count.
     * 
     * @param sumOfDifferences
     * @param largestBinCount
     * @return
     */
    private double calculateMValue(int sumOfDifferences, int largestBinCount) {
        return (double) sumOfDifferences / (double) largestBinCount;
    }

    public class BinnedData {

        List<Bin> bins = new ArrayList<Bin>();
        double    binWidth;
        double    mValue;

        public BinnedData(double binWidth) {
            this.binWidth = binWidth;
        }

        public void addBin(Bin bin) {
            this.bins.add(bin);
        }

        public double midpoint(Bin b) {
            return b.start + (binWidth / 2);
        }

        public void addBin(double start, int value) {
            this.addBin(new Bin(start, value));
        }

        public int size() {
            return bins.size();
        }

        public int[] toArray() {
            List<Integer> result = new ArrayList<Integer>();

            for (Bin b : bins) {
                result.add(b.value);
            }
            return result.stream().mapToInt(d->d.intValue()).toArray();
        }

        public List<Bin> getBins() {
            return this.bins;
        }

        public double[] getLocalMaxima() {

            double[] temp = IntStream.of(toArray()).mapToDouble(i-> (double)i).toArray();

            DoubleProfile profile = new DoubleProfile(temp);
            BooleanProfile maxima = profile.smooth(DEFAULT_SMOOTHING_WINDOW)
            		.smooth(DEFAULT_SMOOTHING_WINDOW)
                    .getLocalMaxima(3);

            List<Double> result = new ArrayList<>();

            for (int i = 0; i < maxima.size(); i++) {
                if (maxima.get(i)) {
                    result.add(this.midpoint(bins.get(i)));
                }
            }
  
            return result.stream().mapToDouble(Double::doubleValue).toArray();
        }

        public class Bin {

            double start;
            int    value;

            public Bin(double start, int value) {
                this.start = start;
                this.value = value;
            }
        }
    }

}
