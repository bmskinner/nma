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
package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import weka.estimators.KernelEstimator;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.stats.Max;
import com.bmskinner.nuclear_morphology.stats.Min;
import com.bmskinner.nuclear_morphology.stats.Quartile;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter;
import com.bmskinner.nuclear_morphology.utility.ArrayConverter.ArrayConversionException;

public class NuclearHistogramDatasetCreator extends AbstractDatasetCreator<ChartOptions> {
	
	public static final int MIN_ROUNDED = 0;
	public static final int MAX_ROUNDED = 1;
	public static final int STEP_SIZE   = 2;
		
	public NuclearHistogramDatasetCreator(final ChartOptions o){
		super(o);
		
	}
	

	public HistogramDataset createNuclearStatsHistogramDataset() throws ChartDatasetCreationException {
		HistogramDataset ds = new HistogramDataset();
		
		finest("Creating histogram dataset: "+options.getStat());
		if( ! options.hasDatasets()){
			return ds;
		}
		
		for(IAnalysisDataset dataset : options.getDatasets()){

			ICellCollection collection = dataset.getCollection();

			PlottableStatistic stat =  options.getStat();
			double[] values = collection.getMedianStatistics(stat, CellularComponent.NUCLEUS, options.getScale());

			double[] minMaxStep = findMinAndMaxForHistogram(values);
			int minRounded = (int) minMaxStep[0];
			int maxRounded = (int) minMaxStep[1];

			int bins = findBinSizeForHistogram(values, minMaxStep);

			String groupLabel = stat.toString();
			
			if(minRounded>=maxRounded){
				throw new ChartDatasetCreationException("Histogram lower bound equal to or grater than upper bound");
			}

			ds.addSeries(groupLabel+"_"+collection.getName(), values, bins, minRounded, maxRounded );
		}
		
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
	 * @return a charting dataset
	 * @throws ChartDatasetCreationException
	 */
	public XYDataset createNuclearDensityHistogramDataset() throws ChartDatasetCreationException {
		DefaultXYDataset ds = new DefaultXYDataset();
		
		if( ! options.hasDatasets()){
			return ds;
		}
		
		
		List<IAnalysisDataset> list = options.getDatasets();
		PlottableStatistic stat    = options.getStat();
		MeasurementScale scale     = options.getScale();
		
		int[] minMaxRange = calculateMinAndMaxRange(list, stat, CellularComponent.NUCLEUS, scale);

		for(IAnalysisDataset dataset : list){
			ICellCollection collection = dataset.getCollection();
			
			String groupLabel = stat.toString();
			double[] values = collection.getMedianStatistics(stat, CellularComponent.NUCLEUS, scale);
			
			KernelEstimator est;
			try {
				est = new NucleusDatasetCreator(options).createProbabililtyKernel(values, 0.001);
			} catch (Exception e1) {
				throw new ChartDatasetCreationException("Cannot make probability kernel", e1);
			}

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
	private static int[] calculateMinAndMaxRange(List<IAnalysisDataset> list, PlottableStatistic stat, String component, MeasurementScale scale) throws ChartDatasetCreationException {
		
		int[] result = new int[2];
		result[0] = Integer.MAX_VALUE; // holds min
		result[1] = 0; // holds max

		for(IAnalysisDataset dataset : list){
			
			double[] values = dataset.getCollection().getMedianStatistics(stat, component, scale);
			
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
	public static int[] updateMinMaxRange(int[] range, double[] values){
		
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
	
	public HistogramDataset createSegmentLengthHistogramDataset() throws ChartDatasetCreationException {
		HistogramDataset ds = new HistogramDataset();
		

		finest("Creating histogram dataset: "+options.getStat());

		
		if( ! options.hasDatasets() ){
			return ds;
		}
		
		for(IAnalysisDataset dataset : options.getDatasets()){

			ICellCollection collection = dataset.getCollection();
			
			/*
			 * Find the seg id for the median segment at the requested position
			 */
			
			try {
			
			IBorderSegment medianSeg = collection
					.getProfileCollection()
					.getSegmentAt(Tag.REFERENCE_POINT, options.getSegPosition());

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			
			double[] values;

			values = collection.getMedianStatistics(PlottableStatistic.LENGTH, 
					CellularComponent.NUCLEAR_BORDER_SEGMENT,
					options.getScale(), 
					medianSeg.getID());
	
			
			double[] minMaxStep = findMinAndMaxForHistogram(values);
			int minRounded = (int) minMaxStep[0];
			int maxRounded = (int) minMaxStep[1];

			int bins = findBinSizeForHistogram(values, minMaxStep);

			ds.addSeries(IBorderSegment.SEGMENT_PREFIX+options.getSegPosition()+"_"+collection.getName(), values, bins, minRounded, maxRounded );
			}catch(UnavailableBorderTagException | ProfileException e){
				throw new ChartDatasetCreationException("Cannot get segments for "+dataset.getName(), e);
			}
		}

		finest("Completed histogram dataset");
		return ds;
	}
	
	/**
	 * Get the lengths of the given segment in the collections
	 * @return
	 * @throws ChartDatasetCreationException 
	 */
	public XYDataset createSegmentLengthDensityDataset() throws ChartDatasetCreationException {

		int[] minMaxRange = {Integer.MAX_VALUE, 0}; // start with extremes, trim to fit data
		for(IAnalysisDataset dataset : options.getDatasets()){
			ICellCollection collection = dataset.getCollection();
			
			/*
			 * Find the seg id for the median segment at the requested position
			 */
			IBorderSegment medianSeg;
			
				try {
					medianSeg = collection
							.getProfileCollection()
							.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
							.getSegmentAt(options.getSegPosition());
				} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException | UnsegmentedProfileException e) {
					fine("Error getting profile from tag", e);
					throw new ChartDatasetCreationException("Unable to get median profile", e);
				}
			

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			int count=0;
			double[] lengths = new double[collection.size()];
			for(Nucleus n : collection.getNuclei()){

				IBorderSegment seg;
				try {
					seg = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
							.getSegment(medianSeg.getID());
					int indexLength = seg.length();
					double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
					double length = n.getStatistic(PlottableStatistic.PERIMETER, options.getScale()) * proportionPerimeter;
					lengths[count] = length;
				} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
					fine("Error getting segment length");
					lengths[count] = 0;
				} finally {
					count++;
				}

			}
			
			minMaxRange = updateMinMaxRange(minMaxRange, lengths);
		}

		
		// Ranges are found, now make the kernel
		DefaultXYDataset ds = new DefaultXYDataset();
		
		

		for(IAnalysisDataset dataset : options.getDatasets()){
			ICellCollection collection = dataset.getCollection();
			/*
			 * Find the seg id for the median segment at the requested position
			 */
			IBorderSegment medianSeg;
			try {
				medianSeg = collection
						.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
						.getSegmentAt(options.getSegPosition());
			} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException | UnsegmentedProfileException e2) {
				fine("Error getting profile from tag", e2);
				throw new ChartDatasetCreationException("Unable to get median profile", e2);
			}

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			int count=0;
			double[] lengths = new double[collection.size()];
			for(Nucleus n : collection.getNuclei()){

				IBorderSegment seg;
				try {
					seg = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
							.getSegment(medianSeg.getID());
					
					int indexLength = seg.length();
					double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
					double length = n.getStatistic(PlottableStatistic.PERIMETER, options.getScale()) * proportionPerimeter;
					lengths[count] = length;
				} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
					fine("Error getting segment length");
					lengths[count] = 0;
				} finally {
					count++;
				}
				
				
			}
			
			KernelEstimator est;
			try {
				est = new NucleusDatasetCreator(options).createProbabililtyKernel(lengths , 0.001);
			} catch (Exception e1) {
				throw new ChartDatasetCreationException("Cannot make probability kernel", e1);
			}

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
				
			
			ds.addSeries(IBorderSegment.SEGMENT_PREFIX+options.getSegPosition()+"_"+collection.getName(), data);
		}

		return ds;
	}
	
	/**
	 * Create a histogram dataset from a list of double values
	 * @param list
	 * @return
	 * @throws Exception
	 */
	public static HistogramDataset createHistogramDatasetFromList(List<Double> list) throws ChartDatasetCreationException {
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
	
	public XYDataset createDensityDatasetFromList(List<Double> list, double binWidth) throws ChartDatasetCreationException{
		DefaultXYDataset ds = new DefaultXYDataset();
		if(!list.isEmpty()){
			

			double[] values;

			try{
				values = new ArrayConverter(list).toDoubleArray();

			} catch (ArrayConversionException e) {
				values = new double[0]; 
			}
			
			KernelEstimator est;
			try {
				est = new NucleusDatasetCreator(options).createProbabililtyKernel(values, binWidth);
			} catch (Exception e1) {
				throw new ChartDatasetCreationException("Cannot make probability kernel", e1);
			}

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
