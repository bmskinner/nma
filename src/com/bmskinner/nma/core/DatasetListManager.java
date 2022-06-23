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
package com.bmskinner.nma.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.gui.events.DatasetAddedListener;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.logging.Loggable;

/**
 * Track the open datasets in the program. Implemented as a singleton, since
 * every component needs to have a consistent view of what is open or selected
 * at any time.
 * 
 * @author bms41
 *
 */
public final class DatasetListManager implements DatasetAddedListener {

	private static final Logger LOGGER = Logger.getLogger(DatasetListManager.class.getName());

	private static DatasetListManager instance = null;
	private static final Object lockObject = new Object(); // synchronisation

	/**
	 * The list of root datasets currently loaded. The order of datasets within the
	 * list is used to determine the order of root datasets within the populations
	 * panel. Note that we do not use a hashset because the hashcodes of datasets
	 * can change frequently.
	 */
	private List<IAnalysisDataset> rootDatasets = new CopyOnWriteArrayList<>();

	/** The datasets currently selected in the UI. Includes child datasets */
	private List<IAnalysisDataset> selected = new CopyOnWriteArrayList<>(); // low efficiency if
																			// this is written
																			// frequently

	/**
	 * This map stores the UUID of a dataset as a key against the hashcode of the
	 * dataset. This is used to compare actual and saved hashcodes, and detect
	 * whether a dataset has changed since the last check.
	 */
	private final Map<UUID, Integer> datasetHashcodeMap = new ConcurrentHashMap<>();

	private final List<IWorkspace> workspaces = new CopyOnWriteArrayList<>();

	/** Hashcodes for workspaces */
	private final Map<UUID, Integer> workspaceHashcodeMap = new ConcurrentHashMap<>();

	private DatasetListManager() {

	}

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
	 * 
	 * @return
	 */
	public final synchronized List<IAnalysisDataset> getRootDatasets() {
		List<IAnalysisDataset> result = new ArrayList<>();
		result.addAll(rootDatasets);
		return result;
	}

	/**
	 * Get the first of the selected datasets, or null if there are no datasets
	 * selected
	 * 
	 * @return
	 */
	@Nullable
	public final IAnalysisDataset getActiveDataset() {
		return selected.isEmpty() ? null : selected.get(0);
	}

	/**
	 * Get the currently selected datasets
	 * 
	 * @return
	 */
	public final List<IAnalysisDataset> getSelectedDatasets() {
		List<IAnalysisDataset> result = new ArrayList<>();
		result.addAll(selected);
		return result;
	}

	/**
	 * Get the datasets with the given ids
	 * 
	 * @param ids
	 * @return
	 */
	public final synchronized List<IAnalysisDataset> getDatasets(@NonNull List<UUID> ids) {
		return ids.stream().map(id -> getDataset(id)).collect(Collectors.toList());
	}

	/**
	 * Test if a single dataset is selected
	 * 
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
	public final synchronized boolean isMultipleSelectedDatasets() {
		return (this.selected.size() > 1);
	}

	/**
	 * Test if the given dataset is a merge source for another dataset.
	 * 
	 * @param d
	 * @return
	 */
	public final boolean isMergeSource(@NonNull final IAnalysisDataset d) {
		for (IAnalysisDataset r : rootDatasets) {
			if (r.hasMergeSource(d))
				return true;
			for (IAnalysisDataset c : r.getAllChildDatasets())
				if (c.hasMergeSource(d))
					return true;
		}
		return false;
	}

	/**
	 * Test if any datasets are selected
	 * 
	 * @return true if at least one dataset is selected, false otherwise
	 */
	public final synchronized boolean hasSelectedDatasets() {
		return !selected.isEmpty();
	}

	/**
	 * Set the selected datasets to the contents of the list
	 * 
	 * @param list
	 */
	public final synchronized void setSelectedDatasets(@NonNull final List<IAnalysisDataset> list) {
		selected.clear();
		selected.addAll(list);
		UIController.getInstance().fireDatasetSelectionUpdated(list);
	}

	/**
	 * Set the given dataset to be the only selected dataset
	 * 
	 * @param d
	 */
	public final synchronized void setSelectedDataset(@NonNull final IAnalysisDataset d) {
		selected.clear();
		selected.add(d);
		UIController.getInstance().fireDatasetSelectionUpdated(d);
	}

	/**
	 * Test if the manager contains at least one dataset
	 * 
	 * @return
	 */
	public synchronized boolean hasDatasets() {
		return datasetHashcodeMap.size() > 0;
	}

	/**
	 * Update the cluster groups for each root dataset and its children. This will
	 * remove any cluster groups with no member datasets.
	 */
	public synchronized void refreshClusters() {

		try {

			if (this.hasDatasets()) {

				List<IAnalysisDataset> allDatasets = this.getAllDatasets().stream().toList();
				for (IAnalysisDataset dataset : allDatasets) {
					dataset.refreshClusterGroups();
				}
				UIController.getInstance().fireClusterGroupsUpdated(allDatasets);
			}

		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error refreshing clusters", e);
		}
	}

