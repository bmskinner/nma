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
import java.util.UUID;

import javax.swing.JColorChooser;
import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.IWorkspace;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;

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
        
        Color newColor = JColorChooser.showDialog((Component) parent, "Choose dataset colour", (Color) oldColour);

        if (newColor != null) {
            dataset.setDatasetColour(newColor);
            parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
        }
        parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    }
    
    /**
     * Rename an existing dataset and update the population list.
     * 
     * @param dataset the dataset to rename
     */
    public void renameDataset(@NonNull IAnalysisDataset dataset) {
        ICellCollection collection = dataset.getCollection();
        String newName = getNewName(collection.getName());
        if (newName == null || newName.isEmpty())
            return;
        collection.setName(newName);
        parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    }
    
    /**
     * Rename an existing group and update the population list.
     * 
     * @param group the group to rename
     */
    public void renameClusterGroup(@NonNull IClusterGroup group) {
    	String newName = getNewName(group.getName());
        if (newName == null || newName.isEmpty())
            return;
        group.setName(newName);
        parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    }
    
    /**
     * Rename an existing workspace and update the population list.
     * 
     * @param workspace the workspace to rename
     */
    public void renameWorkspace(@NonNull IWorkspace workspace) {
        
    	String newName = getNewName(workspace.getName());
        if (newName == null || newName.isEmpty())
            return;
        workspace.setName(newName);
        parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    }
    
    private String getNewName(String oldName) {
    	return JOptionPane.showInputDialog((Component) parent, "Choose a new name", "Rename",
                JOptionPane.INFORMATION_MESSAGE, null, null, oldName).toString();
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
    	
    	Color oldColour = d.getCollection().getSignalGroup(signalGroupId).get().getGroupColour().orElse(Color.YELLOW); 
        Color newColor = JColorChooser.showDialog((Component) parent, Labels.Signals.CHOOSE_SIGNAL_COLOUR, oldColour);

		if (newColor != null) {
		    d.getCollection().getSignalGroup(signalGroupId).get().setGroupColour(newColor);
		    parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, d);
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

    	String newName =getNewName(d.getCollection().getSignalGroup(signalGroup).get().getGroupName());
//		String newName = (String) JOptionPane.showInputDialog("Enter new signal group name");

		if (newName == null)
		    return;

		d.getCollection().getSignalGroup(signalGroup).get().setGroupName(newName);
		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, d);
    }

}


