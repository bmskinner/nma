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
		
	
//	public static final int NUCLEAR_AREA = 0;
//	public static final int NUCLEAR_PERIM = 1;
//	public static final int NUCLEAR_FERET = 2;
//	public static final int NUCLEAR_MIN_DIAM = 3;
//	public static final int NUCLEAR_CIRCULARITY = 4;
//	public static final int NUCLEAR_ASPECT = 5;
//	public static final int NUCLEAR_VARIABILITY = 6;
	
//	/**
//	 * For the given list of datasets, get the nuclear areas as a histogram dataset
//	 * @param list
//	 * @return
//	 */
//	public static HistogramDataset createNuclearAreaHistogramDataset(List<AnalysisDataset> list){
//		HistogramDataset ds = new HistogramDataset();
//		for(AnalysisDataset dataset : list){
//			CellCollection collection = dataset.getCollection();
//			double[] values = collection.getAreas();
//
//			double min  = Stats.min(values);
//			double max = Stats.max(values);
//			
//			// use int truncation to round to nearest 100 above max
//			int maxRounded = (( (int)max + 99) / 100 ) * 100;
//			
//			// use int truncation to round to nearest 100 above min, then subtract 100
//			int minRounded = ((( (int)min + 99) / 100 ) * 100  ) - 100;
//			
//			// bind of width 50
//			int bins = ((maxRounded - minRounded) / 50);
//
//			ds.addSeries("Area_"+collection.getName(), values, bins, minRounded, maxRounded);
//		}
//		return ds;
//	}
//	
//	/**
//	 * For the given list of datasets, get the nuclear perimeters as a histogram dataset
//	 * @param list
//	 * @return
//	 */
//	public static HistogramDataset createNuclearPerimeterHistogramDataset(List<AnalysisDataset> list){
//		HistogramDataset ds = new HistogramDataset();
//		for(AnalysisDataset dataset : list){
//			CellCollection collection = dataset.getCollection();
//			double[] values = collection.getPerimeters(); 
//			
//			double min  = Stats.min(values);
//			double max = Stats.max(values);
//			
//			int maxRounded = (int) Math.ceil(max);
//			int minRounded = (int) Math.floor(min);
//			
//			// put bins of width 1
//			int bins = ((maxRounded - minRounded));
//			
//			ds.addSeries("Perimeter_"+collection.getName(), values, bins, minRounded, maxRounded);
//		}
//		return ds;
//	}
//	
//	/**
//	 * For the given list of datasets, get the nuclear ferets as a histogram dataset
//	 * @param list
//	 * @return
//	 */
//	public static HistogramDataset createNuclearMaxFeretHistogramDataset(List<AnalysisDataset> list){
//		HistogramDataset ds = new HistogramDataset();
//		for(AnalysisDataset dataset : list){
//			CellCollection collection = dataset.getCollection();
//			double[] values = collection.getFerets(); 
//			
//			double min  = Stats.min(values);
//			double max = Stats.max(values);
//			
//			int maxRounded = (int) Math.ceil(max);
//			int minRounded = (int) Math.floor(min);
//			
//			// put bins of width 0.5
//			int bins = ((maxRounded - minRounded) * 2 );
//			
//			ds.addSeries("Max feret_"+collection.getName(), values, bins, minRounded, maxRounded);
//		}
//		return ds;
//	}
//	
//	/**
//	 * For the given list of datasets, get the nuclear minimum diametes
//	 * across the centre of mass as a histogram dataset
//	 * @param list
//	 * @return
//	 */
//	public static HistogramDataset createNuclearMinDiameterHistogramDataset(List<AnalysisDataset> list){
//		HistogramDataset ds = new HistogramDataset();
//		for(AnalysisDataset dataset : list){
//			CellCollection collection = dataset.getCollection();
//			double[] values = collection.getMinFerets(); 
//			double min  = Stats.min(values);
//			double max = Stats.max(values);
//			
//			int maxRounded = (int) Math.ceil(max);
//			int minRounded = (int) Math.floor(min);
//			
//			// put bins of width 0.5
//			int bins = ((maxRounded - minRounded) * 2 );
//			ds.addSeries("Min diameter_"+collection.getName(), values, bins, minRounded, maxRounded);
//		}
//		return ds;
//	}
//	
//	/**
//	 * For the given list of datasets, get the nuclear normalised variability as a histogram dataset
//	 * @param list
//	 * @return
//	 * @throws Exception  
//	 */
//	public static HistogramDataset createNuclearVariabilityHistogramDataset(List<AnalysisDataset> list) throws Exception {
//		HistogramDataset ds = new HistogramDataset();
//		for(AnalysisDataset dataset : list){
//			CellCollection collection = dataset.getCollection();
//			double[] values = collection.getNormalisedDifferencesToMedianFromPoint(collection.getReferencePoint()); 
//			double min  = Stats.min(values);
//			double max = Stats.max(values);
//			
//			int maxRounded = (int) Math.ceil(max);
//			int minRounded = (int) Math.floor(min);
//			
//			// put bins of width 0.1
//			int bins = ((maxRounded - minRounded) * 10 );
//			ds.addSeries("Variability_"+collection.getName(), values, bins, minRounded, maxRounded);
//		}
//		return ds;
//	}
//	
//	/**
//	 * For the given list of datasets, get the nuclear circularity as a histogram dataset
//	 * @param list
//	 * @return
//	 * @throws Exception  
//	 */
//	public static HistogramDataset createNuclearCircularityHistogramDataset(List<AnalysisDataset> list) throws Exception {
//		HistogramDataset ds = new HistogramDataset();
//		for(AnalysisDataset dataset : list){
//			CellCollection collection = dataset.getCollection();
//			double[] values = collection.getCircularities(); 
////			double min  = Stats.min(values);
////			double max = Stats.max(values);
//			
//			int maxRounded = 1;
//			int minRounded = 0;
//			
//			// put bins of width 0.05
//			int bins = ((maxRounded - minRounded) * 20 );
//			ds.addSeries("Circularity_"+collection.getName(), values, bins, minRounded, maxRounded );
//		}
//		return ds;
//	}
//	
//	/**
//	 * For the given list of datasets, get the nuclear circularity as a histogram dataset
//	 * @param list
//	 * @return
//	 * @throws Exception  
//	 */
//	public static HistogramDataset createNuclearAspectRatioHistogramDataset(List<AnalysisDataset> list) throws Exception {
//		HistogramDataset ds = new HistogramDataset();
//		for(AnalysisDataset dataset : list){
//			CellCollection collection = dataset.getCollection();
//			double[] values = collection.getAspectRatios(); 
//			double min  = Stats.min(values);
//			double max = Stats.max(values);
//			
//			int maxRounded = (int) Math.ceil(max);
//			int minRounded = (int) Math.floor(min);
//			
//			// put bins of width 0.05
//			int bins = ((maxRounded - minRounded) * 20 );
//			ds.addSeries("Aspect_"+collection.getName(), values, bins, minRounded, maxRounded );
//		}
//		return ds;
//	}
	
	/**
	 * For the given list of datasets, get the nuclear circularity as a histogram dataset
	 * @param list
	 * @return
	 * @throws Exception  
	 */
	public static HistogramDataset createNuclearStatsHistogramDataset(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			
			double[] values = null; 
			
			int maxRounded = 0;
			int minRounded = 0;
			int bins = 0;
			double min = 0;
			double max = 0;
			
			String groupLabel = null;
			
			switch(stat){
			
				case AREA:
					values = collection.getAreas(scale);
					min = Stats.min(values);
					max = Stats.max(values);
					// use int truncation to round to nearest 100 above max
					maxRounded = (( (int)max + 99) / 100 ) * 100;
					
					// use int truncation to round to nearest 100 above min, then subtract 100
					minRounded = ((( (int)min + 99) / 100 ) * 100  ) - 100;
					
					// bind of width 50
					bins = ((maxRounded - minRounded) / 50);
					groupLabel = "Area";
					break;
					
				case PERIMETER: 
					values = collection.getPerimeters(scale); 
					min = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					
					// put bins of width 0.5
					bins = ((maxRounded - minRounded) * 2 );
					groupLabel = "Perimeter";
					break;
					
				case MAX_FERET:
					values = collection.getFerets(scale); 
					
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					
					// put bins of width 0.5
					bins = ((maxRounded - minRounded) * 2 );
					groupLabel = "Max feret";
					break;
					
				case MIN_DIAMETER: 
					values = collection.getMinFerets(scale); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					
					// put bins of width 0.5
					bins = ((maxRounded - minRounded) * 2 );
					groupLabel = "Min diameter";
					break;
					
				case VARIABILITY:
					values = collection.getNormalisedDifferencesToMedianFromPoint(collection.getReferencePoint()); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					
					// put bins of width 0.1
					bins = ((maxRounded - minRounded) * 10 );
					groupLabel = "Variability";
					break;
					
				case CIRCULARITY:
					values = collection.getCircularities(); 
					
					maxRounded = 1;
					minRounded = 0;
					
					// put bins of width 0.05
					bins = ((maxRounded - minRounded) * 20 );
					groupLabel = "Circularity";
					break;
					
				case ASPECT:
					values = collection.getAspectRatios(); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					
					// put bins of width 0.05
					bins = ((maxRounded - minRounded) * 20 );
					groupLabel = "Aspect";
					break;
			}

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
			
		double[] values = null; 			
		switch(stat){
		
			case AREA:
				values = collection.getAreas(scale);
				break;
				
			case PERIMETER: 
				values = collection.getPerimeters(scale); 
				break;
				
			case MAX_FERET:
				values = collection.getFerets(scale); 
				break;
				
			case MIN_DIAMETER: 
				values = collection.getMinFerets(scale); 
				break;
				
			case VARIABILITY:
				values = collection.getNormalisedDifferencesToMedianFromPoint(collection.getReferencePoint()); 
				break;
				
			case CIRCULARITY:
				values = collection.getCircularities(); 
				break;
				
			case ASPECT:
				values = collection.getAspectRatios(); 
				break;
		}
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
			
			KernelEstimator est = new KernelEstimator(0.001);
			String groupLabel = null;
			double[] values = null; 
			int maxRounded = 0;
			int minRounded = 0;
			double min = 0;
			double max = 0;
			double stepSize = 0.1;
			
			switch(stat){
			
				case AREA:
					values = collection.getAreas(scale);
					min = Stats.min(values);
					max = Stats.max(values);
					// use int truncation to round to nearest 100 above max
					maxRounded = (( (int)max + 99) / 100 ) * 100;
					
					// use int truncation to round to nearest 100 above min, then subtract 100
					minRounded = ((( (int)min + 99) / 100 ) * 100  ) - 100;
					groupLabel = "Area";
					stepSize = 1;
					break;
					
				case PERIMETER: 
					values = collection.getPerimeters(scale); 
					min = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					groupLabel = "Perimeter";
					stepSize = 1;
					break;
					
				case MAX_FERET:
					values = collection.getFerets(scale); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					groupLabel = "Max feret";
					stepSize = 0.1;
					break;
					
				case MIN_DIAMETER: 
					values = collection.getMinFerets(scale); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					groupLabel = "Min diameter";
					stepSize = 0.1;
					break;
					
				case VARIABILITY:
					values = collection.getNormalisedDifferencesToMedianFromPoint(collection.getReferencePoint()); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					groupLabel = "Variability";
					stepSize = 0.01;
					break;
					
				case CIRCULARITY:
					values = collection.getCircularities(); 
					maxRounded = 1;
					minRounded = 0;
					groupLabel = "Circularity";
					stepSize = 0.005;
					break;
					
				case ASPECT:
					values = collection.getAspectRatios(); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					groupLabel = "Aspect";
					stepSize = 0.01;
					break;
			}
	
			// add the values to a kernel estimator
			// give each value equal weighting
			for(double d : values){
				est.addValue(d, 1);
			}
			
			List<Double> xValues = new ArrayList<Double>();
			List<Double> yValues = new ArrayList<Double>();
			
			for(double i=minMaxRange[0]; i<=minMaxRange[1]; i+=stepSize){
				xValues.add(i);
				yValues.add(est.getProbability(i));
//				IJ.log(groupLabel+": "+i+" ");
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
			CellCollection collection = dataset.getCollection();

			double[] values = null; 
			int maxRounded = 0;
			int minRounded = 0;
			double min = 0;
			double max = 0;
			
			switch(stat){
			
				case AREA:
					values = collection.getAreas(scale);
					min = Stats.min(values);
					max = Stats.max(values);
					// use int truncation to round to nearest 100 above max
					maxRounded = (( (int)max + 99) / 100 ) * 100;
					
					// use int truncation to round to nearest 100 above min, then subtract 100
					minRounded = ((( (int)min + 99) / 100 ) * 100  ) - 100;
					break;
					
				case PERIMETER: 
					values = collection.getPerimeters(scale); 
					min = Stats.min(values);
					max = Stats.max(values);
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					break;
					
				case MAX_FERET:
					values = collection.getFerets(scale); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					break;
					
				case MIN_DIAMETER: 
					values = collection.getMinFerets(scale); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					break;
					
				case VARIABILITY:
					values = collection.getNormalisedDifferencesToMedianFromPoint(collection.getReferencePoint()); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					break;
					
				case CIRCULARITY:
					values = collection.getCircularities(); 
					maxRounded = 1;
					minRounded = 0;
					break;
					
				case ASPECT:
					values = collection.getAspectRatios(); 
					min  = Stats.min(values);
					max = Stats.max(values);
					
					maxRounded = (int) Math.ceil(max);
					minRounded = (int) Math.floor(min);
					break;
			}
			
			result[0] = result[0] < minRounded ? result[0] : minRounded;
			result[1] = result[1] > maxRounded ? result[1] : maxRounded;
		}
		
		return result;
	}
}