	/**
	 * Get the unique root parent datasets to the given list of datasets.
	 * 
	 * @param d
	 * @return
	 */
	public synchronized Set<IAnalysisDataset> getRootParents(
			@NonNull List<IAnalysisDataset> datasets) {
		Set<IAnalysisDataset> result = new HashSet<>();
		for (IAnalysisDataset d : datasets) {
			if (d.isRoot())
				result.add(d);
			else
				result.add(getRootParent(d));
		}
		return result;
	}

	/**
	 * Get the root parent dataset to the given dataset. If the given dataset is
	 * root, returns itself
	 * 
	 * @param d
	 * @return
	 */
	public synchronized IAnalysisDataset getRootParent(@NonNull IAnalysisDataset d) {

		if (d.isRoot())
			return d;
		for (IAnalysisDataset root : getRootDatasets())
			if (root.hasAnyChild(d))
				return root;
		return null;
	}

	/**
	 * Get the root parent dataset to the given collection, if present.
	 * 
	 * @param collection the collection
	 * @return
	 */
	public synchronized IAnalysisDataset getRootParent(@NonNull ICellCollection collection) {
		for (IAnalysisDataset d : getRootDatasets()) {
			if (d.getCollection().equals(collection) || d.getAllChildDatasets().stream()
					.map(IAnalysisDataset::getCollection)
					.anyMatch(c -> c.getId().equals(collection.getId())))
				return d;
		}
		return null;
	}

	/**
	 * Get the parent dataset to the given dataset. If the given dataset is root,
	 * returns itself
	 * 
	 * @param d
	 * @return
	 */
	public synchronized IAnalysisDataset getParent(@NonNull IAnalysisDataset d) {

		if (d.isRoot())
			return d;

		IAnalysisDataset result = null;

		for (IAnalysisDataset root : this.getRootDatasets()) {
			if (root.hasDirectChild(d))
				return root;

			if (root.hasAnyChild(d)) {

				// Get the child of the root dataset which is a parent
				// to the input dataset
				for (IAnalysisDataset parent : root.getAllChildDatasets()) {
					if (parent.hasDirectChild(d)) {
						return parent;
					}
				}

			}
		}
		return result;
	}

	/**
	 * Get all the datasets in the manager. Recursively fetches child datasets.
	 * 
	 * @return the datasets, or an empty set if no datasets are loaded
	 */
	@NonNull
	public final synchronized Set<IAnalysisDataset> getAllDatasets() {

		Set<IAnalysisDataset> result = new HashSet<>();
		for (IAnalysisDataset d : rootDatasets) {
			result.add(d);
			result.addAll(d.getAllChildDatasets());
		}
		return result;

	}

