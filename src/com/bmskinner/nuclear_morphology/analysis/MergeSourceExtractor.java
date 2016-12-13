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

package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.MergeSourceAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Extracts source datasets from merged datasets, and restores original 
 * segmentation patterns.
 * @author bms41
 * @since 1.13.3
 *
 */
public class MergeSourceExtractor implements Loggable {

	private final List<Object> datasetListeners   = new CopyOnWriteArrayList<Object>();
	private final List<IAnalysisDataset> list;
	
	/**
	 * Construct with a list of datasets that are to be extracted from 
	 * their parents.
	 * @param list the merge source datasets
	 */
	public MergeSourceExtractor( final List<IAnalysisDataset> list ){
		this.list = list;
	}
		
	/**
	 * Create a new root dataset for each merge source in this extractor
	 * and add it to the populations panel.
	 */
	public void extractSourceDataset(){
		Runnable task = () -> { 
			log("Recovering source dataset");
			for(IAnalysisDataset virtualMergeSource : list){
				
				ICellCollection templateCollection = virtualMergeSource.getCollection();
				// Make a new real cell collection from the virtual collection
				ICellCollection newCollection = new DefaultCellCollection(templateCollection.getFolder(), 
						null, 
						templateCollection.getName(), 
						templateCollection.getNucleusType()
						);
				
				for(ICell c : templateCollection.getCells()){
					
					newCollection.addCell(new DefaultCell(c));
				}
				
				IAnalysisDataset newDataset = new DefaultAnalysisDataset(newCollection);
				newDataset.setRoot(true);
				
				// Copy over the profile collections
				newDataset.getCollection().createProfileCollection();

				// Copy the merged dataset segmentation into the new dataset.
				// This wil match cell segmentations by default, since the cells 
				// have been copied from the merged dataset.
				if(virtualMergeSource instanceof MergeSourceAnalysisDataset){

					MergeSourceAnalysisDataset d = (MergeSourceAnalysisDataset) virtualMergeSource;
					
					try {
						log("Copying profile offsets");
						d.getParent().getCollection().getProfileManager().copyCollectionOffsets(newDataset.getCollection());
						
						
					} catch (ProfileException e) {
						error("Cannot copy profile offsets to recovered merge source", e);
					}
				}
				
				
				
				// Copy over the signal collections where appropriate
				
//				templateCollection.getSignalManager().copySignalGroups(newCollection);
				for(UUID signalGroupId : templateCollection.getSignalGroupIDs()){
					
					ISignalGroup newGroup;
					try {
						
						// We only want to make a signal group if a cell with the signal
						// is present in the merge source.
						boolean addSignalGroup = false;
						 for(Nucleus n : newCollection.getNuclei()){
							  addSignalGroup |=  n.getSignalCollection().hasSignal(signalGroupId);
						  } 
						 
						if(addSignalGroup){
							newGroup = new SignalGroup(templateCollection.getSignalGroup(signalGroupId));
							newDataset.getCollection().addSignalGroup(signalGroupId, newGroup);
						}

						
					} catch (UnavailableSignalGroupException e) {
						warn("Unable to copy signal groups");
						fine("Signal group not present", e);
					}
					
				}
				
				
				
				
				newDataset.setAnalysisOptions(virtualMergeSource.getAnalysisOptions());


				fireDatasetEvent(DatasetEvent.ADD_DATASET, newDataset);

			}

		};
		ThreadManager.getInstance().execute(task);
	}
	            
    public synchronized void addDatasetEventListener( DatasetEventListener l ) {
    	datasetListeners.add( l );
    }

    public synchronized void removeDatasetEventListener( DatasetEventListener l ) {
    	datasetListeners.remove( l );
    }

    /**
     * Fire an event on a single dataset.
     * @param method
     * @param dataset
     */
    private synchronized void fireDatasetEvent(String method, IAnalysisDataset dataset) {

    	List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
    	list.add(dataset);
    	fireDatasetEvent(method, list);
    }

    private synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list) {

    	DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list);
    	Iterator<Object> iterator = datasetListeners.iterator();
    	while( iterator.hasNext() ) {
    		( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
    	}
    }


}
