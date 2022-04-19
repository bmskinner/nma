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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.events.revamp.DatasetAddedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.SwatchUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The populations panel holds the list of open datasets for selection by the
 * user.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class DatasetsPanel extends DetailPanel implements DatasetAddedListener, SwatchUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(DatasetsPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Datasets";

	private final PopulationTreeTable treeTable;

	private PopulationListPopupMenu populationPopup;

	/**
	 * This tracks which datasets are currently selected, and the order in which
	 * they were selected.
	 */
	private final List<IAnalysisDataset> datasetSelectionOrder = new ArrayList<>();

	private final TreeSelectionHandler treeListener = new TreeSelectionHandler();

	private boolean ctrlPressed = false;

	public boolean isCtrlPressed() {
		synchronized (DatasetsPanel.class) {
			return ctrlPressed;
		}
	}

	public DatasetsPanel() {
		super();
		this.setLayout(new BorderLayout());

		this.setMinimumSize(new Dimension(100, 100));

		populationPopup = new PopulationListPopupMenu();
		populationPopup.setEnabled(false);

		treeTable = createTreeTable();

		JScrollPane populationScrollPane = new JScrollPane(treeTable);

		// Track when the Ctrl key is down
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ke -> {

			synchronized (DatasetsPanel.class) {
				switch (ke.getID()) {
				case KeyEvent.KEY_PRESSED:
					if (ke.getKeyCode() == KeyEvent.VK_CONTROL)
						ctrlPressed = true;
					break;
				case KeyEvent.KEY_RELEASED:
					if (ke.getKeyCode() == KeyEvent.VK_CONTROL)
						ctrlPressed = false;
					break;
				default:
					break;
				}

				return false;
			}
		});

		this.add(populationScrollPane, BorderLayout.CENTER);

		uiController.addDatasetAddedListener(this);
		uiController.addSwatchUpdatedListener(this);
	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE_LBL;
	}

	@Override
	public synchronized void update(final List<IAnalysisDataset> list) {
		this.update();
		treeTable.selectDatasets(list);
		treeTable.repaint();
	}

	/**
	 * Find the populations in memory, and display them in the population chooser.
	 * Root populations are ordered according to position in the treeListOrder map.
	 */
	@Override
	public synchronized void update() {

		int nameColWidth = treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).getWidth();
		int colourColWidth = treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).getWidth();

		/*
		 * Determine the ids of collapsed datasets, and store them
		 */
		List<Object> collapsedRows = treeTable.getCollapsedRows();

		// TODO: Need to modify the model, not replace it to keep ordering
		PopulationTreeTableModel newModel = new PopulationTreeTableModel();
		treeTable.setTreeTableModel(newModel);

		/*
		 * Collapse the same ids as saved earlier
		 */
		treeTable.setCollapsedRows(collapsedRows);

		treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).setWidth(nameColWidth);
		treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).setWidth(colourColWidth);
	}

	@Override
	public synchronized void setLoading() {
		// No charts or tables to load
	}

	private PopulationTreeTable createTreeTable() {

		PopulationTreeTableModel treeTableModel = new PopulationTreeTableModel();

		PopulationTreeTable table = new PopulationTreeTable(treeTableModel);

		table.addMouseListener(new MouseAdapter() {

			private static final int DOUBLE_CLICK = 2;

			@Override
			public void mouseClicked(MouseEvent e) {

				PopulationTreeTable table = (PopulationTreeTable) e.getSource();

				int row = table.rowAtPoint((e.getPoint()));
				int column = table.columnAtPoint(e.getPoint());

				Object o = table.getModel().getValueAt(row, PopulationTreeTable.COLUMN_NAME);

				if (e.getClickCount() == DOUBLE_CLICK) { // double click
					if (o instanceof IClusterGroup)
						clusterGroupClicked((IClusterGroup) o, row, column);
					if (o instanceof IAnalysisDataset)
						datasetClicked((IAnalysisDataset) o, row, column);
					if (o instanceof IWorkspace)
						workspaceClicked((IWorkspace) o, row, column);
				}

				if (e.getButton() == MouseEvent.BUTTON3) // right click - show the popup
					populationPopup.show(table, e.getX(), e.getY());
			}

			private void clusterGroupClicked(IClusterGroup g, int row, int column) {
				cosmeticHandler.renameClusterGroup(g);
				table.getModel().setValueAt(g, row, column); // ensure column length supports name by triggering update
			}

			private void workspaceClicked(IWorkspace w, int row, int column) {
				cosmeticHandler.renameWorkspace(w);
				table.getModel().setValueAt(w, row, column); // ensure column length supports name
			}

			private void datasetClicked(IAnalysisDataset d, int row, int column) {

				switch (column) {

				case PopulationTreeTable.COLUMN_NAME: {
					cosmeticHandler.renameDataset(d);
					table.getModel().setValueAt(d, row, column); // ensure column length supports name
					break;
				}

				case PopulationTreeTable.COLUMN_COLOUR: {
					cosmeticHandler.changeDatasetColour(d);
					break;
				}

				default:
					break;

				}
			}
		});

		TreeSelectionModel tableSelectionModel = table.getTreeSelectionModel();
		tableSelectionModel.addTreeSelectionListener(treeListener);
		return table;
	}

	/**
	 * Select the given dataset in the tree table
	 * 
	 * @param dataset the dataset to select
	 */
	private void selectDataset(@NonNull IAnalysisDataset dataset) {
		if (dataset != null) {
			List<IAnalysisDataset> list = new ArrayList<>();
			list.add(dataset);
			treeTable.selectDatasets(list);
		}
	}

	/**
	 * Select the given datasets in the tree table
	 * 
	 * @param dataset the dataset to select
	 */
	private void selectDatasets(List<IAnalysisDataset> list) {
		treeTable.selectDatasets(list);
		DatasetListManager.getInstance().setSelectedDatasets(list);
	}

	public void repaintTreeTable() {
		treeTable.repaint();
	}

	public synchronized void selectDataset(@NonNull UUID id) {
		IAnalysisDataset d = DatasetListManager.getInstance().getDataset(id);
		this.selectDataset(d);
		DatasetListManager.getInstance().setSelectedDataset(d);
	}

	/**
	 * Move the selected dataset in the list
	 * 
	 * @param isDown move the dataset down (true) or up (false)
	 */
	private void moveDataset(boolean isDown) {
		LOGGER.finer("Move dataset heard");
		List<IAnalysisDataset> datasets = DatasetListManager.getInstance().getSelectedDatasets();
		List<PopulationTreeTableNode> nodes = treeTable.getSelectedNodes();

		if (nodes.isEmpty() || nodes.size() > 1) {
			return;
		}

		// May be a dataset or cluster group selected
		IAnalysisDataset datasetToMove = datasets.isEmpty() ? null : datasets.get(0);

		// Get the node containing the dataset
		PopulationTreeTableModel model = (PopulationTreeTableModel) treeTable.getTreeTableModel();

		if (isDown) {
			model.moveNodesDown(nodes);
		} else {
			model.moveNodesUp(nodes);
		}

		if (datasetToMove != null) {
			selectDataset(datasetToMove);
		}

	}

	private synchronized void deleteSelectedDatasets() {
		final List<IAnalysisDataset> datasets = DatasetListManager.getInstance().getSelectedDatasets();
		final List<PopulationTreeTableNode> nodes = treeTable.getSelectedNodes();

		// Check if cluster groups need removing
		if (nodes.size() > datasets.size()) {
			// cluster groups are also selected, add to list
			for (PopulationTreeTableNode n : treeTable.getSelectedNodes()) {

				if (n.hasClusterGroup()) {
					IClusterGroup g = n.getGroup();
					for (UUID childID : g.getUUIDs()) {
						IAnalysisDataset child = DatasetListManager.getInstance().getDataset(childID);
						datasets.add(child);
					}

				}
			}
		}

		if (datasets.isEmpty())
			return;

		DatasetDeleter deleter = new DatasetDeleter(getInputSupplier());
		deleter.deleteDatasets(datasets);
		update();
		treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME).setHeaderValue("Dataset (0)");
		treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_CELL_COUNT).setHeaderValue("Cells (0)");
		DatasetListManager.getInstance().setSelectedDatasets(new ArrayList<>());
	}

	/**
	 * Establish the rows in the population tree that are currently selected. Set
	 * the possible menu options accordingly, and call the panel updates
	 */
	public class TreeSelectionHandler implements TreeSelectionListener {
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			try {

				if (!isCtrlPressed())
					datasetSelectionOrder.clear();

				// Track the datasets currently selected
				TreeSelectionModel lsm = (TreeSelectionModel) e.getSource();

				// Correlate dataset index with the order it was selected in
				Map<Integer, Integer> selectedIndexes = getSelectedIndexes(lsm);

				DatasetListManager.getInstance().setSelectedDatasets(datasetSelectionOrder);

				PopulationTableCellRenderer rend = new PopulationTableCellRenderer(selectedIndexes);

				// Update the table headers
				treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_COLOUR).setCellRenderer(rend);
				treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_NAME)
						.setHeaderValue(String.format("Dataset (%d)", datasetSelectionOrder.size()));
				treeTable.getColumnModel().getColumn(PopulationTreeTable.COLUMN_CELL_COUNT)
						.setHeaderValue(String.format("Cells (%d)", getCellTotal()));

				final List<Object> selectedObjects = new ArrayList<>();
				for (int i : selectedIndexes.keySet()) {
					selectedObjects.add(treeTable.getValueAt(i, PopulationTreeTable.COLUMN_NAME));
				}
				populationPopup.updateSelectionContext(selectedObjects);

			} catch (Exception ex) {
				LOGGER.warning("Error in tree selection handler");
				LOGGER.log(Loggable.STACK, "Error in tree selection handler", ex);
			}
		}

		private int getCellTotal() {
			int cellTotal = 0;
			for (IAnalysisDataset d : datasetSelectionOrder) {
				cellTotal += d.getCollection().size();
			}
			return cellTotal;
		}

		/**
		 * Get the currently selected indexes containing datasets in the table, mapped
		 * to the dataset order
		 * 
		 * @param lsm
		 * @return a map of which indexes are selected: table index : dataset index in
		 *         the selection order
		 */
		private Map<Integer, Integer> getSelectedIndexes(TreeSelectionModel lsm) {
			Map<Integer, Integer> selectedIndexes = new HashMap<>();
			List<IAnalysisDataset> datasets = new ArrayList<>();

			if (!lsm.isSelectionEmpty()) {
				// Find out which indexes are selected.
				int minIndex = lsm.getMinSelectionRow();
				int maxIndex = lsm.getMaxSelectionRow();
				for (int i = minIndex; i <= maxIndex; i++) {
					if (lsm.isRowSelected(i) && treeTable.isDataset(i)) {

						IAnalysisDataset d = treeTable.getDatasetAtRow(i);
						datasets.add(d);
						datasetSelectionOrder.add(d);

						int selectionIndex = 0;
						for (IAnalysisDataset an : datasetSelectionOrder) {

							if (an == d) {
								selectedIndexes.put(i, selectionIndex);
								break;
							}
							selectionIndex++;
						}
					}
				}

				// Ctrl deselect happened - a dataset has been deselected
				// and remains in the
				// datasetSelectionOrder map
				if (datasetSelectionOrder.size() > datasets.size()) {
					// Go through tree table and check for deselected dataset
					Iterator<IAnalysisDataset> it = datasetSelectionOrder.iterator();

					while (it.hasNext()) {
						IAnalysisDataset d = it.next();
						if (!datasets.contains(d)) {
							it.remove();
						}
					}

					// Adjust the indexes of the remaining datasets
					fixDiscontinuousPositions(selectedIndexes);

				}
			}
			return selectedIndexes;
		}

		private Map<Integer, Integer> fixDiscontinuousPositions(Map<Integer, Integer> selectedIndexes) {
			// Find a discontinuity in the indexes - one value is missing
			List<Integer> values = new ArrayList<>(selectedIndexes.values());
			Collections.sort(values);

			int prev = -1;
			for (int i : values) {
				if (i - prev > 1) {
					// a value was skipped
					for (Entry<Integer, Integer> entry : selectedIndexes.entrySet()) {
						int k = entry.getKey();
						int j = entry.getValue();
						if (j == i) { // this is the entry that is too high
							selectedIndexes.put(k, j - 1); // Move index down by
							// 1
						}
					}
					fixDiscontinuousPositions(selectedIndexes); // there will now be a new
					// discontinuity. Fix until end of list
				}
				prev = i;
			}
			return selectedIndexes;
		}

	}

