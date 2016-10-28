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

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import charting.options.DefaultTableOptions;
import charting.options.DefaultTableOptions.TableType;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import analysis.IAnalysisDataset;
import components.CellCollection;
import components.ClusterGroup;
import components.ICellCollection;
import components.IClusterGroup;
import components.generic.BorderTagObject;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderSegment;
import components.nuclear.NucleusBorderSegment;
import stats.ConfidenceInterval;
import stats.DipTester;
import stats.Mean;
import stats.NucleusStatistic;
import stats.Quartile;
import stats.SegmentStatistic;
import stats.Stats;
import utility.Constants;

public class AnalysisDatasetTableCreator extends AbstractDatasetCreator {
	
	protected final DefaultTableOptions options;

	/**
	 * Create with a set of table options
	 * @param o
	 */
	public AnalysisDatasetTableCreator(final DefaultTableOptions o){
		if(o==null){
			throw new IllegalArgumentException("Options cannot be null");
		}
		options = o;
	}
		
	public TableModel createMedianProfileStatisticTable() throws Exception{
		
		if( ! options.hasDatasets()){
			return createBlankTable();
		}
		
		if(options.isSingleDataset()){
			return createMedianProfileSegmentStatsTable(options.firstDataset(), options.getScale());
		}
		
		if(options.isMultipleDatasets()){
			return createMultiDatasetMedianProfileSegmentStatsTable();
		}
		
		return createBlankTable();
	}
	
