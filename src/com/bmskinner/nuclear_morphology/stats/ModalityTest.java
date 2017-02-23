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
package com.bmskinner.nuclear_morphology.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.BooleanProfile;
import com.bmskinner.nuclear_morphology.components.generic.DoubleProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.stats.ModalityTest.BinnedData.Bin;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter.ArrayConversionException;

/**
 * Test a set of values for multimodality
 * and return the values of the modes. This uses the 
 * mvalue approach.
 *
 */
public class ModalityTest implements SignificanceTest {

	private IProfile profile;
	
	private BinnedData binData;
	
	public static final int DEFAULT_SMOOTHING_WINDOW = 3;
	
	
	/**
	 * 
	 * @param data
	 */
	public ModalityTest(double[] data, double minBinWidth, double maxBinWidth, double stepSize){
		double[] cleanedData = trimOutliers(data);
		this.profile = new DoubleProfile(cleanedData);
		
//		http://www.brendangregg.com/FrequencyTrails/modes.html

//	    Remove outliers from data.
//	    Select the smallest bin size.
//	    Group data into equally sized bins for the range.
//	    Step through bins, summing the absolute difference in bin counts, adding terminator bins of zero.
//	    Calculate mvalue: divide sum by largest bin count.
//	    Select a larger bin size.
//	    Goto 3, and repeat until largest bin size tried.
//	    Use the largest mvalue found.
		double binWidth = minBinWidth;
		
		double mValue = 0;

		
		while(binWidth <= maxBinWidth){
			
			BinnedData bins = calculateBins(profile.asArray(), binWidth);
			
			int sum = sumDifferencesInBins(bins);
			
			int largestBinCount = getLargestBinCount(bins);
			
			bins.mValue = calculateMValue(sum, largestBinCount);
			
			if(bins.mValue > mValue){
				mValue = bins.mValue;
				this.binData = bins;
			}
			
			binWidth += stepSize;
			
		}

	}

	
	public double getMValue(){
		return binData.mValue;
	}
	
	public BinnedData getBinnedData(){
		return this.binData;
	}
	
	
	
	/**
	 * Remove the outliers of data.
	 * Only include data within the range: Q1 - 1.5 x IQR to Q3 + 1.5 x IQR
	 * @param data the array of input data
	 * @return the data without outliers
	 */
	private double[] trimOutliers(double data[]){
		
		
		double q1 = new Quartile(data, Quartile.LOWER_QUARTILE).doubleValue();
		double q3 = new Quartile(data, Quartile.UPPER_QUARTILE).doubleValue();
		double iqr = q3 - q1;
		double minValue = q1 - (1.5 * iqr);
		double maxValue = q3 + (1.5 * iqr);
		
		List<Double> result = new ArrayList<Double>();
		for(double d : data){
			if(d >= minValue && d <= maxValue){
				result.add(d);
			}
		}
		
		double[] temp;
		try {
			temp = new ArrayConverter(result).toDoubleArray();
		} catch (ArrayConversionException e) {
			temp = new double[0]; 
		}
		
		return temp;
		
	}
	
	private BinnedData calculateBins(double[] data, double binWidth){
		
		Arrays.sort(data);
		
		BinnedData result = new BinnedData(binWidth);

		double binStart = data[0];
		double binEnd = binStart + binWidth;
		
		// assign each value to a bin
		int count = 0;
		for(double d : data){
			
			if(d >= binStart && d < binEnd){
				count++;
			} else {
				// new bin
				result.addBin(binStart, count);
//				bins[bin] = count;
//				bin++;
				binStart = binEnd;
				binEnd += binWidth;
				count = 0;
			}
		}
		return result;
	}
	
	/**
	 * Calculate the sum of differences in a dataset
	 * @param data
	 * @return
	 */
	private int sumDifferencesInBins(BinnedData data){
		int sum = 0;
		
		int prevValue = 0;
		for(Bin b : data.getBins()){
			sum += Math.abs(b.value - prevValue);
			prevValue = b.value;
		}
		sum += Math.abs(0 - prevValue); // last value
		return sum;
	}
	
	private int getLargestBinCount(BinnedData bins){
		return Arrays.stream(bins.toArray()).max().orElse(0);
//		return Stats.max(bins.toArray());
	}
	
	
	/**
	 * Calculate mvalue: divide sum of differences by largest bin count.
	 * @param sumOfDifferences
	 * @param largestBinCount
	 * @return
	 */
	private double calculateMValue(int sumOfDifferences, int largestBinCount){
		double result = (double) sumOfDifferences / (double) largestBinCount;
		return result;
	}
	
	public class BinnedData {
		
		List<Bin> bins = new ArrayList<Bin>();
		double binWidth;
		double mValue;
		
		public BinnedData(double binWidth){
			this.binWidth = binWidth;
		}
		
		public void addBin(Bin bin){
			this.bins.add(bin);
		}
		
		public double midpoint(Bin b){
			return b.start + (binWidth/2);
		}
		
		public void addBin(double start, int value){
			this.addBin(new Bin(start, value));
		}
		
		public int size(){
			return bins.size();
		}
		
		public int[] toArray(){
			List<Integer> result = new ArrayList<Integer>();
			
			for(Bin b : bins){
				result.add(b.value);
			}
			
			int[] temp;
			try {
				temp = new ArrayConverter(result).toIntArray();
			} catch (ArrayConversionException e) {
				temp = new int[0]; 
			}
			
			return  temp;
		}
		
		public List<Bin> getBins(){
			return this.bins;
		}
		
		public double[] getLocalMaxima(){
			
			double[] temp;
			try {
				temp = new ArrayConverter(this.toArray()).toDoubleArray();
			} catch (ArrayConversionException e) {
				temp = new double[0]; 
			}
			

			
			IProfile profile = new DoubleProfile(  temp  );
			BooleanProfile maxima = profile.smooth(DEFAULT_SMOOTHING_WINDOW).smooth(DEFAULT_SMOOTHING_WINDOW).getLocalMaxima(3);
			
			List<Double> result = new ArrayList<Double>();
			
			for(int i=0; i<maxima.size(); i++){
				if(maxima.get(i)==true){
					result.add( new Double( this.midpoint(bins.get(i)))  );
				}
			}
			
			double[] temp2;
			try {
				temp2 = new ArrayConverter(result).toDoubleArray();
			} catch (ArrayConversionException e) {
				temp2 = new double[0]; 
			}
			
			return temp2;
		}
		
		
		public class Bin {
			
			double start;
			int value;
			public Bin(double start, int value){
				this.start = start;
				this.value = value;
			}
		}
	}
	
}