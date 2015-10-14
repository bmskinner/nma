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

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

import utility.Utils;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions.NuclearSignalOptions;
import analysis.nucleus.CurveRefolder;

import components.CellCollection;
import components.generic.XYPoint;
import components.nuclear.NuclearSignal;
import components.nuclear.ShellResult;
import components.nuclei.Nucleus;

public class NuclearSignalDatasetCreator {

	/**
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers analysis parameters for the signals
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 */
	public static TableModel createSignalDetectionParametersTable(List<AnalysisDataset> list){

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
				
		// find the collection with the most channels
		// this defines  the number of rows

		if(list==null){
			model.addColumn("No data loaded");
			
		} else {
			
			int maxChannels = 0;
			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();
				maxChannels = Math.max(collection.getHighestSignalGroup(), maxChannels);
			}
			if(maxChannels>0){
			
				// create the row names
				fieldNames.add("Number of signal groups");
				
				for(int i=0;i<maxChannels;i++){
					fieldNames.add("");
					fieldNames.add("Signal group");
					fieldNames.add("Group name");
					fieldNames.add("Channel");
					fieldNames.add("Source");
					fieldNames.add("Threshold");
					fieldNames.add("Min size");
					fieldNames.add("Max fraction");
					fieldNames.add("Min circ");
					fieldNames.add("Max circ");
					fieldNames.add("Signal detection");
				}
				
				int numberOfRowsPerSignalGroup = fieldNames.size()/ (maxChannels+1);
				model.addColumn("", fieldNames.toArray(new Object[0])); // separate row block for each channel
				
					
				// format the numbers and make into a tablemodel
				DecimalFormat df = new DecimalFormat("#0.00"); 
	
				// make a new column for each collection
				for(AnalysisDataset dataset : list){
					CellCollection collection = dataset.getCollection();
					
					List<Object> rowData = new ArrayList<Object>(0);
					rowData.add(collection.getSignalGroups().size());
	
					for(int signalGroup : collection.getSignalGroups()){
						
						NuclearSignalOptions ns = dataset.getAnalysisOptions()
														.getNuclearSignalOptions(collection.getSignalGroupName(signalGroup));
						

						if(ns==null){ // occurs when no signals are present?
//							ns = dataset.getAnalysisOptions()
//									.getNuclearSignalOptions("default");
							for(int i=0; i<numberOfRowsPerSignalGroup;i++){
								rowData.add("");
							}
							
							
						} else {
							Object signalThreshold = ns.getMode()==NuclearSignalOptions.FORWARD
									? ns.getSignalThreshold()
									: "Variable";

							Object signalMode = ns.getMode()==NuclearSignalOptions.FORWARD
											? "Forward"
											: ns.getMode()==NuclearSignalOptions.REVERSE
													? "Reverse"
													: "Adaptive";					


							rowData.add("");
							rowData.add(signalGroup);
							rowData.add(collection.getSignalGroupName(signalGroup));
							rowData.add(collection.getSignalChannel(signalGroup));
							rowData.add(collection.getSignalSourceFolder(signalGroup));
							rowData.add(  signalThreshold );
							rowData.add(ns.getMinSize());
							rowData.add(df.format(ns.getMaxFraction()));
							rowData.add(df.format(ns.getMinCirc()));
							rowData.add(df.format(ns.getMaxCirc()));
							rowData.add(signalMode);
						}

					}
					model.addColumn(collection.getName(), rowData.toArray(new Object[0])); // separate row block for each channel
				}
			} else {
				model.addColumn("No data loaded");
			}
		}
//		IJ.log("Created model");
		return model;	
	}
	
	
	/**
	 * Create a histogram dataset covering the signal angles for the given analysis datasets
	 * @param list the list of datasets
	 * @return a histogram of angles
	 */
	public static HistogramDataset createSignalAngleHistogramDataset(List<AnalysisDataset> list){
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();
			
			for(int signalGroup : collection.getSignalGroups()){
				
				if(dataset.isSignalGroupVisible(signalGroup)){

					if(collection.hasSignals(signalGroup)){

						List<Double> angles = new ArrayList<Double>(0);

						for(Nucleus n : collection.getNuclei()){
							angles.addAll(n.getSignalCollection().getAngles(signalGroup));
						}
						double[] values = Utils.getdoubleFromDouble(angles.toArray(new Double[0]));
						ds.addSeries("Group_"+signalGroup+"_"+collection.getName(), values, 12);
					}
				}
			}
			
		}
		return ds;
	}
	
	/**
	 * Create a histogram of signal distances from the nuclear centre of mass for
	 * the given list of datasets
	 * @param list the list of datasets
	 * @return a histogram of distances
	 */
	public static HistogramDataset createSignalDistanceHistogramDataset(List<AnalysisDataset> list){
		HistogramDataset ds = new HistogramDataset();
		for(AnalysisDataset dataset : list){
			CellCollection collection = dataset.getCollection();

			for(int signalGroup : collection.getSignalGroups()){
				
				if(dataset.isSignalGroupVisible(signalGroup)){

					if(collection.hasSignals(signalGroup)){

						List<Double> angles = new ArrayList<Double>(0);

						for(Nucleus n : collection.getNuclei()){
							angles.addAll(n.getSignalCollection().getDistances(signalGroup));
						}
						double[] values = Utils.getdoubleFromDouble(angles.toArray(new Double[0]));
						ds.addSeries("Group_"+signalGroup+"_"+collection.getName(), values, 12);
					}
				}
			}

		}
		return ds;
	}
	
	/**
	 * Get the XY coordinates of a given signal centre of mass on a nuclear outline
	 * @param n the signal to plos
	 * @param outline the outline to draw the signal on
	 * @return the point of the signal centre of mass
	 */
	public static XYPoint getXYCoordinatesForSignal(NuclearSignal n, Nucleus outline){
		double angle = n.getAngle();

		double fractionalDistance = n.getFractionalDistanceFromCoM();

		// determine the total distance to the border at this angle
		double distanceToBorder = CurveRefolder.getDistanceFromAngle(angle, outline);

		// convert to fractional distance to signal
		double signalDistance = distanceToBorder * fractionalDistance;

		// adjust X and Y because we are now counting angles from the vertical axis
		double signalX = Utils.getXComponentOfAngle(signalDistance, angle-90);
		double signalY = Utils.getYComponentOfAngle(signalDistance, angle-90);
		return new XYPoint(signalX, signalY);
	}
	
	/**
	 * Create a chart dataset for the centres of mass of signals in the dataset
	 * @param dataset the dataset
	 * @return
	 */
	public static XYDataset createSignalCoMDataset(AnalysisDataset dataset){
		DefaultXYDataset ds = new DefaultXYDataset();
		CellCollection collection = dataset.getCollection();

		if(collection.hasSignals()){

			for(int group : collection.getSignalGroups()){

				if(dataset.isSignalGroupVisible(group)){

					double[] xpoints = new double[collection.getSignals(group).size()];
					double[] ypoints = new double[collection.getSignals(group).size()];

					int signalCount = 0;
					for(NuclearSignal n : collection.getSignals(group)){

						XYPoint p = getXYCoordinatesForSignal(n, collection.getConsensusNucleus());

						xpoints[signalCount] = p.getX();
						ypoints[signalCount] = p.getY();
						signalCount++;

					}
					double[][] data = { xpoints, ypoints };
					ds.addSeries("Group_"+group, data);
				}
			}
		}
		return ds;
	}
	
	public static List<Shape> createSignalRadiusDataset(AnalysisDataset dataset, int signalGroup){

		CellCollection collection = dataset.getCollection();
		List<Shape> result = new ArrayList<Shape>(0);
		
		if(dataset.isSignalGroupVisible(signalGroup)){
			if(collection.hasSignals(signalGroup)){

				for(NuclearSignal n : collection.getSignals(signalGroup)){
					XYPoint p = getXYCoordinatesForSignal(n, collection.getConsensusNucleus());

					// ellipses are drawn starting from x y at upper left. Provide an offset from the centre
					double offset = n.getRadius(); 

					result.add(new Ellipse2D.Double(p.getX()-offset, p.getY()-offset, n.getRadius()*2, n.getRadius()*2));
				}

			}
		}
		return result;
	}
	
	/**
	 * Create a table of signal stats for the given list of datasets. This table
	 * covers size, number of signals
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 */
	public static TableModel createSignalStatsTable(List<AnalysisDataset> list){

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> fieldNames = new ArrayList<Object>(0);
				
		// find the collection with the most channels
		// this defines  the number of rows

		if(list==null){
			model.addColumn("No data loaded");
			
		} else {
			
			int maxChannels = 0;
			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();
				maxChannels = Math.max(collection.getHighestSignalGroup(), maxChannels);
			}
			if(maxChannels>0){
				// create the row names
				fieldNames.add("Number of signal groups");
				
				for(int i=0;i<maxChannels;i++){
					fieldNames.add("");
					fieldNames.add("Signal group");
					fieldNames.add("Group name");
					fieldNames.add("Channel");
					fieldNames.add("Source");
					fieldNames.add("Signals");
					fieldNames.add("Signals per nucleus");
					fieldNames.add("Median area");
					fieldNames.add("Median angle");
					fieldNames.add("Median feret");
					fieldNames.add("Median distance from CoM");
				}
				
				int numberOfRowsPerSignalGroup = fieldNames.size()/(maxChannels+1);
				model.addColumn("", fieldNames.toArray(new Object[0])); // separate row block for each channel
				
	//			IJ.log("Added headers");
					
				// format the numbers and make into a tablemodel
				DecimalFormat df = new DecimalFormat("#0.00"); 
	
				// make a new column for each collection
				for(AnalysisDataset dataset : list){
					CellCollection collection = dataset.getCollection();
					
	//				IJ.log("Adding collection");
					List<Object> rowData = new ArrayList<Object>(0);
					rowData.add(collection.getSignalGroups().size());
	
					for(int signalGroup : collection.getSignalGroups()){
						if(collection.hasSignals(signalGroup)){
							rowData.add("");
							rowData.add(signalGroup);
							rowData.add(collection.getSignalGroupName(signalGroup));
							rowData.add(collection.getSignalChannel(signalGroup));
							rowData.add(collection.getSignalSourceFolder(signalGroup));
							rowData.add(collection.getSignalCount(signalGroup));
							double signalPerNucleus = (double) collection.getSignalCount(signalGroup)/  (double) collection.getCellsWithNuclearSignals(signalGroup, true).size();
							rowData.add(df.format(signalPerNucleus));
							rowData.add(df.format(collection.getMedianSignalArea(signalGroup)));
							rowData.add(df.format(collection.getMedianSignalAngle(signalGroup)));
							rowData.add(df.format(collection.getMedianSignalFeret(signalGroup)));
							rowData.add(df.format(collection.getMedianSignalDistance(signalGroup)));
						} else {
							
							for(int i = 0; i<numberOfRowsPerSignalGroup;i++){
								rowData.add("");
							}
						}
					}
					model.addColumn(collection.getName(), rowData.toArray(new Object[0])); // separate row block for each channel
				}
			} else {
				model.addColumn("No data loaded");
			}
		}
