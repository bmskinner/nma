package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.events.ChartSetEventListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ConsensusUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.NuclearSignalUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.gui.tabs.TableDetailPanel;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.datasets.SignalTableCell;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.tables.NuclearSignalTableCreator;

public class SignalTablePanel extends TableDetailPanel
		implements ChartSetEventListener, ConsensusUpdatedListener, NuclearSignalUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(SignalsOverviewPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Signal stats";

	/** signal stats */
	private ExportableTable statsTable;

	/** Signal visibility checkbox panel */
	private JPanel checkboxPanel;

	/** Launch signal warping */
	private JButton warpButton;

	/** Launch signal merging */
	private JButton mergeButton;

	/** Show signal radius or just CoM */
	boolean isShowAnnotations = false;

	/** Messages to clarify when UI is disabled */
	private JLabel headerText;

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

		JScrollPane scrollPane = new JScrollPane(statsTable);
		return scrollPane;
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

//			getInterfaceEventHandler().fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
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

	private SignalTableCell getSignalGroupFromTable(JTable table, int row, int column) {
		return (SignalTableCell) table.getModel().getValueAt(row, column);
	}

	/**
	 * Update the signal stats with the given datasets
	 * 
	 * @param list the datasets
	 * @throws Exception
	 */
	private void updateSignalStatsPanel() {

		TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).setTarget(statsTable)
				.setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new SignalTableCellRenderer()).build();

		setTable(options);

	}

	@Override
	protected void updateSingle() {
		updateMultiple();

	}

	@Override
	protected void updateMultiple() {
		updateSignalStatsPanel();
	}

	@Override
	protected void updateNull() {
		updateMultiple();

	}

	@Override
	public void setLoading() {
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
	public void consensusUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void consensusUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void nuclearSignalUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void nuclearSignalUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}
}
