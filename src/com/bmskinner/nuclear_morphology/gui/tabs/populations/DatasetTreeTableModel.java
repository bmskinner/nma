package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.util.logging.Logger;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

public class DatasetTreeTableModel extends AbstractTreeTableModel {

	private static final Logger LOGGER = Logger.getLogger(DatasetTreeTableModel.class.getName());

	private static final String[] COL_NAMES = { "Dataset", "Cell(s)", "" };

	public DatasetTreeTableModel() {
		// The root node is never seen in the UI
		super(new DefaultMutableTreeTableNode(""));
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

}
