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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import components.Cell;
import components.CellCollection;
import components.nuclear.NucleusType;
import components.nuclear.SignalGroup;

public class DatasetMerger extends AnalysisWorker {

	private List<AnalysisDataset> datasets;
	
	private File saveFile;
	
	private List<AnalysisDataset> resultDatasets = new ArrayList<AnalysisDataset>();
	
	private Map<UUID, Set<UUID>> pairedSignalGroups = null;
		
	private static final int MAX_PROGRESS = 100;
	
	
	/**
	 * Create the merger or splitter for the given datasets. Call the appropriate funciton
	 * @param datasets
	 * @param function
	 * @param saveFile the file to save the new dataset as
	 */
	public DatasetMerger(List<AnalysisDataset> datasets, File saveFile){
		super(datasets.get(0));
		this.setProgressTotal(MAX_PROGRESS);
		this.datasets = datasets;
		this.saveFile = saveFile;
	}
	
	public DatasetMerger(List<AnalysisDataset> datasets, File saveFile, Map<UUID, Set<UUID>> pairedSignalGroups){
		this(datasets, saveFile);
		this.pairedSignalGroups = pairedSignalGroups;
	}
	
	@Override
	protected Boolean doInBackground() {
		try {
			boolean result = merge();
			return result;
		} catch(Exception e){
			error("Error merging datasets", e);
			return false;
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
		NucleusType testClass = datasets.get(0).getCollection().getNucleusType();
		for(AnalysisDataset d : datasets){

			if(!d.getCollection().getNucleusType().equals(testClass)){
				result =  false;
			}
		}
		return result;
	}
		
	private boolean merge(){
				
		if(datasets.size() <= 1){
			warn("Must have multiple datasets to merge");
			return false;
		}
		
		// check we are not merging a parent and child (would just get parent)
		if(datasets.size()==2){ 
			if(datasets.get(0).hasChild(datasets.get(1))  || datasets.get(1).hasChild(datasets.get(0)) ){
				warn("Merging parent and child would be silly.");
				return false;
			}
		}

		try{
			fine("Finding new names");

			// Set the names for the new collection
			File   newDatasetFolder = saveFile.getParentFile();
			File   newDatasetFile   = saveFile;

			// ensure the new file name is valid
			newDatasetFile = checkName(newDatasetFile);

			String newDatasetName = newDatasetFile.getName().replace(Constants.SAVE_FILE_EXTENSION, "");
			fine("Checked new file names");

			// check all collections are of the same type
			if(! checkNucleusClass()){
				warn( "Error: cannot merge collections of different class");
				return false;
			}
			fine("Checked nucleus classes match");


//			// make a new collection based on the first dataset
			CellCollection templateCollection = datasets.get(0).getCollection();

			CellCollection newCollection = new CellCollection(newDatasetFolder, 
					null, 
					newDatasetName, 
					templateCollection.getNucleusType()
					);
			
			AnalysisDataset newDataset = performMerge(newCollection, datasets);

			resultDatasets.add(newDataset);


			spinWheels();

			return true;
		} catch (Exception e){
			logError("Error in merging", e);
			return false;
		}


	}
	
	/**
	 * Merge the given datasets, copying each cell into the new collection.
	 * @param newCollection
	 * @param sources
	 * @return
	 * @throws Exception 
	 */
	private AnalysisDataset performMerge(CellCollection newCollection, List<AnalysisDataset> sources) throws Exception{

		fine("Merging datasets");

		for(AnalysisDataset d : datasets){

			for(Cell cell : d.getCollection().getCells()){
				
				if(!newCollection.getCells().contains(cell)){
					
					Cell newCell = new Cell(cell);
					
					// remove all signal information
//					newCell.getNucleus().getSignalCollection().removeSignals();
					
					newCollection.addCell(newCell); // make a copy of the cell so segments can be merged
					
				}
			}
			
			// All all the exisiting signal groups before merging
			for(UUID signalGroupID : d.getCollection().getSignalGroupIDs()){
				newCollection.addSignalGroup(signalGroupID, new SignalGroup(d.getCollection().getSignalGroup(signalGroupID)));
			}
			

		}
		
		// Remove signal groups
		mergeSignalGroups(newCollection);
//		newCollection.getSignalManager().removeSignalGroups();
		
		// Remove existing profiles
//		newCollection.getProfileManager().removeProfiles();


		// create the dataset; has no analysis options at present
		AnalysisDataset newDataset = new AnalysisDataset(newCollection);
		newDataset.setRoot(true);

		// Add the original datasets as merge sources
		for(AnalysisDataset d : datasets){
			newDataset.addMergeSource(d);
		}

		// a merged dataset should not have analysis options
		// of its own; it lets each merge source display options
		// appropriately
		newDataset.setAnalysisOptions(null);
		
		return newDataset;
	}
	
	private void mergeSignalGroups(CellCollection newCollection){
		if(pairedSignalGroups==null){
			finer("No signal groups to merge");
			return;
		}
		
		// Decide which signal groups get which new ids
		// Key is old signal group. Entry is new id
		Map<UUID, UUID> mergedSignalGroups = new HashMap<UUID, UUID>();
		
		for(UUID id1 : pairedSignalGroups.keySet()){
			
			// If this id is not encountered, make a new one
			if( ! mergedSignalGroups.keySet().contains(id1)){
				mergedSignalGroups.put(id1, UUID.randomUUID());
			}
			
			UUID newID = mergedSignalGroups.get(id1);
			
			// All the set share this new id
			Set<UUID> id2Set = pairedSignalGroups.get(id1);
			for(UUID id2 : id2Set){
				mergedSignalGroups.put(id2, newID);
			}
		}
		
		// Now, all the old ids have a link to a new id
		// Update the signal groups in the merged dataset
		
		// Add the old signal groups to the new collection
		
		
		finer("Updating signal group ids");
		for(UUID oldID : mergedSignalGroups.keySet()){
			
			UUID newID = mergedSignalGroups.get(oldID);
			newCollection.getSignalManager().updateSignalGroupID(oldID, newID);
		}
		
		
		
	}
	
	// 
	/**
	 * Check if the new dataset filename already exists. If so,
	 * append _1 to the end and check again
	 * @param name
	 * @return
	 */
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
				logError("Thread interrupted", e);
			}
		}
	}
}