	public TableModel createMergeSourcesTable(){
		
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

			for( IAnalysisDataset mergeSource : options.firstDataset().getMergeSources()){
//				AnalysisDataset mergeSource = options.firstDataset().getMergeSource(id);
				names.add(mergeSource.getName());
				nuclei.add(mergeSource.getCollection().size());
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
	private TableModel createMedianProfileSegmentStatsTable(IAnalysisDataset dataset, MeasurementScale scale) throws Exception {

		DefaultTableModel model = new DefaultTableModel();

		if(dataset==null){
			model.addColumn("No data loaded");

		} else {
			ICellCollection collection = dataset.getCollection();
			// check which reference point to use
			Tag point = Tag.REFERENCE_POINT;

			// get mapping from ordered segments to segment names
			List<IBorderSegment> segments = collection.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, point, Quartile.MEDIAN)
					.getOrderedSegments();
			
			// create the row names
			Object[] fieldNames = {
					"Colour",
					"Length",
					"Start index",
					"End index",
					"Mean length ("+ scale+")",
					"Mean length 95% CI ("+ scale+")",
					"Length std err. ("+ scale+")",
					"Length p(unimodal)"
			};

			model.addColumn("", fieldNames);
						
//			DecimalFormat df = new DecimalFormat("#.##");
//			df.setMaximumFractionDigits(2);
//			df.setMinimumFractionDigits(2);
//			df.setMinimumIntegerDigits(1);
			DecimalFormat pf = new DecimalFormat("#.###");

			for(IBorderSegment segment : segments) {

				List<Object> rowData = new ArrayList<Object>(0);
				
				rowData.add("");
				rowData.add(segment.length());
				rowData.add(segment.getStartIndex());
				rowData.add(segment.getEndIndex());
							
				double[] meanLengths = collection.getMedianStatistics(SegmentStatistic.LENGTH, scale, segment.getID());

				double mean = new Mean(meanLengths).doubleValue(); 

				double sem  = Stats.stderr(meanLengths);
				
				ConfidenceInterval ci = new ConfidenceInterval(meanLengths, 0.95);
				
				rowData.add(  DEFAULT_DECIMAL_FORMAT.format(mean ) );
				rowData.add(  DEFAULT_DECIMAL_FORMAT.format(ci.getLower().doubleValue())+" - "+ DEFAULT_DECIMAL_FORMAT.format(ci.getUpper().doubleValue()));
				rowData.add(  DEFAULT_DECIMAL_FORMAT.format(sem) );
				
				
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
	private TableModel createMultiDatasetMedianProfileSegmentStatsTable() throws Exception {

		List<IAnalysisDataset> list = options.getDatasets();
		MeasurementScale scale = options.getScale();
		
		DefaultTableModel model = new DefaultTableModel();
//		DecimalFormat df = new DecimalFormat("0.00");
//		df.setMinimumFractionDigits(2);
//		df.setMaximumFractionDigits(2);
//		df.setMinimumIntegerDigits(1);
		
		// If the datasets have different segment counts, show error message
		if( ! IBorderSegment.segmentCountsMatch(list)){
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
		
		BorderTagObject point = Tag.REFERENCE_POINT;//.ORIENTATION_POINT;

		// assumes all datasets have the same number of segments
		List<IBorderSegment> segments = list.get(0)
				.getCollection()
				.getProfileCollection()
				.getSegments(point);


		// Add the dataset names column
		fieldNames.add("Dataset");
		for(IBorderSegment segment : segments) {
			fieldNames.add(segment.getName());
		}
		model.setColumnIdentifiers(fieldNames.toArray());;

		// Add the segment colours column
		List<Object> colours = new ArrayList<Object>(0);
		colours.add("");

		
		for(int i=0; i<segments.size();i++) {
			colours.add("");
		}
		model.addRow(colours.toArray(new Object[0]));

		// Add the segment stats columns

		for(IAnalysisDataset dataset : list){

			ICellCollection collection = dataset.getCollection();		

			// get the offset segments
			//				List<NucleusBorderSegment> segs = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);

			List<IBorderSegment> segs = collection
					.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, point, Quartile.MEDIAN)
					.getOrderedSegments();

			List<Object> rowData = new ArrayList<Object>(0);
			rowData.add(dataset.getName());

			for(IBorderSegment segment : segs) {

				double[] meanLengths = collection.getMedianStatistics(SegmentStatistic.LENGTH, scale, segment.getID());

				double mean = new Mean(meanLengths).doubleValue(); 

				ConfidenceInterval ci = new ConfidenceInterval(meanLengths, 0.95);
				rowData.add(DEFAULT_DECIMAL_FORMAT.format(mean)+" ± "+ DEFAULT_DECIMAL_FORMAT.format(ci.getSize().doubleValue()));
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
	public TableModel createAnalysisTable() {

		if( ! options.hasDatasets()){
			finest("No datasets, creating blank table");
			return createBlankTable();
		} 
		
		finest("Table options type "+options.getType());
		
		if(options.getType().equals(TableType.ANALYSIS_PARAMETERS)){
			finest("Creating analysis parameters table model");
			return createAnalysisParametersTable();
		}
		
		if(options.getType().equals(TableType.ANALYSIS_STATS)){
			finest("Creating analysis stats table model");
			return createStatsTable();
		}
		
		return createBlankTable();
	}
	
	
	/**
	 * Create a table model of analysis parameters from a nucleus collection.
	 * If null parameter is passed, will create an empty table
	 * @param collection
	 * @return
	 */
	private TableModel createAnalysisParametersTable(){

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


		List<IAnalysisDataset> list = options.getDatasets();

		for(IAnalysisDataset dataset : list){

			// only display if there are options available
			// This may not be the case for a merged dataset or its children
			if( dataset.hasAnalysisOptions()){ 

				// Do not provide an options as an argument here.
				// This will cause the existing dataset options to be used
				Object[] collectionData = formatAnalysisOptionsForTable(dataset, null);

				model.addColumn(dataset.getCollection().getName(), collectionData);

			} else {
				fine("No analysis options in dataset "+dataset.getName());
				Object[] collectionData =  new Object[columnData.length];
				if(dataset.hasMergeSources()){

					fine("Dataset has merge sources");				
					if( testMergedDatasetOptionsAreSame(dataset)){
						
						fine("Merge source options are the same");

						// The options are the same in all merge sources
						// Show the first options from the first source

						AnalysisOptions op = dataset.getAllMergeSources().get(0).getAnalysisOptions();

						// Provide an options to be used
						collectionData = formatAnalysisOptionsForTable(dataset, op);

					} else {
						// Merge sources have different options
						Arrays.fill(collectionData, "N/A - merged");
						fine("Merge source options are different");	
					}

				} else {
					// there are no options to use; fill blank
					Arrays.fill(collectionData, "N/A");
					fine("No merge sources, and no options");	
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
	private boolean testMergedDatasetOptionsAreSame(IAnalysisDataset dataset){
		finest( "Testing merged dataset options for "+dataset.getName() );
		List<IAnalysisDataset> list = dataset.getMergeSources();
		
		boolean ok = true;

		for(IAnalysisDataset d1 : list){
			
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
				
				for(IAnalysisDataset d2 : list){
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
	private Object[] formatAnalysisOptionsForTable(IAnalysisDataset dataset, AnalysisOptions options){
		options = options == null ? dataset.getAnalysisOptions() : options;
		
//		DecimalFormat df = new DecimalFormat("#0.00"); 
		
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
				DEFAULT_DECIMAL_FORMAT.format(options.getMinNucleusCirc()),
				DEFAULT_DECIMAL_FORMAT.format(options.getMaxNucleusCirc()),
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
	private TableModel createStatsTable() {

		if( ! options.hasDatasets()){
			return createBlankTable();
		} 
		
		DefaultTableModel model = new DefaultTableModel();
		
		List<IAnalysisDataset> list = options.getDatasets();
		
		List<Object> columnData = new ArrayList<Object>();	
		columnData.add("Nuclei");
		for(NucleusStatistic stat : NucleusStatistic.values()){
			columnData.add(stat.toString()+" median");
			columnData.add(stat.toString()+" mean");
			columnData.add(stat.toString()+" S.E.M.");
			columnData.add(stat.toString()+" mean 95% CI");
			columnData.add(stat.toString()+" p(unimodal)");
		}


		model.addColumn("", columnData.toArray());
		
		finest("Created model row headers");

		finest("Making column for each dataset");
		for(IAnalysisDataset dataset : list){
			
			finest("Making column for "+dataset.getName());
			
			List<Object> datasetData = createDatasetStatsTableColumn(dataset, options.getScale());

			model.addColumn(dataset.getName(), datasetData.toArray());
			finest("Added column for "+dataset.getName());
		}
		finest("Created table model");
		
		return model;	
	}
	
	private List<Object> createDatasetStatsTableColumn(IAnalysisDataset dataset, MeasurementScale scale){
		
		// format the numbers and make into a tablemodel
//		DecimalFormat df = new DecimalFormat("#0.00"); 
		DecimalFormat pf = new DecimalFormat("#0.000"); 
		
		ICellCollection collection = dataset.getCollection();

		List<Object> datasetData = new ArrayList<Object>();			
//		double signalPerNucleus = (double) collection.getSignalManager().getSignalCount()/  (double) collection.getNucleusCount();

		datasetData.add(collection.size());

		for(NucleusStatistic stat : NucleusStatistic.values()){
			double[] stats 	= collection.getMedianStatistics(stat, scale);
			
			double mean     = new Mean(stats).doubleValue(); 
			double sem      = Stats.stderr(stats);
			double median 	= new Quartile(stats, Quartile.MEDIAN).doubleValue();
			
			ConfidenceInterval ci = new ConfidenceInterval(stats, 0.95);
			String ciString = DEFAULT_DECIMAL_FORMAT.format(mean)+" ± "+ DEFAULT_DECIMAL_FORMAT.format(ci.getSize().doubleValue());
			double diptest 	= DipTester.getDipTestPValue(stats);

			datasetData.add(DEFAULT_DECIMAL_FORMAT.format(median));
			datasetData.add(DEFAULT_DECIMAL_FORMAT.format(mean));
			datasetData.add(DEFAULT_DECIMAL_FORMAT.format(sem));
			datasetData.add(ciString);
			datasetData.add(pf.format(diptest));					
		}
		
		return datasetData;
		
	}
	
	public TableModel createVennTable(){
		
				
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
		
		List<IAnalysisDataset> list = options.getDatasets();
		
		// set rows
		Object[] columnData = new Object[list.size()];
		int row = 0;
		for(IAnalysisDataset dataset : list){
			columnData[row] = dataset.getName();
			row++;
		}
		model.addColumn("Population", columnData);
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		// add columns
		for(IAnalysisDataset dataset : list){
			
			finest("Fetching comparisons for "+dataset.getName());
			
			Object[] popData = new Object[list.size()];
			
			int i = 0;
			for(IAnalysisDataset dataset2 : list){
								
				String valueString = "";
				
				if( ! dataset2.getUUID().equals(dataset.getUUID())){	

					int shared = dataset.getCollection().countShared(dataset2);

					double pct = ((double) shared / (double) dataset2.getCollection().size())*100;
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
	public TableModel createPairwiseVennTable() {
				
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
			
		
		List<IAnalysisDataset> list = options.getDatasets();
		// Track the pairwase comparisons performed to avoid duplicates
		Map<UUID, ArrayList<UUID>> existingMatches = new HashMap<UUID, ArrayList<UUID>>();
		
		// add columns
		for(IAnalysisDataset dataset1 : list){
			
			ArrayList<UUID> set1List = new ArrayList<UUID>();
			existingMatches.put(dataset1.getUUID(), set1List);
			
			for(IAnalysisDataset dataset2 : list){

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
					int shared = dataset1.getCollection().countShared(dataset2);

					popData[4] = shared;

					int unique1 = dataset1.getCollection().size() - shared;
					int unique2 = dataset2.getCollection().size() - shared; 
					popData[2] = unique1;
					popData[6] = unique2;
					
					double uniquePct1 = ((double) unique1 / (double) dataset1.getCollection().size())*100;
					double uniquePct2 = ((double) unique2 / (double) dataset2.getCollection().size())*100;
					
					popData[1] = DEFAULT_DECIMAL_FORMAT.format(uniquePct1);
					popData[7] = DEFAULT_DECIMAL_FORMAT.format(uniquePct2);
					
					
					double sharedpct1 = ((double) shared / (double) dataset1.getCollection().size())*100;
					double sharedpct2 = ((double) shared / (double) dataset2.getCollection().size())*100;
					
					popData[3] = DEFAULT_DECIMAL_FORMAT.format(sharedpct1);
					popData[5] = DEFAULT_DECIMAL_FORMAT.format(sharedpct2);

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
	private DefaultTableModel makeEmptyWilcoxonTable(List<IAnalysisDataset> list){
		DefaultTableModel model = new DefaultTableModel();

		if(list==null){
			Object[] columnData = { "" };
			model.addColumn("Population", columnData );
			model.addColumn("", columnData );
		} else {

			// set rows
			Object[] columnData = new Object[list.size()];
			int row = 0;
			for(IAnalysisDataset dataset : list){
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
	public TableModel createWilcoxonStatisticTable() throws Exception{
		
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		if(options.getStat().getClass()==NucleusStatistic.class){
			return createWilcoxonNuclearStatTable();
		}
		
		if(options.getStat().getClass()==SegmentStatistic.class){
			return createWilcoxonSegmentStatTable();
		}
		
		return makeEmptyWilcoxonTable(null);
		
	}
	
	/**
	 * Carry out pairwise wilcoxon rank-sum test on the given stat of the given datasets
	 * @param list the datasets to test
	 * @param stat the statistic to measure
	 * @return a tablemodel for display
	 */	
	private TableModel createWilcoxonNuclearStatTable() throws Exception {
		
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
		
		NucleusStatistic stat = (NucleusStatistic) options.getStat();

		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(IAnalysisDataset dataset : options.getDatasets()){

			Object[] popData = new Object[options.datasetCount()];

			int i = 0;
			boolean getPValue = false;
			for(IAnalysisDataset dataset2 : options.getDatasets()){

				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
					getPValue = true;
				} else {
					popData[i] = df.format( runWilcoxonTest( 
							dataset.getCollection().getMedianStatistics(stat, MeasurementScale.PIXELS), 
							dataset2.getCollection().getMedianStatistics(stat, MeasurementScale.PIXELS), 
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
	private TableModel createWilcoxonSegmentStatTable() throws Exception {
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
		
		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(IAnalysisDataset dataset : options.getDatasets()){
			
			Object[] popData = new Object[options.datasetCount()];
			
			IBorderSegment medianSeg1 = dataset.getCollection()
					.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
					.getSegmentAt(options.getSegPosition());

			int i = 0;
			boolean getPValue = false;
			for(IAnalysisDataset dataset2 : options.getDatasets()){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";
					getPValue = true;
				} else {
					
					IBorderSegment medianSeg2 = dataset2.getCollection()
							.getProfileCollection()
							.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
							.getSegmentAt(options.getSegPosition());
					
					popData[i] = df.format( runWilcoxonTest( 
							dataset.getCollection().getMedianStatistics(SegmentStatistic.LENGTH, MeasurementScale.PIXELS, medianSeg1.getID()),

							dataset2.getCollection().getMedianStatistics(SegmentStatistic.LENGTH, MeasurementScale.PIXELS, medianSeg2.getID()),
							
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
	public TableModel createMagnitudeStatisticTable() throws Exception{
		
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		if(options.getStat() instanceof NucleusStatistic){
			return createMagnitudeNuclearStatTable();
		}
		
		if(options.getStat() instanceof SegmentStatistic){
			return createMagnitudeSegmentStatTable();
		}
		
		return makeEmptyWilcoxonTable(null);
		
	}
	
	
	/**
	 * Generate a table of magnitude difference between datasets
	 * @param list the datasets to test
	 * @param stat the statistic to measure
	 * @return a tablemodel for display
	 */	
	private TableModel createMagnitudeNuclearStatTable() throws Exception {
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
		
		NucleusStatistic stat = (NucleusStatistic) options.getStat();

		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(IAnalysisDataset dataset : options.getDatasets()){

			double value1 =  dataset.getCollection().getMedianStatistic(stat, MeasurementScale.PIXELS);
			
			Object[] popData = new Object[options.datasetCount()];

			int i = 0;

			for(IAnalysisDataset dataset2 : options.getDatasets()){

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
	private TableModel createMagnitudeSegmentStatTable() throws Exception {
		if( ! options.hasDatasets()){
			return makeEmptyWilcoxonTable(null);
		}
		
		DefaultTableModel model = makeEmptyWilcoxonTable(options.getDatasets());
				
		// add columns
		DecimalFormat df = new DecimalFormat("#0.0000"); 
		for(IAnalysisDataset dataset : options.getDatasets()){
			
			IBorderSegment medianSeg1 = dataset.getCollection()
					.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
					.getSegmentAt(options.getSegPosition());
									
		
			
			double value1 =  new Quartile( dataset.getCollection()
					.getMedianStatistics(SegmentStatistic.LENGTH, MeasurementScale.PIXELS, 
							medianSeg1.getID()), 
							Quartile.MEDIAN).doubleValue();

			Object[] popData = new Object[options.datasetCount()];

			int i = 0;

			for(IAnalysisDataset dataset2 : options.getDatasets()){
				
				if(dataset2.getUUID().equals(dataset.getUUID())){
					popData[i] = "";

				} else {
					
					IBorderSegment medianSeg2 = dataset2.getCollection()
							.getProfileCollection()
							.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
							.getSegmentAt(options.getSegPosition());
					
					double value2 = new Quartile( dataset2.getCollection()
							.getMedianStatistics(SegmentStatistic.LENGTH,
									MeasurementScale.PIXELS, 
									medianSeg2.getID()),
									Quartile.MEDIAN).doubleValue();

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
	public TableModel createClusterOptionsTable(){
		
		if( ! options.hasDatasets()){
			return createBlankTable();
		}
		
		DefaultTableModel model = new DefaultTableModel();

		List<Object> columnList = new ArrayList<Object>();
		columnList.add("Cluster group");
		columnList.add("Clusters found");
		columnList.add("Method");
		columnList.add("Iterations");
		columnList.add("Hierarchical method");
		columnList.add("Target cluster number");
		columnList.add("Include profile");
		columnList.add("Profile type");
		columnList.add("Include mesh");

		for(NucleusStatistic stat : NucleusStatistic.values()){
			columnList.add("Include "+stat.toString());
		}
		
		columnList.add("Include segments");
		columnList.add("Tree");

		model.addColumn("", columnList.toArray());

		List<IAnalysisDataset> list = options.getDatasets();



		// format the numbers and make into a tablemodel
		for(IAnalysisDataset dataset : list){
			List<ClusterGroup> clusterGroups = dataset.getClusterGroups();

			for(IClusterGroup g : clusterGroups ){
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

										String profileTypeString = op.isIncludeProfile() ? op.getProfileType().toString() : "N/A";
										dataList.add(profileTypeString);

										dataList.add(op.isIncludeMesh());

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

		return model;	
	}

}
