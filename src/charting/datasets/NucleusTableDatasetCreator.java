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

import gui.Labels;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import logging.Loggable;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import charting.charts.BoxplotChartFactory;
import charting.options.TableOptions;
import charting.options.TableOptions.TableType;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import analysis.profiles.ProfileManager;
import components.CellCollection;
import components.ClusterGroup;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import stats.DipTester;
import stats.NucleusStatistic;
import stats.SegmentStatistic;
import stats.Stats;
import utility.Constants;

public class NucleusTableDatasetCreator implements Loggable {
	
private static NucleusTableDatasetCreator instance = null;
	
	protected NucleusTableDatasetCreator(){}
	
	/**
	 * Fetch an instance of the factory
	 * @return
	 */
	public static NucleusTableDatasetCreator getInstance(){
		if(instance==null){
			instance = new NucleusTableDatasetCreator();
		}
		return instance;
	}
		
	public TableModel createMedianProfileStatisticTable(TableOptions options) throws Exception{
		
		if( ! options.hasDatasets()){
			return createBlankTable();
		}
		
		if(options.isSingleDataset()){
			return createMedianProfileSegmentStatsTable(options.firstDataset(), options.getScale());
		}
		
		if(options.isMultipleDatasets()){
			return createMultiDatasetMedianProfileSegmentStatsTable(options);
		}
		
		return createBlankTable();
	}
	
	public TableModel createBlankTable(){
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(Labels.NO_DATA_LOADED);
		return model;
	}
	
	public TableModel createMergeSourcesTable(TableOptions options){
		
		if( ! options.hasDatasets()){
			DefaultTableModel model = new DefaultTableModel();

			Vector<Object> names 	= new Vector<Object>();
			Vector<Object> nuclei 	= new Vector<Object>();

			names.add("No merge sources");
			nuclei.add("");


			model.addColumn("Merge source", names);
			model.addColumn("Nuclei", nuclei);
			return model;
		}
		
		if(options.firstDataset().hasMergeSources()){

			DefaultTableModel model = new DefaultTableModel();

			Vector<Object> names 	= new Vector<Object>();
			Vector<Object> nuclei 	= new Vector<Object>();

			for( AnalysisDataset mergeSource : options.firstDataset().getMergeSources()){
//				AnalysisDataset mergeSource = options.firstDataset().getMergeSource(id);
				names.add(mergeSource.getName());
				nuclei.add(mergeSource.getCollection().getNucleusCount());
			}
			model.addColumn("Merge source", names);
			model.addColumn("Nuclei", nuclei);
			return model;
		} 
		return createBlankTable();
	}
	
