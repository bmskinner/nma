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

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;

/**
 * The model for datasets within the populations table
 * 
 * @author ben
 *
 */
public class PopulationTreeTableModel extends DefaultTreeTableModel {

	private static final Logger LOGGER = Logger.getLogger(PopulationTreeTableModel.class.getName());

	private static final String DATASET_COLUMN_LBL = "Dataset (0)";
	private static final String CELL_COLUMN_LBL = "Cells (0)";
	private static final String COLOUR_COLUMN_LBL = "";

	public PopulationTreeTableModel() {
		super();

		// Populations columns
		List<String> columns = new ArrayList<>();
		columns.add(DATASET_COLUMN_LBL);
		columns.add(CELL_COLUMN_LBL);
		columns.add(COLOUR_COLUMN_LBL);

		PopulationTreeTableNode root = new PopulationTreeTableNode();
		this.setRoot(root);
		this.setColumnIdentifiers(columns);
		addExistingWorkspaces();
		addExistingRootDatasets();

	}

	/**
	 * Move the given nodes one position up in the model. If the node is at the top
	 * of its sib list, this has no effect. The tree hierarchy is not changed.
	 * 
	 * @param nodes
	 */
	public void moveNodesDown(List<PopulationTreeTableNode> nodes) {

		for (PopulationTreeTableNode n : nodes) {
			PopulationTreeTableNode parent = (PopulationTreeTableNode) n.getParent();

			// get the index of the child in the parent node
			int oldIndex = this.getIndexOfChild(parent, n);

			// if the index is last, do nothing
			if (oldIndex == parent.getChildCount() - 1)
				return;

			int sibIndex = oldIndex + 1;

			// Get the next node up
			PopulationTreeTableNode sib = (PopulationTreeTableNode) parent.getChildAt(sibIndex);
			this.removeNodeFromParent(n);
			this.removeNodeFromParent(sib);

			this.insertNodeInto(sib, parent, oldIndex);
			this.insertNodeInto(n, parent, sibIndex);

		}
	}

	/**
	 * Move the given nodes one position down in the model. If the node is at the
	 * bottom of its sib list, this has no effect. The tree hierarchy is not
	 * changed.
	 * 
	 * @param nodes
	 */
	public void moveNodesUp(List<PopulationTreeTableNode> nodes) {

		for (PopulationTreeTableNode n : nodes) {
			PopulationTreeTableNode parent = (PopulationTreeTableNode) n.getParent();

			// get the index of the child in the parent node
			int oldIndex = this.getIndexOfChild(parent, n);

			// if the index is first, do nothing
			if (oldIndex == 0)
				return;

			int sibIndex = oldIndex - 1;
			// Get the next node up
			PopulationTreeTableNode sib = (PopulationTreeTableNode) parent.getChildAt(sibIndex);
			this.removeNodeFromParent(n);
			this.removeNodeFromParent(sib);

			this.insertNodeInto(n, parent, sibIndex);
			this.insertNodeInto(sib, parent, oldIndex);

		}
	}

	/**
	 * Add root datasets and their children which are in workspaces
	 */
	private void addExistingWorkspaces() {
		if (DatasetListManager.getInstance().hasWorkspaces()) {
			List<IWorkspace> ws = DatasetListManager.getInstance().getWorkspaces();

			for (IWorkspace workspace : ws)
				addWorkspace(workspace);
		}
	}

	/**
	 * Add root datasets and their children which are not in workspaces
	 */
	private void addExistingRootDatasets() {
		if (!DatasetListManager.getInstance().hasDatasets())
			return;

		for (@NonNull
		IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()) {
			// Datasets in workspaces are handled separately
			if (!DatasetListManager.getInstance().isInWorkspace(root)) {
				addDataset(root);
			}
		}
	}

	/**
	 * Add a dataset to the model. If root, this will be a child of the model root
	 * node. Otherwise, the dataset will be added to the appropriate parent dataset
	 * node
	 * 
	 * @param dataset
	 */
	private void addDataset(@NonNull IAnalysisDataset dataset) {

		if (hasNode(dataset))
			return; // ignore datasets already present

		LOGGER.finer("Adding dataset " + dataset.getName() + " to population model");

		// If dataset is root, parent will be the same dataset
		IAnalysisDataset parent = DatasetListManager.getInstance().getParent(dataset);
		PopulationTreeTableNode parentNode = dataset.isRoot() ? (PopulationTreeTableNode) getRoot() : getNode(parent);
		PopulationTreeTableNode newNode = createNodes(dataset);
		parentNode.add(newNode);
	}

