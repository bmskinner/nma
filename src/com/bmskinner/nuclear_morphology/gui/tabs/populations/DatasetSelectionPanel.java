package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreeSelectionModel;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.events.revamp.DatasetAddedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.SwatchUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class DatasetSelectionPanel extends DetailPanel implements DatasetAddedListener, SwatchUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(DatasetsPanel.class.getName());

	private final DatasetTreeTable treeTable;

	private final DatasetTreeTableModel model = new DatasetTreeTableModel();

	private final TreeSelectionHandler treeListener = new TreeSelectionHandler();

	private boolean ctrlPressed = false;

	public boolean isCtrlPressed() {
		synchronized (DatasetSelectionPanel.class) {
			return ctrlPressed;
		}
	}

	public DatasetSelectionPanel() {
		super();
		this.setLayout(new BorderLayout());

		this.setMinimumSize(new Dimension(100, 100));

		treeTable = new DatasetTreeTable(model);
		treeTable.getTreeSelectionModel().addTreeSelectionListener(treeListener);

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
	public void datasetSelectionUpdated(IAnalysisDataset d) {
		// no action if dataset selection is not different to current
//		if (!datasetSelectionOrder.equals(List.of(d))) {
//			update(List.of(d));
//		}
	}

	@Override
	public void datasetSelectionUpdated(List<IAnalysisDataset> d) {
		// no action if dataset selection is not different to current
//		if (!datasetSelectionOrder.equals(d)) {
//			update(d);
//		}
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
		for (IAnalysisDataset d : datasets)
			model.addDataset(d);

//		update(List.of(datasets.get(datasets.size() - 1)));
		// This will also trigger a dataset update event as the dataset
		// is selected, so don't trigger another update here.
	}

	@Override
	public void swatchUpdated() {
		update(getDatasets());
	}

	/**
	 * Establish the rows in the population tree that are currently selected. Set
	 * the possible menu options accordingly, and call the panel updates
	 */
	public class TreeSelectionHandler implements TreeSelectionListener {

		/**
		 * This tracks which datasets are currently selected, and the order in which
		 * they were selected.
		 */
		private final List<IAnalysisDataset> datasetSelectionOrder = new ArrayList<>();

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			try {
				if (!isCtrlPressed())
					datasetSelectionOrder.clear();

				// Track the datasets currently selected
				TreeSelectionModel lsm = (TreeSelectionModel) e.getSource();

				datasetSelectionOrder.addAll(getSelectedDatasets(lsm));

				DatasetListManager.getInstance().setSelectedDatasets(datasetSelectionOrder);

			} catch (Exception ex) {
				LOGGER.warning("Error in tree selection handler");
				LOGGER.log(Loggable.STACK, "Error in tree selection handler", ex);
			}
		}

		/**
		 * Get the currently selected indexes containing datasets in the table, mapped
		 * to the dataset order
		 * 
		 * @param lsm
		 * @return a map of which indexes are selected: table index : dataset index in
		 *         the selection order
		 */
		private List<IAnalysisDataset> getSelectedDatasets(TreeSelectionModel lsm) {

			List<IAnalysisDataset> datasets = new ArrayList<>();

			int[] selectedRows = lsm.getSelectionRows();
			for (int i = 0; i < selectedRows.length; i++) {
				LOGGER.fine("Selected row " + selectedRows[i]);
				if (treeTable.getValueAt(selectedRows[i], 0)instanceof IAnalysisDataset d)
					datasets.add(d);
			}

			// Ctrl deselect happened - a dataset has been deselected
			// and must be removed from the datasetSelectionOrder map
			if (datasetSelectionOrder.size() > datasets.size()) {
				// Go through tree table and check for deselected dataset
				Iterator<IAnalysisDataset> it = datasetSelectionOrder.iterator();

				while (it.hasNext()) {
					IAnalysisDataset d = it.next();
					if (!datasets.contains(d))
						it.remove();
				}
			}
			return datasets;

		}

		private Map<Integer, Integer> getSelectedIndexes(TreeSelectionModel lsm) {
			Map<Integer, Integer> selectedIndexes = new HashMap<>();
			List<IAnalysisDataset> datasets = new ArrayList<>();

			int[] selectedRows = lsm.getSelectionRows();
			for (int i = 0; i < selectedRows.length; i++) {
				LOGGER.fine("Selected row " + selectedRows[i]);
				if (treeTable.getValueAt(selectedRows[i], 0)instanceof IAnalysisDataset d) {
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
			// and must be removed from the datasetSelectionOrder map
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

}
