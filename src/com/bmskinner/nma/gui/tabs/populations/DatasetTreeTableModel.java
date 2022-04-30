package com.bmskinner.nma.gui.tabs.populations;

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

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.core.DatasetListManager;

public class DatasetTreeTableModel extends AbstractTreeTableModel {

	private static final Logger LOGGER = Logger.getLogger(DatasetTreeTableModel.class.getName());

	private static final String[] COL_NAMES = { "Dataset (0)", "Cells (0)", "" };

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
	public TreePath addDataset(@NonNull IAnalysisDataset dataset) {

		if (hasNode(dataset))
			return null; // ignore datasets already present in the model

		// If dataset is root, parent will be the same dataset
		IAnalysisDataset parent = DatasetListManager.getInstance().getParent(dataset);
		if (parent == null) {
			LOGGER.fine("No parent dataset found for " + dataset.getName());
			return null;
		}

		MutableTreeTableNode parentNode = dataset.isRoot() ? (MutableTreeTableNode) getRoot()
				: getNode(parent);

		MutableTreeTableNode newNode = createNode(dataset);
		int newIndex = parentNode.getChildCount();
		parentNode.insert(newNode, newIndex);
		TreePath path = new TreePath(getPathToRoot(parentNode));
		modelSupport.fireChildAdded(path, newIndex, newNode);
		return path;
	}

	/**
	 * @param group
	 * @return the path to the new node, or null if no new node was created
	 */
	public TreePath addClusterGroup(@NonNull IClusterGroup group) {

		if (hasNode(group))
			return null; // ignore groups already present in the model

		for (IAnalysisDataset d : DatasetListManager.getInstance().getAllDatasets()) {
			if (d.hasClusterGroup(group)) {
				MutableTreeTableNode parentNode = getNode(d);
				MutableTreeTableNode newNode = createNode(group);
				int newIndex = parentNode.getChildCount();
				parentNode.insert(newNode, newIndex);
				TreePath newPath = new TreePath(getPathToRoot(parentNode));
				modelSupport.fireChildAdded(newPath, newIndex, newNode);
				return new TreePath(getPathToRoot(newNode));
			}
		}
		return null;
	}

	public TreePath addWorkspace(@NonNull IWorkspace ws) {

		if (this.getNode(ws) != null)
			return null; // ignore workspaces already present

		MutableTreeTableNode parentNode = (MutableTreeTableNode) this.getRoot();
		MutableTreeTableNode newNode = createNode(ws);
		int newIndex = parentNode.getChildCount();
		parentNode.insert(newNode, newIndex);
		modelSupport.fireChildAdded(new TreePath(getPathToRoot(parentNode)), newIndex, newNode);
		return new TreePath(getPathToRoot(newNode));
	}

	/**
	 * Add the given root dataset to a workspace. Moves the node containing the
	 * dataset into the workspace node
	 * 
	 * @param ws
	 * @param d
	 * @return the path to the workspace
	 */
	public TreePath addDatasetToWorkspace(@NonNull IWorkspace ws, @NonNull IAnalysisDataset d) {
		MutableTreeTableNode wsNode = getNode(ws);
		MutableTreeTableNode dsNode = getNode(d);

		if (wsNode == null || dsNode == null)
			return null;

		removeNodeFromParent(dsNode);
		insertNodeInto(dsNode, wsNode, 0);

		return new TreePath(getPathToRoot(wsNode));
	}

	/**
	 * Invoked this to insert newChild at location index in parents children. This
	 * will then message nodesWereInserted to create the appropriate event. This is
	 * the preferred way to add children as it will create the appropriate event.
	 */
	public void insertNodeInto(MutableTreeTableNode newChild,
			MutableTreeTableNode parent, int index) {
		parent.insert(newChild, index);

		modelSupport.fireChildAdded(new TreePath(getPathToRoot(parent)), index,
				newChild);
	}

	/**
	 * Message this to remove node from its parent. This will message
	 * nodesWereRemoved to create the appropriate event. This is the preferred way
	 * to remove a node as it handles the event creation for you.
	 */
	public void removeNodeFromParent(MutableTreeTableNode node) {
		MutableTreeTableNode parent = (MutableTreeTableNode) node.getParent();

		if (parent == null) {
			throw new IllegalArgumentException("node does not have a parent.");
		}

		int index = parent.getIndex(node);
		node.removeFromParent();

		modelSupport.fireChildRemoved(new TreePath(getPathToRoot(parent)),
				index, node);
	}

