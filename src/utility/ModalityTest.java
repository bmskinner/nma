package utility;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import utility.ModalityTest.BinnedData.Bin;
import components.generic.Profile;

/**
 * Test a set of values for multimodality
 * and return the values of the modes. This uses the 
 * mvalue approach.
 *
 */
public class ModalityTest {

	private Profile profile;
	
	private BinnedData binData;
	
	
	/**
	 * 
	 * @param data
	 */
	public ModalityTest(double[] data, double minBinWidth, double maxBinWidth, double stepSize){
		double[] cleanedData = trimOutliers(data);
		this.profile = new Profile(cleanedData);
		
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
		
		
		double q1 = Stats.quartile(data, 25);
		double q3 = Stats.quartile(data, 75);
		double iqr = q3 - q1;
		double minValue = q1 - (1.5 * iqr);
		double maxValue = q3 + (1.5 * iqr);
		
		List<Double> result = new ArrayList<Double>();
		for(double d : data){
			if(d >= minValue && d <= maxValue){
				result.add(d);
			}
		}
		return Utils.getdoubleFromDouble(result.toArray(new Double[0]));
		
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
		return Stats.max(bins.toArray());
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
			
			return  Utils.getintFromInteger(  result.toArray(new Integer[0])  );
		}
		
		public List<Bin> getBins(){
			return this.bins;
		}
		
		public double[] getLocalMaxima(){
			Profile profile = new Profile(     Utils.getdoubleFromInt( this.toArray()   )  );
			Profile maxima = profile.smooth(3).smooth(3).getLocalMaxima(3);
//			profile.smooth(3).smooth(3).print();
//			maxima.print();
			
			List<Double> result = new ArrayList<Double>();
			
			for(int i=0; i<maxima.size(); i++){
				if(maxima.get(i)==1){
					result.add( new Double( this.midpoint(bins.get(i)))  );
				}
			}
			
			return Utils.getdoubleFromDouble(   result.toArray(new Double[0]  ) );
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