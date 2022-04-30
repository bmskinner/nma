package com.bmskinner.nma.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.events.ChartSetEventListener;
import com.bmskinner.nma.gui.events.NuclearSignalUpdatedListener;
import com.bmskinner.nma.gui.events.ScaleUpdatedListener;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.gui.tabs.TableDetailPanel;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.datasets.SignalTableCell;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.NuclearSignalTableCreator;

public class SignalTablePanel extends TableDetailPanel
		implements ChartSetEventListener, NuclearSignalUpdatedListener, ScaleUpdatedListener {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = Logger.getLogger(SignalTablePanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Signal stats";

	/** signal stats */
	private ExportableTable statsTable;

	/** Show signal radius or just CoM */
	boolean isShowAnnotations = false;

	/**
	 * Create with an input supplier
	 * 
	 * @param inputSupplier the input supplier
	 */
	public SignalTablePanel() {
		super();

		setLayout(new BorderLayout());

		JScrollPane scrollPane = createStatsPane();
		add(scrollPane, BorderLayout.CENTER);

		uiController.addNuclearSignalUpdatedListener(this);
		uiController.addScaleUpdatedListener(this);

	}

	@Override
	public String getPanelTitle() {
		return PANEL_TITLE_LBL;
	}

	private JScrollPane createStatsPane() {

		TableModel tableModel = AbstractTableCreator.createBlankTable();
		statsTable = new ExportableTable(tableModel); // table for basic stats
		statsTable.setEnabled(false);

		statsTable.addMouseListener(new SignalStatsTableMouseListener());

		return new JScrollPane(statsTable);
	}

	/**
	 * Listener for interaction with the signal stats table
	 * 
	 * @author bms41
	 * @since 1.15.0
	 *
	 */
	private class SignalStatsTableMouseListener extends MouseAdapter {

		private SignalStatsTableMouseListener() {
			super();
		}

		private SignalTableCell getSignalGroupFromTable(JTable table, int row, int column) {
			return (SignalTableCell) table.getModel().getValueAt(row, column);
		}

		private boolean isSignalIdRow(JTable table, int row) {
			return table.getModel().getValueAt(row, 0).toString().equals(Labels.Signals.SIGNAL_ID_LABEL);
		}

		private boolean isSignalColourRow(JTable table, int row) {
			int nextRow = row + 1;
			String nextRowName = table.getModel().getValueAt(nextRow, 0).toString();
			return nextRowName.equals(Labels.Signals.SIGNAL_GROUP_LABEL);
		}

		private void offerToDeleteSignals(JTable table, int row, int col) {
			IAnalysisDataset d = getDatasets().get(col - 1);
			int signalGroupNameRow = row - 3;
			String signalGroupRowName = table.getModel().getValueAt(signalGroupNameRow, 0).toString();
			String signalGroupName = table.getModel().getValueAt(row, col).toString();
			if (signalGroupRowName.equals(Labels.Signals.SIGNAL_GROUP_LABEL))
				signalGroupName = table.getModel().getValueAt(signalGroupNameRow, col).toString();

			UUID signalGroup = UUID.fromString(table.getModel().getValueAt(row, col).toString());

			String[] options = { "Don't delete signals", "Delete signals" };

			try {
				int result = getInputSupplier().requestOptionAllVisible(options,
						String.format("Delete signal group %s in %s?", signalGroupName, d.getName()),
						"Delete signal group?");
				if (result != 0) {
					d.getCollection().getSignalManager().removeSignalGroup(signalGroup);
					UIController.getInstance().fireNuclearSignalUpdated(d);
				}

			} catch (RequestCancelledException e1) {
			} // no action
		}

		@Override
		public void mouseClicked(MouseEvent e) {

			JTable table = (JTable) e.getSource();

			int row = table.rowAtPoint(e.getPoint());
			int column = table.columnAtPoint(e.getPoint());

			if (e.getClickCount() == DOUBLE_CLICK && column > 0) {

				IAnalysisDataset d = getDatasets().get(column - 1);

				if (isSignalColourRow(table, row)) {
					SignalTableCell signalGroup = getSignalGroupFromTable(table, row + 1, column);
					cosmeticHandler.changeSignalColour(d, signalGroup.getID());
					UIController.getInstance().fireNuclearSignalUpdated(d);
				}

				if (isSignalIdRow(table, row))
					offerToDeleteSignals(table, row, column);
			}
		}

	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();
	}

	@Override
	protected synchronized void updateMultiple() {

		TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).setTarget(statsTable)
				.setScale(GlobalOptions.getInstance().getScale())
				.setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new SignalTableCellRenderer()).build();
		setTable(options);
	}

	@Override
	protected synchronized void updateNull() {
		updateMultiple();

	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		statsTable.setModel(AbstractTableCreator.createLoadingTable());
	}

	@Override
	protected TableModel createPanelTableType(@NonNull TableOptions options) {
		return new NuclearSignalTableCreator(options).createSignalStatsTable();
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		((ExportableChartPanel) e.getSource()).restoreAutoBounds();

	}

	@Override
	public void nuclearSignalUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void nuclearSignalUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void scaleUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void scaleUpdated() {
		update();
	}

}
