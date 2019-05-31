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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.Color;
import java.awt.Paint;
import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.utility.FileUtils;

/**
 * Handle cosmetic changes in datasets. Generates the dialogs
 * for confirmation.
 * @author bms41
 * @since 1.13.8
 *
 */
public class CosmeticHandler {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);
    
    private static final String CHOOSE_A_NEW_NAME_LBL = "Choose a new name";
	private final TabPanel parent;
    
    /**
     * Create the handler for a panel
     * @param p the panel to register the handler to 
     */
    public CosmeticHandler(@NonNull TabPanel p){
        parent = p;
    }
    
    /**
     * Choose a new scale for the dataset and apply it to all cells
     * 
     * @param dataset
     * @param row
     */
    public void changeDatasetScale(@NonNull IAnalysisDataset dataset) {
    	
    	try {
    		double initialScale = 1;
    		Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
    		if(op.isPresent()){
    			Optional<IDetectionOptions> nOp = op.get().getDetectionOptions(IAnalysisOptions.NUCLEUS);
    			if(nOp.isPresent())
    				initialScale = nOp.get().getScale();
    		}
    		
			double scale = parent.getInputSupplier().requestDouble(Labels.Cells.CHOOSE_NEW_SCALE_LBL, initialScale, 1, 100000, 1);
			dataset.setScale(scale);
		} catch (RequestCancelledException e) {
			return;
		}
    }
    
    /**
     * Make a JColorChooser for the given dataset, and set the color.
     * 
     * @param dataset
     * @param row
     */
    public void changeDatasetColour(@NonNull IAnalysisDataset dataset) {

    	int row = DatasetListManager.getInstance().getSelectedDatasets().indexOf(dataset);
    	Paint oldColour = dataset.getDatasetColour().orElse(ColourSelecter.getColor(row));

    	try {
    		Color newColor = parent.getInputSupplier().requestColor("Choose dataset colour", (Color) oldColour);
    		dataset.setDatasetColour(newColor);
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, dataset);
    		parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);

    	} catch(RequestCancelledException e) {
    		return;
    	}
    }

    /**
     * Rename an existing dataset and update the population list.
     * 
     * @param dataset the dataset to rename
     */
    public void renameDataset(@NonNull IAnalysisDataset dataset) {
        ICellCollection collection = dataset.getCollection();
        
        try {
    		String newName = parent.getInputSupplier().requestString(CHOOSE_A_NEW_NAME_LBL, collection.getName());
    		collection.setName(newName);
    		parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    	} catch(RequestCancelledException e) {}
    }
    
    /**
     * Rename an existing group and update the population list.
     * 
     * @param group the group to rename
     */
    public void renameClusterGroup(@NonNull IClusterGroup group) {

    	try {
    		String newName = parent.getInputSupplier().requestString(CHOOSE_A_NEW_NAME_LBL, group.getName());
    		group.setName(newName);
    		parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    	} catch(RequestCancelledException e) {
    		return;
    	}
    }
    
    /**
     * Rename an existing workspace and update the population list.
     * 
     * @param workspace the workspace to rename
     */
    public void renameWorkspace(@NonNull IWorkspace workspace) {
        
    	try {
    		String newName = parent.getInputSupplier().requestString(CHOOSE_A_NEW_NAME_LBL, workspace.getName());
    		workspace.setName(newName);
            parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    	} catch(RequestCancelledException e) {
    		return;
    	}
    }
        
    /**
     * Update the colour of a signal group
     * @param d the dataset
     * @param oldColour the old colour
     * @param signalGroupId the signal group to change
     * @return true if the colour was changed, false otherwise
     */
    public boolean changeSignalColour(@NonNull IAnalysisDataset d, @NonNull UUID signalGroupId) {

    	if(!d.getCollection().hasSignalGroup(signalGroupId))
    		return false;
    	
    	try {

    		Color oldColour = d.getCollection().getSignalGroup(signalGroupId).get().getGroupColour().orElse(Color.YELLOW); 
    		Color newColor = parent.getInputSupplier().requestColor(Labels.Signals.CHOOSE_SIGNAL_COLOUR, (Color) oldColour);

    		d.getCollection().getSignalGroup(signalGroupId).get().setGroupColour(newColor);
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, d);
    	} catch(RequestCancelledException e) {
    		return false;
    	}
    	return true;
    }
    
    /**
     * Update the name of a signal group in the active dataset
     * 
     * @param signalGroup
     */
    public void renameSignalGroup(@NonNull IAnalysisDataset d, @NonNull UUID signalGroup) {
    	Optional<ISignalGroup> groupValue = d.getCollection().getSignalGroup(signalGroup);
    	if(!groupValue.isPresent())
    		return;
    	ISignalGroup group = groupValue.get();
    	String oldName = group.getGroupName();
    	
    	try {
    		String newName = parent.getInputSupplier().requestString(CHOOSE_A_NEW_NAME_LBL, oldName);
    		group.setGroupName(newName);
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, d);
    	} catch(RequestCancelledException e) {}
    }

    /**
     * Update the source image folder for the given signal group
     * @param d
     * @param signalGroup
     */
    public void updateSignalSource(@NonNull IAnalysisDataset d, @NonNull UUID signalGroup) {

    	LOGGER.finest( "Updating signal source for signal group " + signalGroup);

    	try {
    		
    		File currentFolder = d.getAnalysisOptions().get().getNuclearSignalOptions(signalGroup).getFolder();
    		File newFolder = parent.getInputSupplier().requestFolder(FileUtils.extantComponent(currentFolder));

    		d.getCollection().getSignalManager().updateSignalSourceFolder(signalGroup, newFolder.getAbsoluteFile());
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, d);
    	} catch (RequestCancelledException e) {}           

    }
    
    /**
     * Update the nucleus folder for nuclei in the given image
     * @param d the dataset to update
     * @param image the image to update cells within
     */
    public void updateNucleusSource(@NonNull IAnalysisDataset d, File image) {

    	try {
    		File folder = parent.getInputSupplier().requestFolder(FileUtils.extantComponent(image.getParentFile()));

    		Set<ICell> cells = d.getCollection().getCells(image);
    		
    		for(ICell c : cells)
    			for(Nucleus n : c.getNuclei())
    				n.setSourceFolder(folder);
    		
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, d);    				
    	} catch (RequestCancelledException e) {}           

    }
    
    /**
     * Update the source image folder for the given signal group
     * @param d the dataset to update
     */
    public void updateNucleusSource(@NonNull IAnalysisDataset d) {

    	try {
    		File currentFolder = d.getAnalysisOptions().get().getDetectionOptions(CellularComponent.NUCLEUS).get().getFolder();
    		File newFolder = parent.getInputSupplier().requestFolder(FileUtils.extantComponent(currentFolder));
    		    		
    		d.getCollection().setSourceFolder(newFolder);
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, d);
    	} catch (RequestCancelledException e) {}           

    }
}


