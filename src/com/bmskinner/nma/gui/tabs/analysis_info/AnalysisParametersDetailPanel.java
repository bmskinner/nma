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
package com.bmskinner.nma.gui.tabs.analysis_info;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.components.renderers.JTextAreaCellRenderer;
import com.bmskinner.nma.gui.events.ScaleUpdatedListener;
import com.bmskinner.nma.gui.tabs.TableDetailPanel;
import com.bmskinner.nma.visualisation.options.AbstractOptions;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nma.visualisation.tables.AnalysisDatasetTableCreator;

/**
 * Display the nuclear detection parameters
 *
 */
@SuppressWarnings("serial")
public class AnalysisParametersDetailPanel extends TableDetailPanel
		implements ScaleUpdatedListener {

	private static final String PANEL_TITLE_LBL = "Detection parameters";
	private static final String PANEL_DESC_LBL = "Show nucleus detection parameters for datasets";

	private static final String HEADER_LBL = "Green rows have the same value in all columns";
	private ExportableTable table;
	JScrollPane scrollPane;

	public AnalysisParametersDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		this.setLayout(new BorderLayout());

		JPanel header = new JPanel();
		header.add(new JLabel(HEADER_LBL));

		this.add(header, BorderLayout.NORTH);
		this.add(createTablePanel(), BorderLayout.CENTER);

		uiController.addScaleUpdatedListener(this);

	}

	private JPanel createTablePanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		table = new ExportableTable();
		table.setModel(AbstractTableCreator.createBlankTable());

		table.setEnabled(false);
		table.setDefaultRenderer(Object.class, new JTextAreaCellRenderer());
		scrollPane = new JScrollPane(table);

		JPanel tablePanel = new JPanel(new BorderLayout());

		tablePanel.add(scrollPane, BorderLayout.CENTER);
		tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);

		panel.add(tablePanel);
		return panel;
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		table.setModel(AbstractTableCreator.createLoadingTable());
	}

	@Override
	protected TableModel createPanelTableType(@NonNull TableOptions options) {
		return new AnalysisDatasetTableCreator(options).createAnalysisParametersTable();
	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();
	}

	@Override
	protected synchronized void updateMultiple() {
		updateAnalysisParametersPanel();
	}

	@Override
	protected synchronized void updateNull() {
		table.setModel(AbstractTableCreator.createBlankTable());
	}

	/**
	 * Update the analysis panel with data from the given datasets
	 * 
	 */
	private void updateAnalysisParametersPanel() {

		TableOptions options = new TableOptionsBuilder()
				.setDatasets(getDatasets())
				.setTarget(table)
				.setScrollPane(scrollPane)
				.setBoolean(AbstractOptions.IS_MERGE_SOURCE_OPTIONS_TABLE, false)
				.build();

		setTable(options);

	}

	@Override
	public void scaleUpdated(List<IAnalysisDataset> datasets) {
		clearCache(datasets);
		update(datasets);

	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		clearCache(dataset);
		update(List.of(dataset));
	}

	@Override
	public void scaleUpdated() {
		clearCache();
		update();
	}

}
