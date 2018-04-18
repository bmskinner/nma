/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.main;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IWorkspace;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationTreeTableNode;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Track the open datasets in the program
 * 
 * @author bms41
 *
 */
public final class DatasetListManager implements Loggable {

    private static volatile DatasetListManager instance   = null;
    private static final Object                lockObject = new Object(); // synchronisation

    /**
     * The list of root datasets currently loaded. The order of datasets within
     * the list can be used to determine the order of root datasets within the
     * populations panel.
     */
    private final List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();

    /**
     * The datasets currently selected in the UI. Includes child datasets
     */
    private final List<IAnalysisDataset> selected = new ArrayList<IAnalysisDataset>();

    /**
     * This map stores the UUID of a dataset as a key against the hashcode of
     * the dataset. This is used to compare actual and saved hashcodes, and
     * detect whether a dataset has changed since the last check.
     */
    private final Map<UUID, Integer> map = new HashMap<UUID, Integer>(); // store
                                                                         // the
                                                                         // hash
                                                                         // for
                                                                         // a
                                                                         // dataset
                                                                         // id
    
    private final List<IWorkspace> workspaces = new ArrayList<IWorkspace>();

    protected DatasetListManager() {
    }

    /**
     * Fetch an instance of the factory
     * 
     * @return
     */
    public static DatasetListManager getInstance() {

        if (instance != null) {
            return instance;
        } else {

            synchronized (lockObject) {
                if (instance == null) {
                    instance = new DatasetListManager();
                }
            }

            return instance;
        }

    }

    public synchronized List<IAnalysisDataset> getRootDatasets() {
        return list;
    }

    /**
     * Get the first of the selected datasets
     * 
     * @return
     */
    public synchronized IAnalysisDataset getActiveDataset() {
        return selected.get(0);
    }

    public synchronized List<IAnalysisDataset> getSelectedDatasets() {
        return selected;
    }

    public synchronized List<IAnalysisDataset> getDatasets(List<UUID> ids) {

        return ids.stream().map(id -> getDataset(id)).collect(Collectors.toList());
    }

    public synchronized boolean isSingleDataset() {
        return (selected.size() == 1);
    }

    /**
     * Test if multiple datasets are selected
     * 
     * @return
     */
    public synchronized boolean isMultipleDatasets() {
        return (this.selected.size() > 1);
    }

    public synchronized boolean hasSelectedDatasets() {
        return !selected.isEmpty();
    }

    public synchronized void setSelectedDatasets(Collection<IAnalysisDataset> list) {
        selected.clear();
        selected.addAll(list);
    }

    public synchronized void setSelectedDataset(IAnalysisDataset d) {
        selected.clear();
        selected.add(d);
    }

    /**
     * Get the index of the given dataset in the list. Returns -1 if the dataset
     * is not root, not found, or null.
     * 
     * @param d
     * @return the index, or -1
     */
    public synchronized int getPosition(IAnalysisDataset d) {
        if (d.isRoot()) {
            return list.indexOf(d);
        }
        return -1;
    }

    public synchronized boolean hasDatasets() {
        return map.size() > 0;
    }

    /**
     * Update the cluster groups for each root dataset and its children. This
     * will remove any cluster groups with no member datasets.
     */
    public synchronized void refreshClusters() {
        try {
            finest("Refreshing clusters...");
            if (this.hasDatasets()) {

                for (IAnalysisDataset rootDataset : this.getRootDatasets()) {

                    finest("  Root dataset " + rootDataset.getName());
                    rootDataset.refreshClusterGroups();
                    for (IAnalysisDataset child : rootDataset.getAllChildDatasets()) {
                        finest("    Child dataset " + child.getName());
                        child.refreshClusterGroups();
                    }

                }
            }
        } catch (Exception e) {
            error("Error refreshing clusters", e);
        }
    }

