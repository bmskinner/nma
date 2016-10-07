/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package charting.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import logging.Loggable;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;

import charting.options.ChartOptions;
import utility.ArrayConverter;
import utility.Constants;
import utility.ArrayConverter.ArrayConversionException;
import weka.estimators.KernelEstimator;
import analysis.AnalysisDataset;
import components.CellCollection;
import components.generic.BorderTagObject;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import stats.Max;
import stats.Min;
import stats.NucleusStatistic;
import stats.SegmentStatistic;
import stats.Stats;

public class NuclearHistogramDatasetCreator implements Loggable {
	
	private static NuclearHistogramDatasetCreator instance = null;
	
	private NuclearHistogramDatasetCreator(){}
	
	public static NuclearHistogramDatasetCreator getInstance(){
		if(instance==null){
			instance = new NuclearHistogramDatasetCreator();
		}
		return instance;
	}
	
	public static final int MIN_ROUNDED = 0;
	public static final int MAX_ROUNDED = 1;
	public static final int STEP_SIZE   = 2;
	
	public HistogramDataset createNuclearStatsHistogramDataset(ChartOptions options) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		
			finest("Creating histogram dataset: "+options.getStat());

		
		if(options.hasDatasets()){

			for(AnalysisDataset dataset : options.getDatasets()){

				CellCollection collection = dataset.getCollection();


				NucleusStatistic stat = (NucleusStatistic) options.getStat();
				double[] values = collection.getNuclearStatistics(stat, options.getScale());
				
				double[] minMaxStep = findMinAndMaxForHistogram(values);
				int minRounded = (int) minMaxStep[0];
				int maxRounded = (int) minMaxStep[1];

				int bins = findBinSizeForHistogram(values, minMaxStep);

				String groupLabel = options.getStat().toString();

				ds.addSeries(groupLabel+"_"+collection.getName(), values, bins, minRounded, maxRounded );
			}
		}
//		options.log(Level.FINEST, "Completed histogram dataset");
		return ds;
	}
		
	private double[] findMinAndMaxForHistogram(double[] values){
		double min = Arrays.stream(values).min().orElse(0); //Stats.min(values);
		double max = Arrays.stream(values).max().orElse(0); //Stats.max(values);

		int log = (int) Math.floor(  Math.log10(min)  ); // get the log scale
		
		int roundLog = log-1 == 0 ? log-2 : log-1; // get the nearest log value that is not zero
		double roundAbs = Math.pow(10, roundLog); // find the absolute value of the log
		
		int binLog = log-2; // get a value for the bin sizes that is 1/100 of the main log
		double stepSize = Math.pow(10, binLog); // turn the log into an absolute step size
		
		finest("Range finding: binLog: "+binLog+"; step: "+stepSize);
		// If stepsize is < 1 for stats that increment in steps of 1, we will get blanks in the histogram
		// Correct based on the stat.
		if(stepSize<=1){
			
			// Only worry if there are non integer values in the array
			boolean isInteger = true;
			for(double value : values){
				// Check is an integer equivalent
				if (   value != Math.floor(value)) {
					isInteger = false;
				}
			}
			
			if(isInteger){
				finest("Detected integer only values: setting histogram step size to 1");
				stepSize=1;
			} else {
				finest("Non-integer values: setting histogram step size to "+stepSize);
			}
			
		}
		
		
		// use int truncation to round to nearest 100 above max
		int maxRounded = (int) ((( (int)max + (roundAbs) ) / roundAbs ) * roundAbs);
		maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct offsets for measures between 0-1
		int minRounded = (int) (((( (int)min + (roundAbs) ) / roundAbs ) * roundAbs  ) - roundAbs);
		minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1;  // correct offsets for measures between 0-1
		minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures start from at least zero

		double[] result = new double[3];
		result[0] = minRounded;
		result[1] = maxRounded;
		result[2] = stepSize;
		return result;
	}
	
	private int findBinSizeForHistogram(double[] values, double[] minMaxStep){

		int minRounded = (int) minMaxStep[0];
		int maxRounded = (int) minMaxStep[1];
		double stepSize= minMaxStep[2];
		
		int bins = (int) (( (double) maxRounded - (double) minRounded) / stepSize);
		
		if(stepSize == 1d){
			bins = maxRounded - minRounded; // set integer steps directly
		}
		
		bins = bins>100 ? 100 : bins; // but don't have too many bins
		
		bins = bins < 1 ? 11 : bins;  // and also don't have too few bins, or the chart looks silly
		return bins;
	}
	
	/**
	 * Make an XY dataset corresponding to the probability density of a given nuclear statistic
	 * @param list the datasets to draw
	 * @param stat the statistic to measure
	 * @return a charting dataset
	 * @throws Exception
	 */
	public DefaultXYDataset createNuclearDensityHistogramDataset(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale) throws Exception {
		DefaultXYDataset ds = new DefaultXYDataset();
		
		int[] minMaxRange = calculateMinAndMaxRange(list, stat, scale);

		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			
			String groupLabel = stat.toString();
			double[] values = collection.getNuclearStatistics(stat, scale);
			KernelEstimator est = NucleusDatasetCreator.getInstance().createProbabililtyKernel(values, 0.001);
	
			
			double[] minMax = findMinAndMaxForHistogram(values);

			List<Double> xValues = new ArrayList<Double>();
			List<Double> yValues = new ArrayList<Double>();

			for(double i=minMaxRange[0]; i<=minMaxRange[1]; i+=minMax[STEP_SIZE]){


				xValues.add(i);
				yValues.add(est.getProbability(i));

			}
			
			// Make into an array or arrays
			
			double[] xData;
			double[] yData;
			
			try{
				
				xData = new ArrayConverter(xValues).toDoubleArray();
				yData = new ArrayConverter(yValues).toDoubleArray();
				
			} catch (ArrayConversionException e) {
				xData = new double[0]; 
				yData = new double[0]; 
			}
			double[][] data = { xData, yData} ;


			ds.addSeries(groupLabel+"_"+collection.getName(), data);

		}
		return ds;
	}
		
	/**
	 * Calculate the minimum and maximum ranges in a list of datasets
	 * for the given stat type
	 * @param list the datasets
	 * @param stat the statistic to use
	 * @return an array with the min and max of the range
	 * @throws Exception
	 */
	private int[] calculateMinAndMaxRange(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale) throws Exception {
		
		int[] result = new int[2];
		result[0] = Integer.MAX_VALUE; // holds min
		result[1] = 0; // holds max

		for(AnalysisDataset dataset : list){
			
			double[] values = dataset.getCollection().getNuclearStatistics(stat, scale);
			
			updateMinMaxRange(result, values);
		}
		
		return result;
	}
	
	/**
	 * Given an existing range for an axis scale, check if the range must be expanded for
	 * the given set of values
	 * @param range the existing min and max
	 * @param values the new values
	 * @return
	 */
	public int[] updateMinMaxRange(int[] range, double[] values){
		
		double min = Arrays.stream(values).min().orElse(0); //Stats.min(values);
		double max = Arrays.stream(values).max().orElse(0); //Stats.max(values);
					
		int log = (int) Math.floor(  Math.log10(min)  ); // get the log scale
					
		int roundLog = log-1 == 0 ? log-2 : log-1;
		double roundAbs = Math.pow(10, roundLog);
					
		// use int truncation to round to nearest 100 above max
		int maxRounded = (int) ((( (int)max + (roundAbs) ) / roundAbs ) * roundAbs);
		maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct offsets for measures between 0-1
		int minRounded = (int) (((( (int)min + (roundAbs) ) / roundAbs ) * roundAbs  ) - roundAbs);
		minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1;  // correct offsets for measures between 0-1
		minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures start from at least zero
		
		range[0] = range[0] < minRounded ? range[0] : minRounded;
		range[1] = range[1] > maxRounded ? range[1] : maxRounded;
		
		return range;
	}
	
	public HistogramDataset createSegmentLengthHistogramDataset(ChartOptions options) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		

		options.log(Level.FINEST, "Creating histogram dataset: "+options.getStat());

		
		if( ! options.hasDatasets() ){
			return ds;
		}
		
		for(AnalysisDataset dataset : options.getDatasets()){

//			options.log(Level.FINEST, "  Dataset: "+dataset.getName());

			CellCollection collection = dataset.getCollection();
			
			/*
			 * Find the seg id for the median segment at the requested position
			 */
			NucleusBorderSegment medianSeg = collection
					.getProfileCollection(ProfileType.ANGLE)
					.getSegmentedProfile(BorderTagObject.REFERENCE_POINT)
					.getSegmentAt(options.getSegPosition());

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			
			double[] values = collection.getSegmentStatistics(SegmentStatistic.LENGTH, 
					options.getScale(), 
					medianSeg.getID());
			
			double[] minMaxStep = findMinAndMaxForHistogram(values);
			int minRounded = (int) minMaxStep[0];
			int maxRounded = (int) minMaxStep[1];

			int bins = findBinSizeForHistogram(values, minMaxStep);

			ds.addSeries(Constants.SEGMENT_PREFIX+options.getSegPosition()+"_"+collection.getName(), values, bins, minRounded, maxRounded );
		}

		options.log(Level.FINEST, "Completed histogram dataset");
		return ds;
	}
	
	/**
	 * Get the lengths of the given segment in the collections
	 * @param collections
	 * @param segName
	 * @return
	 * @throws Exception 
	 */
	public DefaultXYDataset createSegmentLengthDensityDataset(ChartOptions options) throws Exception {

		int[] minMaxRange = {Integer.MAX_VALUE, 0}; // start with extremes, trim to fit data
		for(AnalysisDataset dataset : options.getDatasets()){
			CellCollection collection = dataset.getCollection();
			
			/*
			 * Find the seg id for the median segment at the requested position
			 */
			NucleusBorderSegment medianSeg = collection
					.getProfileCollection(ProfileType.ANGLE)
					.getSegmentedProfile(BorderTagObject.REFERENCE_POINT)
					.getSegmentAt(options.getSegPosition());

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			int count=0;
			double[] lengths = new double[collection.cellCount()];
			for(Nucleus n : collection.getNuclei()){

				NucleusBorderSegment seg = n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT)
						.getSegment(medianSeg.getID());

				int indexLength = seg.length();
				double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
				double length = n.getStatistic(NucleusStatistic.PERIMETER, options.getScale()) * proportionPerimeter;
				lengths[count++] = length;
			}
			
			minMaxRange = updateMinMaxRange(minMaxRange, lengths);
		}

		
		// Ranges are found, now make the kernel
		DefaultXYDataset ds = new DefaultXYDataset();
		
		

		for(AnalysisDataset dataset : options.getDatasets()){
			CellCollection collection = dataset.getCollection();
			/*
			 * Find the seg id for the median segment at the requested position
			 */
			NucleusBorderSegment medianSeg = collection
					.getProfileCollection(ProfileType.ANGLE)
					.getSegmentedProfile(BorderTagObject.REFERENCE_POINT)
					.getSegmentAt(options.getSegPosition());

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			int count=0;
			double[] lengths = new double[collection.cellCount()];
			for(Nucleus n : collection.getNuclei()){

				NucleusBorderSegment seg = n.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT)
						.getSegment(medianSeg.getID());
				
				int indexLength = seg.length();
				double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
				double length = n.getStatistic(NucleusStatistic.PERIMETER, options.getScale()) * proportionPerimeter;
				lengths[count++] = length;
			}
			
			KernelEstimator est = NucleusDatasetCreator.getInstance().createProbabililtyKernel(  lengths , 0.001 );
	
			double min = Arrays.stream(lengths).min().orElse(0); //Stats.min(values);
			double max = Arrays.stream(lengths).max().orElse(0); //Stats.max(values);

			int log = (int) Math.floor(  Math.log10(min)  ); // get the log scale
			
			int roundLog = log-1 == 0 ? log-2 : log-1;
			double roundAbs = Math.pow(10, roundLog);
			
			int binLog = log-2;
			double stepSize = Math.pow(10, binLog);
			
