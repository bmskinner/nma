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
package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Handle the deletion of selected datasets in the populations panel
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DatasetDeleter implements Loggable {

    private static final String DELETE_LBL = "Close";
    private static final String KEEP_LBL   = "Don't close";

    private static final String TITLE_LBL   = "Close dataset?";
    private static final String WARNING_LBL = "Dataset not saved. Close without saving?";
    
    private InputSupplier is;
    
    public DatasetDeleter(InputSupplier is) {
    	this.is = is;
    }

    /**
     * Delete or close the given datasets
     * 
     * @param datasets
     */
    public void deleteDatasets(List<IAnalysisDataset> datasets) {

        if (datasets == null || datasets.isEmpty())
            return;

        try {

            Deque<UUID> list = unique(datasets);

            // Ask before closing, if any root datasets have changed since last save
            if (rootHasChanged(list)) {
                warn("A root dataset has changed since last save");
                String[] buttonLabels = { KEEP_LBL, DELETE_LBL };
                int option = is.requestOption(buttonLabels, 0, WARNING_LBL, TITLE_LBL);
                if(option==0)
                	return;
            }

            deleteDatasetsInList(list);
            DatasetListManager.getInstance().refreshClusters();
        } catch (RequestCancelledException e) {
            return;
        } catch (Exception e) {
            warn("Error deleting dataset");
            stack("Error deleting dataset", e);
        }
    }

    private boolean rootHasChanged(Deque<UUID> list) {

        for (UUID id : list) {

            IAnalysisDataset d = DatasetListManager.getInstance().getDataset(id);

            if (d.isRoot()) {
                if (DatasetListManager.getInstance().hashCodeChanged(d))
                    return true;
            }
        }
        return false;
    }

    
    /**
     * Recursively delete datasets. Remove all datasets with no children from
     * the list. Recurse until all dataset have been deleted.
     * 
     * @param ids the dataset IDs to delete
     */
    private void deleteDatasetsInList(Deque<UUID> ids) {

        if (ids.isEmpty())
            return;

        UUID id = ids.removeFirst();

        IAnalysisDataset d = DatasetListManager.getInstance().getDataset(id);

        if (d.hasChildren()) {
        	 ids.addLast(id); // put at the end of the deque to be handled last
        } else {
        	deleteDataset(d);
        }

        deleteDatasetsInList(ids);
    }
    
    /**
     * Delete a single dataset
     * @param d
     */
    private void deleteDataset(IAnalysisDataset d) {

        UUID id = d.getId();
        fine("Removing dataset "+d.getName());
        // remove the dataset from its parents
        for (IAnalysisDataset parent : DatasetListManager.getInstance().getAllDatasets()) {
            if(parent.hasChild(id)) {
                parent.deleteChild(id);
                fine("Successfully removed child: "+!parent.hasChild(id));
            }
        }

        if (d.isRoot())
            DatasetListManager.getInstance().removeDataset(d);
    }



    /**
     * Get the list of unique datasets that must be removed
     * 
     * @param list
     * @return
     */
    private Deque<UUID> unique(List<IAnalysisDataset> list) {
        Set<UUID> set = new HashSet<UUID>();
        for (IAnalysisDataset d : list) {
            set.add(d.getId());

            if (d.hasChildren()) {
                // add all the children of a dataset
                for (UUID childID : d.getAllChildUUIDs()) {
                    set.add(childID);
                }
            }
        }

        Deque<UUID> result = new ArrayDeque<UUID>();
        result.addAll(set);

        return result;
    }

}
