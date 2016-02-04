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
import java.util.List;
import java.util.logging.Level;

import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;

import charting.options.ChartOptions;
import utility.Constants;
import utility.Utils;
import weka.estimators.KernelEstimator;
import analysis.AnalysisDataset;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import stats.NucleusStatistic;
import stats.Stats;

public class NuclearHistogramDatasetCreator {
	
	public static final int MIN_ROUNDED = 0;
	public static final int MAX_ROUNDED = 1;
	public static final int STEP_SIZE   = 2;
	
	public static HistogramDataset createNuclearStatsHistogramDataset(ChartOptions options) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		
			options.log(Level.FINEST, "Creating histogram dataset: "+options.getStat());

		
		if(options.hasDatasets()){

			for(AnalysisDataset dataset : options.getDatasets()){

				
					options.log(Level.FINEST, "  Dataset: "+dataset.getName());
				

				CellCollection collection = dataset.getCollection();

				
					options.log(Level.FINEST, "  Stat: "+options.getStat().toString()+"; Scale: "+options.getScale().toString());
				
				
				NucleusStatistic stat = (NucleusStatistic) options.getStat();
				double[] values = findDatasetValues(dataset, stat, options.getScale()); 

				String groupLabel = options.getStat().toString();

				double min = Stats.min(values);
				double max = Stats.max(values);

				options.log(Level.FINEST, "  Min: "+min+"; max: "+max);

				int log = (int) Math.floor(  Math.log10(min)  ); // get the log scale

				int roundLog = log-1 == 0 ? log-2 : log-1;
				double roundAbs = Math.pow(10, roundLog);

				// use int truncation to round to nearest 100 above max
				int maxRounded = (int) ((( (int)max + (roundAbs) ) / roundAbs ) * roundAbs);
				maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct offsets for measures between 0-1
				int minRounded = (int) (((( (int)min + (roundAbs) ) / roundAbs ) * roundAbs  ) - roundAbs);
				minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1;  // correct offsets for measures between 0-1
				minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures start from at least zero


				options.log(Level.FINEST, "  Rounded min: "+minRounded+"; max: "+maxRounded);


				int bins = 100;

				ds.addSeries(groupLabel+"_"+collection.getName(), values, bins, minRounded, maxRounded );
			}
		}
		options.log(Level.FINEST, "Completed histogram dataset");
		return ds;
	}
	
	/**
	 * Given a dataset and a stats parameter, get the values for that stat
	 * @param dataset the Analysis Dataset
	 * @param stat the statistic to fetch
	 * @param scale the scale to display at
	 * @return the array of values
	 * @throws Exception
	 */
	public static double[] findDatasetValues(AnalysisDataset dataset, NucleusStatistic stat, MeasurementScale scale) throws Exception {
		
		CellCollection collection = dataset.getCollection();			
		double[] values = collection.getNuclearStatistics(stat, scale); 			
		return values;
	}
	
	
	private static double[] findMinAndMaxForHistogram(double[] values){
		double min = Stats.min(values);
		double max = Stats.max(values);

		int log = (int) Math.floor(  Math.log10(min)  ); // get the log scale
		
		int roundLog = log-1 == 0 ? log-2 : log-1;
		double roundAbs = Math.pow(10, roundLog);
		
		int binLog = log-2;
		double stepSize = Math.pow(10, binLog);
		
//		IJ.log("   roundLog: "+roundLog);
//		IJ.log("   round to nearest: "+roundAbs);
		
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
	
	/**
	 * Make an XY dataset corresponding to the probability density of a given nuclear statistic
	 * @param list the datasets to draw
	 * @param stat the statistic to measure
	 * @return a charting dataset
	 * @throws Exception
	 */
	public static DefaultXYDataset createNuclearDensityHistogramDataset(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale) throws Exception {
		DefaultXYDataset ds = new DefaultXYDataset();
		
		int[] minMaxRange = calculateMinAndMaxRange(list, stat, scale);

		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			
			String groupLabel = stat.toString();
			double[] values = findDatasetValues(dataset, stat, scale); 
			KernelEstimator est = NucleusDatasetCreator.createProbabililtyKernel(values, 0.001);
	
			
			double[] minMax = findMinAndMaxForHistogram(values);
//			double min = Stats.min(values);
//			double max = Stats.max(values);
//
//			int log = (int) Math.floor(  Math.log10(min)  ); // get the log scale
//			
//			int roundLog = log-1 == 0 ? log-2 : log-1;
//			double roundAbs = Math.pow(10, roundLog);
//			
//			int binLog = log-2;
//			double stepSize = Math.pow(10, binLog);
//			
////			IJ.log("   roundLog: "+roundLog);
////			IJ.log("   round to nearest: "+roundAbs);
//			
//			// use int truncation to round to nearest 100 above max
//			int maxRounded = (int) ((( (int)max + (roundAbs) ) / roundAbs ) * roundAbs);
//			maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct offsets for measures between 0-1
//			int minRounded = (int) (((( (int)min + (roundAbs) ) / roundAbs ) * roundAbs  ) - roundAbs);
//			minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1;  // correct offsets for measures between 0-1
//			minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures start from at least zero
//	
			
			List<Double> xValues = new ArrayList<Double>();
			List<Double> yValues = new ArrayList<Double>();
			
			for(double i=minMaxRange[0]; i<=minMaxRange[1]; i+=minMax[STEP_SIZE]){
				xValues.add(i);
				yValues.add(est.getProbability(i));
			}
	
			double[][] data = { Utils.getdoubleFromDouble(xValues.toArray(new Double[0])),  
					Utils.getdoubleFromDouble(yValues.toArray(new Double[0])) };
			
			
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
	private static int[] calculateMinAndMaxRange(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale) throws Exception {
		
		int[] result = new int[2];
		result[0] = Integer.MAX_VALUE; // holds min
		result[1] = 0; // holds max

		for(AnalysisDataset dataset : list){
			
			double[] values = findDatasetValues(dataset, stat, scale); 
			
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
		
		double min = Stats.min(values);
		double max = Stats.max(values);
					
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
	
	public static HistogramDataset createSegmentLengthHistogramDataset(ChartOptions options) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		

		options.log(Level.FINEST, "Creating histogram dataset: "+options.getStat());

		
		if( ! options.hasDatasets() ){
			return ds;
		}
		
		for(AnalysisDataset dataset : options.getDatasets()){

			options.log(Level.FINEST, "  Dataset: "+dataset.getName());

			CellCollection collection = dataset.getCollection();
			
			/*
			 * Find the seg id for the median segment at the requested position
			 */
			NucleusBorderSegment medianSeg = collection
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getSegmentAt(options.getSegPosition());

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			int count=0;
			double[] lengths = new double[collection.cellCount()];
			for(Nucleus n : collection.getNuclei()){

				NucleusBorderSegment seg = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT)
						.getSegment(medianSeg.getID());


				double length = 0;
				if(seg!=null){
					int indexLength = seg.length();
					double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
					length = n.getStatistic(NucleusStatistic.PERIMETER, options.getScale()) * proportionPerimeter;

				}
				
				lengths[count++] = length;
			}

			double min = Stats.min(lengths);
			double max = Stats.max(lengths);


			options.log(Level.FINEST, "  Min: "+min+"; max: "+max);


			int log = (int) Math.floor(  Math.log10(min)  ); // get the log scale

			int roundLog = log-1 == 0 ? log-2 : log-1;
			double roundAbs = Math.pow(10, roundLog);

			// use int truncation to round to nearest 100 above max
			int maxRounded = (int) ((( (int)max + (roundAbs) ) / roundAbs ) * roundAbs);
			maxRounded = roundAbs > 1 ? maxRounded + (int) roundAbs : maxRounded + 1; // correct offsets for measures between 0-1
			int minRounded = (int) (((( (int)min + (roundAbs) ) / roundAbs ) * roundAbs  ) - roundAbs);
			minRounded = roundAbs > 1 ? minRounded - (int) roundAbs : minRounded - 1;  // correct offsets for measures between 0-1
			minRounded = minRounded < 0 ? 0 : minRounded; // ensure all measures start from at least zero

				options.log(Level.FINEST, "  Rounded min: "+minRounded+"; max: "+maxRounded);


			int bins = 100;

			ds.addSeries(Constants.SEGMENT_PREFIX+options.getSegPosition()+"_"+collection.getName(), lengths, bins, minRounded, maxRounded );
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
	public static DefaultXYDataset createSegmentLengthDensityDataset(ChartOptions options) throws Exception {

		int[] minMaxRange = {Integer.MAX_VALUE, 0}; // start with extremes, trim to fit data
		for(AnalysisDataset dataset : options.getDatasets()){
			CellCollection collection = dataset.getCollection();
			
			/*
			 * Find the seg id for the median segment at the requested position
			 */
			NucleusBorderSegment medianSeg = collection
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getSegmentAt(options.getSegPosition());

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			int count=0;
			double[] lengths = new double[collection.cellCount()];
			for(Nucleus n : collection.getNuclei()){

				NucleusBorderSegment seg = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT)
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
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getSegmentAt(options.getSegPosition());

			
			/*
			 * Use the segment id for this collection to fetch the individual nucleus segments
			 */
			int count=0;
			double[] lengths = new double[collection.cellCount()];
			for(Nucleus n : collection.getNuclei()){

				NucleusBorderSegment seg = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT)
						.getSegment(medianSeg.getID());
				
				int indexLength = seg.length();
				double proportionPerimeter = (double) indexLength / (double) seg.getTotalLength();
				double length = n.getStatistic(NucleusStatistic.PERIMETER, options.getScale()) * proportionPerimeter;
				lengths[count++] = length;
			}
			
			KernelEstimator est = NucleusDatasetCreator.createProbabililtyKernel(  lengths , 0.001 );
	
			double min = Stats.min(lengths);
			double max = Stats.max(lengths);

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
	
			double[][] data = { Utils.getdoubleFromDouble(xValues.toArray(new Double[0])),  
					Utils.getdoubleFromDouble(yValues.toArray(new Double[0])) };
			
			
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
	public static HistogramDataset createHistogramDatasetFromList(List<Double> list) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		if(!list.isEmpty()){
		
			double[] values = Utils.getdoubleFromDouble(list.toArray(new Double[0]));

			int bins = 100;

			ds.addSeries("Sample", values, bins, 0.95, 1.00 );
		}
		return ds;
	}
	
	public static DefaultXYDataset createDensityDatasetFromList(List<Double> list, double binWidth) throws Exception{
		DefaultXYDataset ds = new DefaultXYDataset();
		if(!list.isEmpty()){
			double[] values = Utils.getdoubleFromDouble(list.toArray(new Double[0]));
			KernelEstimator est = NucleusDatasetCreator.createProbabililtyKernel(values, binWidth);

			List<Double> xValues = new ArrayList<Double>();
			List<Double> yValues = new ArrayList<Double>();

			for(double i=0.95; i<=1.00; i+=0.0001){
				xValues.add(i);
				yValues.add(est.getProbability(i));
			}

			double[][] data = { Utils.getdoubleFromDouble(xValues.toArray(new Double[0])),  
					Utils.getdoubleFromDouble(yValues.toArray(new Double[0])) };


			ds.addSeries("Density", data);
		}
		return ds;
		
	}
	
}
