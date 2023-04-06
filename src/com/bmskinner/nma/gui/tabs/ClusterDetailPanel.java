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
package com.bmskinner.nma.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.components.renderers.JTextAreaCellRenderer;
import com.bmskinner.nma.gui.dialogs.ClusterTreeDialog;
import com.bmskinner.nma.gui.dialogs.DimensionalityReductionPlotDialog;
import com.bmskinner.nma.gui.events.ClusterGroupsUpdatedListener;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.AnalysisDatasetTableCreator;
import com.bmskinner.nma.visualisation.tables.ClusterGroupTableModel;

/**
 * This panel shows any cluster groups that have been created, and the
 * clustering options that were used to create them.
 * 
 * @author bms41
 * @since 1.9.0
 *
 */
@SuppressWarnings("serial")
public class ClusterDetailPanel extends TableDetailPanel implements ClusterGroupsUpdatedListener {

	private static final String PANEL_TITLE_LBL = "Clusters";
	private static final String PANEL_DESC_LBL = "Show clustering parameters and display cluster outputs";

	private static final String NO_CLUSTERS_LBL = "No clusters present";

	private JLabel statusLabel = new JLabel(NO_CLUSTERS_LBL, SwingConstants.CENTER);
	private JPanel statusPanel = new JPanel(new BorderLayout());

	private JPanel mainPanel;
	private ExportableTable table;

	public ClusterDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		this.setLayout(new BorderLayout());

		mainPanel = createMainPanel();
		statusPanel = createHeader();

		this.add(mainPanel, BorderLayout.CENTER);
		this.add(statusPanel, BorderLayout.NORTH);

		setEnabled(false);

		UIController.getInstance().addClusterGroupsUpdatedListener(this);

	}

	/**
	 * Create the main panel with cluster table
	 * 
	 * @return
	 */
	private JPanel createMainPanel() {
		JPanel panel = new JPanel();

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		TableModel optionsModel = AbstractTableCreator.createBlankTable();

		TableCellRenderer buttonRenderer = new JButtonRenderer();
		TableCellRenderer textRenderer = new JTextAreaCellRenderer(false);

		table = new ExportableTable(optionsModel) {

			@Override
			public boolean isCellEditable(int rowIndex, int columnIndex) {
				return false;
			}

			@Override
			public TableCellRenderer getCellRenderer(int row, int column) {
				if ((this.getValueAt(row, 0).equals(Labels.Clusters.TREE)
						|| this.getValueAt(row, 0).equals(Labels.Clusters.CLUSTER_DIM_PLOT))
						&& column > 0
						&& !(getValueAt(row, column).equals(Labels.NA))) {
					return buttonRenderer;
				}
				return textRenderer;
			}
		};

		MouseListener mouseListener = new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				int col = table.columnAtPoint(e.getPoint());
				if (col == 0)
					return;

				ClusterGroupTableModel model = (ClusterGroupTableModel) table.getModel();

				IClusterGroup group = model.getClusterGroup(table.convertColumnIndexToModel(col));
				IAnalysisDataset d = model.getDataset(table.convertColumnIndexToModel(col));

				if (table.getValueAt(row, 0).equals(Labels.Clusters.TREE)
						&& !table.getValueAt(row, col).equals(Labels.NA)) {
					Runnable r = () -> new ClusterTreeDialog(d, group);
					new Thread(r).start();
				}

				if (table.getValueAt(row, 0).equals(Labels.Clusters.CLUSTER_DIM_PLOT)
						&& !table.getValueAt(row, col).equals(Labels.NA)) {
					Runnable r = () -> new DimensionalityReductionPlotDialog(d, group);
					new Thread(r).start();
				}

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mousePressed(MouseEvent e) {
				// Not needed
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// Not needed
			}

		};

		table.addMouseListener(mouseListener);

		table.setRowSelectionAllowed(false);

		JScrollPane scrollPane = new JScrollPane(table);

		JPanel tablePanel = new JPanel(new BorderLayout());

		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);

		panel.add(tablePanel);
		return panel;

	}

	/**
	 * This panel shows the status of the dataset, and holds the clustering button
	 * 
	 * @return
	 */
	private JPanel createHeader() {

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(statusLabel, BorderLayout.CENTER);
		return panel;
	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();

	}

	@Override
	protected synchronized void updateMultiple() {
		setEnabled(true);

		TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).setTarget(table)
				.build();

		setTable(options);

		if (!hasDatasets()) {
			statusLabel.setText(Labels.NULL_DATASETS);
			setEnabled(false);
		} else {

			if (isSingleDataset()) {

				setEnabled(true);

				if (!activeDataset().hasClusters()) {

					statusLabel.setText(NO_CLUSTERS_LBL);

				} else {
					int nGroups = activeDataset().getClusterGroups().size();
					String plural = nGroups == 1 ? "" : "s";
					statusLabel.setText(
							"Dataset has " + activeDataset().getClusterGroups().size()
									+ " cluster group" + plural);
				}
			} else { // more than one dataset selected
				statusLabel.setText(Labels.MULTIPLE_DATASETS);
				setEnabled(false);
			}
		}
	}

	@Override
	protected synchronized void updateNull() {
		updateMultiple();

	}

	@Override
	protected TableModel createPanelTableType(@NonNull TableOptions options) {
		return new AnalysisDatasetTableCreator(options).createClusterOptionsTable();
	}

	/**
	 * Render a button in a cell. Note, this is non-functional - it just paints a
	 * button shape. Use a mouse listener on the table for functionality
	 * 
	 * @author bms41
	 * @since 1.16.0
	 *
	 */
	private class JButtonRenderer extends JButton implements TableCellRenderer {
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus,
				int row, int column) {

			String text = "";
			if (null != value) {
				text = value instanceof IClusterGroup
						? Labels.Clusters.CLUSTER_SHOW_TREE
						: value.toString();
			}

			setText(text);
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			return this;
		}
	}

	@Override
	public void clusterGroupsUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void clusterGroupsUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void clusterGroupAdded(IAnalysisDataset dataset, IClusterGroup group) {
		refreshCache(dataset);
	}

}