	private void addWorkspace(@NonNull IWorkspace ws) {

		if (this.getNode(ws) != null)
			return; // ignore datasets already present

		LOGGER.finer("Adding workspace " + ws.getName() + " to population model");
		PopulationTreeTableNode parentNode = ((PopulationTreeTableNode) this.getRoot());
		PopulationTreeTableNode newNode = createNodes(ws);
		parentNode.add(newNode);
	}

	/**
	 * Create a node in the tree table, recursively adding all the children of the
	 * given dataset id. If the child of a dataset is not already in the names list,
	 * add it
	 * 
	 * @param dataset the dataset to add
	 * @return
	 */
	private PopulationTreeTableNode createNodes(@NonNull IAnalysisDataset dataset) {
		PopulationTreeTableNode category = new PopulationTreeTableNode(dataset);

		// Add cluster groups separately
		Set<UUID> clusterIDs = new HashSet<>(); // track the child datasets in clusters, so they are not added twice

		for (IClusterGroup group : dataset.getClusterGroups()) {
			PopulationTreeTableNode clusterGroupNode = new PopulationTreeTableNode(group);
			category.add(clusterGroupNode);

			for (UUID clusterID : group.getUUIDs()) {
				IAnalysisDataset clusterDataset = DatasetListManager.getInstance().getDataset(clusterID);
				PopulationTreeTableNode childNode = createNodes(clusterDataset);
				clusterGroupNode.add(childNode);
				clusterIDs.add(clusterID);
			}

		}

		// Add remaining child datasets not in clusters

		for (IAnalysisDataset childDataset : dataset.getChildDatasets()) {
			if (!clusterIDs.contains(childDataset.getId())) {
				PopulationTreeTableNode childNode = createNodes(childDataset);
				category.add(childNode);
			}
		}
		return category;
	}

	/**
	 * Create a node in the tree table, recursively adding all the children of the
	 * given dataset id. If the child of a dataset is not already in the names list,
	 * add it
	 * 
	 * @param dataset the dataset to add
	 * @return
	 */
	private PopulationTreeTableNode createNodes(IWorkspace ws) {

		if (ws == null)
			throw new IllegalArgumentException("Workspace is null when generating population table nodes");

		PopulationTreeTableNode category = new PopulationTreeTableNode(ws);

		Set<File> files = ws.getFiles();
		for (IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()) {
			if (files.contains(d.getSavePath())) {
				PopulationTreeTableNode rootNode = createNodes(d);
				category.add(rootNode);
			}
		}

		return category;
	}

	/**
	 * Get the node in the tree corresponding to the given group, or null if no
	 * group is found
	 * 
	 * @param g
	 * @return
	 */
	private PopulationTreeTableNode getNode(IWorkspace w) {

		if (w == null)
			throw new IllegalArgumentException("Cluster group cannot be null");
		PopulationTreeTableNode result = null;

		PopulationTreeTableNode root = (PopulationTreeTableNode) this.getRoot();

		Enumeration<PopulationTreeTableNode> en = (Enumeration<PopulationTreeTableNode>) root.children();

		while (en.hasMoreElements()) {
			PopulationTreeTableNode p = en.nextElement();
			if (p.hasWorkspace() && p.getWorkspace() == w) {
				return p;
			}
		}
		return result;
	}

	/**
	 * Get the node in the tree corresponding to the given dataset, or null if no
	 * group is found
	 * 
	 * @param g
	 * @return
	 */
	private PopulationTreeTableNode getNode(IAnalysisDataset dataset) {

		if (dataset == null)
			throw new IllegalArgumentException("Dataset cannot be null");

		PopulationTreeTableNode result = null;

		PopulationTreeTableNode root = (PopulationTreeTableNode) this.getRoot();

		Enumeration<PopulationTreeTableNode> en = (Enumeration<PopulationTreeTableNode>) root.children();

		while (en.hasMoreElements()) {
			PopulationTreeTableNode p = en.nextElement();
			if (p.hasDataset() && p.getDataset() == dataset)
				return p;
		}
		return result;
	}

	/**
	 * Test if the given dataset is within a node of the model
	 * 
	 * @param dataset the dataset to test
	 * @return true if the dataset is in a node, false otherwise
	 */
	private boolean hasNode(IAnalysisDataset dataset) {
		if (dataset == null)
			return false;

		PopulationTreeTableNode root = (PopulationTreeTableNode) this.getRoot();

		Enumeration<PopulationTreeTableNode> en = (Enumeration<PopulationTreeTableNode>) root.children();

		while (en.hasMoreElements()) {
			PopulationTreeTableNode p = en.nextElement();
			if (p.hasDataset() && p.getDataset() == dataset)
				return true;
		}
		return false;
	}

}
