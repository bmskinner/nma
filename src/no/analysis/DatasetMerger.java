package no.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import cell.Cell;
import no.collections.CellCollection;
import utility.Constants;
import utility.Logger;

public class DatasetMerger extends SwingWorker<Boolean, Integer> {
	
	private Logger logger;
	private List<AnalysisDataset> datasets;
	private String function;
	
	private List<AnalysisDataset> resultDatasets = new ArrayList<AnalysisDataset>();
	
	public static final String DATASET_MERGE = "merge";
	public static final String DATASET_SPLIT = "split";
	
	private static final int MAX_PROGRESS = 100;
	
	
	/**
	 * Create the merger or splitter for the given datasets. Call the appropriate funciton
	 * @param datasets
	 * @param function
	 */
	public DatasetMerger(List<AnalysisDataset> datasets, String function){
		
		AnalysisDataset firstDataset = datasets.get(0);
		logger = new Logger(firstDataset.getDebugFile(), "DatasetMerger");
		this.datasets = datasets;
		this.function = function;
	}
	
	@Override
	protected Boolean doInBackground() {
		
		boolean result = false;
		if(function.equals(DATASET_MERGE)){
			result = merge();
		}
		
//		if(function.equals(DATASET_SPLIT)){
////			result = split();
//		}
		return result;
	}
	
	@Override
	protected void process( List<Integer> integers ) {
		//update the number of entries added
		int lastCycle = integers.get(integers.size()-1);
		int percent = (int) ( (double) lastCycle / (double) MAX_PROGRESS * 100);

		setProgress(percent); // the integer representation of the percent
	}
	
	@Override
	public void done() {
		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());
			} else {
				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
			}
		} catch (InterruptedException e) {
			logger.error("Unable to "+function+" datasets", e);
		} catch (ExecutionException e) {
			logger.error("Unable to "+function+" datasets", e);
		}
	} 
	
	
	public List<AnalysisDataset> getResults(){
		return resultDatasets;
	}
	
	/**
	 * Check if the nucleus classes of all datasets match
	 * Cannot merge collections with different classes
	 * @return ok or not
	 */
	private boolean checkNucleusClass(){
		boolean result = true;
		Class<?> testClass = datasets.get(0).getAnalysisOptions().getNucleusClass();
		for(AnalysisDataset d : datasets){

			if(d.getAnalysisOptions().getNucleusClass()!=testClass){
				result =  false;
			}
		}
		return result;
	}
	
	/**
	 * Check if a new root population must be created
	 * or whether the merged population should be
	 * a child of an existing root. Currently not used.
	 * @return true always
	 */
	private boolean checkNewRootNeeded(){
		boolean result = true;
		
//		for(AnalysisDataset d : datasets){
//
//			// check if a root population is included in the merge;
//			// if so, we must make the result a root population too
//			// otherwise, it may be a subpopulation
//			if(d.isRoot()){
//				result = true;
//			}
//		}
		return result;
	}
	
	private boolean merge(){
				
		if(datasets.size()>1){
			logger.log("Prepare to merge");

			// check we are not merging a parent and child (would just get parent)
			if(datasets.size()==2){ 
				if(datasets.get(0).hasChild(datasets.get(1))  || datasets.get(1).hasChild(datasets.get(0)) ){
					logger.log("Merging parent and child would be silly.");
					return false;
				}
			}

			// check all collections are of the same type
			if(! checkNucleusClass()){
				logger.log("Error: cannot merge collections of different class");
				return false;
			}
			
			// check if the new dataset should be root
			boolean newRoot = checkNewRootNeeded();

			// make a new collection based on the first dataset
			CellCollection templateCollection = datasets.get(0).getCollection();

			CellCollection newCollection = new CellCollection(templateCollection.getFolder(), 
					templateCollection.getOutputFolderName(), 
					"Merged", 
					templateCollection.getDebugFile(),
					templateCollection.getNucleusClass()
					);
			
			// add the cells from each population to the new collection
			logger.log("Merging datasets",Logger.DEBUG);
			for(AnalysisDataset d : datasets){

				for(Cell n : d.getCollection().getCells()){
					if(!newCollection.getCells().contains(n)){
						newCollection.addCell(n);
					}
				}

			}
			

			// create the dataset; has no analysis options at present
			AnalysisDataset newDataset = new AnalysisDataset(newCollection);
			newDataset.setName("Merge_of_datasets");
			newDataset.setRoot(newRoot);
			
			// Add the original datasets as merge sources
			for(AnalysisDataset d : datasets){
				newDataset.addMergeSource(d);
			}
			
			// a merged dataset should not have analysis options
			// of its own; it lets each merge source display options
			// appropriately
			newDataset.setAnalysisOptions(null);

			resultDatasets.add(newDataset);
			
			
			spinWheels();

			return true;

		} else {
			// there is only one datast
			logger.log("Cannot merge single dataset");
			return false;
		}
	}
	
	// Ensure the progress bar does something
	private void spinWheels(){
		for(int i=0; i<MAX_PROGRESS; i++){
			publish(i);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
