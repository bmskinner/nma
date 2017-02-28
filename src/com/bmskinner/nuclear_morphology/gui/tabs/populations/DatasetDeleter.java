/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.DatasetListManager;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Handle the deletion of selected datasets in the populations panel
 * @author bms41
 * @since 1.13.3
 *
 */
public class DatasetDeleter implements Loggable {
	
	private static final String DELETE_LBL = "Close";
	private static final String KEEP_LBL   = "Don't close";
	
	private static final String TITLE_LBL   = "Close dataset?";
	private static final String WARNING_LBL = "Dataset not saved. Close without saving?";
	
	/**
	 * Delete or close the given datasets
	 * @param datasets
	 */
	public void deleteDatasets(List<IAnalysisDataset> datasets){

		if(datasets==null || datasets.isEmpty()){
			fine("No datasets selected");
			return;
		}

		try {

			// Now extract the unique UUIDs of all datasets to be deleted (including children)
			fine("There are "+datasets.size()+" datasets selected");
			
			Deque<UUID> list = unique(datasets);
			
			
			// Ask before closing, if any root datasets have changed since
			// last save
			if(rootHasChanged(list)){
				warn("A root dataset has changed since last save");
				
				Object[] possibleValues = { KEEP_LBL, DELETE_LBL };
				
				Object selectedValue = JOptionPane.showOptionDialog(null, 
						WARNING_LBL, 
				        TITLE_LBL, 
				        JOptionPane.OK_CANCEL_OPTION, 
				        JOptionPane.INFORMATION_MESSAGE, 
				        null, 
				        possibleValues, // this is the array
				        KEEP_LBL);
								
				if(! selectedValue.equals(DELETE_LBL)){
					return;
				}
				
				
			}

			deleteDatasetsInList(list);
			DatasetListManager.getInstance().refreshClusters(); // remove unneeded cluster groups from datasets

			fine("Updating cluster groups in tree panel");
			fine("Deletion complete");
		} catch (Exception e){
			warn("Error deleting dataset");
			stack("Error deleting dataset", e);
		}

	}
	
	private boolean rootHasChanged(Deque<UUID> list){
		
		for(UUID id : list){
			
			IAnalysisDataset d = DatasetListManager.getInstance().getDataset(id);
			
			if(d.isRoot()){
			
				if(DatasetListManager.getInstance().hashCodeChanged(d)){
					return true;
				}
			}
		}
		return false;
	}

	private void deleteDataset(IAnalysisDataset d){

		fine("Deleting dataset: "+d.getName());
		UUID id = d.getUUID();


		// remove the dataset from its parents
		for(IAnalysisDataset parent : DatasetListManager.getInstance().getAllDatasets()){ //analysisDatasets.keySet()){

			fine("Parent dataset "+parent.getName());

			if(parent.hasChild(id)){
				fine("  "+parent.getName() + " is a parent to "+d.getName());
				parent.deleteChild(id);
			}

		}

		fine("Checking if dataset is root");

		if(d.isRoot()){
			
			DatasetListManager.getInstance().removeDataset(d);
		}
		
		fine("Clearing dataset from memory");

		d=null; // clear from memory
		fine("Deleted dataset");

	}

	/**
	 * Recursively delete datasets. Remove all datasets with no children
	 * from the list, then call this method again on all the remaining ids
	 * @param ids the dataset IDs to delete
	 */
	private void deleteDatasetsInList(Deque<UUID> ids){

		if(ids.isEmpty()){
			return;
		}


		fine("Candidate delete list has "+ids.size()+" datasets");

		UUID id = ids.removeFirst();

		IAnalysisDataset d = DatasetListManager.getInstance().getDataset(id);

		if( ! d.hasChildren()){
			deleteDataset(d);
		} else {
			fine(d.getName()+" still has children");
			ids.addLast(id); // put at the end of the deque to be handled last
		}

		deleteDatasetsInList(ids);
	}

	

	/**
	 * Get the list of unique datasets that must be removed
	 * @param list
	 * @return
	 */
	private Deque<UUID> unique(List<IAnalysisDataset> list){
		Set<UUID> set = new HashSet<UUID>();
		for(IAnalysisDataset d : list){
			fine("Selected dataset for deletion: "+d.getName());

			set.add(d.getUUID());

			if(d.hasChildren()){
				fine("Children found in: "+d.getName());
				// add all the children of a dataset
				for(UUID childID : d.getAllChildUUIDs()){
					set.add(childID);
				}
			} else {
				fine("No children in: "+d.getName());
			}
		}

		Deque<UUID> result = new ArrayDeque<UUID>();
		result.addAll(set);
		
		

		return result;
	}

}
