/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs.editing;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.SegmentationHandler;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.BorderTag.BorderTagType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.components.BorderTagEvent;
import com.bmskinner.nuclear_morphology.gui.events.BorderTagEventListener;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEventListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.EditingTabPanel;

@SuppressWarnings("serial")
public abstract class AbstractEditingPanel extends DetailPanel
        implements SegmentEventListener, BorderTagEventListener, EditingTabPanel {  
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    
    public AbstractEditingPanel(@NonNull InputSupplier context, String title){
        super(context, title);
    }

    /**
     * Check if any of the cells in the active collection are locked for
     * editing. If so, ask the user whether to unlock all cells, or leave cells
     * locked.
     */
    @Override
	public void checkCellLock() {
    	if(activeDataset()==null)
    		return;
        ICellCollection collection = activeDataset().getCollection();

        if (collection.isVirtual())
            return;

        if (collection.hasLockedCells()) {
            String[] options = { "Keep manual values", "Overwrite manual values" };

            try {
            	int result = getInputSupplier().requestOptionAllVisible(options, 0, "Some cells have been manually segmented. Keep manual values?", "Keep manual values?");
            	if (result != 0)
            		collection.setCellsLocked(false);
            } catch(RequestCancelledException e) {} // no action
        }
    }

    /**
     * Update the border tag in the median profile to the given index, and
     * update individual nuclei to match.
     * 
     * @param tag
     * @param newTagIndex
     */
    @Override
	public void setBorderTagAction(@NonNull Tag tag, int newTagIndex) {
    	if(activeDataset()==null)
    		return;
        if (activeDataset().getCollection().isVirtual() && tag.equals(Tag.REFERENCE_POINT)) {
            LOGGER.warning("Cannot update core border tag for a child dataset");
            return;
        }

        checkCellLock();

        LOGGER.info("Updating " + tag + " to index " + newTagIndex);

        setAnalysing(true);

        SegmentationHandler sh = new SegmentationHandler(activeDataset());
        sh.setBorderTag(tag, newTagIndex);

        refreshChartCache(); // immediate visualisation of result

        if (tag.type().equals(BorderTagType.CORE)) {
            this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SEGMENTATION_ACTION, getDatasets());
        } else {
            getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
        }

        this.setAnalysing(false);

    }

    /**
     * This triggers a general chart recache for the active dataset and all its
     * children, but performs the recache on the currnt tab first so results are
     * showed at once
     */
    protected void refreshEditingPanelCharts() {
        this.refreshChartCache();

        List<IAnalysisDataset> list = new ArrayList<>();

        list.addAll(getDatasets());
        list.addAll(activeDataset().getAllChildDatasets());

        this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, list);
    }

    /**
     * Update the start index of the given segment to the given index in the
     * median profile, and update individual nuclei to match
     * 
     * @param id
     * @param index
     * @throws Exception
     */
    @Override
	public void updateSegmentStartIndexAction(@NonNull UUID id, int index) throws Exception {

        checkCellLock();

        SegmentationHandler sh = new SegmentationHandler(activeDataset());
        sh.updateSegmentStartIndexAction(id, index);

        refreshEditingPanelCharts();

        this.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_MORPHOLOGY, getDatasets());

    }
    
    @Override
    public void borderTagEventReceived(BorderTagEvent event) {
    }

    @Override
    public void segmentEventReceived(SegmentEvent event) {
    }
    
    

}
