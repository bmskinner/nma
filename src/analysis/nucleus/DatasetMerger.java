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
package analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.Cell;
import components.CellCollection;
import components.nuclear.NucleusType;

public class DatasetMerger extends AnalysisWorker {

	private List<AnalysisDataset> datasets;
	private String function;
	
	private File saveFile;
	
	private List<AnalysisDataset> resultDatasets = new ArrayList<AnalysisDataset>();
	
	public static final String DATASET_MERGE = "merge";
	public static final String DATASET_SPLIT = "split";
	
	private static final int MAX_PROGRESS = 100;
	
	
	/**
	 * Create the merger or splitter for the given datasets. Call the appropriate funciton
	 * @param datasets
	 * @param function
	 * @param saveFile the file to save the new dataset as
	 */
	public DatasetMerger(List<AnalysisDataset> datasets, String function, File saveFile, Logger programLogger){
		super(datasets.get(0), programLogger);
		this.setProgressTotal(MAX_PROGRESS);
		this.datasets = datasets;
		this.function = function;
		this.saveFile = saveFile;
	}
	
	@Override
	protected Boolean doInBackground() {
		
		boolean result = false;
		if(function.equals(DATASET_MERGE)){
			result = merge();
		}
		
		return result;
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
		NucleusType testClass = datasets.get(0).getCollection().getNucleusType();
		for(AnalysisDataset d : datasets){

			if(!d.getCollection().getNucleusType().equals(testClass)){
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
			log(Level.INFO, "Prepare to merge");

			// check we are not merging a parent and child (would just get parent)
			if(datasets.size()==2){ 
				if(datasets.get(0).hasChild(datasets.get(1))  || datasets.get(1).hasChild(datasets.get(0)) ){
					log(Level.WARNING, "Merging parent and child would be silly.");
					return false;
				}
			}

			try{
				log(Level.FINE, "Finding new names");

				// Set the names of folders for the new collection

				String newDatasetName = saveFile.getName();
				File newDatasetFolder = saveFile.getParentFile();
				File newDatasetFile = saveFile;


				// ensure the file is valid
				newDatasetFile = checkName(newDatasetFile);

				newDatasetName = newDatasetFile.getName().replace(Constants.SAVE_FILE_EXTENSION, "");

				File mergeDebugFile = new File(newDatasetFolder+File.separator+newDatasetName+Constants.LOG_FILE_EXTENSION);

//				logger.log("Handing logging off to merged dataset debug file: "+mergeDebugFile.getAbsolutePath(), Logger.DEBUG);

//				logger = new Logger(mergeDebugFile, "DatasetMerger");

				log(Level.FINE, "Checked new file names");
				
				// check all collections are of the same type
				if(! checkNucleusClass()){
					log(Level.WARNING, "Error: cannot merge collections of different class");
					return false;
				}
				log(Level.FINE, "Checked nucleus classes match");

				// check if the new dataset should be root
				boolean newRoot = checkNewRootNeeded();
				log(Level.FINE, "Checked root status");

				// make a new collection based on the first dataset
				CellCollection templateCollection = datasets.get(0).getCollection();

				CellCollection newCollection = new CellCollection(newDatasetFolder, 
						null, 
						newDatasetName, 
						mergeDebugFile,
						templateCollection.getNucleusType()
						);

				log(Level.FINE, "Created collection");

				// add the cells from each population to the new collection
				log(Level.FINE, "Merging datasets");
				for(AnalysisDataset d : datasets){

					for(Cell n : d.getCollection().getCells()){
						if(!newCollection.getCells().contains(n)){
							newCollection.addCell(new Cell(n)); // make a copy of the cell so segments can be merged
						}
					}

				}


				// create the dataset; has no analysis options at present
				AnalysisDataset newDataset = new AnalysisDataset(newCollection);
				newDataset.setName(newDatasetName);
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
			} catch (Exception e){
				logError("Error in merging", e);
				return false;
			}

		} else {
			// there is only one datast
			log(Level.WARNING, "Cannot merge single dataset");
			return false;
		}
	}
	
	// check if the new dataset already exists
	private File checkName(File name){
		String fileName = name.getName();
		String datasetName = fileName.replace(Constants.SAVE_FILE_EXTENSION, "");
		
		File newFile = new File(name.getParentFile()+File.separator+datasetName+Constants.SAVE_FILE_EXTENSION);
		if(name.exists()){
			datasetName += "_1";
			newFile = new File(name.getParentFile()+File.separator+datasetName+Constants.SAVE_FILE_EXTENSION);
			newFile = checkName(newFile);
		}
		return newFile;
	}
	
	// Ensure the progress bar does something for debugging
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
