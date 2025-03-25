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
package com.bmskinner.nma.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.Labels;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.components.renderers.JTextAreaCellRenderer;
import com.bmskinner.nma.gui.events.NuclearSignalUpdatedListener;
import com.bmskinner.nma.gui.tabs.TableDetailPanel;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.NuclearSignalDetectionTableModel;
import com.bmskinner.nma.visualisation.tables.NuclearSignalTableCreator;

@SuppressWarnings("serial")
public class SignalsAnalysisPanel extends TableDetailPanel implements NuclearSignalUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(SignalsAnalysisPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Detection settings";
	private static final String PANEL_DESC_LBL = "Settings used to detect signals";

	private ExportableTable table; // table for analysis parameters

	public SignalsAnalysisPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		this.setLayout(new BorderLayout());
		uiController.addNuclearSignalUpdatedListener(this);

		table = new ExportableTable();
		table.setModel(AbstractTableCreator.createBlankTable());
		table.setEnabled(false);
		table.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {

				if (e.getClickCount() != 2)
					return;

				if (!(table.getModel() instanceof NuclearSignalDetectionTableModel))
					return;

				int row = table.rowAtPoint(e.getPoint());
				int column = table.columnAtPoint(e.getPoint());

				NuclearSignalDetectionTableModel model = (NuclearSignalDetectionTableModel) table
						.getModel();

				String rowName = model.getValueAt(row, 0).toString();
				UUID signalGroupId = model.getSignalGroup(row, column);

				if (signalGroupId == null)
					return;

				IAnalysisDataset d = model.getDataset(column);

				if (rowName.equals(Labels.Signals.SIGNAL_SOURCE_LABEL)) {
					cosmeticHandler.updateSignalSource(d, signalGroupId);
				}

				if (rowName.equals(Labels.Signals.SIGNAL_GROUP_LABEL)) {
					cosmeticHandler.renameSignalGroup(d, signalGroupId);
				}

				// Empty string rowname has colour
				if (rowName.equals(Labels.EMPTY_STRING)) {
					cosmeticHandler.changeSignalColour(d, signalGroupId);
				}

			}
		});

		table.setDefaultRenderer(Object.class, new JTextAreaCellRenderer());

		JScrollPane scrollPane = new JScrollPane(table);

		JPanel tablePanel = new JPanel(new BorderLayout());

		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);

		this.add(tablePanel, BorderLayout.CENTER);
	}

	@Override
	protected synchronized void updateSingle() {

		TableOptions options = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.setTarget(table)
				.setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN,
						new SignalDetectionSettingsTableCellRenderer())
				.build();

		setTable(options);
	}

	@Override
	protected synchronized void updateMultiple() {
		updateSingle();
	}

	@Override
	protected synchronized void updateNull() {

		TableOptions options = new TableOptionsBuilder().setDatasets(null).build();

		TableModel model = getTable(options);
		table.setModel(model);
		table.createDefaultColumnsFromModel();

	}

	@Override
	public void setLoading() {
		super.setLoading();
		table.setModel(AbstractTableCreator.createLoadingTable());

	}

	@Override
	protected TableModel createPanelTableType(TableOptions options) {
		return new NuclearSignalTableCreator(options).createSignalDetectionParametersTable();
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
