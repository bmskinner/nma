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


package com.bmskinner.nuclear_morphology.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Track the open datasets in the program. Implemented as a singleton, since
 * every component needs to have a consistent view of what is open or selected
 * at any time.
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
    private volatile List<IAnalysisDataset> list = new CopyOnWriteArrayList<>();

    /**The datasets currently selected in the UI. Includes child datasets */
    private volatile List<IAnalysisDataset> selected = new CopyOnWriteArrayList<>(); // low efficiency if this is written frequently

    /**
     * This map stores the UUID of a dataset as a key against the hashcode of
     * the dataset. This is used to compare actual and saved hashcodes, and
     * detect whether a dataset has changed since the last check.
     */
    private final Map<UUID, Integer> datasetHashcodeMap = new ConcurrentHashMap<>();
    
    private final List<IWorkspace> workspaces = new CopyOnWriteArrayList<>();
    
    /** Hashcodes for workspaces */
    private final Map<UUID, Integer> workspaceHashcodeMap = new ConcurrentHashMap<>();

    private DatasetListManager() { }

    /**
     * Fetch an instance of the manager
     * 
     * @return
     */
    public static DatasetListManager getInstance() {

        if (instance != null)
            return instance;
        
        synchronized (lockObject) {
        	if (instance == null) 
        		instance = new DatasetListManager();

        }
        return instance;
    }
    
    /**
     * Get all root datasets
     * @return
     */
    public synchronized final List<IAnalysisDataset> getRootDatasets() {
        List<IAnalysisDataset> result = new ArrayList<>();
        result.addAll(list);
    	return result;
    }

    /**
     * Get the first of the selected datasets, or null if there are no
     * datasets selected
     * 
     * @return
     */
    @Nullable public synchronized final IAnalysisDataset getActiveDataset() {
        return selected.isEmpty() ? null : selected.get(0);
    }

    /**
     * Get the currently selected datasets
     * @return
     */
    public synchronized final List<IAnalysisDataset> getSelectedDatasets() {
    	 List<IAnalysisDataset> result = new ArrayList<>();
         result.addAll(selected);
         return result;
    }

    /**
     * Get the datasets with the given ids
     * @param ids
     * @return
     */
    public synchronized final List<IAnalysisDataset> getDatasets(@NonNull List<UUID> ids) {
        return ids.stream().map(id -> getDataset(id)).collect(Collectors.toList());
    }

    /**
     * Test if a single dataset is selected
     * @return
     */
    public synchronized boolean isSingleSelectedDataset() {
        return (selected.size() == 1);
    }

    /**
     * Test if multiple datasets are selected
     * 
     * @return
     */
    public synchronized final boolean isMultipleSelectedDatasets() {
        return (this.selected.size() > 1);
    }

    /**
     * Test if any datasets are selected
     * @return true if at least one dataset is selected, false otherwise
     */
    public synchronized final boolean hasSelectedDatasets() {
        return !selected.isEmpty();
    }

    /**
     * Set the selected datasets to the contents of the list
     * @param list
     */
    public synchronized final void setSelectedDatasets(@NonNull final Collection<IAnalysisDataset> list) {
        selected.clear();
        selected.addAll(list);
    }

    /**
     * Set the given dataset to be the only selected dataset
     * @param d
     */
    public synchronized final void setSelectedDataset(@NonNull final IAnalysisDataset d) {
        selected.clear();
        selected.add(d);
    }

    /**
     * Test if the manager contains at least one dataset
     * @return
     */
    public synchronized boolean hasDatasets() {
        return datasetHashcodeMap.size() > 0;
    }

    /**
     * Update the cluster groups for each root dataset and its children. This
     * will remove any cluster groups with no member datasets.
     */
    public synchronized void refreshClusters() {
        try {
            if (this.hasDatasets()) {
                for (IAnalysisDataset rootDataset : this.getRootDatasets()) {
                    rootDataset.refreshClusterGroups();
                    for (IAnalysisDataset child : rootDataset.getAllChildDatasets()) {
                        child.refreshClusterGroups();
                    }

                }
            }
        } catch (Exception e) {
            error("Error refreshing clusters", e);
        }
    }
    
    /**
     * Get the rott parent dataset to the given dataset. If the given dataset is
     * root, returns itself
     * 
     * @param d
     * @return
     */
    public synchronized IAnalysisDataset getRootParent(@NonNull IAnalysisDataset d) {

        if (d.isRoot())
            return d;
        for (IAnalysisDataset root : getRootDatasets())
            if (root.hasRecursiveChild(d))
            	return root;
        return null;
    }
    
    /**
     * Get the root parent dataset to the given collection, if present.
     * @param collection the collection
     * @return
     */
    public synchronized IAnalysisDataset getRootParent(@NonNull ICellCollection collection) {
    	for(IAnalysisDataset d : getRootDatasets()){
			if(d.getCollection().equals(collection) 
					|| d.getAllChildDatasets().stream().map(t->t.getCollection()).anyMatch(c->c.getID().equals(collection.getID())))
				return d;
		}
		return null;
    }

    /**
     * Get the parent dataset to the given dataset. If the given dataset is
     * root, returns itself
     * 
     * @param d
     * @return
     */
    public synchronized IAnalysisDataset getParent(@NonNull IAnalysisDataset d) {

        if (d.isRoot())
            return d;

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

    /**
     * Get all the datasets in the manager. Recursively fetches child datasets.
     * @return the datasets, or an empty set if no datasets are loaded
     */
    @NonNull public synchronized final Set<IAnalysisDataset> getAllDatasets() {

        Set<IAnalysisDataset> result = new HashSet<>();
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
    public synchronized final boolean hasDataset(@NonNull UUID id) {
        for (IAnalysisDataset d : list) {
            if (d.getId().equals(id)) {
                return true;
            }

            for (IAnalysisDataset child : d.getAllChildDatasets()) {
                if (child.getId().equals(id)) {
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
    public synchronized final IAnalysisDataset getDataset(@NonNull UUID id) {
        for (IAnalysisDataset d : list) {
            if (d.getId().equals(id)) {
                return d;
            }

            for (IAnalysisDataset child : d.getAllChildDatasets()) {
                if (child.getId().equals(id)) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * Add the given dataset to the manager if not already present
     * @param d
     */
    public synchronized final void addDataset(@NonNull IAnalysisDataset d) {
        if (d.isRoot() && !list.contains(d)) {
            list.add(d);
            datasetHashcodeMap.put(d.getId(), d.hashCode());
        }
    }

    /**
     * Remove the selected dataset from the list of open datasets
     * @param d
     */
    public synchronized final void removeDataset(@NonNull IAnalysisDataset d) {
        if (!d.isRoot()) // only remove root datasets
            return;

        if (!list.contains(d))
        	return;

        list.remove(d);
        datasetHashcodeMap.remove(d.getId());
        selected.remove(d);
    }
    

    /**
     * Get the number of datasets loaded
     * 
     * @return
     */
    public synchronized final int datasetCount() {
        return datasetHashcodeMap.size();
    }
    
    public synchronized final int workspaceCount() {
        return workspaceHashcodeMap.size();
    }

    /**
     * Close all datasets without saving and clear them from memory
     */
    public void clear() {
        list.clear();
        datasetHashcodeMap.clear();
        workspaceHashcodeMap.clear();
        selected.clear();
    }

    /**
     * Check if the stored hashcode for the given dataset is different to the
     * actual dataset hashcode
     * 
     * @param d
     * @return true if the hashcode is different to the stored value
     */
    public synchronized final boolean hashCodeChanged(@NonNull IAnalysisDataset d) {
        if (d.isRoot()) {
            if (datasetHashcodeMap.containsKey(d.getId())) {
                return d.hashCode() != datasetHashcodeMap.get(d.getId());
            }
			warn("Missing root dataset hashcode");

        }
        return false;
    }
    
    /**
     * Check if the stored hashcode for the given workspace is different to the
     * actual workspace hashcode
     * 
     * @param d
     * @return true if the hashcode is different to the stored value
     */
    public synchronized final boolean hashCodeChanged(@NonNull IWorkspace w) {
    	if (workspaceHashcodeMap.containsKey(w.getId())) {
    		return w.hashCode() != workspaceHashcodeMap.get(w.getId());
    	}
		warn("Missing workspace hashcode");

    	return false;
    }

    /**
     * Check if any of the root datasets or workspaces have a different hashcode to their last
     * save
     * 
     * @return
     */
    public synchronized final boolean hashCodeChanged() {
        for (IAnalysisDataset d : list) {
            if (hashCodeChanged(d))
                return true;
        }
        
        for (IWorkspace w :workspaces) {
            if (hashCodeChanged(w))
                return true;
        }
        return false;
    }

    /**
     * Update the stored hashcode for the given dataset to its current actual
     * value
     * 
     * @param d
     */
    public synchronized final void updateHashCode(@NonNull IAnalysisDataset d) {
        if (d.isRoot()) {
            datasetHashcodeMap.put(d.getId(), d.hashCode());
        }
    }
    
    /**
     * Update the stored hashcode for the given dataset to its current actual
     * value
     * 
     * @param d
     */
    public synchronized final void updateHashCode(@NonNull IWorkspace w) {
    	workspaceHashcodeMap.put(w.getId(), w.hashCode());
    }

    /**
     * Update the stored hashcode for all root datasets to their current actual
     * values
     * 
     * @param d
     */
    public synchronized final void updateHashCodes() {
        for (IAnalysisDataset d : list) {
            updateHashCode(d);
        }
        for (IWorkspace w : workspaces) {
            updateHashCode(w);
        }
    }
    
    
    
    /**
     * Add the given workspace to the manager
     * @param w
     */
    public synchronized final void addWorkspace(@NonNull IWorkspace w) {
        workspaces.add(w);
        workspaceHashcodeMap.put(w.getId(), w.hashCode());
    }
    
    /**
     * Get the currently loaded workspaces
     * @return
     */
    public synchronized final List<IWorkspace> getWorkspaces() {
        return workspaces;
    }
    
    /**
     * Test if workspaces are present
     * @return
     */
    public synchronized final boolean hasWorkspaces() {
        return workspaces.size() > 0;
    }
       
    /**
     * Test if the given dataset is in a workspace
     * @param d
     * @return
     */
    public synchronized final boolean isInWorkspace(@NonNull IAnalysisDataset d){
    	for(IWorkspace w : workspaces) {
    		if(w.has(d))
    			return true;
    	}
    	return false;
    }
    
    /**
     * Get the workspaces that the given dataset is a member of
     * @param d the dataset
     * @return
     */
    public synchronized final @NonNull List<IWorkspace> getWorkspaces(@NonNull IAnalysisDataset d) {
    	List<IWorkspace> list = new ArrayList<>();
    	for(IWorkspace w : workspaces) {
    		if(w.has(d))
    			list.add(w);
    	}
    	return list;
    }

}
