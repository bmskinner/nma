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

		// don't re-add datasets already present in the model
		if (hasNode(dataset)) {
			LOGGER.finer("Dataset node %s exists in table model, not re-adding".formatted(dataset.getName()));
			final MutableTreeTableNode node = this.getNode(dataset);
			return new TreePath(getPathToRoot(node));
		}

		// If dataset is root, parent will be the same dataset
		final IAnalysisDataset parent = DatasetListManager.getInstance().getParent(dataset);
		if (parent == null) {
			LOGGER.finer("No parent dataset found for " + dataset.getName());
			return null;
		}

		LOGGER.finer("Adding new node for dataset %s".formatted(dataset.getName()));

		final MutableTreeTableNode parentNode = dataset.isRoot() ? (MutableTreeTableNode) getRoot()
				: getNode(parent);

		final MutableTreeTableNode newNode = createNode(dataset);
		final int newIndex = parentNode.getChildCount();
		parentNode.insert(newNode, newIndex);
		final TreePath path = new TreePath(getPathToRoot(parentNode));
		modelSupport.fireChildAdded(path, newIndex, newNode);
		return path;
	}

	/**
	 * @param group
	 * @return the path to the new node, or null if no new node was created
	 */
	public TreePath addClusterGroup(@NonNull IClusterGroup group) {

		// don't re-add groups already present in the model
		if (hasNode(group)) {
			final MutableTreeTableNode node = this.getNode(group);
			return new TreePath(getPathToRoot(node));
		}

		for (final IAnalysisDataset d : DatasetListManager.getInstance().getAllDatasets()) {
			if (d.hasClusterGroup(group)) {
				final MutableTreeTableNode parentNode = getNode(d);

				final List<IAnalysisDataset> clusterDatasets = d.getClusterGroup(group.getId());
				final MutableTreeTableNode newNode = createNode(group, clusterDatasets);
				final int newIndex = parentNode.getChildCount();
				parentNode.insert(newNode, newIndex);
				final TreePath newPath = new TreePath(getPathToRoot(parentNode));
				modelSupport.fireChildAdded(newPath, newIndex, newNode);
				return new TreePath(getPathToRoot(newNode));
			}
		}
		return null;
	}

	public TreePath addWorkspace(@NonNull IWorkspace ws) {

		// don't re-add workspaces already present in the model
		if (hasNode(ws)) {
			LOGGER.finer("Workspace node %s exists, not re-adding".formatted(ws.getName()));
			final MutableTreeTableNode node = this.getNode(ws);
			return new TreePath(getPathToRoot(node));
		}

		LOGGER.finer("Adding new node for workspace %s".formatted(ws.getName()));
		final MutableTreeTableNode parentNode = (MutableTreeTableNode) this.getRoot();
		final MutableTreeTableNode newNode = createNode(ws);
		final int newIndex = parentNode.getChildCount();
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
	public synchronized TreePath addDatasetToWorkspace(@NonNull IWorkspace ws, @NonNull IAnalysisDataset d) {
		final MutableTreeTableNode wsNode = getNode(ws);
		final MutableTreeTableNode dsNode = getNode(d);

		if (wsNode == null) {
			LOGGER.finer("No node detected for %s, not adding dataset %s".formatted(ws.getName(), d.getName()));
			return null;
		}

		if (dsNode == null) {
			LOGGER.finer("No existing node detected for %s, adding dataset to workspace node".formatted(d.getName()));
			// Get the root dataset node for the given dataset
			final IAnalysisDataset parent = DatasetListManager.getInstance().getParent(d);
			final MutableTreeTableNode parentNode = d.isRoot() ? wsNode
					: getNode(parent);

			final MutableTreeTableNode newNode = createNode(d);
			insertNodeInto(newNode, wsNode, 0);
		} else if (hasNode(wsNode, dsNode)) {
			LOGGER.finer(
					"Node %s is already in the workspace %s, no action needed".formatted(d.getName(), ws.getName()));
		} else {

			LOGGER.finer(
					"Dataset node for %s detected, moving to workspace %s node".formatted(d.getName(), ws.getName()));
			removeNodeFromParent(dsNode);
			insertNodeInto(dsNode, wsNode, 0);
		}

		return new TreePath(getPathToRoot(wsNode));
	}

	/**
	 * Add the given root dataset to a workspace. Moves the node containing the
	 * dataset into the workspace node
	 * 
	 * @param ws
	 * @param d
	 * @return the path to the workspace
	 */
	public TreePath removeDatasetFromWorkspace(@NonNull IWorkspace ws,
			@NonNull IAnalysisDataset d) {
		final MutableTreeTableNode root = (MutableTreeTableNode) this.getRoot();
		final MutableTreeTableNode wsNode = getNode(ws);
		final MutableTreeTableNode dsNode = getNode(d);

		if (wsNode == null || dsNode == null)
			return null;

		removeNodeFromParent(dsNode);
		insertNodeInto(dsNode, root, root.getChildCount());

		return new TreePath(getPathToRoot(dsNode));
	}

	/**
	 * Invoked this to insert newChild at location index in parents children. This
	 * will then message nodesWereInserted to create the appropriate event. This is
	 * the preferred way to add children as it will create the appropriate event.
	 */
	public void insertNodeInto(MutableTreeTableNode newChild,
			MutableTreeTableNode parent, int index) {

		LOGGER.finer("Inserting node %s into parent".formatted(newChild.toString()));
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
		LOGGER.finer("Removing node %s from parent".formatted(node.toString()));
		final MutableTreeTableNode parent = (MutableTreeTableNode) node.getParent();

		if (parent == null)
			throw new IllegalArgumentException("node does not have a parent.");

		final int index = parent.getIndex(node);
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
			final MutableTreeTableNode node = getNode(obj);
			final TreeTableNode parent = node.getParent();
			final int nodeIndex = getIndexOfChild(parent, node);
			final TreePath parentPath = new TreePath(getPathToRoot(parent));
			node.removeFromParent();
			modelSupport.fireChildRemoved(parentPath, nodeIndex, node);

			// Once cluster groups have no child datasets in them, they can be removed
			if (parent.getUserObject() instanceof IClusterGroup && parent.getChildCount() == 0) {
				removeNode(parent.getUserObject());
			}

			// If the node is a root dataset in a workspace, and there are no more
			// other datasets in that workspace, remove the workspace node
			if (parent instanceof WorkspaceTreeTableNode && parent.getChildCount() == 0) {
				removeNode(parent.getUserObject());
			}
		}
	}

	/**
	 * Test if the given dataset is present in the model. This checks on dataset ID,
	 * not direct object equality.
	 * 
	 * @param d
	 * @return
	 */
	public boolean contains(IAnalysisDataset d) {

		return contains(((MutableTreeTableNode) root), d);
	}

	private boolean contains(MutableTreeTableNode node, IAnalysisDataset d) {

		final Enumeration<? extends MutableTreeTableNode> en = node.children();

		while (en.hasMoreElements()) {
			final MutableTreeTableNode p = en.nextElement();
			if (p != null) {
				if (p.getUserObject() instanceof final IAnalysisDataset other) {
					if (d.getId().equals(other.getId()))
						return true;
				}
			}

			if (hasNode(p, d))
				return true;
		}
		return false;
	}

	/**
	 * Create a node in the tree table, recursively adding all the children of the
	 * given dataset id. If the child of a dataset is not already in the names list,
	 * add it
	 * 
	 * @param group the group to add as a node
	 * @return
	 */
	private MutableTreeTableNode createNode(@NonNull IClusterGroup group, List<IAnalysisDataset> clusterDatasets) {
		final ClusterGroupTreeTableNode n = new ClusterGroupTreeTableNode(group);
		for (final IAnalysisDataset clusterDataset : clusterDatasets) {

			// LOGGER.fine("Found dataset list with cluster id %s is
			// %s".formatted(clusterID, clusterDataset.getName()));
			final MutableTreeTableNode childNode = createNode(clusterDataset);
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
		LOGGER.finer("Creating new table node for %s".formatted(dataset.getName()));
		final DatasetTreeTableNode n = new DatasetTreeTableNode(dataset);

		// Add cluster groups separately
		final Set<UUID> clusterIDs = new HashSet<>(); // track the child datasets in clusters, so they are
		// not added twice
		for (final IClusterGroup group : dataset.getClusterGroups()) {
			clusterIDs.addAll(group.getUUIDs());

			final List<IAnalysisDataset> clusterDatasets = dataset.getClusterGroup(group.getId());
			final MutableTreeTableNode cgNode = createNode(group, clusterDatasets);
			n.add(cgNode);

		}

		// Add remaining child datasets not in clusters
		for (final IAnalysisDataset childDataset : dataset.getChildDatasets()) {
			if (!clusterIDs.contains(childDataset.getId())) {
				final MutableTreeTableNode childNode = createNode(childDataset);
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
		final WorkspaceTreeTableNode n = new WorkspaceTreeTableNode(ws);

		final Set<File> files = ws.getFiles();
		for (final IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()) {
			if (files.contains(d.getSavePath())) {

				// Check if the node needs to be moved or created
				if (hasNode(d)) {

					LOGGER.finer("Dataset %s belongs to workspace %s, moving as a child of the workspace node"
							.formatted(d.getName(), ws.getName()));
					final MutableTreeTableNode dsNode = getNode(d);
					removeNodeFromParent(dsNode);
					n.add(dsNode);
				} else {
					LOGGER.finer("Dataset %s does not have a node, creating within workspace %s".formatted(d.getName(),
							ws.getName()));
					n.add(createNode(d));
				}
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

	/**
	 * Test if the given node or its children has an object
	 * 
	 * @param node
	 * @param obj
	 * @return
	 */
	private static boolean hasNode(MutableTreeTableNode node, Object obj) {
		final Enumeration<? extends MutableTreeTableNode> en = node.children();

		while (en.hasMoreElements()) {
			final MutableTreeTableNode p = en.nextElement();

			// If we are looking for the node itself
			if (p != null && p == obj)
				return true;

			// If we are looking at user objects within the node
			if (p != null && obj == p.getUserObject())
				return true;
			if (hasNode(p, obj))
				return true;
		}
		return false;
	}

	/**
	 * Get the node in the tree corresponding to the given dataset, or null if no
	 * matching node is found. Searches all nodes from the root node in a depth-first manner.
	 * 
	 * @param obj the dataset whose node to fetch
	 * @return
	 */
	public MutableTreeTableNode getNode(@NonNull Object obj) {
		return getNode((MutableTreeTableNode) root, obj);
	}

	private static MutableTreeTableNode getNode(MutableTreeTableNode node, Object obj) {
		final Enumeration<? extends MutableTreeTableNode> en = node.children();

		while (en.hasMoreElements()) {
			final MutableTreeTableNode currentNode = en.nextElement();

			final Object nodeObject = currentNode.getUserObject();

			if (currentNode != null && nodeObject != null && nodeObject instanceof final IWorkspace w) {
				if (obj.hashCode() == w.hashCode())
					return currentNode;
			}

			// If the node holds a dataset, check the hashcode of the object
			if (currentNode != null && nodeObject != null && nodeObject instanceof final IAnalysisDataset d
					&& obj instanceof final IAnalysisDataset o) {

				if (o.getId().equals(d.getId()))
					return currentNode;
				if (o.hashCode() == d.hashCode())
					return currentNode;
			}

			// If they are the same object
			if (nodeObject.equals(obj))
				return currentNode;



			// Check the children of this node
			final MutableTreeTableNode n = getNode(currentNode, obj);
			if (n != null)
				return n;
		}
		return null;
	}

	/**
	 * Get the path from the root node to the node containing the desired object
	 * 
	 * @param obj the object to find
	 * @return
	 */
	public TreePath getPath(@NonNull Object obj) {
		final MutableTreeTableNode node = getNode(obj);
		return new TreePath(getPathToRoot(node));
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

		final TreeTableNode ttn = (TreeTableNode) node;
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
					"Parent %s must be a TreeTableNode managed by this model".formatted(parent.toString()));

		return ((TreeTableNode) parent).getChildCount();
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		if (!isValidTreeTableNode(parent) || !isValidTreeTableNode(child))
			return -1;

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
		final List<TreeTableNode> path = new ArrayList<>();
		TreeTableNode node = aNode;

		while (node != root | node.getParent() != null) {
			path.add(0, node);
			node = node.getParent();
		}

		// ensure root node is added
		path.add(0, node);

		return path.toArray(new MutableTreeTableNode[0]);
	}

}
