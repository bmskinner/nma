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

package gui.tabs.editing;

import java.util.UUID;

import gui.BorderTagEventListener;
import gui.DatasetEvent;
import gui.SegmentEvent;
import gui.SegmentEventListener;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.BorderTagEvent;
import gui.tabs.DetailPanel;

import javax.swing.JOptionPane;

import components.CellCollection;
import components.ICellCollection;
import components.active.ProfileableCellularComponent.IndexOutOfBoundsException;
import components.generic.BorderTagObject;
import components.generic.BorderTag.BorderTagType;
import components.generic.Tag;

@SuppressWarnings("serial")
public abstract class AbstractEditingPanel extends DetailPanel implements SegmentEventListener, BorderTagEventListener{
	
	/**
	 * Check if any of the cells in the active collection are locked for
	 * editing. If so, ask the user whether to unlock all cells,
	 * or leave cells locked.
	 */
	protected void checkCellLock(){
		ICellCollection collection = activeDataset().getCollection();
		
		if(collection.hasLockedCells()){
			Object[] options = { "Keep manual values" , "Overwrite manual values" };
			int result = JOptionPane.showOptionDialog(null,
					"Some cells have been manually segmented. Keep manual values?", 
					"Overwrite manually segmented cells?",
					JOptionPane.DEFAULT_OPTION, 
					JOptionPane.QUESTION_MESSAGE,
					null, options, options[0]);
			
			if(result!=0){
				collection.setCellsLocked(false);
			}
		}
	}
	
	/**
	 * Update the border tag in the median profile to the given index, 
	 * and update individual nuclei to match.
	 * @param tag
	 * @param newTagIndex
	 */
	protected void setBorderTagAction(Tag tag, int newTagIndex){

		if(tag==null){
			fine("Tag is null");
			return;
		}
			
		checkCellLock();

		log("Updating "+tag+" to index "+newTagIndex);

		setAnalysing(true);

		try {
			activeDataset()
				.getCollection()
				.getProfileManager()
				.updateBorderTag(tag, newTagIndex);
		} catch (IndexOutOfBoundsException e) {
			warn("Unable to update border tag index");
			fine("Index not in profile", e);
			return;
		}


		refreshChartCache();

		if(tag.type().equals(BorderTagType.CORE)){
			log("Resegmenting dataset");
			fireDatasetEvent(DatasetEvent.REFRESH_MORPHOLOGY, getDatasets());
		} else {					
			fine("Firing refresh cache request for loaded datasets");
			fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
		}

		this.setAnalysing(false);

	}
	
	/**
	 * Update the start index of the given segment to the given index in the 
	 * median profile, and update individual nuclei to match
	 * @param id
	 * @param index
	 * @throws Exception
	 */
	protected void updateSegmentStartIndexAction(UUID id, int index) throws Exception{

		checkCellLock();
		
		// Update the median profile
		activeDataset()
			.getCollection()
			.getProfileManager()
			.updateMedianProfileSegmentIndex(true, id, index); // DraggablePanel always uses seg start index

		// Lock all the segments except the one to change
		activeDataset()
			.getCollection()
			.getProfileManager()
			.setLockOnAllNucleusSegmentsExcept(id, true);
						
		/*
		 * Invalidate the chart cache for the active dataset.
		 * This will force the morphology refresh to create a new chart
		 */
		finest("Clearing chart cache for editing panel");
		fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
		
		
		//  Update each nucleus profile
		finest("Firing refresh morphology action");
		fireDatasetEvent(DatasetEvent.REFRESH_MORPHOLOGY, getDatasets());

		
	}
	
	@Override
	public void borderTagEventReceived(BorderTagEvent event) {
		fine("Border tag event heard, not responding: "+this.getClass().getSimpleName());
	}
	
	@Override
	public void segmentEventReceived(SegmentEvent event) {
		fine("Segment event heard, not responding: "+this.getClass().getSimpleName());
	}

}
