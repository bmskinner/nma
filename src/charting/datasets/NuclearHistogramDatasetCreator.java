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

import gui.components.MeasurementUnitSettingsPanel.MeasurementScale;
import ij.IJ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import components.CellCollection;
import components.CellCollection.NucleusStatistic;
import components.generic.Profile;
import analysis.AnalysisDataset;
import utility.Stats;
import utility.Utils;
import weka.estimators.KernelEstimator;

public class NuclearHistogramDatasetCreator {
	
	
	public static HistogramDataset createNuclearStatsHistogramDataset(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			
			CellCollection collection = dataset.getCollection();
			
			double[] values = findDatasetValues(dataset, stat, scale); 
						
			String groupLabel = stat.toString();
						
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
			
			int bins = 100;

			ds.addSeries(groupLabel+"_"+collection.getName(), values, bins, minRounded, maxRounded );
		}
		return ds;
	}
	
	/**
	 * Given a dataset and a stats parameter, get the values for that stat
	 * @param dataset the Analysis Dataset
	 * @param stat the statistic to fetch (use NuclearHistogramDatasetCreator.NUCLEAR_x constants)
	 * @return the array of values
	 * @throws Exception
	 */
	public static double[] findDatasetValues(AnalysisDataset dataset, NucleusStatistic stat, MeasurementScale scale) throws Exception {

		CellCollection collection = dataset.getCollection();			
		double[] values = collection.getNuclearStatistics(stat, scale); 			
		return values;
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
			KernelEstimator est = NucleusDatasetCreator.createProbabililtyKernel(values);
	
			double min = Stats.min(values);
			double max = Stats.max(values);

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
			
			
			ds.addSeries(groupLabel+"_"+collection.getName(), data);
		}

		return ds;
	}
		
	/**
	 * Calculate the minimum and maximum ranges in a list of datasets
	 * for the given stat type
	 * @param list the datasets
	 * @param stat the statistic to use (NuclearHistogramDatasetCreator.NUCLEAR_x constants)
	 * @return an array with the min and max of the range
	 * @throws Exception
	 */
	private static int[] calculateMinAndMaxRange(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale) throws Exception {
		
		int[] result = new int[2];
		result[0] = 1000000; // holds min
		result[1] = 0; // holds max

		for(AnalysisDataset dataset : list){
			
			double[] values = findDatasetValues(dataset, stat, scale); 
									
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
			
			result[0] = result[0] < minRounded ? result[0] : minRounded;
			result[1] = result[1] > maxRounded ? result[1] : maxRounded;
		}
		
		return result;
	}
}