	/**
	 * Test if a dataset with the given id is present, either as a root or child
	 * dataset
	 * 
	 * @param id the id to check
	 * @return
	 */
	public final synchronized boolean hasDataset(@NonNull UUID id) {
		for (IAnalysisDataset d : rootDatasets) {
			if (d.getId().equals(id))
				return true;

			for (IAnalysisDataset child : d.getAllChildDatasets()) {
				if (child.getId().equals(id)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Test if a root dataset with the given id is present.
	 * 
	 * @param id the id to check
	 * @return
	 */
	public final synchronized boolean hasRootDataset(@NonNull UUID id) {
		for (IAnalysisDataset d : rootDatasets) {
			if (d.getId().equals(id)) {
				return true;
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
	public final synchronized IAnalysisDataset getDataset(@NonNull UUID id) {
		for (IAnalysisDataset d : rootDatasets) {
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
	 * 
	 * @param d
	 */
	public final synchronized void addDataset(@NonNull IAnalysisDataset d) {
		if (d.isRoot() && !rootDatasets.contains(d)) {
			rootDatasets.add(d);
			datasetHashcodeMap.put(d.getId(), d.hashCode());
			LOGGER.fine("Added dataset  " + d.getName() + ": " + d.hashCode());
		}
	}

	/**
	 * Remove the selected dataset from the list of open datasets
	 * 
	 * @param d
	 */
	public final synchronized void removeDataset(@NonNull IAnalysisDataset d) {
		if (!d.isRoot()) // only remove root datasets
			return;

		if (rootDatasets.stream().noneMatch(e -> e.getId().equals(d.getId())))
			return;

		rootDatasets = rootDatasets.stream().filter(e -> !e.getId().equals(d.getId()))
				.collect(Collectors.toCollection(CopyOnWriteArrayList::new));

		datasetHashcodeMap.remove(d.getId());
		selected.remove(d);
	}

	/**
	 * Get the number of datasets loaded
	 * 
	 * @return
	 */
	public final synchronized int datasetCount() {
		return datasetHashcodeMap.size();
	}

	public final synchronized int workspaceCount() {
		return workspaceHashcodeMap.size();
	}

	/**
	 * Close all datasets without saving and clear them from memory
	 */
	public void clear() {
		UIController.getInstance().fireDatasetDeleted(rootDatasets);
		rootDatasets.clear();
		datasetHashcodeMap.clear();
		workspaceHashcodeMap.clear();
		selected.clear();
	}

	/**
	 * Check if the stored hashcode for the given dataset is different to the actual
	 * dataset hashcode
	 * 
	 * @param d
	 * @return true if the hashcode is different to the stored value
	 */
	public final synchronized boolean hashCodeChanged(@NonNull IAnalysisDataset d) {
		if (d.isRoot()) {
			if (datasetHashcodeMap.containsKey(d.getId())) {
				return d.hashCode() != datasetHashcodeMap.get(d.getId());
			}
			LOGGER.warning("Missing root dataset hashcode");

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
	public final synchronized boolean hashCodeChanged(@NonNull IWorkspace w) {
		if (workspaceHashcodeMap.containsKey(w.getId())) {
			return w.hashCode() != workspaceHashcodeMap.get(w.getId());
		}
		LOGGER.warning("Missing workspace hashcode");

		return false;
	}

	/**
	 * Check if any of the root datasets or workspaces have a different hashcode to
	 * their last save
	 * 
	 * @return
	 */
	public final synchronized boolean hashCodeChanged() {
		for (IAnalysisDataset d : rootDatasets) {
			if (hashCodeChanged(d))
				return true;
		}

		for (IWorkspace w : workspaces) {
			if (hashCodeChanged(w))
				return true;
		}
		return false;
	}

	/**
	 * Get all root datasets with a hashcode that has changed since last save
	 * 
	 * @return the datasets that have changed
	 */
	public final synchronized @NonNull Set<IAnalysisDataset> getUnsavedRootDatasets() {
		Set<IAnalysisDataset> result = new HashSet<>();
		for (IAnalysisDataset d : rootDatasets) {
			if (hashCodeChanged(d))
				result.add(d);
		}
		return result;
	}

	/**
	 * Update the stored hashcode for the given dataset to its current actual value
	 * 
	 * @param d
	 */
	public final synchronized void updateHashCode(@NonNull IAnalysisDataset d) {
		if (d.isRoot()) {
			datasetHashcodeMap.put(d.getId(), d.hashCode());
		}
	}

	/**
	 * Update the stored hashcode for the given dataset to its current actual value
	 * 
	 * @param d
	 */
	public final synchronized void updateHashCode(@NonNull IWorkspace w) {
		workspaceHashcodeMap.put(w.getId(), w.hashCode());
	}

	/**
	 * Update the stored hashcode for all root datasets to their current actual
	 * values
	 * 
	 * @param d
	 */
	public final synchronized void updateHashCodes() {
		for (IAnalysisDataset d : rootDatasets) {
			updateHashCode(d);
		}
		for (IWorkspace w : workspaces) {
			updateHashCode(w);
		}
	}

	/**
	 * Add the given workspace to the manager
	 * 
	 * @param w
	 */
	public final synchronized void addWorkspace(@NonNull IWorkspace w) {
		workspaces.add(w);
		workspaceHashcodeMap.put(w.getId(), w.hashCode());
		UIController.getInstance().fireWorkspaceAdded(w);
	}

	/**
	 * Get the currently loaded workspaces
	 * 
	 * @return
	 */
	public final synchronized List<IWorkspace> getWorkspaces() {
		return workspaces;
	}

	/**
	 * Test if workspaces are present
	 * 
	 * @return
	 */
	public final synchronized boolean hasWorkspaces() {
		return !workspaces.isEmpty();
	}

	/**
	 * Test if the given dataset is in a workspace
	 * 
	 * @param d
	 * @return
	 */
	public final synchronized boolean isInWorkspace(@NonNull IAnalysisDataset d) {
		for (IWorkspace w : workspaces) {
			if (w.has(d))
				return true;
		}
		return false;
	}

	/**
	 * Get the workspaces that the given dataset is a member of
	 * 
	 * @param d the dataset
	 * @return
	 */
	public final synchronized @NonNull List<IWorkspace> getWorkspaces(@NonNull IAnalysisDataset d) {
		List<IWorkspace> result = new ArrayList<>();
		for (IWorkspace w : workspaces) {
			if (w.has(d))
				result.add(w);
		}
		return result;
	}

	/**
	 * Get all workspaces with changed hashcodes since last save
	 * 
	 * @return
	 */
	public final synchronized @NonNull List<IWorkspace> getUnsavedWorkspaces() {
		List<IWorkspace> result = new ArrayList<>();
		for (IWorkspace w : workspaces) {
			if (hashCodeChanged(w))
				result.add(w);
		}
		return result;
	}

	@Override
	public void datasetAdded(List<IAnalysisDataset> datasets) {
		for (IAnalysisDataset d : datasets)
			addDataset(d);
	}

	@Override
	public void datasetAdded(IAnalysisDataset dataset) {
		addDataset(dataset);
	}

	@Override
	public void datasetDeleted(List<IAnalysisDataset> datasets) {
		// No action here

	}

}
