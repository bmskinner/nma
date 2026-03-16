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
package com.bmskinner.nma.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;


@SuppressWarnings("serial")
public class CellsListPanel extends AbstractCellDetailPanel implements TreeSelectionListener {

	private static final Logger LOGGER = Logger.getLogger(CellsListPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Cell list";
	private final JTree tree;

	public CellsListPanel(CellViewModel model) {
		super(model, PANEL_TITLE_LBL);
		this.setLayout(new BorderLayout());

		final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeData("Cells", null));
		final TreeModel treeModel = new DefaultTreeModel(root);
		tree = new JTree(treeModel);
		tree.addTreeSelectionListener(this);

		tree.setEnabled(false);
		final JScrollPane scrollPane = new JScrollPane(tree);
		final Dimension size = new Dimension(120, 200);
		scrollPane.setMinimumSize(size);
		scrollPane.setPreferredSize(size);

		this.add(scrollPane, BorderLayout.CENTER);
	}

	/**
	 * Trigger an update with a given dataset
	 * 
	 * @param dataset
	 */
	@Override
	protected void updateSingle() {
		final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeData("Cells", null));

		createNodes(root, activeDataset());
		tree.setEnabled(true);

		final TreeModel model = new DefaultTreeModel(root);

		tree.removeTreeSelectionListener(this);
		tree.setModel(model);

		// If a cell is still active in view, select it in the list
		if (this.getCellModel().hasCell()) {
			final DefaultMutableTreeNode node = getNode(this.getCellModel().getCell());

			if (node != null) {
				final TreePath path = new TreePath(node.getPath());

				tree.setSelectionPath(path);
			}
		}

		// Replace the listener
		tree.addTreeSelectionListener(this);
	}

	@Override
	protected void updateMultiple() {
		updateNull();
	}

	@Override
	protected void updateNull() {
		final DefaultMutableTreeNode root = new DefaultMutableTreeNode(new NodeData("Cells", null));

		tree.setEnabled(false);

		final TreeModel model = new DefaultTreeModel(root);

		tree.removeTreeSelectionListener(this);
		tree.setModel(model);
		tree.addTreeSelectionListener(this);
	}

	/**
	 * Create the nodes in the tree
	 * 
	 * @param root    the root node
	 * @param dataset the dataset to use
	 */
	private synchronized void createNodes(DefaultMutableTreeNode root, IAnalysisDataset dataset) {
		if (dataset == null)
			return;

		final List<ICell> cells = new ArrayList<>(dataset.getCollection().getCells());
		Collections.sort(cells);

		for (final ICell cell : cells) {

			final String name = cell.getNuclei().get(0).getNameAndNumber();
			final UUID id = cell.getId();

			root.add(new DefaultMutableTreeNode(new NodeData(name, id)));
		}

	}

	private DefaultMutableTreeNode getNode(ICell cell) {
		final DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();

		for (int i = 0; i < root.getChildCount() - 1; i++) {
			final DefaultMutableTreeNode n = (DefaultMutableTreeNode) root.getChildAt(i);
			final NodeData data = (NodeData) n.getUserObject();
			if (data.getID().equals(cell.getId()))
				return n;
		}
		return null;
	}

	public class NodeData {
		private final String name;
		private final UUID id;
		private String imageName;
		private int nucleusNumber;

		public NodeData(final String name, final UUID id) {
			this.name = name;
			this.id = id;
			if (!name.equals("Cells")) {
				final String[] array = name.split("\\.\\w+-"); // remove file
															// extension and dash,
															// leaving filename and
															// nucleus number
				this.imageName = array[0];

				try {
					nucleusNumber = Integer.valueOf(array[1]);
				} catch (final NumberFormatException e) {
					// Not the expected format of xxxx.tif-01
					// Maybe single nucleus images - xxxx.tif-01-uuid
					// Try parsing it out
					nucleusNumber = Integer.valueOf(array[1].split("_")[0]);
				}
			}

		}

		public String getName() {
			return name;
		}

		public UUID getID() {
			return id;
		}

		@Override
		public String toString() {
			if (name.equals("Cells"))
				return name;
			final NumberFormat df = DecimalFormat.getInstance();
			df.setMaximumFractionDigits(0);
			df.setMinimumIntegerDigits(2);
			return imageName + "-" + df.format(nucleusNumber);
		}
	}

	@Override
	public void valueChanged(TreeSelectionEvent arg0) {

		final DefaultMutableTreeNode node = (DefaultMutableTreeNode) arg0.getPath().getLastPathComponent();
		final NodeData data = (NodeData) node.getUserObject();

		if (this.isSingleDataset()) {
			try {

				if (null == data.id) { // only null for root
					this.getCellModel().setCell(null);
				} else {
					this.getCellModel().setCell(activeDataset().getCollection().getCell(data.id));
				}

			} catch (final Exception e1) {
				LOGGER.log(Level.SEVERE, "Error fetching cell %s: %s".formatted(data.name, e1.getMessage()), e1);
			}
		}

	}

	@Override
	public void update() {
		// No action
	}

}
