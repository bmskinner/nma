/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.io.File;
import java.util.List;
import java.util.UUID;

import javax.swing.JColorChooser;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.charting.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.io.DirectoryChooser;

/**
 * Handle cosmetic changes in datasets. Generates the dialogs
 * for confirmation.
 * @author bms41
 * @since 1.13.8
 *
 */
public class CosmeticHandler implements Loggable {
    
    private final TabPanel parent;
    
    public CosmeticHandler(@NonNull TabPanel p){
        parent = p;
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
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
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
    		String newName = parent.getInputSupplier().requestString("Choose a new name", collection.getName());
    		collection.setName(newName);
    		parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    	} catch(RequestCancelledException e) {
    		return;
    	}
    }
    
    /**
     * Rename an existing group and update the population list.
     * 
     * @param group the group to rename
     */
    public void renameClusterGroup(@NonNull IClusterGroup group) {

    	try {
    		String newName = parent.getInputSupplier().requestString("Choose a new name", group.getName());
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
    		String newName = parent.getInputSupplier().requestString("Choose a new name", workspace.getName());
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
     */
    public void changeSignalColour(@NonNull IAnalysisDataset d, @NonNull UUID signalGroupId) {

    	if(!d.getCollection().hasSignalGroup(signalGroupId))
    		return;
    	
    	try {

    		Color oldColour = d.getCollection().getSignalGroup(signalGroupId).get().getGroupColour().orElse(Color.YELLOW); 
    		Color newColor = parent.getInputSupplier().requestColor(Labels.Signals.CHOOSE_SIGNAL_COLOUR, (Color) oldColour);

    		d.getCollection().getSignalGroup(signalGroupId).get().setGroupColour(newColor);
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, d);
    	} catch(RequestCancelledException e) {
    		return;
    	}
    }
    
    /**
     * Update the name of a signal group in the active dataset
     * 
     * @param signalGroup
     */
    public void renameSignalGroup(@NonNull IAnalysisDataset d, @NonNull UUID signalGroup) {
    	if(!d.getCollection().hasSignalGroup(signalGroup))
    		return;
    	
    	try {
    		String oldName = d.getCollection().getSignalGroup(signalGroup).get().getGroupName();
    		String newName = parent.getInputSupplier().requestString("Choose a new name", oldName);
    		d.getCollection().getSignalGroup(signalGroup).get().setGroupName(newName);
    		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, d);
    	} catch(RequestCancelledException e) {
    		return;
    	}
    }

    /**
     * Update the source image folder for the given signal group
     * @param d
     * @param signalGroup
     */
    public void updateSignalSource(@NonNull IAnalysisDataset d, @NonNull UUID signalGroup) {

    	finest("Updating signal source for signal group " + signalGroup);

    	try {
    		File folder = parent.getInputSupplier().requestFolder();

    		d.getCollection().getSignalManager().updateSignalSourceFolder(signalGroup, folder);
    		finest("Updated signal source for signal group " + signalGroup + " to " + folder.getAbsolutePath());

    	} catch (RequestCancelledException e) {
    		return;
    	}           

    }
}