	/**
	 * Remove node containing the given object if present
	 * 
	 * @param obj
	 */
	public void removeNode(Object obj) {
		if (hasNode(obj)) {
			MutableTreeTableNode node = getNode(obj);
			TreeTableNode parent = node.getParent();
			int nodeIndex = getIndexOfChild(parent, node);
			TreePath parentPath = new TreePath(getPathToRoot(parent));
			node.removeFromParent();
			modelSupport.fireChildRemoved(parentPath, nodeIndex, node);

			// Once cluster groups have no child datasets in them, they can be removed
			if (parent.getUserObject() instanceof IClusterGroup && parent.getChildCount() == 0) {
				removeNode(parent.getUserObject());
			}

			// If the node is a root dataset in a workspace, and there are no more
			// other datasets in that workspace, remove the workspace node
			if (parent instanceof WorkspaceTreeTableNode && parent.getChildCount() == 0)
				removeNode(parent.getUserObject());
		}
	}

	/**
	 * Create a node in the tree table, recursively adding all the children of the
	 * given dataset id. If the child of a dataset is not already in the names list,
	 * add it
	 * 
	 * @param group the group to add as a node
	 * @return
	 */
	private MutableTreeTableNode createNode(@NonNull IClusterGroup group) {
		ClusterGroupTreeTableNode n = new ClusterGroupTreeTableNode(group);
		for (UUID clusterID : group.getUUIDs()) {
			IAnalysisDataset clusterDataset = DatasetListManager.getInstance()
					.getDataset(clusterID);
			MutableTreeTableNode childNode = createNode(clusterDataset);
			n.add(childNode);
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
	private MutableTreeTableNode createNode(@NonNull IAnalysisDataset dataset) {
		DatasetTreeTableNode n = new DatasetTreeTableNode(dataset);

		// Add cluster groups separately
		Set<UUID> clusterIDs = new HashSet<>(); // track the child datasets in clusters, so they are
												// not added twice
		for (IClusterGroup group : dataset.getClusterGroups()) {
			clusterIDs.addAll(group.getUUIDs());
			MutableTreeTableNode cgNode = createNode(group);
			n.add(cgNode);

		}

		// Add remaining child datasets not in clusters
		for (IAnalysisDataset childDataset : dataset.getChildDatasets()) {
			if (!clusterIDs.contains(childDataset.getId())) {
				MutableTreeTableNode childNode = createNode(childDataset);
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
	private MutableTreeTableNode createNode(@NonNull IWorkspace ws) {
		WorkspaceTreeTableNode n = new WorkspaceTreeTableNode(ws);

		Set<File> files = ws.getFiles();
		for (IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()) {
			if (files.contains(d.getSavePath())) {
				n.add(createNode(d));
			}
		}

		return n;
	}

	/**
	 * Test if the given object is within a node of the model
	 * 
	 * @param obj the object to test
	 * @return true if the dataset is in a node, false otherwise
	 */
	private boolean hasNode(@NonNull Object obj) {
		return hasNode((MutableTreeTableNode) root, obj);
	}

	private static boolean hasNode(MutableTreeTableNode node, Object obj) {
		Enumeration<? extends MutableTreeTableNode> en = node.children();

		while (en.hasMoreElements()) {
			MutableTreeTableNode p = en.nextElement();
			if (p != null && obj == p.getUserObject())
				return true;
			if (hasNode(p, obj))
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
		return getNode((MutableTreeTableNode) root, obj);
	}

	private static MutableTreeTableNode getNode(MutableTreeTableNode node, Object obj) {
		Enumeration<? extends MutableTreeTableNode> en = node.children();

		while (en.hasMoreElements()) {
			MutableTreeTableNode p = en.nextElement();
			if (p != null && obj == p.getUserObject())
				return p;
			MutableTreeTableNode n = getNode(p, obj);
			if (n != null)
				return n;
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
			throw new IllegalArgumentException(
					"Parent must be a TreeTableNode managed by this model");
		return ((TreeTableNode) parent).getChildAt(index);
	}

	@Override
	public int getChildCount(Object parent) {
		if (!isValidTreeTableNode(parent))
			throw new IllegalArgumentException(
					"Parent must be a TreeTableNode managed by this model");

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
	public TreeTableNode[] getPathToRoot(TreeTableNode aNode) {
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
