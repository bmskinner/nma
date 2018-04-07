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

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
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
    
    public CosmeticHandler(TabPanel p){
        parent = p;
    }
    
    /**
     * Make a JColorChooser for the given dataset, and set the color.
     * 
     * @param dataset
     * @param row
     */
    public void changeDatasetColour(IAnalysisDataset dataset) {
        
        int row = DatasetListManager.getInstance().getSelectedDatasets().indexOf(dataset);
        Paint oldColour = dataset.hasDatasetColour() ? dataset.getDatasetColour() : ColourSelecter.getColor(row);
        
        Color newColor = JColorChooser.showDialog((Component) parent, "Choose dataset Color", (Color) oldColour);

        if (newColor != null) {
            dataset.setDatasetColour(newColor);
            parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, dataset);
        }
        parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    }
    
    /**
     * Rename an existing dataset and update the population list.
     * 
     * @param dataset
     *            the dataset to rename
     */
    public void renameDataset(IAnalysisDataset dataset) {
        ICellCollection collection = dataset.getCollection();
        String newName = JOptionPane.showInputDialog((Component) parent, "Choose a new name", "Rename collection",
                JOptionPane.INFORMATION_MESSAGE, null, null, collection.getName()).toString();

        // validate
        if (newName == null || newName.isEmpty()) {
            return;
        }

        // Get the existing names and check duplicates
        boolean nameMatches =  DatasetListManager.getInstance().getAllDatasets().stream().anyMatch(d->d.getName().equals(newName));

        if (nameMatches) {
            int result = JOptionPane.showConfirmDialog((Component) parent, "Chosen name exists. Use anyway?");

            if (result != JOptionPane.OK_OPTION) {
                return;
            }
        }

        collection.setName(newName);

//        log("Collection renamed: " + newName);

        File saveFile = dataset.getSavePath();
        if (saveFile.exists())
            saveFile.delete();

        parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SAVE, dataset);
        parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    }
    
    /**
     * Rename an existing dataset and update the population list.
     * 
     * @param dataset
     *            the dataset to rename
     */
    public void renameWorkspace(IWorkspace workspace) {
        
        String newName = JOptionPane.showInputDialog((Component) parent, "Choose a new name", "Rename workspace",
                JOptionPane.INFORMATION_MESSAGE, null, null, workspace.toString()).toString();

        // validate
        if (newName == null || newName.isEmpty())
            return;

        workspace.setName(newName);
        parent.getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
    }

    
    /**
     * Update the colour of the clicked signal group
     * 
     * @param row
     *            the row selected (the colour bar, one above the group name)
     */
    public void changeSignalColour(IAnalysisDataset d, Color oldColour, UUID signalGroupId) {

    	if(!d.getCollection().hasSignalGroup(signalGroupId))
    		return;
    	
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
    public void renameSignalGroup(IAnalysisDataset d, UUID signalGroup) {
    	if(!d.getCollection().hasSignalGroup(signalGroup))
    		return;

		String newName = (String) JOptionPane.showInputDialog("Enter new signal group name");

		if (newName == null)
		    return;

		d.getCollection().getSignalGroup(signalGroup).get().setGroupName(newName);
		parent.getDatasetEventHandler().fireDatasetEvent(DatasetEvent.REFRESH_CACHE, d);
    }

}


