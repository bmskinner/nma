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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;

import charting.options.TableOptions;
import charting.options.TableOptions.TableType;
import analysis.AnalysisDataset;
import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import analysis.ClusteringOptions;
import analysis.SignalManager;
import analysis.ClusteringOptions.ClusteringMethod;
import components.CellCollection;
import components.ClusterGroup;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import ij.IJ;
import stats.DipTester;
import stats.NucleusStatistic;
import stats.SegmentStatistic;
import stats.Stats;
import utility.Constants;

public class NucleusTableDatasetCreator {
		
	public static TableModel createMedianProfileStatisticTable(TableOptions options) throws Exception{
		
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
	
	public static TableModel createBlankTable(){
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn("No data loaded");
		return model;
	}
	
	public static TableModel createMergeSourcesTable(TableOptions options){
		
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

			for( UUID id : options.firstDataset().getMergeSources()){
				AnalysisDataset mergeSource = options.firstDataset().getMergeSource(id);
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
	private static TableModel createMedianProfileSegmentStatsTable(AnalysisDataset dataset, MeasurementScale scale) throws Exception {

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
							
				double[] meanLengths = collection.getSegmentLengths(segment.getID(), scale);
//				double[] meanLengths = collection.getSegmentLengths(map.get(segment.getName()), scale);
				
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
	private static TableModel createMultiDatasetMedianProfileSegmentStatsTable(TableOptions options) throws Exception {

		List<AnalysisDataset> list = options.getDatasets();
		MeasurementScale scale = options.getScale();
		
		DefaultTableModel model = new DefaultTableModel();
		DecimalFormat df = new DecimalFormat("0.00");
		df.setMinimumFractionDigits(2);
		df.setMaximumFractionDigits(2);
		df.setMinimumIntegerDigits(1);

		List<Object> fieldNames = new ArrayList<Object>(0);
		if(list==null){
			model.addColumn("No data loaded");

		} else {
			
			BorderTag point = BorderTag.REFERENCE_POINT;//.ORIENTATION_POINT;
			
			// assumes all datasets have the same number of segments
			List<NucleusBorderSegment> segments = list.get(0)
					.getCollection()
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(point)
					.getOrderedSegments();
			

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
//				List<NucleusBorderSegment> segs = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegments(point);
				
				List<NucleusBorderSegment> segs = collection
						.getProfileCollection(ProfileType.REGULAR)
						.getSegmentedProfile(point)
						.getOrderedSegments();
				
				List<Object> rowData = new ArrayList<Object>(0);
				rowData.add(dataset.getName());

				for(NucleusBorderSegment segment : segs) {
					
					double[] meanLengths = collection.getSegmentLengths(segment.getID(), scale);
					double mean = Stats.mean(meanLengths); 

					double ci = Stats.calculateConfidenceIntervalSize(meanLengths, 0.95);
					
					rowData.add(df.format(mean)+" ± "+ df.format(ci));
				}
				model.addRow(rowData.toArray(new Object[0]));
			}
		}
		return model;	
	}
	
	/**
	 * Create a table model of analysis parameters or stats from datasets.
	 * If null parameter is passed, will create an empty table
	 * @param collection
	 * @return
	 */
	public static TableModel createAnalysisTable(TableOptions options) throws Exception{
		
		if(options.getType().equals(TableType.ANALYSIS_PARAMETERS)){
			return createAnalysisParametersTable(options);
		}
		
		if(options.getType().equals(TableType.ANALYSIS_STATS)){
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
	private static TableModel createAnalysisParametersTable(TableOptions options){

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
				"Shell analysis run",
				"Run date",
				"Run time",
				"Collection source",
				"Log file",
				"Type",
				"Version"};
		model.addColumn("", columnData);
		
		if( ! options.hasDatasets()){
			options.log(Level.FINE, "No datasets in options, returning blank parameter table");
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
	private static boolean testMergedDatasetOptionsAreSame(AnalysisDataset dataset){

		
		List<AnalysisDataset> list = dataset.getAllMergeSources();
		
		boolean ok = true;

		for(AnalysisDataset d1 : list){
			
			// If the dataset has merge sources, the options are null
			if(! d1.hasMergeSources()){
			
				for(AnalysisDataset d2 : list){

					if(! d2.hasMergeSources()){

						if( ! d1.getAnalysisOptions().equals(d2.getAnalysisOptions())){
							ok = false;
						}
					}
				}
			} else {
				ok = testMergedDatasetOptionsAreSame(d1);
			}

		}
		
		return ok;
	}
	
	/**
	 * Get an array of formatted info from a dataset analysis options
	 * @param dataset
	 * @return
	 */
	private static Object[] formatAnalysisOptionsForTable(AnalysisDataset dataset, AnalysisOptions options){
		options = options == null ? dataset.getAnalysisOptions() : options;
		
		DecimalFormat df = new DecimalFormat("#0.00"); 
		
		// only display refold mode if nucleus was refolded
		String refoldMode = options.refoldNucleus() 
				? options.getRefoldMode()
						: "N/A";

		String date;
		String time;
				
		if(dataset.hasMergeSources()){
			date = "N/A - merge";
			time = "N/A - merge";
			
		} else {
			String[] times = dataset.getCollection().getOutputFolderName().split("_");
			date = times[0];
			time = times[1];
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
				options.getAngleProfileWindowSize(),
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
				dataset.hasShellResult(),
				date,
				time,
				dataset.getCollection().getFolder(),
				dataset.getDebugFile().getAbsolutePath(),
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
	private static TableModel createStatsTable(TableOptions options) throws Exception {

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
		
		if(list==null){
			model.addColumn("No data loaded");
		} else {

			// format the numbers and make into a tablemodel
			DecimalFormat df = new DecimalFormat("#0.00"); 
			DecimalFormat pf = new DecimalFormat("#0.000"); 

			for(AnalysisDataset dataset : list){
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
				
				model.addColumn(collection.getName(), datasetData.toArray());
			}
		}
		return model;	
	}
	
	public static TableModel createVennTable(TableOptions options){
		DefaultTableModel model = new DefaultTableModel();
				
		if( ! options.hasDatasets()){
			Object[] columnData = {""};
			model.addColumn("Population", columnData );
			model.addColumn("", columnData );
			return model;
		}
		
		List<AnalysisDataset> list = options.getDatasets();
		
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
	public static TableModel createPairwiseVennTable(TableOptions options) {
				
		if( ! options.hasDatasets()){
			return createBlankTable();
		}
		
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
	 * @param options the table options
	 * @return a tablemodel for display
	 */	
	public static TableModel createWilcoxonStatisticTable(TableOptions options) throws Exception{
		
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
	private static TableModel createWilcoxonNuclearStatTable(TableOptions options) throws Exception {
		
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
	private static TableModel createWilcoxonSegmentStatTable(TableOptions options) throws Exception {
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
							 dataset.getCollection().getSegmentLengths(medianSeg1.getID(), MeasurementScale.PIXELS),
							dataset2.getCollection().getSegmentLengths(medianSeg2.getID(), MeasurementScale.PIXELS),
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
	public static TableModel createMagnitudeStatisticTable(TableOptions options) throws Exception{
		
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
	private static TableModel createMagnitudeNuclearStatTable(TableOptions options) throws Exception {
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
	private static TableModel createMagnitudeSegmentStatTable(TableOptions options) throws Exception {
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
					.getSegmentLengths(medianSeg1.getID(), MeasurementScale.PIXELS), Constants.MEDIAN);

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
					
					double value2 = Stats.quartile( dataset2.getCollection().getSegmentLengths(medianSeg2.getID(), MeasurementScale.PIXELS), Constants.MEDIAN);

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
	public static TableModel createClusterOptionsTable(List<AnalysisDataset> list){
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
