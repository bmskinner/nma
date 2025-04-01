package com.bmskinner.nma.gui.tabs.populations;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JScrollPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jdesktop.swingx.treetable.TreeTableNode;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.CtrlPressedListener;
import com.bmskinner.nma.gui.events.ClusterGroupsUpdatedListener;
import com.bmskinner.nma.gui.events.DatasetAddedListener;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.events.WorkspaceAddedListener;
import com.bmskinner.nma.gui.tabs.DetailPanel;


@SuppressWarnings("serial")
public class DatasetSelectionPanel extends DetailPanel
		implements DatasetAddedListener, SwatchUpdatedListener, ClusterGroupsUpdatedListener,
		WorkspaceAddedListener {

	private static final Logger LOGGER = Logger.getLogger(DatasetSelectionPanel.class.getName());

	private final transient DatasetTreeTable treeTable;

	private final transient DatasetTreeTableModel model = new DatasetTreeTableModel();

	private final transient TreeSelectionHandler treeListener = new TreeSelectionHandler();

	private transient CtrlPressedListener ctrlPress = new CtrlPressedListener(this);

	public DatasetSelectionPanel() {
		super();
		this.setLayout(new BorderLayout());

		this.setMinimumSize(new Dimension(100, 100));

		treeTable = new DatasetTreeTable(model);
		treeTable.getTreeSelectionModel().addTreeSelectionListener(treeListener);
		treeTable.addMouseListener(new DatasetMouseAdapter());
		treeTable.setBorder(BorderFactory.createEmptyBorder());

		JScrollPane js = new JScrollPane(treeTable);
		add(js, BorderLayout.CENTER);

		uiController.addDatasetAddedListener(this);
		uiController.addSwatchUpdatedListener(this);
		uiController.addClusterGroupsUpdatedListener(this);
		uiController.addWorkspaceAddedListener(this);
	}

	@Override
	public void datasetSelectionUpdated(IAnalysisDataset d) {
		datasetSelectionUpdated(List.of(d));
	}

	@Override
	public void datasetSelectionUpdated(List<IAnalysisDataset> datasets) {

		// don't get caught in infinite loops
		if (treeListener.datasetSelectionOrder.equals(datasets))
			return;

		DefaultListSelectionModel tsm = new DefaultListSelectionModel();

		for (IAnalysisDataset d : datasets) {

			int i = treeTable.getRowForPath(model.getPath(d));
			tsm.addSelectionInterval(i, i);
		}
		treeTable.setSelectionModel(tsm);

	}

	@Override
	public void datasetAdded(List<IAnalysisDataset> datasets) {
		addDataset(datasets);
	}

	@Override
	public void datasetAdded(IAnalysisDataset dataset) {
		addDataset(List.of(dataset));
	}

	@Override
	public void datasetDeleted(List<IAnalysisDataset> datasets) {
		for (IAnalysisDataset d : datasets)
			model.removeNode(d);
	}

	@Override
	public void workspaceAdded(IWorkspace ws) {
		TreePath path = model.addWorkspace(ws);
		expandAll(path);
	}

	@Override
	public void workspaceDeleted(IWorkspace ws) {
		model.removeNode(ws);
	}

	@Override
	public void datasetAdded(IWorkspace ws, IAnalysisDataset d) {
		TreePath path = model.addDatasetToWorkspace(ws, d);
		expandAll(path);
	}

	@Override
	public void datasetRemoved(IWorkspace ws, IAnalysisDataset d) {
		TreePath path = model.removeDatasetFromWorkspace(ws, d);
		expandAll(path);
	}

	/**
	 * Add the given dataset and all its children to the populations panel
	 * 
	 * @param dataset
	 */
	private synchronized void addDataset(final List<IAnalysisDataset> datasets) {
		for (IAnalysisDataset d : datasets) {

			// The first item in the path is the root node - don't expand this
			Runnable r = () -> model.addDataset(d);
			// Keep off the EDT
			ThreadManager.getInstance().submitUIUpdate(r);
			

			// Get the path for the dataset node to expand
			TreePath path = new TreePath(model.getPathToRoot(model.getNode(d)));
			expandAll(path);
		}
	}

	/**
	 * Expand all the nodes on the given path
	 * 
	 * @param parent
	 */
	private void expandAll(TreePath parent) {

		if (parent == null)
			return;

		TreeTableNode lastNode = (TreeTableNode) parent.getLastPathComponent();
		// expand the node
		expandNode(lastNode);

		// Traverse children and expand
		if (lastNode.getChildCount() > 0) {

			for (Enumeration<?> e = lastNode.children(); e.hasMoreElements();) {

				TreeTableNode n = (TreeTableNode) e.nextElement();
				if (n == null)
					continue;

				expandAll(new TreePath(model.getPathToRoot(n)));
			}
		}

	}

	private void expandNode(TreeTableNode node) {
		TreeTableNode[] nodes = model.getPathToRoot(node);
		TreePath tp = new TreePath(nodes);
		treeTable.expandPath(tp);
	}

	@Override
	public void globalPaletteUpdated() {
		update(getDatasets());
	}

	@Override
	public void colourUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
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

	private class DatasetMouseAdapter extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {

			int row = treeTable.rowAtPoint((e.getPoint()));
			int col = treeTable.columnAtPoint(e.getPoint());

			Object o = treeTable.getModel().getValueAt(row, 0);

			if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == DOUBLE_CLICK) {
				if (o instanceof IClusterGroup g)
					cosmeticHandler.renameClusterGroup(g);
				if (o instanceof IAnalysisDataset d)
					datasetClicked(d, col);
				if (o instanceof IWorkspace w)
					cosmeticHandler.renameWorkspace(w);
			}

			if (e.getButton() == MouseEvent.BUTTON3) {
				// No actions yet
			}
		}

		private void datasetClicked(IAnalysisDataset d, int column) {
			if (column == 0)
				cosmeticHandler.renameDataset(d);

			if (column == 2)
				cosmeticHandler.changeDatasetColour(d);
		}

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
		public final List<IAnalysisDataset> datasetSelectionOrder = new ArrayList<>();

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			try {

				if (!ctrlPress.isCtrlPressed())
					datasetSelectionOrder.clear();

				// Find the selected rows
				TreeSelectionModel lsm = (TreeSelectionModel) e.getSource();

				// Track the order in which the rows are selected
				setSelectedDatasets(lsm);

				// Update table header with number of selected cells
				int cellCount = datasetSelectionOrder.stream().map(d -> d.getCollection().size())
						.reduce(0, Integer::sum);

				treeTable.getColumnModel().getColumn(0)
						.setHeaderValue(
								String.format("Dataset (%d)", datasetSelectionOrder.size()));

				treeTable.getColumnModel().getColumn(1)
						.setHeaderValue(String.format("Cells (%d)", cellCount));

				DatasetListManager.getInstance().setSelectedDatasets(datasetSelectionOrder);

			} catch (Exception ex) {
				LOGGER.log(Level.SEVERE, "Error in tree selection handler: %s".formatted(ex.getMessage()),
						ex);
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
			List<Integer> currentHashCodes = datasetSelectionOrder.stream()
					.map(IAnalysisDataset::hashCode) .collect(Collectors.toCollection(ArrayList::new));
			
			List<IAnalysisDataset> datasets = new ArrayList<>();
			
			for (int i = 0; i < selectedRows.length; i++) {

				if (treeTable.getValueAt(selectedRows[i], 0)instanceof IAnalysisDataset d) {
					datasets.add(d);
					if (!currentHashCodes.contains(d.hashCode())) {
						datasetSelectionOrder.add(d);
						currentHashCodes.add(d.hashCode());
					}
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