//	@Override
//	public void eventReceived(UserActionEvent event) {
//
//		switch (event.type()) {
//		// catch any signals that affect the datasets directly
//		case UserActionEvent.MOVE_DATASET_DOWN_ACTION:
//			moveDataset(true);
//			break;
//		case UserActionEvent.MOVE_DATASET_UP_ACTION:
//			moveDataset(false);
//			break;
//		case UserActionEvent.DELETE_DATASET:
//			deleteSelectedDatasets();
//			break;
//		}
//
//	}

	@Override
	public void datasetSelectionUpdated(IAnalysisDataset d) {
		// no action if dataset selection is not different to current
		if (!datasetSelectionOrder.equals(List.of(d))) {
			update(List.of(d));
		}
	}

	@Override
	public void datasetSelectionUpdated(List<IAnalysisDataset> d) {
		// no action if dataset selection is not different to current
		if (!datasetSelectionOrder.equals(d)) {
			update(d);
		}
	}

	@Override
	public void datasetAdded(List<IAnalysisDataset> datasets) {
		addDataset(datasets);
	}

	@Override
	public void datasetAdded(IAnalysisDataset dataset) {
		addDataset(List.of(dataset));
	}

	/**
	 * Add the given dataset and all its children to the populations panel
	 * 
	 * @param dataset
	 */
	private synchronized void addDataset(final List<IAnalysisDataset> datasets) {
		update(List.of(datasets.get(datasets.size() - 1)));
		// This will also trigger a dataset update event as the dataset
		// is selected, so don't trigger another update here.
	}

	@Override
	public void swatchUpdated() {
		update(getDatasets());
	}
}
