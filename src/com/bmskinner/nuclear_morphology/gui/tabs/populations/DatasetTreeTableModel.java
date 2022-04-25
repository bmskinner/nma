package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.tree.TreePath;

import org.eclipse.jdt.annotation.NonNull;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;

public class DatasetTreeTableModel extends AbstractTreeTableModel {

	private static final Logger LOGGER = Logger.getLogger(DatasetTreeTableModel.class.getName());

	private static final String[] COL_NAMES = { "Dataset", "Cell(s)", "" };

	public DatasetTreeTableModel() {
		// The root node is never seen in the UI
		super(new DefaultMutableTreeTableNode(""));
	}

	/**
	 * Add a dataset to the model. If root, this will be a child of the model root
	 * node. Otherwise, the dataset will be added to the appropriate parent dataset
	 * node
	 * 
	 * @param dataset
	 */
	public void addDataset(@NonNull IAnalysisDataset dataset) {

		if (hasNode(dataset))
			return; // ignore datasets already present in the model

		LOGGER.finer("Adding dataset " + dataset.getName() + " to population model");

		// If dataset is root, parent will be the same dataset
		IAnalysisDataset parent = DatasetListManager.getInstance().getParent(dataset);
		MutableTreeTableNode parentNode = dataset.isRoot() ? (MutableTreeTableNode) getRoot() : getNode(parent);

		MutableTreeTableNode newNode = createNodes(dataset);
		int newIndex = parentNode.getChildCount();
		parentNode.insert(newNode, newIndex);
		modelSupport.fireChildAdded(new TreePath(getPathToRoot(parentNode)), newIndex, newNode);
	}

	public void addWorkspace(@NonNull IWorkspace ws) {

		if (this.getNode(ws) != null)
			return; // ignore workspaces already present

		LOGGER.finer("Adding workspace " + ws.getName() + " to population model");
		MutableTreeTableNode parentNode = (MutableTreeTableNode) this.getRoot();
		MutableTreeTableNode newNode = createNodes(ws);
		int newIndex = parentNode.getChildCount();
		parentNode.insert(newNode, newIndex);
		modelSupport.fireChildAdded(new TreePath(getPathToRoot(parentNode)), newIndex, newNode);
	}

	/**
	 * Create a node in the tree table, recursively adding all the children of the
	 * given dataset id. If the child of a dataset is not already in the names list,
	 * add it
	 * 
	 * @param dataset the dataset to add
	 * @return
	 */
	private MutableTreeTableNode createNodes(@NonNull IAnalysisDataset dataset) {
		DatasetTreeTableNode n = new DatasetTreeTableNode(dataset);

		// Add cluster groups separately
		Set<UUID> clusterIDs = new HashSet<>(); // track the child datasets in clusters, so they are not added twice

		for (IClusterGroup group : dataset.getClusterGroups()) {
			ClusterGroupTreeTableNode cgNode = new ClusterGroupTreeTableNode(group);
			n.add(cgNode);

			for (UUID clusterID : group.getUUIDs()) {
				IAnalysisDataset clusterDataset = DatasetListManager.getInstance().getDataset(clusterID);
				MutableTreeTableNode childNode = createNodes(clusterDataset);
				cgNode.add(childNode);
				clusterIDs.add(clusterID);
			}

		}

		// Add remaining child datasets not in clusters
		for (IAnalysisDataset childDataset : dataset.getChildDatasets()) {
			if (!clusterIDs.contains(childDataset.getId())) {
				MutableTreeTableNode childNode = createNodes(childDataset);
				n.add(childNode);
			}
		}
		return n;
	}

	/**
	 * Create a node in the tree table, recursively adding all the children of the
	 * given dataset id. If the child of a dataset is not already in the names list,
	 * add it
	 * 
	 * @param dataset the dataset to add
	 * @return
	 */
	private MutableTreeTableNode createNodes(@NonNull IWorkspace ws) {
		WorkspaceTreeTableNode n = new WorkspaceTreeTableNode(ws);

		Set<File> files = ws.getFiles();
		for (IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()) {
			if (files.contains(d.getSavePath())) {
				n.add(createNodes(d));
			}
		}

		return n;
	}

	/**
	 * Test if the given dataset is within a node of the model
	 * 
	 * @param dataset the dataset to test
	 * @return true if the dataset is in a node, false otherwise
	 */
	private boolean hasNode(@NonNull Object obj) {
		Enumeration<? extends MutableTreeTableNode> en = ((MutableTreeTableNode) root).children();

		while (en.hasMoreElements()) {
			MutableTreeTableNode p = en.nextElement();
			if (p != null && obj == p.getUserObject())
				return true;
		}
		return false;
	}

	/**
	 * Get the node in the tree corresponding to the given dataset, or null if no
	 * group is found
	 * 
	 * @param g
	 * @return
	 */
	private MutableTreeTableNode getNode(@NonNull Object obj) {
		Enumeration<? extends MutableTreeTableNode> en = ((MutableTreeTableNode) root).children();

		while (en.hasMoreElements()) {
			MutableTreeTableNode p = en.nextElement();
			if (p != null && obj == p.getUserObject())
				return p;
		}
		return null;
	}

	@Override
	public String getColumnName(int column) {
		return COL_NAMES[column];
	}

	@Override
	public int getColumnCount() {
		return 3;
	}

	private boolean isValidTreeTableNode(Object node) {
		boolean result = false;

		if (node instanceof TreeTableNode ttn) {
			while (!result && ttn != null) {
				result = ttn == root;
				ttn = ttn.getParent();
			}
		}

		return result;
	}

	@Override
	public Object getValueAt(Object node, int column) {
		if (!isValidTreeTableNode(node))
			throw new IllegalArgumentException("Node must be a valid node managed by this model");

		if (column < 0 || column >= getColumnCount())
			throw new IllegalArgumentException("column must be a valid index");

		TreeTableNode ttn = (TreeTableNode) node;
		return ttn.getValueAt(column);
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (!isValidTreeTableNode(parent))
			throw new IllegalArgumentException("Parent must be a TreeTableNode managed by this model");
		return ((TreeTableNode) parent).getChildAt(index);
	}

	@Override
	public int getChildCount(Object parent) {
		if (!isValidTreeTableNode(parent))
			throw new IllegalArgumentException("Parent must be a TreeTableNode managed by this model");

		return ((TreeTableNode) parent).getChildCount();
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (!isValidTreeTableNode(parent) || !isValidTreeTableNode(child)) {
			return -1;
		}

		return ((TreeTableNode) parent).getIndex((TreeTableNode) child);
	}

	/**
	 * Gets the path from the root to the specified node.
	 * 
	 * @param aNode the node to query
	 * @return an array of {@code TreeTableNode}s, where
	 *         {@code arr[0].equals(getRoot())} and
	 *         {@code arr[arr.length - 1].equals(aNode)}, or an empty array if the
	 *         node is not found.
	 * @throws NullPointerException if {@code aNode} is {@code null}
	 */
	public MutableTreeTableNode[] getPathToRoot(MutableTreeTableNode aNode) {
		List<TreeTableNode> path = new ArrayList<>();
		TreeTableNode node = aNode;

		while (node != root) {
			path.add(0, node);
			node = node.getParent();
		}

		if (node == root) {
			path.add(0, node);
		}

		return path.toArray(new MutableTreeTableNode[0]);
	}

}