    /**
     * Get the parent dataset to the given dataset. If the given dataset is
     * root, returns itself
     * 
     * @param d
     * @return
     */
    public synchronized IAnalysisDataset getParent(IAnalysisDataset d) {

        if (d.isRoot()) {
            return d;
        }

        IAnalysisDataset result = null;

        for (IAnalysisDataset root : this.getRootDatasets()) {

            if (root.hasRecursiveChild(d)) {

                // Get the child of the root dataset which is a parent
                // to the input dataset

                for (IAnalysisDataset parent : root.getAllChildDatasets()) {
                    if (parent.hasChild(d)) {
                        return parent;
                    }
                }

            }
        }
        return result;
    }

    public synchronized Set<IAnalysisDataset> getAllDatasets() {

        Set<IAnalysisDataset> result = new HashSet<IAnalysisDataset>();
        for (IAnalysisDataset d : list) {
            result.add(d);
            result.addAll(d.getAllChildDatasets());
        }
        return result;

    }

    /**
     * Get the dataset with the given id, or null
     * 
     * @param id
     * @return
     */
    public synchronized boolean hasDataset(UUID id) {
        for (IAnalysisDataset d : list) {
            if (d.getUUID().equals(id)) {
                return true;
            }

            for (IAnalysisDataset child : d.getAllChildDatasets()) {
                if (child.getUUID().equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the dataset with the given id, or null
     * 
     * @param id
     * @return
     */
    public synchronized IAnalysisDataset getDataset(UUID id) {
        for (IAnalysisDataset d : list) {
            if (d.getUUID().equals(id)) {
                return d;
            }

            for (IAnalysisDataset child : d.getAllChildDatasets()) {
                if (child.getUUID().equals(id)) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Add the given dataset to the manager
     * @param d
     */
    public synchronized void addDataset(IAnalysisDataset d) {
        if (d.isRoot() && !list.contains(d)) {
            list.add(d);
            map.put(d.getUUID(), d.hashCode());
        }
    }

    /**
     * Remove the selected dataset from the list of open datasets
     * @param d
     */
    public synchronized void removeDataset(IAnalysisDataset d) {

        if (!d.isRoot()) // only remove root datasets
            return;

        if (!list.contains(d))
        	return;

        list.remove(d);
        map.remove(d.getUUID());
        selected.remove(d);
    }

    /**
     * Get the number of datasets loaded
     * 
     * @return
     */
    public synchronized int datasetCount() {
        return map.size();
    }
    
    public synchronized int workspaceCount() {
        return map.size();
    }

    /**
     * Close all datasets without saving and clear them from memory
     */
    public void clear() {
        list.clear();
        map.clear();
        selected.clear();
    }

    /**
     * Check if the stored hashcode for the given dataset is different to the
     * actual dataset hashcode
     * 
     * @param d
     * @return true if the hashcode is different to the stored value
     */
    public boolean hashCodeChanged(IAnalysisDataset d) {
        if (d.isRoot()) {

            if (map.containsKey(d.getUUID())) {
                return d.hashCode() != map.get(d.getUUID());
            } else {
                warn("Missing root dataset hashcode");
            }

        }
        return false;
    }

    /**
     * Check if any of the root datasets have a different hashcode to their last
     * save
     * 
     * @return
     */
    public boolean hashCodeChanged() {

        for (IAnalysisDataset d : list) {
            if (hashCodeChanged(d)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update the stored hashcode for the given dataset to its current actual
     * value
     * 
     * @param d
     */
    public void updateHashCode(IAnalysisDataset d) {
        if (d.isRoot()) {
            map.put(d.getUUID(), d.hashCode());
        }
    }

    /**
     * Update the stored hashcode for all root datasets to their current actual
     * values
     * 
     * @param d
     */
    public void updateHashCodes() {
        for (IAnalysisDataset d : list) {
            updateHashCode(d);
        }
    }
    
    
    
    public synchronized void addWorkspace(IWorkspace w) {
        workspaces.add(w);
    }
    
    public synchronized List<IWorkspace> getWorkspaces() {
        return workspaces;
    }
    
    public synchronized boolean hasWorkspaces() {
        return workspaces.size() > 0;
    }
    
    public synchronized boolean isInWorkspace(IAnalysisDataset d){

        for(IWorkspace w : workspaces){
            
            if(w.getFiles().contains(d.getSavePath())){
                return true;
            } 
        }
        return false;
    }

}
