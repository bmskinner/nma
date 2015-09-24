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
package datasets;

import java.util.List;

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;

import org.jfree.data.statistics.HistogramDataset;

import utility.Stats;

public class NuclearHistogramDatasetCreator {
		
	/**
	 * For the given list of datasets, get the nuclear areas as a histogram dataset
	 * @param list
	 * @return
	 */
	public static HistogramDataset createNuclearAreaHistogramDataset(List<AnalysisDataset> list){
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			double[] values = collection.getAreas();

			double min  = Stats.min(values);
			double max = Stats.max(values);
			
			// use int truncation to round to nearest 100 above max
			int maxRounded = (( (int)max + 99) / 100 ) * 100;
			
			// use int truncation to round to nearest 100 above min, then subtract 100
			int minRounded = ((( (int)min + 99) / 100 ) * 100  ) - 100;
			
			// bind of width 50
			int bins = ((maxRounded - minRounded) / 50);

			ds.addSeries("Area_"+collection.getName(), values, bins, minRounded, maxRounded);
		}
		return ds;
	}
	
	/**
	 * For the given list of datasets, get the nuclear perimeters as a histogram dataset
	 * @param list
	 * @return
	 */
	public static HistogramDataset createNuclearPerimeterHistogramDataset(List<AnalysisDataset> list){
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			double[] values = collection.getPerimeters(); 
			
			double min  = Stats.min(values);
			double max = Stats.max(values);
			
			int maxRounded = (int) Math.ceil(max);
			int minRounded = (int) Math.floor(min);
			
			// put bins of width 1
			int bins = ((maxRounded - minRounded));
			
			ds.addSeries("Perimeter_"+collection.getName(), values, bins, minRounded, maxRounded);
		}
		return ds;
	}
	
	/**
	 * For the given list of datasets, get the nuclear ferets as a histogram dataset
	 * @param list
	 * @return
	 */
	public static HistogramDataset createNuclearMaxFeretHistogramDataset(List<AnalysisDataset> list){
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			double[] values = collection.getFerets(); 
			
			double min  = Stats.min(values);
			double max = Stats.max(values);
			
			int maxRounded = (int) Math.ceil(max);
			int minRounded = (int) Math.floor(min);
			
			// put bins of width 0.5
			int bins = ((maxRounded - minRounded) * 2 );
			
			ds.addSeries("Max feret_"+collection.getName(), values, bins, minRounded, maxRounded);
		}
		return ds;
	}
	
	/**
	 * For the given list of datasets, get the nuclear minimum diametes
	 * across the centre of mass as a histogram dataset
	 * @param list
	 * @return
	 */
	public static HistogramDataset createNuclearMinDiameterHistogramDataset(List<AnalysisDataset> list){
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			double[] values = collection.getMinFerets(); 
			double min  = Stats.min(values);
			double max = Stats.max(values);
			
			int maxRounded = (int) Math.ceil(max);
			int minRounded = (int) Math.floor(min);
			
			// put bins of width 0.5
			int bins = ((maxRounded - minRounded) * 2 );
			ds.addSeries("Min diameter_"+collection.getName(), values, bins, minRounded, maxRounded);
		}
		return ds;
	}
	
	/**
	 * For the given list of datasets, get the nuclear normalised variability as a histogram dataset
	 * @param list
	 * @return
	 * @throws Exception  
	 */
	public static HistogramDataset createNuclearVariabilityHistogramDataset(List<AnalysisDataset> list) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			double[] values = collection.getNormalisedDifferencesToMedianFromPoint(collection.getReferencePoint()); 
			double min  = Stats.min(values);
			double max = Stats.max(values);
			
			int maxRounded = (int) Math.ceil(max);
			int minRounded = (int) Math.floor(min);
			
			// put bins of width 0.1
			int bins = ((maxRounded - minRounded) * 10 );
			ds.addSeries("Variability_"+collection.getName(), values, bins, minRounded, maxRounded);
		}
		return ds;
	}
	
	/**
	 * For the given list of datasets, get the nuclear circularity as a histogram dataset
	 * @param list
	 * @return
	 * @throws Exception  
	 */
	public static HistogramDataset createNuclearCircularityHistogramDataset(List<AnalysisDataset> list) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			double[] values = collection.getCircularities(); 
//			double min  = Stats.min(values);
//			double max = Stats.max(values);
			
			int maxRounded = 1;
			int minRounded = 0;
			
			// put bins of width 0.05
			int bins = ((maxRounded - minRounded) * 20 );
			ds.addSeries("Circularity_"+collection.getName(), values, bins, minRounded, maxRounded );
		}
		return ds;
	}
	
	/**
	 * For the given list of datasets, get the nuclear circularity as a histogram dataset
	 * @param list
	 * @return
	 * @throws Exception  
	 */
	public static HistogramDataset createNuclearAspectRatioHistogramDataset(List<AnalysisDataset> list) throws Exception {
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			double[] values = collection.getAspectRatios(); 
			double min  = Stats.min(values);
			double max = Stats.max(values);
			
			int maxRounded = (int) Math.ceil(max);
			int minRounded = (int) Math.floor(min);
			
			// put bins of width 0.05
			int bins = ((maxRounded - minRounded) * 20 );
			ds.addSeries("Aspect_"+collection.getName(), values, bins, minRounded, maxRounded );
		}
		return ds;
	}
}
