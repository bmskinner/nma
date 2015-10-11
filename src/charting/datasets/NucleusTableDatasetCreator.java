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

import ij.IJ;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import components.CellCollection;
import components.ClusterGroup;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import components.nuclei.RoundNucleus;
import components.nuclei.sperm.PigSpermNucleus;
import components.nuclei.sperm.RodentSpermNucleus;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import analysis.ClusteringOptions;
import utility.Constants;

public class NucleusTableDatasetCreator {
	
	/**
	 * Create a table of segment stats for the given nucleus.
	 * @param list the AnalysisDatasets to include
	 * @return a table model
	 * @throws Exception 
	 */
	public static TableModel createSegmentStatsTable(Nucleus nucleus) throws Exception{

		DefaultTableModel model = new DefaultTableModel();

		List<Object> fieldNames = new ArrayList<Object>(0);
		
		if(nucleus==null){
			model.addColumn("No data loaded");

		} else {
			// check which reference point to use
			String referencePoint = null;
			if(nucleus.getClass()==RodentSpermNucleus.class){
				referencePoint = Constants.Nucleus.RODENT_SPERM.referencePoint();
			}

			if(nucleus.getClass()==PigSpermNucleus.class){
				referencePoint = Constants.Nucleus.PIG_SPERM.referencePoint();
			}

			if(nucleus.getClass()==RoundNucleus.class){
				referencePoint = Constants.Nucleus.ROUND.referencePoint();
			}


			// get the offset segments
			List<NucleusBorderSegment> segments = nucleus.getAngleProfile(referencePoint).getSegments();
			

			// create the row names
			fieldNames.add("Colour");
			fieldNames.add("Length");
			fieldNames.add("Start index");
			fieldNames.add("End index");

			model.addColumn("", fieldNames.toArray(new Object[0]));

			for(NucleusBorderSegment segment : segments) {

				List<Object> rowData = new ArrayList<Object>(0);
				
				rowData.add("");
				rowData.add(segment.length());
				rowData.add(segment.getStartIndex());
				rowData.add(segment.getEndIndex());

				model.addColumn(segment.getName(), rowData.toArray(new Object[0])); // separate column per segment
			}

		}

		return model;	
	}
	
	/**
	 * Create a table of segment stats for median profile of the given dataset.
	 * @param dataset the AnalysisDataset to include
	 * @return a table model
	 * @throws Exception 
	 */
	public static TableModel createMedianProfileSegmentStatsTable(AnalysisDataset dataset) throws Exception {

		DefaultTableModel model = new DefaultTableModel();

		List<Object> fieldNames = new ArrayList<Object>(0);
		
		if(dataset==null){
			model.addColumn("No data loaded");

		} else {
			CellCollection collection = dataset.getCollection();
			// check which reference point to use
			String point = collection.getOrientationPoint();


			// get the offset segments
			List<NucleusBorderSegment> segments = collection.getProfileCollection().getSegments(point);
			//.getAngleProfile(referencePoint).getSegments();

			// create the row names
			fieldNames.add("Colour");
			fieldNames.add("Length");
			fieldNames.add("Start index");
			fieldNames.add("End index");
			fieldNames.add(""); // empty for merge buttons

			model.addColumn("", fieldNames.toArray(new Object[0]));

			for(NucleusBorderSegment segment : segments) {


				List<Object> rowData = new ArrayList<Object>(0);
				
				rowData.add("");
				rowData.add(segment.length());
				rowData.add(segment.getStartIndex());
				rowData.add(segment.getEndIndex());
				rowData.add(""); // empty for merge buttons

				model.addColumn(segment.getName(), rowData.toArray(new Object[0])); // separate column per segment
			}
		}
		return model;	
	}
	
