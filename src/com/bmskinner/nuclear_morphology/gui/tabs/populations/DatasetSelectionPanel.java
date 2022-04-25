package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.CtrlPressedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ClusterGroupsUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.DatasetAddedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.SwatchUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class DatasetSelectionPanel extends DetailPanel
		implements DatasetAddedListener, SwatchUpdatedListener, ClusterGroupsUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(DatasetsPanel.class.getName());

	private final DatasetTreeTable treeTable;

	private final DatasetTreeTableModel model = new DatasetTreeTableModel();

	private final TreeSelectionHandler treeListener = new TreeSelectionHandler();

	private CtrlPressedListener ctrlPress = new CtrlPressedListener(this);

	public DatasetSelectionPanel() {
		super();
		this.setLayout(new BorderLayout());

		this.setMinimumSize(new Dimension(100, 100));

		treeTable = new DatasetTreeTable(model);
		treeTable.getTreeSelectionModel().addTreeSelectionListener(treeListener);

		add(new JScrollPane(treeTable), BorderLayout.CENTER);

		uiController.addDatasetAddedListener(this);
		uiController.addSwatchUpdatedListener(this);
		uiController.addClusterGroupsUpdatedListener(this);
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
		for (IAnalysisDataset d : datasets) {
			TreePath path = model.addDataset(d);
			treeTable.expandPath(path);
		}
	}

	@Override
	public void swatchUpdated() {
		update(getDatasets());
	}

	@Override
	public void clusterGroupsUpdated(List<IAnalysisDataset> datasets) {
		// No action
	}

	@Override
	public void clusterGroupsUpdated(IAnalysisDataset dataset) {
		// No action
	}

	@Override
	public void clusterGroupAdded(IAnalysisDataset dataset, IClusterGroup group) {
		// Update the model
		TreePath path = model.addClusterGroup(group);
		treeTable.expandPath(path);
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

				if (!ctrlPress.isCtrlPressed())
					datasetSelectionOrder.clear();

				// Find the selected rows
				TreeSelectionModel lsm = (TreeSelectionModel) e.getSource();

				// Track the order in which the rows are selected
				setSelectedDatasets(lsm);
//
//				LOGGER.fine("Selection order:");
//				for (IAnalysisDataset d : datasetSelectionOrder)
//					LOGGER.fine(d.toString());

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
		private void setSelectedDatasets(TreeSelectionModel lsm) {

			// Add all the datasets in the selection to a new list
			int[] selectedRows = lsm.getSelectionRows();
			List<IAnalysisDataset> datasets = new ArrayList<>();
			for (int i = 0; i < selectedRows.length; i++) {
//				LOGGER.fine("Selected row " + selectedRows[i]);
				if (treeTable.getValueAt(selectedRows[i], 0)instanceof IAnalysisDataset d) {
					datasets.add(d);
					if (!datasetSelectionOrder.contains(d))
						datasetSelectionOrder.add(d);
				}
			}

			// If ctrl deselect happened - a dataset is deselected
			// and must be removed from the datasetSelectionOrder list
			if (datasetSelectionOrder.size() > datasets.size()) {
				// Go through tree table and check for the deselected dataset
				Iterator<IAnalysisDataset> it = datasetSelectionOrder.iterator();

				while (it.hasNext()) {
					IAnalysisDataset d = it.next();
					if (!datasets.contains(d))
						it.remove();
				}
			}
		}
	}

}
