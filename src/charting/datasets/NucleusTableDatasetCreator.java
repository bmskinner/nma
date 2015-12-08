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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import components.CellCollection;
import components.ClusterGroup;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileCollectionType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import ij.IJ;
import stats.DipTester;
import stats.NucleusStatistic;
import stats.Stats;

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

			// get the offset segments
			List<NucleusBorderSegment> segments = nucleus.getAngleProfile(BorderTag.REFERENCE_POINT).getSegments();
			

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
	public static TableModel createMedianProfileSegmentStatsTable(AnalysisDataset dataset, MeasurementScale scale) throws Exception {

		DefaultTableModel model = new DefaultTableModel();

		List<Object> fieldNames = new ArrayList<Object>(0);
		if(dataset==null){
			model.addColumn("No data loaded");

		} else {
			CellCollection collection = dataset.getCollection();
			// check which reference point to use
			BorderTag point = BorderTag.ORIENTATION_POINT;

			// get the offset segments
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);

			// create the row names
			fieldNames.add("Colour");
			fieldNames.add("Length");
			fieldNames.add("Start index");
			fieldNames.add("End index");
			fieldNames.add("Mean length ("+ scale+")");
			fieldNames.add("Mean length 95% CI ("+ scale+")");
			fieldNames.add("Length std err. ("+ scale+")");
			fieldNames.add("Length p(unimodal)");

			model.addColumn("", fieldNames.toArray(new Object[0]));
						
			DecimalFormat df = new DecimalFormat("#.##");
			df.setMaximumFractionDigits(2);
			df.setMinimumFractionDigits(2);
			df.setMinimumIntegerDigits(1);
			DecimalFormat pf = new DecimalFormat("#.###");

			for(NucleusBorderSegment segment : segments) {

				List<Object> rowData = new ArrayList<Object>(0);
				
				rowData.add("");
				rowData.add(segment.length());
				rowData.add(segment.getStartIndex());
				rowData.add(segment.getEndIndex());
								
				double[] meanLengths = collection.getSegmentLengths(segment.getName(), scale);
				
				double mean = Stats.mean( meanLengths);
				double sem  = Stats.stderr(meanLengths);
				double[] ci = Stats.calculateMeanConfidenceInterval(meanLengths, 0.95);
				
				rowData.add(  df.format(mean ) );
				rowData.add(  df.format(ci[0])+" - "+ df.format(ci[1]));
				rowData.add(  df.format(sem) );
				
				
				double pval = DipTester.getDipTestPValue(meanLengths);
				rowData.add( pf.format(pval) );

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
	public static TableModel createMultiDatasetMedianProfileSegmentStatsTable(List<AnalysisDataset> list, MeasurementScale scale) throws Exception {

		DefaultTableModel model = new DefaultTableModel();
		DecimalFormat df = new DecimalFormat("0.00");
		df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);
		df.setMinimumIntegerDigits(1);

		List<Object> fieldNames = new ArrayList<Object>(0);
		if(list==null){
			model.addColumn("No data loaded");

		} else {
			
			BorderTag point = BorderTag.ORIENTATION_POINT;
			
			// assumes all datasets have the same number of segments
			List<NucleusBorderSegment> segments = list.get(0).getCollection().getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);
			
			fieldNames.add("Dataset");
			for(NucleusBorderSegment segment : segments) {
				fieldNames.add(segment.getName());
			}
			model.setColumnIdentifiers(fieldNames.toArray());;
			
			// add the segment colours
			List<Object> colours = new ArrayList<Object>(0);
			colours.add("");
			
			for(NucleusBorderSegment segment : segments) {
				colours.add("");
			}
			model.addRow(colours.toArray(new Object[0]));
			
			

			for(AnalysisDataset dataset : list){

				CellCollection collection = dataset.getCollection();		

				// get the offset segments
				List<NucleusBorderSegment> segs = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);
				
				List<Object> rowData = new ArrayList<Object>(0);
				rowData.add(dataset.getName());

				for(NucleusBorderSegment segment : segs) {
					
					// Convert array index lengths to pixel lengths
//					double perimeter = collection.getMedianNuclearPerimeter(); // get the perimeter in pixels
//					double maxIndex = segment.getTotalLength();
//					double segPixelLength = (  (double) segment.length() / maxIndex) * perimeter;
					
					double[] meanLengths = collection.getSegmentLengths(segment.getName(), scale);
					double mean = Stats.mean(meanLengths); 
//					DescriptiveStatistics stat = new DescriptiveStatistics(meanLengths);
					
//					rowData.add(  df.format(stat.getMean() ) );
					double ci = Stats.calculateConfidenceIntervalSize(meanLengths, 0.95);
//					rowData.add(  df.format(sem) );
					
					rowData.add(df.format(mean)+" ± "+ df.format(ci));
				}
				model.addRow(rowData.toArray(new Object[0]));
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
				"Kuwahara filter radius",
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
				"Type",
				"Version"};
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
							
							String kuwaharaRadius = nucleusCannyOptions.isUseKuwahara() ? String.valueOf(nucleusCannyOptions.getKuwaharaKernel()) : "N/A";
							
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
									kuwaharaRadius,
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
									options.getNucleusType().toString(),
									dataset.getVersion()
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
	public static TableModel createStatsTable(List<AnalysisDataset> list) throws Exception {

		DefaultTableModel model = new DefaultTableModel();
		
		List<Object> columnData = new ArrayList<Object>();	
		columnData.add("Nuclei");
		for(NucleusStatistic stat : NucleusStatistic.values()){
			columnData.add(stat.toString()+" median");
			columnData.add(stat.toString()+" mean 95% CI");
			columnData.add(stat.toString()+" p(unimodal)");
		}
		columnData.add("Signal channels");
		columnData.add("Number of signals");
		columnData.add("Signals per nucleus");

		model.addColumn("", columnData.toArray());
		
		if(list==null){
			model.addColumn("No data loaded");
		} else {

			// format the numbers and make into a tablemodel
			DecimalFormat df = new DecimalFormat("#0.00"); 
			DecimalFormat pf = new DecimalFormat("#0.000"); 

			for(AnalysisDataset dataset : list){
				CellCollection collection = dataset.getCollection();

				List<Object> datasetData = new ArrayList<Object>();			
				double signalPerNucleus = (double) collection.getSignalCount()/  (double) collection.getNucleusCount();

				datasetData.add(collection.getNucleusCount());

				for(NucleusStatistic stat : NucleusStatistic.values()){
					double[] stats 	= collection.getNuclearStatistics(stat, MeasurementScale.PIXELS);
					double median 	= Stats.quartile(stats, 50);
					double[] ci 	= Stats.calculateMeanConfidenceInterval(stats, 0.95);
					String ciString = df.format(ci[0]) + " - " + df.format(ci[1]);
					double diptest 	= DipTester.getDipTestPValue(stats);

					datasetData.add(df.format(median));
					datasetData.add(ciString);
					datasetData.add(pf.format(diptest));					
				}
				
				datasetData.add(collection.getSignalGroups().size());
				datasetData.add(collection.getSignalCount());
				datasetData.add(df.format(signalPerNucleus));
				
				model.addColumn(collection.getName(), datasetData.toArray());
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
	 * Create a pairwise Venn table showing all combinations
	 * @param list
	 * @return
	 */
	public static TableModel createPairwiseVennTable(List<AnalysisDataset> list) {
		DefaultTableModel model = new DefaultTableModel();
		
		if(list==null){
			Object[] columnData = {""};
			model.addColumn("Population 1", columnData );
			model.addColumn("Unique %", columnData );
			model.addColumn("Unique", columnData );
			model.addColumn("Shared %", columnData );
			model.addColumn("Shared", columnData );
			model.addColumn("Shared %", columnData );
			model.addColumn("Unique", columnData );
			model.addColumn("Unique %", columnData );
			model.addColumn("Population 2", columnData );
			return model;
		}
		
		// set rows
		Object[] columnData = {""};
		model.addColumn("Population 1", columnData ); //0
		model.addColumn("Unique %", columnData ); //1
		model.addColumn("Unique", columnData ); //2
		model.addColumn("Shared %", columnData ); //3
		model.addColumn("Shared", columnData ); //4
		model.addColumn("Shared %", columnData ); //5
		model.addColumn("Unique", columnData ); //6
		model.addColumn("Unique %", columnData ); //7
		model.addColumn("Population 2", columnData ); //8

		// add columns
		for(AnalysisDataset dataset1 : list){

			for(AnalysisDataset dataset2 : list){

				// Ignore self-self matches
				if( ! dataset2.getUUID().equals(dataset1.getUUID())){

					Object[] popData = new Object[9];

					popData[0] = dataset1.getName();
					popData[8] = dataset2.getName();

					// compare the number of shared nucleus ids
					int shared = 0;
					for(Nucleus n1 : dataset1.getCollection().getNuclei()){

						for(Nucleus n2 : dataset2.getCollection().getNuclei()){
							if( n2.getID().equals(n1.getID())){
								shared++;
							}
						}

					}
					popData[4] = shared;

					DecimalFormat df = new DecimalFormat("#0.00"); 

					int unique1 = dataset1.getCollection().getNucleusCount() - shared;
					int unique2 = dataset2.getCollection().getNucleusCount() - shared; 
					popData[2] = unique1;
					popData[6] = unique2;
					
					double uniquePct1 = ((double) unique1 / (double) dataset1.getCollection().getNucleusCount())*100;
					double uniquePct2 = ((double) unique2 / (double) dataset2.getCollection().getNucleusCount())*100;
					
					popData[1] = df.format(uniquePct1);
					popData[7] = df.format(uniquePct2);
					
					
					double sharedpct1 = ((double) shared / (double) dataset1.getCollection().getNucleusCount())*100;
					double sharedpct2 = ((double) shared / (double) dataset2.getCollection().getNucleusCount())*100;
					
					popData[3] = df.format(sharedpct1);
					popData[5] = df.format(sharedpct2);

					model.addRow(popData);
				}
			}
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
	 * Carry out pairwise wilcoxon rank-sum test on the given stat of the given datasets
	 * @param list the datasets to test
	 * @param stat the statistic to measure
	 * @return a tablemodel for display
	 */	
	public static TableModel createWilcoxonNuclearStatTable(List<AnalysisDataset> list, NucleusStatistic stat) throws Exception {
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
							dataset.getCollection().getNuclearStatistics(stat, MeasurementScale.PIXELS), 
							dataset2.getCollection().getNuclearStatistics(stat, MeasurementScale.PIXELS), 
							getPValue) );
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
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
				"Clusters found",
				"Method", 
				"Iterations",
				"Hierarchical method",
				"Cluster number",
				"Include modality",
				"Modality points",
				"Include profile",
				"Include area",
				"Include aspect",
				"Tree"};
		model.addColumn("", columnData);
		
		if(list==null){
			model.addColumn("No data loaded");
		} else {

			// format the numbers and make into a tablemodel
//			DecimalFormat df = new DecimalFormat("#0.00"); 

			for(AnalysisDataset dataset : list){
				List<ClusterGroup> clusterGroups = dataset.getClusterGroups();
				
				for(ClusterGroup g : clusterGroups ){
					ClusteringOptions op = g.getOptions();
					
					Object iterationString 	= op.getType().equals(ClusteringMethod.EM) 
											? op.getIterations()
											: "N/A";
											
					Object hierarchicalMethodString = op.getType().equals(ClusteringMethod.HIERARCHICAL) 
							? op.getHierarchicalMethod().toString()
							: "N/A";
							
					Object hierarchicalClusterString = op.getType().equals(ClusteringMethod.HIERARCHICAL) 
							? op.getClusterNumber()
							: "N/A";
							
					String tree = g.hasTree() ? g.getTree() : "N/A";
											
					Object[] data = {
						g.getName(),
						g.size(),
						op.getType().toString(),
						iterationString,
						hierarchicalMethodString,
						hierarchicalClusterString,
						op.isIncludeModality(),
						op.getModalityRegions(),
						op.isIncludeProfile(),
						op.isIncludeStatistic(NucleusStatistic.AREA),
						op.isIncludeStatistic(NucleusStatistic.ASPECT),
						tree
					};
					model.addColumn(dataset.getName(), data);
				}
			}
		}
		return model;	
	}

}