	/**
	 * Create a table model of analysis parameters from a nucleus collection.
	 * If null parameter is passed, will create an empty table
	 * @param collection
	 * @return
	 */
	public static TableModel createAnalysisParametersTable(List<AnalysisDataset> list){

		DefaultTableModel model = new DefaultTableModel();

		Object[] columnData = {
				"Profile window",
				"Nucleus detection method",
				"Nucleus threshold",
				"Canny auto threshold",
				"Canny low threshold",
				"Canny high threshold",
				"Canny kernel radius",
				"Canny kernel width",
				"Closing radius",
				"Nucleus min size",
				"Nucleus max size",
				"Nucleus min circ",
				"Nucleus max circ",
				"Consensus folded",
				"Refold mode",
				"Shell analysis run",
				"Run date",
				"Run time",
				"Collection source",
				"Type"};
		model.addColumn("", columnData);
		
		if(list==null){
			model.addColumn("No data loaded");
		} else {

			// format the numbers and make into a tablemodel
			DecimalFormat df = new DecimalFormat("#0.00"); 

			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();
				AnalysisOptions options = dataset.getAnalysisOptions();

				
				// only display if there are options available
				// This may not be the case for a merged dataset or its children
				if(options!=null){ 

					// only display refold mode if nucleus was refolded
					String refoldMode = options.refoldNucleus() 
							? options.getRefoldMode()
									: "N/A";

							String[] times = collection.getOutputFolderName().split("_");
							String date = times[0];
							String time = times[1];

							CannyOptions nucleusCannyOptions = options.getCannyOptions("nucleus");

							String detectionMethod = nucleusCannyOptions.isUseCanny() ? "Canny edge detection" : "Thresholding";
							String nucleusThreshold = nucleusCannyOptions.isUseCanny() ? "N/A" : String.valueOf(options.getNucleusThreshold());
							String cannyAutoThreshold = nucleusCannyOptions.isUseCanny() ? String.valueOf(nucleusCannyOptions.isCannyAutoThreshold()) : "N/A";
							String cannyLowThreshold = nucleusCannyOptions.isUseCanny()  && !nucleusCannyOptions.isCannyAutoThreshold() ? String.valueOf(nucleusCannyOptions.getLowThreshold()) : "N/A";
							String cannyHighThreshold = nucleusCannyOptions.isUseCanny() && !nucleusCannyOptions.isCannyAutoThreshold() ? String.valueOf(nucleusCannyOptions.getHighThreshold()) : "N/A";
							String cannyKernelRadius = nucleusCannyOptions.isUseCanny() ? String.valueOf(nucleusCannyOptions.getKernelRadius()) : "N/A";
							String cannyKernelWidth = nucleusCannyOptions.isUseCanny() ? String.valueOf(nucleusCannyOptions.getKernelWidth()) : "N/A";
							String cannyClosingRadius = nucleusCannyOptions.isUseCanny() ? String.valueOf(nucleusCannyOptions.getClosingObjectRadius()) : "N/A";

							Object[] collectionData = {
									options.getAngleProfileWindowSize(),
									detectionMethod,
									nucleusThreshold,
									cannyAutoThreshold,
									cannyLowThreshold,
									cannyHighThreshold,
									cannyKernelRadius,
									cannyKernelWidth,
									cannyClosingRadius,
									options.getMinNucleusSize(),
									options.getMaxNucleusSize(),
									df.format(options.getMinNucleusCirc()),
									df.format(options.getMaxNucleusCirc()),
									options.refoldNucleus(),
									refoldMode,
									dataset.hasShellResult(),
									date,
									time,
									collection.getFolder(),
									options.getNucleusClass().getSimpleName()				
							};

							model.addColumn(collection.getName(), collectionData);
				} else {
					// there are no options to use; fill blank
					Object[] collectionData =  new Object[columnData.length];
					if(dataset.hasMergeSources()){
						Arrays.fill(collectionData, "N/A - merged");

					} else {
						Arrays.fill(collectionData, "N/A");
					}
					
					model.addColumn(collection.getName(), collectionData);
				}
			}
		}
		return model;	
	}
	
	/**
	 * Create a table model of basic stats from a nucleus collection.
	 * If null parameter is passed, will create an empty table
	 * @param collection
	 * @return
	 */
	public static TableModel createStatsTable(List<AnalysisDataset> list){

		DefaultTableModel model = new DefaultTableModel();

		Object[] columnData = {
				"Nuclei", 
				"Median area",
				"Median perimeter",
				"Median feret",
				"Signal channels",
				"Number of signals",
				"Signals per nucleus"};
		model.addColumn("", columnData);
		
		if(list==null){
			model.addColumn("No data loaded");
		} else {

			// format the numbers and make into a tablemodel
			DecimalFormat df = new DecimalFormat("#0.00"); 

			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();
								
				double signalPerNucleus = (double) collection.getSignalCount()/  (double) collection.getNucleusCount();
				
				Object[] collectionData = {
						collection.getNucleusCount(),
						df.format(collection.getMedianNuclearArea()),
						df.format(collection.getMedianNuclearPerimeter()),
						df.format(collection.getMedianFeretLength()),
						collection.getSignalGroups().size(),
						collection.getSignalCount(),
						df.format(signalPerNucleus)
				};

				model.addColumn(collection.getName(), collectionData);
			}
		}
		return model;	
	}
	
	public static TableModel createVennTable(List<AnalysisDataset> list){
		DefaultTableModel model = new DefaultTableModel();
		
		if(list==null){
			Object[] columnData = {""};
			model.addColumn("Population", columnData );
			model.addColumn("", columnData );
			return model;
		}
		
		// set rows
		Object[] columnData = new Object[list.size()];
		int row = 0;
		for(AnalysisDataset dataset : list){
			columnData[row] = dataset.getName();
			row++;
		}
		model.addColumn("Population", columnData);
		
		// add columns
		for(AnalysisDataset dataset : list){
			
			Object[] popData = new Object[list.size()];
			
			int i = 0;
			for(AnalysisDataset dataset2 : list){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
				} else {
					// compare the number of shared nucleus ids
					int shared = 0;
					for(Nucleus n : dataset.getCollection().getNuclei()){
						UUID n1id = n.getID();
						for(Nucleus n2 : dataset2.getCollection().getNuclei()){
							if( n2.getID().equals(n1id)){
								shared++;
							}
						}
//						if( dataset2.getCollection().getNuclei().contains(n)){
//							shared++;
//						}
					}
					DecimalFormat df = new DecimalFormat("#0.00"); 
					double pct = ((double) shared / (double) dataset2.getCollection().getNucleusCount())*100;
					popData[i] = shared+" ("+df.format(pct)+"% of row)";
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
		}
		return model;
	}
				
	/**
	 * Create an empty table to display.
	 * @param list
	 * @return
	 */
	private static DefaultTableModel makeEmptyWilcoxonTable(List<AnalysisDataset> list){
		DefaultTableModel model = new DefaultTableModel();

		if(list==null){
			Object[] columnData = { "" };
			model.addColumn("Population", columnData );
			model.addColumn("", columnData );
		} else {

			// set rows
			Object[] columnData = new Object[list.size()];
			int row = 0;
			for(AnalysisDataset dataset : list){
				columnData[row] = dataset.getName();
				row++;
			}
			model.addColumn("Population", columnData);
		}
		return  model;
	}
	
	/**
	 * Run a Wilcoxon test on the given datasets. 
	 * @param dataset1
	 * @param dataset2
	 * @param getPValue
	 * @return
	 */
	private static double runWilcoxonTest(double[] dataset1, double[] dataset2, boolean getPValue){

		double result;
		MannWhitneyUTest test = new MannWhitneyUTest();

		if(getPValue){ // above diagonal, p-value
			result = test.mannWhitneyUTest(dataset1, dataset2); // correct for the number of datasets tested

		} else { // below diagonal, U statistic
			result = test.mannWhitneyU(dataset1, dataset2);
		}
		return result;
	}
	
	/**
	 * Carry out pairwise wilcoxon rank-sum test on the perimeters of the given datasets
	 * @param list the datasets to test
	 * @return a tablemodel for display
	 */
	public static TableModel createWilcoxonPerimeterTable(List<AnalysisDataset> list){

		DefaultTableModel model = makeEmptyWilcoxonTable(list);
		if(list==null){
			return model;
		}

		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : list){
			
			Object[] popData = new Object[list.size()];
			
			int i = 0;
			boolean getPValue = false;
			for(AnalysisDataset dataset2 : list){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
					getPValue = true;
				} else {
					popData[i] = df.format( runWilcoxonTest( 
							dataset.getCollection().getPerimeters(), 
							dataset2.getCollection().getPerimeters(), 
							getPValue) );
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
		}
		return model;
	}
	
	/**
	 * Carry out pairwise wilcoxon rank-sum test on the min ferets of the given datasets
	 * @param list the datasets to test
	 * @return a tablemodel for display
	 */
	public static TableModel createWilcoxonMinFeretTable(List<AnalysisDataset> list){
		DefaultTableModel model = makeEmptyWilcoxonTable(list);
		if(list==null){
			return model;
		}
		
		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : list){
			
			Object[] popData = new Object[list.size()];
			
			int i = 0;
			boolean getPValue = false;
			for(AnalysisDataset dataset2 : list){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
					getPValue = true;
				} else {
					popData[i] = df.format( runWilcoxonTest( 
							dataset.getCollection().getMinFerets(), 
							dataset2.getCollection().getMinFerets(), 
							getPValue) );
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
		}
		return model;
	}
	
	/**
	 * Carry out pairwise wilcoxon rank-sum test on the ferets of the given datasets
	 * @param list the datasets to test
	 * @return a tablemodel for display
	 */
	public static TableModel createWilcoxonMaxFeretTable(List<AnalysisDataset> list){
		DefaultTableModel model = makeEmptyWilcoxonTable(list);
		if(list==null){
			return model;
		}
		
		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : list){
			
			Object[] popData = new Object[list.size()];
			
			int i = 0;
			boolean getPValue = false;
			for(AnalysisDataset dataset2 : list){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
					getPValue = true;
				} else {
					popData[i] = df.format( runWilcoxonTest( 
							dataset.getCollection().getFerets(), 
							dataset2.getCollection().getFerets(), 
							getPValue) );
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
		}
		return model;
	}
	
	/**
	 * Carry out pairwise wilcoxon rank-sum test on the variability of the given datasets
	 * @param list the datasets to test
	 * @return a tablemodel for display
	 * @throws Exception 
	 */
	public static TableModel createWilcoxonVariabilityTable(List<AnalysisDataset> list) throws Exception{
		DefaultTableModel model = makeEmptyWilcoxonTable(list);
		if(list==null){
			return model;
		}
		
		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : list){
			
			Object[] popData = new Object[list.size()];
			
			int i = 0;
			boolean getPValue = false;
			for(AnalysisDataset dataset2 : list){

				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
					getPValue = true;
				} else {
					popData[i] = df.format( runWilcoxonTest( 
							dataset.getCollection()
							.getDifferencesToMedianFromPoint(dataset.getCollection()
									.getOrientationPoint()  ), 
									dataset2.getCollection()
									.getDifferencesToMedianFromPoint(dataset2.getCollection()
											.getOrientationPoint() ), 
											getPValue) );
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
		}
		return model;
	}

	/**
	 * Carry out pairwise wilcoxon rank-sum test on the areas of the given datasets
	 * @param list the datasets to test
	 * @return a tablemodel for display
	 */
	public static TableModel createWilcoxonAreaTable(List<AnalysisDataset> list){
		DefaultTableModel model = makeEmptyWilcoxonTable(list);
		if(list==null){
			return model;
		}
		
		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : list){
			
			Object[] popData = new Object[list.size()];
			
			int i = 0;
			boolean getPValue = false;
			for(AnalysisDataset dataset2 : list){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
					getPValue = true;
				} else {
					popData[i] = df.format( runWilcoxonTest( 
							dataset.getCollection().getAreas(), 
							dataset2.getCollection().getAreas(), 
							getPValue) );
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
		}
		return model;
	}
	
	/**
	 * Get the cluster groups from a list of datasets as a table
	 * @param list
	 * @return
	 */
	public static TableModel createClusterGroupsTable(List<AnalysisDataset> list){
		DefaultTableModel model = new DefaultTableModel();

		Object[] columnData = {
				"Cluster group", 
				"Number of clusters"};
		model.setColumnIdentifiers(columnData);
		
		if(list==null){
			model.addColumn("No data loaded");
		} else {

			for(AnalysisDataset dataset : list){
				List<ClusterGroup> clusterGroups = dataset.getClusterGroups();
				
				for(ClusterGroup g : clusterGroups ){
					Object[] rowData = {
						g.getName(),
						g.size()
					};
					model.addRow(rowData);
				}
			}
		}
		return model;	
	}
	
	/**
	 * Get the options used for clustering as a table
	 * @param list
	 * @return
	 */
	public static TableModel createClusterOptionsTable(List<AnalysisDataset> list){
		DefaultTableModel model = new DefaultTableModel();

		Object[] columnData = {
				"Cluster group",
				"Method", 
				"Cluster number",
				"Iterations",
				"Hi method",
				"Include modality",
				"Modality points"};
		model.setColumnIdentifiers(columnData);
		
		if(list==null){
			model.addColumn("No data loaded");
		} else {

			// format the numbers and make into a tablemodel
//			DecimalFormat df = new DecimalFormat("#0.00"); 

			for(AnalysisDataset dataset : list){
				List<ClusterGroup> clusterGroups = dataset.getClusterGroups();
				
				for(ClusterGroup g : clusterGroups ){
					ClusteringOptions op = g.getOptions();
					Object[] rowData = {
						g.getName(),
						op.getType(),
						op.getClusterNumber(),
						op.getIterations(),
						op.getHierarchicalMethod().toString(),
						op.isIncludeModality(),
						op.getModalityRegions()
					};
					model.addRow(rowData);
				}
			}
		}
		return model;	
	}

}