//		IJ.log("Created model");
		return model;	
	}
	
	/**
	 * Create a boxplot dataset for signal areas
	 * @param dataset the AnalysisDataset to get signal info from
	 * @return a boxplot dataset
	 */
	public static BoxAndWhiskerCategoryDataset createSignalAreaBoxplotDataset(AnalysisDataset dataset) {

		DefaultBoxAndWhiskerCategoryDataset result = new DefaultBoxAndWhiskerCategoryDataset();

		CellCollection c = dataset.getCollection();
		
		for(int signalGroup : c.getSignalGroups()){
			
			List<Double> list = new ArrayList<Double>();
			for(NuclearSignal s : c.getSignals(signalGroup)){
				
				list.add(s.getArea());
			}
			result.add(list, "Group_"+signalGroup, "Area");
		}
		return result;
	}
	
	public static CategoryDataset createShellBarChartDataset(List<AnalysisDataset> list){
		DefaultStatisticalCategoryDataset ds = new DefaultStatisticalCategoryDataset();
		for(AnalysisDataset dataset : list){
			
			CellCollection collection = dataset.getCollection();

			for(int signalGroup : collection.getSignalGroups()){
				
				if(collection.hasSignals(signalGroup)){
					ShellResult r = dataset.getShellResult(signalGroup);

					for(int shell = 0; shell<r.getNumberOfShells();shell++){
						Double d = r.getMeans().get(shell);
						Double std = r.getStandardErrors().get(shell);
						ds.add(d*100, std.doubleValue()*100, "Group_"+signalGroup+"_"+collection.getName(), String.valueOf(shell)); 
						// we need the string value for shell otherwise we get error
						// "the method addValue(Number, Comparable, Comparable) is ambiguous for the type DefaultCategoryDataset"
						// ditto the doublevalue for std

					}
				}
			}
		}
		return ds;
	}
}
