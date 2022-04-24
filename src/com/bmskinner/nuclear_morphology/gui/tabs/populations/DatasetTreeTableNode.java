package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.swing.tree.TreeNode;

import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.jdesktop.swingx.treetable.TreeTableNode;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * A node that can hold a dataset
 * 
 * @author ben
 *
 */
public class DatasetTreeTableNode implements MutableTreeTableNode {

	private static final int COL_COUNT = 3;

	IAnalysisDataset dataset = null;

	protected MutableTreeTableNode parent = null;

	protected final List<MutableTreeTableNode> children = new ArrayList<>();

	public DatasetTreeTableNode(IAnalysisDataset d) {
		dataset = d;
	}

	@Override
	public Object getValueAt(int column) {
		return switch (column) {
		case 0 -> dataset;
		case 1 -> String.valueOf(dataset.getCollection().size());
		case 2 -> dataset.getDatasetColour();
		default -> null;
		};
	}

	@Override
	public TreeTableNode getChildAt(int childIndex) {
		return children.get(childIndex);
	}

	@Override
	public int getColumnCount() {
		return COL_COUNT;
	}

	@Override
	public TreeTableNode getParent() {
		return parent;
	}

	@Override
	public boolean isEditable(int column) {
		return false;
	}

	@Override
	public void setValueAt(Object aValue, int column) {
		// noop, this is immutable
	}

	@Override
	public Object getUserObject() {
		return dataset;
	}

	@Override
	public void setUserObject(Object userObject) {
		// noop, this is immutable
	}

	@Override
	public int getChildCount() {
		return children.size();
	}

	@Override
	public int getIndex(TreeNode node) {
		return children.indexOf(node);
	}

	@Override
	public boolean getAllowsChildren() {
		return true;
	}

	@Override
	public boolean isLeaf() {
		return !children.isEmpty();
	}

	@Override
	public Enumeration<? extends MutableTreeTableNode> children() {
		return Collections.enumeration(children);
	}

	public void add(MutableTreeTableNode child) {
		insert(child, getChildCount());
	}

	@Override
	public void insert(MutableTreeTableNode child, int index) {
		if (children.contains(child)) {
			children.remove(child);
			index--;
		}

		children.add(index, child);

		if (child.getParent() != this) {
			child.setParent(this);
		}
	}

	@Override
	public void remove(int index) {
		children.remove(index).setParent(null);
	}

	@Override
	public void remove(MutableTreeTableNode node) {
		children.remove(node);
		node.setParent(null);
	}

	@Override
	public void removeFromParent() {
		parent.remove(this);
	}

	@Override
	public void setParent(MutableTreeTableNode newParent) {
		if (this != newParent)
			parent = newParent;
	}

}