//			IJ.log("   roundLog: "+roundLog);
//			IJ.log("   round to nearest: "+roundAbs);
			
			// use int truncation to round to nearest 100 above max
			int maxRounded = (int) ((( (int)max + (roundAbs) ) / roundAbs ) * roundAbs);
			maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct offsets for measures between 0-1
			int minRounded = (int) (((( (int)min + (roundAbs) ) / roundAbs ) * roundAbs  ) - roundAbs);
			minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1;  // correct offsets for measures between 0-1
			minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures start from at least zero
	
			
			List<Double> xValues = new ArrayList<Double>();
			List<Double> yValues = new ArrayList<Double>();
			
			for(double i=minMaxRange[0]; i<=minMaxRange[1]; i+=stepSize){
				xValues.add(i);
				yValues.add(est.getProbability(i));
			}
			
			double[] xData;
			double[] yData;
			
			try{
				
				xData = new ArrayConverter(xValues).toDoubleArray();
				yData = new ArrayConverter(yValues).toDoubleArray();
				
			} catch (ArrayConversionException e) {
				xData = new double[0]; 
				yData = new double[0]; 
			}
			double[][] data = { xData, yData} ;
				
			
			ds.addSeries(Constants.SEGMENT_PREFIX+options.getSegPosition()+"_"+collection.getName(), data);
		}

		return ds;
	}
	
	/**
	 * Create a histogram dataset from a list of double values
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public HistogramDataset createHistogramDatasetFromList(List<Double> list) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		if(!list.isEmpty()){
		
			double[] values;

			try{
				values = new ArrayConverter(list).toDoubleArray();

			} catch (ArrayConversionException e) {
				values = new double[0]; 
			}
			
			double min = new Min(list).doubleValue(); 
			double max = new Max(list).doubleValue();
			int bins = 100;

			ds.addSeries("Sample", values, bins, min, max );
		}
		return ds;
	}
	
	public DefaultXYDataset createDensityDatasetFromList(List<Double> list, double binWidth) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		if(!list.isEmpty()){
			

			double[] values;

			try{
				values = new ArrayConverter(list).toDoubleArray();

			} catch (ArrayConversionException e) {
				values = new double[0]; 
			}
			
			KernelEstimator est = NucleusDatasetCreator.getInstance().createProbabililtyKernel(values, binWidth);

			List<Double> xValues = new ArrayList<Double>();
			List<Double> yValues = new ArrayList<Double>();

			double min = new Min(list).doubleValue(); 
			double max = new Max(list).doubleValue();
			
			for(double i=min; i<=max; i+=0.0001){
				xValues.add(i);
				yValues.add(est.getProbability(i));
			}
			
			double[] xData;
			double[] yData;
			
			try{
				
				xData = new ArrayConverter(xValues).toDoubleArray();
				yData = new ArrayConverter(yValues).toDoubleArray();
				
			} catch (ArrayConversionException e) {
				xData = new double[0]; 
				yData = new double[0]; 
			}
			double[][] data = { xData, yData} ;

			ds.addSeries("Density", data);
		}
		return ds;
		
	}
	
}
