package no.analysis;

import ij.IJ;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import cell.Cell;
import no.collections.CellCollection;
import no.nuclei.sperm.RodentSpermNucleus;
import utility.Constants;
import utility.Logger;

public class DatasetMerger extends SwingWorker<Boolean, Integer> {
	
	private Logger logger;
	private List<AnalysisDataset> datasets;
	private String function;
	
	private List<AnalysisDataset> resultDatasets = new ArrayList<AnalysisDataset>();
	
	public static final String DATASET_MERGE = "merge";
	public static final String DATASET_SPLIT = "split";
	
	
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
//			IJ.log("Merging");
			result = merge();
//			IJ.log("Merge "+result);
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
		
		

		int percent = (int) ( (double) lastCycle / (double) 5 * 100);

		setProgress(percent); // the integer representation of the percent
	}
	
	@Override
	public void done() {
		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.PROGRESS_FINISHED);
			} else {
				firePropertyChange("Error", getProgress(), Constants.PROGRESS_ERROR);
			}
		} catch (InterruptedException e) {
			logger.log("Unable to refold nucleus: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (ExecutionException e) {
			logger.log("Unable to refold nucleus: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
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
	 * a child of an existing root
	 * @return
	 */
	private boolean checkNewRootNeeded(){
		boolean result = true;
		
		for(AnalysisDataset d : datasets){

			// check if a root population is included in the merge;
			// if so, we must make the result a root population too
			// otherwise, it may be a subpopulation
			if(d.isRoot()){
				result = true;
			}
		}
		return result;
	}
	
	private boolean merge(){
				
		if(datasets.size()>1){
			logger.log("Prepare to merge");
//			IJ.log("Preparing");

			if(datasets.size()==2){ // check we are not merging a parent and child (would just get parent)
				if(datasets.get(0).hasChild(datasets.get(1))  || datasets.get(1).hasChild(datasets.get(0)) ){
					//					log("No. That would be silly.");
					return false;
				}
			}

			logger.log("Merging collections");
			

			// check all collections are of the same type
//			IJ.log("Checking types");
			if(! checkNucleusClass()){
				logger.log("Error: cannot merge collections of different class");
				return false;
			}
			
			boolean newRoot = checkNewRootNeeded();
			
			
			publish(1);

//			AnalysisDataset mergeParent = null;
//			if(!newRoot) { // unless we have forced root above
//				// check if all datasets are children of one root dataset
//				for(AnalysisDataset parent : datasets){
//					if(parent.isRoot()){ // only look at top level datasets for now
//						boolean ok = true; 
//						for(AnalysisDataset d : datasets){
//							if(!parent.hasChild(d)){
//								ok = false;
//							}
//						}
//						if(ok){
//							mergeParent = parent;
//						}
//					}
//				}
//
//				// if a merge parent was found, new collection is not root
//				if(mergeParent!=null){
//					newRoot = false;
//				} else {
//					newRoot = true; // if we cannot find a consistent parent, make a new root population
//				}
//			}
			publish(2);

			// add the nuclei from each population to the new collection
			
			CellCollection templateCollection = datasets.get(0).getCollection();

			CellCollection newCollection = new CellCollection(templateCollection.getFolder(), 
					templateCollection.getOutputFolderName(), 
					"Merged", 
					templateCollection.getDebugFile(),
					templateCollection.getNucleusClass()
					);
			
//			IJ.log("Running merge");
			for(AnalysisDataset d : datasets){

				for(Cell n : d.getCollection().getCells()){
					if(!newCollection.getCells().contains(n)){
						newCollection.addCell(n);
					}
				}

			}
			
			publish(3);

			// create the dataset; has no analysis options at present
			AnalysisDataset newDataset = new AnalysisDataset(newCollection);
			newDataset.setName("Merge_of_datasets");
			newDataset.setRoot(newRoot);

			// if applicable, add the new dataset to a parent
			
//			IJ.log("Applying morphology");
//			if(newRoot==false && mergeParent!=null){
//
////				logc("Reapplying morphology...");
//				boolean ok = MorphologyAnalysis.reapplyProfiles(newCollection, mergeParent.getCollection());
//				if(ok){
////					log("OK");
//				} else {
////					log("Error");
//				}
//				newDataset.setAnalysisOptions(mergeParent.getAnalysisOptions());
//				newDataset.getAnalysisOptions().setRefoldNucleus(false);
//
//				mergeParent.addChildDataset(newDataset);
//			} else {
				
				// otherwise, it is a new root population
				// we need to run a fresh morphology analysis
//				logc("Running morphology analysis...");
				boolean ok = MorphologyAnalysis.run(newDataset.getCollection());
				if(!ok){
//					IJ.log("Error in morphology");
					return false;
				}
				publish(4);
//				IJ.log("Morphology applied");
				
				newDataset.setAnalysisOptions(datasets.get(0).getAnalysisOptions());
				newDataset.getAnalysisOptions().setRefoldNucleus(false);
//				IJ.log("Options set");
//			}

			// add the new collection to the list
//			populationsPanel.addDataset(newDataset);
//			populationsPanel.update();
			
			resultDatasets.add(newDataset);
//			IJ.log("Added dataset");
			publish(5);
			return true;

		} else {
			logger.log("Cannot merge single dataset");
			return false;
		}
//		return true;
	}
}
