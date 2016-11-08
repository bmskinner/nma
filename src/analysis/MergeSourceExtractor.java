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

package analysis;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.ThreadManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import logging.Loggable;
import analysis.profiles.ProfileException;
import components.ICell;
import components.ICellCollection;
import components.active.DefaultAnalysisDataset;
import components.active.DefaultCell;
import components.active.DefaultCellCollection;

/**
 * Extracts source datasets from merged datasets, and restores original 
 * segmentation patterns.
 * @author bms41
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
			for(IAnalysisDataset d : list){
				
				ICellCollection templateCollection = d.getCollection();
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
				
				newDataset.getCollection().createProfileCollection();
				
				try {
					log("Copying profile offsets");
					d.getCollection().getProfileManager().copyCollectionOffsets(newDataset.getCollection());
					
					//TODO update cells to the segment positions of the restored median profile
				} catch (ProfileException e) {
					error("Cannot copy profile offsets to recovered merge source", e);
				}
				
				newDataset.setAnalysisOptions(d.getAnalysisOptions());


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