	/**
	 * Create a table of segment stats for median profile of the given dataset.
	 * @param dataset the AnalysisDataset to include
	 * @return a table model
	 * @throws Exception 
	 */
	private TableModel createMedianProfileSegmentStatsTable(AnalysisDataset dataset, MeasurementScale scale) throws Exception {

		DefaultTableModel model = new DefaultTableModel();

		List<Object> fieldNames = new ArrayList<Object>(0);
		if(dataset==null){
			model.addColumn("No data loaded");

		} else {
			CellCollection collection = dataset.getCollection();
			// check which reference point to use
			BorderTag point = BorderTag.REFERENCE_POINT;

			// get mapping from ordered segments to segment names
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(point)
					.getOrderedSegments();

//			Map<String, String> map = new HashMap<String, String>();
//			for(NucleusBorderSegment seg : segments){
//				
//				NucleusBorderSegment realSeg = collection.getProfileCollection(ProfileType.REGULAR).getSegmentedProfile(point).getSegment(seg);;
//				map.put(seg.getName(), realSeg.getName());
//			}
			
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
							
				double[] meanLengths = collection.getSegmentStatistics(SegmentStatistic.LENGTH, scale, segment.getID());

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
	private TableModel createMultiDatasetMedianProfileSegmentStatsTable(TableOptions options) throws Exception {

		List<AnalysisDataset> list = options.getDatasets();
		MeasurementScale scale = options.getScale();
		
		DefaultTableModel model = new DefaultTableModel();
		DecimalFormat df = new DecimalFormat("0.00");
		df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);
		df.setMinimumIntegerDigits(1);
		
		// If the datasets have different segment counts, show error message
		if( ! ProfileManager.segmentCountsMatch(list)){
			model.addColumn(Labels.INCONSISTENT_SEGMENT_NUMBER);
			return model;
		}
		
		// If there are no datasets, show error message
		if( ! options.hasDatasets()){
			model.addColumn(Labels.NO_DATA_LOADED);
			return model;

		} 

		// Everything after this is building the table assuming valid data
		
		List<Object> fieldNames = new ArrayList<Object>(0);
		
		BorderTag point = BorderTag.REFERENCE_POINT;//.ORIENTATION_POINT;

		// assumes all datasets have the same number of segments
		List<NucleusBorderSegment> segments = list.get(0)
				.getCollection()
				.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(point)
				.getOrderedSegments();


		// Add the dataset names column
		fieldNames.add("Dataset");
		for(NucleusBorderSegment segment : segments) {
			fieldNames.add(segment.getName());
		}
		model.setColumnIdentifiers(fieldNames.toArray());;

		// Add the segment colours column
		List<Object> colours = new ArrayList<Object>(0);
		colours.add("");

		for(NucleusBorderSegment segment : segments) {
			colours.add("");
		}
		model.addRow(colours.toArray(new Object[0]));

		// Add the segment stats columns

		for(AnalysisDataset dataset : list){

			CellCollection collection = dataset.getCollection();		

			// get the offset segments
			//				List<NucleusBorderSegment> segs = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);

			List<NucleusBorderSegment> segs = collection
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(point)
					.getOrderedSegments();

			List<Object> rowData = new ArrayList<Object>(0);
			rowData.add(dataset.getName());

			for(NucleusBorderSegment segment : segs) {

				double[] meanLengths = collection.getSegmentStatistics(SegmentStatistic.LENGTH, scale, segment.getID());

				double mean = Stats.mean(meanLengths); 

				double ci = Stats.calculateConfidenceIntervalSize(meanLengths, 0.95);

				rowData.add(df.format(mean)+" ± "+ df.format(ci));
			}
			model.addRow(rowData.toArray(new Object[0]));
		}

		return model;	
	}
	
	/**
	 * Create a table model of analysis parameters or stats from datasets.
	 * If null parameter is passed, will create an empty table
	 * @param collection
	 * @return
	 */
	public TableModel createAnalysisTable(TableOptions options) {

		if( ! options.hasDatasets()){
			finest("No datasets, creating blank table");
			return createBlankTable();
		} 
		
		finest("Table options type "+options.getType());
		
		if(options.getType().equals(TableType.ANALYSIS_PARAMETERS)){
			finest("Creating analysis parameters table model");
			return createAnalysisParametersTable(options);
		}
		
		if(options.getType().equals(TableType.ANALYSIS_STATS)){
			finest("Creating analysis stats table model");
			return createStatsTable(options);
		}
		
		return createBlankTable();
	}
	
	
	/**
	 * Create a table model of analysis parameters from a nucleus collection.
	 * If null parameter is passed, will create an empty table
	 * @param collection
	 * @return
	 */
	private TableModel createAnalysisParametersTable(TableOptions options){

		DefaultTableModel model = new DefaultTableModel();

		Object[] columnData = {
				"Profile window",
				"Nucleus detection method",
				"Nucleus threshold",
				"Kuwahara filter radius",
				"Chromocentre flattening threshold",
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
				"Run date",
				"Run time",
				"Collection source",
				"Log file",
				"Type",
				"Version"};
		model.addColumn("", columnData);
		
		if( ! options.hasDatasets()){
			finer("No datasets in options, returning blank parameter table");
			model.addColumn("No data loaded");
			return model;
		} 


		List<AnalysisDataset> list = options.getDatasets();

		for(AnalysisDataset dataset : list){

			// only display if there are options available
			// This may not be the case for a merged dataset or its children
			if( dataset.hasAnalysisOptions()){ 

				// Do not provide an options as an argument here.
				// This will cause the existing dataset options to be used
				Object[] collectionData = formatAnalysisOptionsForTable(dataset, null);

				model.addColumn(dataset.getCollection().getName(), collectionData);

			} else {
				options.log(Level.FINE, "No analysis options in dataset "+dataset.getName());
				Object[] collectionData =  new Object[columnData.length];
				if(dataset.hasMergeSources()){

					options.log(Level.FINE, "Dataset has merge sources");				
					if( testMergedDatasetOptionsAreSame(dataset)){
						
						options.log(Level.FINE, "Merge source options are the same");

						// The options are the same in all merge sources
						// Show the first options from the first source

						AnalysisOptions op = dataset.getAllMergeSources().get(0).getAnalysisOptions();

						// Provide an options to be used
						collectionData = formatAnalysisOptionsForTable(dataset, op);

					} else {
						// Merge sources have different options
						Arrays.fill(collectionData, "N/A - merged");
						options.log(Level.FINE, "Merge source options are different");	
					}

				} else {
					// there are no options to use; fill blank
					Arrays.fill(collectionData, "N/A");
					options.log(Level.FINE, "No merge sources, and no options");	
				}

				model.addColumn(dataset.getCollection().getName(), collectionData);
			}
		}
		
		return model;	
	}
	
	/**
	 * Test if the merge sources of a dataset have the same analysis options
	 * TODO: make recursive; what happens when two merged datsets are merged?
	 * @param dataset
	 * @return the common options, or null if an options is different
	 */
	private boolean testMergedDatasetOptionsAreSame(AnalysisDataset dataset){
		finest( "Testing merged dataset options for "+dataset.getName() );
		List<AnalysisDataset> list = dataset.getMergeSources();
		
		boolean ok = true;

		for(AnalysisDataset d1 : list){
			
			finest( "Testing merge source "+d1.getName() );
			/*
			 * If the dataset has merge sources, the options are null
			 * In this case, recursively go through the dataset's merge sources
			 * until the root datasets are found with analysis options
			 */
			if( d1.hasMergeSources() ){
				finest( d1.getName() +" has merge sources");
				ok = testMergedDatasetOptionsAreSame(d1);
				
			} else {
				
				for(AnalysisDataset d2 : list){
					finest( "Comparing to merge source "+d2.getName() );
					if(d1==d2){
						finest( "Skip self self comparison");
						continue; // ignore self self comparisons
					}
					
					// ignore d2 with a merge source - it will be covered in the d1 loop
					if(d2.hasMergeSources()){
						finest( "Ignoring d2 merge source with merge sources"+d2.getName() );
						continue;
					}

					finest( "Checking equality of options in "+d1.getName()+" vs "+d2.getName() );
					
					AnalysisOptions a1 = d1.getAnalysisOptions();
					AnalysisOptions a2 = d2.getAnalysisOptions();
					
					if(a1==null || a2==null){
						finest( "Found null analysis options; comparison is false" );
						ok = false;
						continue;
					}
					
					if( ! a1.equals(a2) ){
						finest( "Found unequal options in "+d1.getName()+" vs "+d2.getName() );
						ok = false;
					} else {
						finest( "Found equal options in "+d1.getName()+" vs "+d2.getName() );
					}
					finest( "Done comparing to merge source "+d2.getName() );
				}
				finest( "Done testing list against merge source "+d1.getName() );
			}
			finest( "Done testing merge source "+d1.getName() );
		}
		finest( "Done testing merged dataset options for "+dataset.getName()+": "+ok );
		return ok;
	}
	
	/**
	 * Get an array of formatted info from a dataset analysis options
	 * @param dataset
	 * @return
	 */
	private Object[] formatAnalysisOptionsForTable(AnalysisDataset dataset, AnalysisOptions options){
		options = options == null ? dataset.getAnalysisOptions() : options;
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		// only display refold mode if nucleus was refolded
		String refoldMode = options.refoldNucleus() 
				? options.getRefoldMode()
						: "N/A";

		String date;
		String time;
		String folder;
		String logFile;
				
		if(dataset.hasMergeSources()){
			date    = "N/A - merge";
			time    = "N/A - merge";
			folder  = "N/A - merge";
			logFile = "N/A - merge";
			
		} else {
			String[] times = dataset.getCollection().getOutputFolderName().split("_");
			date = times[0];
			time = times[1];
			folder = dataset.getCollection().getFolder().getAbsolutePath();
			logFile = dataset.getDebugFile().getAbsolutePath();
		}

		CannyOptions nucleusCannyOptions = options.getCannyOptions("nucleus");

		String detectionMethod = nucleusCannyOptions.isUseCanny() ? "Canny edge detection" : "Thresholding";
		String nucleusThreshold = nucleusCannyOptions.isUseCanny() ? "N/A" : String.valueOf(options.getNucleusThreshold());
		
		String kuwaharaRadius = nucleusCannyOptions.isUseKuwahara() ? String.valueOf(nucleusCannyOptions.getKuwaharaKernel()) : "N/A";
		
		String chromocentreThreshold = nucleusCannyOptions.isUseFlattenImage() ? String.valueOf(nucleusCannyOptions.getFlattenThreshold()) : "N/A";
		
		String cannyAutoThreshold = nucleusCannyOptions.isUseCanny() ? String.valueOf(nucleusCannyOptions.isCannyAutoThreshold()) : "N/A";
		String cannyLowThreshold = nucleusCannyOptions.isUseCanny()  && !nucleusCannyOptions.isCannyAutoThreshold() ? String.valueOf(nucleusCannyOptions.getLowThreshold()) : "N/A";
		String cannyHighThreshold = nucleusCannyOptions.isUseCanny() && !nucleusCannyOptions.isCannyAutoThreshold() ? String.valueOf(nucleusCannyOptions.getHighThreshold()) : "N/A";
		String cannyKernelRadius = nucleusCannyOptions.isUseCanny() ? String.valueOf(nucleusCannyOptions.getKernelRadius()) : "N/A";
		String cannyKernelWidth = nucleusCannyOptions.isUseCanny() ? String.valueOf(nucleusCannyOptions.getKernelWidth()) : "N/A";
		String cannyClosingRadius = nucleusCannyOptions.isUseCanny() ? String.valueOf(nucleusCannyOptions.getClosingObjectRadius()) : "N/A";

		Object[] collectionData = {
				options.getAngleWindowProportion(),
				detectionMethod,
				nucleusThreshold,
				kuwaharaRadius,
				chromocentreThreshold,
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
				date,
				time,
				folder,
				logFile,
				options.getNucleusType().toString(),
				dataset.getVersion().toString()
				};
		return collectionData;
	}
	
	/**
	 * Create a table model of basic stats from a nucleus collection.
	 * If null parameter is passed, will create an empty table
	 * @param collection
	 * @return
	 */
	private TableModel createStatsTable(TableOptions options) {

		if( ! options.hasDatasets()){
			return createBlankTable();
		} 
		
		DefaultTableModel model = new DefaultTableModel();
		
		List<AnalysisDataset> list = options.getDatasets();
		
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
		
		finest("Created model row headers");

		finest("Making column for each dataset");
		for(AnalysisDataset dataset : list){
			
			finest("Making column for "+dataset.getName());
			
			List<Object> datasetData = createDatasetStatsTableColumn(dataset);

			model.addColumn(dataset.getName(), datasetData.toArray());
			finest("Added column for "+dataset.getName());
		}
		finest("Created table model");
		
		return model;	
	}
	
	private List<Object> createDatasetStatsTableColumn(AnalysisDataset dataset){
		
		// format the numbers and make into a tablemodel
		DecimalFormat df = new DecimalFormat("#0.00"); 
		DecimalFormat pf = new DecimalFormat("#0.000"); 
		
		CellCollection collection = dataset.getCollection();

		List<Object> datasetData = new ArrayList<Object>();			
		double signalPerNucleus = (double) collection.getSignalManager().getSignalCount()/  (double) collection.getNucleusCount();

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

		datasetData.add(dataset.getCollection().getSignalManager().getSignalGroupCount());
		datasetData.add(collection.getSignalManager().getSignalCount());
		datasetData.add(df.format(signalPerNucleus));
		
		return datasetData;
		
	}
	
	public TableModel createVennTable(TableOptions options){
		
				
		if( ! options.hasDatasets()){
			finest("No datasets, creating blank venn table");
			return createBlankTable();
		}
		
		if(options.isSingleDataset()){
			finest("Single dataset, creating blank venn table");
			return createBlankTable();
		}
		
		finer("Creating venn table model");
		DefaultTableModel model = new DefaultTableModel();
		
		List<AnalysisDataset> list = options.getDatasets();
		
		// set rows
		Object[] columnData = new Object[list.size()];
		int row = 0;
		for(AnalysisDataset dataset : list){
			columnData[row] = dataset.getName();
			row++;
		}
		model.addColumn("Population", columnData);
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		// add columns
		for(AnalysisDataset dataset : list){
			
			finest("Fetching comparisons for "+dataset.getName());
			
			Object[] popData = new Object[list.size()];
			
			int i = 0;
			for(AnalysisDataset dataset2 : list){
								
				String valueString = "";
				
				if( ! dataset2.getUUID().equals(dataset.getUUID())){	

					int shared = dataset.getCollection().getSharedNucleusCount(dataset2);

					double pct = ((double) shared / (double) dataset2.getCollection().getNucleusCount())*100;
					valueString = shared+" ("+df.format(pct)+"% of row)";
				}
				
				popData[i++] = valueString;
			}
			model.addColumn(dataset.getName(), popData);
		}
		finer("Created venn table model");
		return model;
	}
	

	
	
	/**
	 * Create a pairwise Venn table showing all combinations
	 * @param list
	 * @return
	 */
	public TableModel createPairwiseVennTable(TableOptions options) {
				
		if( ! options.hasDatasets()){
			finest("No datasets, creating blank pairwise venn table");
			return createBlankTable();
		}
		
		if(options.isSingleDataset()){
			finest("Single dataset, creating blank pairwise venn table");
			return createBlankTable();
		}
		finer("Creating venn pairwise table model");
		DefaultTableModel model = new DefaultTableModel();
		
		Object[] columnNames = new Object[] {
				"Population 1",
				"Unique %",
				"Unique",
				"Shared %",
				"Shared",
				"Shared %",
				"Unique",
				"Unique %",
				"Population 2"
				};
		model.setColumnIdentifiers(columnNames);
			
		
		List<AnalysisDataset> list = options.getDatasets();
		// Track the pairwase comparisons performed to avoid duplicates
		Map<UUID, ArrayList<UUID>> existingMatches = new HashMap<UUID, ArrayList<UUID>>();
		
		DecimalFormat df = new DecimalFormat("#0.00"); 

		// add columns
		for(AnalysisDataset dataset1 : list){
			
			ArrayList<UUID> set1List = new ArrayList<UUID>();
			existingMatches.put(dataset1.getUUID(), set1List);
			
			for(AnalysisDataset dataset2 : list){

				// Ignore self-self matches
				if( ! dataset2.getUUID().equals(dataset1.getUUID())){
					
					set1List.add(dataset2.getUUID());
					
					if(existingMatches.get(dataset2.getUUID())!=null){
						if(existingMatches.get(dataset2.getUUID()).contains(dataset1.getUUID())    ){
							continue;
						}
					}
				
					

					Object[] popData = new Object[9];

					popData[0] = dataset1.getName();
					popData[8] = dataset2.getName();

					// compare the number of shared nucleus ids
					int shared = dataset1.getCollection().getSharedNucleusCount(dataset2);

					popData[4] = shared;

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
		finer("Created venn pairwise table model");
		return model;
	}
				
	/**
	 * Create an empty table to display.
	 * @param list
	 * @return
	 */
	private DefaultTableModel makeEmptyWilcoxonTable(List<AnalysisDataset> list){
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
	private double runWilcoxonTest(double[] dataset1, double[] dataset2, boolean getPValue){

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
	 * @param options the table options
	 * @return a tablemodel for display
	 */	
	public TableModel createWilcoxonStatisticTable(TableOptions options) throws Exception{
		
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		if(options.getStat().getClass()==NucleusStatistic.class){
			return createWilcoxonNuclearStatTable(options);
		}
		
		if(options.getStat().getClass()==SegmentStatistic.class){
			return createWilcoxonSegmentStatTable(options);
		}
		
		return makeEmptyWilcoxonTable(null);
		
	}
	
	/**
	 * Carry out pairwise wilcoxon rank-sum test on the given stat of the given datasets
	 * @param list the datasets to test
	 * @param stat the statistic to measure
	 * @return a tablemodel for display
	 */	
	private TableModel createWilcoxonNuclearStatTable(TableOptions options) throws Exception {
		
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
		
		NucleusStatistic stat = (NucleusStatistic) options.getStat();

		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : options.getDatasets()){

			Object[] popData = new Object[options.datasetCount()];

			int i = 0;
			boolean getPValue = false;
			for(AnalysisDataset dataset2 : options.getDatasets()){

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
	 * Carry out pairwise wilcoxon rank-sum test on the given stat of the segments
	 * @param list the datasets to test
	 * @param stat the statistic to measure
	 * @param segName the segment to create the table for
	 * @return a tablemodel for display
	 */	
	private TableModel createWilcoxonSegmentStatTable(TableOptions options) throws Exception {
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
		
		SegmentStatistic stat = (SegmentStatistic) options.getStat();
		
		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : options.getDatasets()){
			
			Object[] popData = new Object[options.datasetCount()];
			
			NucleusBorderSegment medianSeg1 = dataset.getCollection()
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getSegmentAt(options.getSegPosition());

			int i = 0;
			boolean getPValue = false;
			for(AnalysisDataset dataset2 : options.getDatasets()){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
					getPValue = true;
				} else {
					
					NucleusBorderSegment medianSeg2 = dataset2.getCollection()
							.getProfileCollection(ProfileType.REGULAR)
							.getSegmentedProfile(BorderTag.REFERENCE_POINT)
							.getSegmentAt(options.getSegPosition());
					
					popData[i] = df.format( runWilcoxonTest( 
							dataset.getCollection().getSegmentStatistics(SegmentStatistic.LENGTH, MeasurementScale.PIXELS, medianSeg1.getID()),

							dataset2.getCollection().getSegmentStatistics(SegmentStatistic.LENGTH, MeasurementScale.PIXELS, medianSeg2.getID()),
							
							getPValue) );
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
		}
		return model;
	}

	/**
	 * Generate a table of magnitude difference between datasets
	 * @param options the table options
	 * @return a tablemodel for display
	 */	
	public TableModel createMagnitudeStatisticTable(TableOptions options) throws Exception{
		
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		if(options.getStat().getClass()==NucleusStatistic.class){
			return createMagnitudeNuclearStatTable(options);
		}
		
		if(options.getStat().getClass()==SegmentStatistic.class){
			return createMagnitudeSegmentStatTable(options);
		}
		
		return makeEmptyWilcoxonTable(null);
		
	}
	
	
	/**
	 * Generate a table of magnitude difference between datasets
	 * @param list the datasets to test
	 * @param stat the statistic to measure
	 * @return a tablemodel for display
	 */	
	private TableModel createMagnitudeNuclearStatTable(TableOptions options) throws Exception {
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
		
		NucleusStatistic stat = (NucleusStatistic) options.getStat();

		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : options.getDatasets()){

			double value1 =  dataset.getCollection().getMedianStatistic(stat, MeasurementScale.PIXELS);
			
			Object[] popData = new Object[options.datasetCount()];

			int i = 0;

			for(AnalysisDataset dataset2 : options.getDatasets()){

				if(dataset2.getUUID().equals(dataset.getUUID())){
					
					popData[i] = "";

				} else {
					
					double value2 =  dataset2.getCollection().getMedianStatistic(stat, MeasurementScale.PIXELS);
					
					double magnitude = value2 / value1;
					popData[i] = df.format( magnitude );
				}
				i++;
			}
			model.addColumn(dataset.getName(), popData);
		}
		return model;
	}
	
	/**
	 * Generate a table of segmment magnitude differences between datasets
	 * @param list the datasets to test
	 * @param stat the statistic to measure
	 * @param segName the segment to create the table for
	 * @return a tablemodel for display
	 */	
	private TableModel createMagnitudeSegmentStatTable(TableOptions options) throws Exception {
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
		
		SegmentStatistic stat = (SegmentStatistic) options.getStat();
		
		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(AnalysisDataset dataset : options.getDatasets()){
			
			NucleusBorderSegment medianSeg1 = dataset.getCollection()
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getSegmentAt(options.getSegPosition());
									
		
			
			double value1 = Stats.quartile( dataset.getCollection()
					.getSegmentStatistics(SegmentStatistic.LENGTH, MeasurementScale.PIXELS, medianSeg1.getID()), Constants.MEDIAN);

			Object[] popData = new Object[options.datasetCount()];

			int i = 0;

			for(AnalysisDataset dataset2 : options.getDatasets()){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";

				} else {
					
					NucleusBorderSegment medianSeg2 = dataset2.getCollection()
							.getProfileCollection(ProfileType.REGULAR)
							.getSegmentedProfile(BorderTag.REFERENCE_POINT)
							.getSegmentAt(options.getSegPosition());
					
					double value2 = Stats.quartile( dataset2.getCollection()
							.getSegmentStatistics(SegmentStatistic.LENGTH, MeasurementScale.PIXELS, medianSeg2.getID()), Constants.MEDIAN);

					double magnitude = value2 / value1;
					popData[i] = df.format( magnitude );
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
	public TableModel createClusterOptionsTable(List<AnalysisDataset> list){
		DefaultTableModel model = new DefaultTableModel();

		List<Object> columnList = new ArrayList<Object>();
		columnList.add("Cluster group");
		columnList.add("Clusters found");
		columnList.add("Method");
		columnList.add("Iterations");
		columnList.add("Hierarchical method");
		columnList.add("Target cluster number");
		columnList.add("Include profile");

		for(NucleusStatistic stat : NucleusStatistic.values()){
			columnList.add("Include "+stat.toString());
		}
		
		columnList.add("Include segments");
		columnList.add("Tree");

		model.addColumn("", columnList.toArray());
		
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
									
					List<Object> dataList = new ArrayList<Object>();
					dataList.add(g.getName());
					dataList.add(g.size());
					dataList.add(op.getType().toString());
					dataList.add(iterationString);
					dataList.add(hierarchicalMethodString);
					dataList.add(hierarchicalClusterString);
					dataList.add(op.isIncludeProfile());
					for(NucleusStatistic stat : NucleusStatistic.values()){
						try{
							dataList.add(op.isIncludeStatistic(stat));
						} catch(NullPointerException e){
							dataList.add("N/A");
						}
					}
					
					boolean seg = false;
					for(UUID id : op.getSegments()){
						if(op.isIncludeSegment(id)){
							seg=true;
						}
					}
					dataList.add(seg);
					dataList.add(tree);
					
//					
					model.addColumn(dataset.getName(), dataList.toArray());
				}
			}
		}
		return model;	
	}

}
