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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.TableDetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.visualisation.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.tables.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.tables.CellTableDatasetCreator;

@SuppressWarnings("serial")
public class CellSignalStatsPanel extends TableDetailPanel implements CellEditingTabPanel {

	private static final Logger LOGGER = Logger.getLogger(CellSignalStatsPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Signals";
	private static final String HEADER_LBL = "Pairwise distances between the centres of mass of all signals";
	private static final String TABLE_TOOLTIP = "Shows the distances between the centres of mass of signals";

	private ExportableTable table; // individual cell stats

	private JScrollPane scrollPane;

	private CellViewModel model;

	public CellSignalStatsPanel(CellViewModel model) {
		super(PANEL_TITLE_LBL);
		this.model = model;
		this.setLayout(new BorderLayout());

		JPanel header = createHeader();

		scrollPane = new JScrollPane();

		TableModel tableModel = AnalysisDatasetTableCreator.createBlankTable();

		table = new ExportableTable(tableModel);
		table.setEnabled(false);
		table.setToolTipText(TABLE_TOOLTIP);

		scrollPane.setViewportView(table);
		scrollPane.setColumnHeaderView(table.getTableHeader());

		this.add(header, BorderLayout.NORTH);
		this.add(scrollPane, BorderLayout.CENTER);

		this.setEnabled(false);
	}

	/**
	 * Create the header panel
	 * 
	 * @return
	 */
	private JPanel createHeader() {
		JPanel panel = new JPanel();

		JLabel label = new JLabel(HEADER_LBL);

		panel.add(label);
		return panel;
	}

	@Override
	public synchronized void update() {

		if (this.isMultipleDatasets() || !this.hasDatasets()) {
			table.setModel(AbstractTableCreator.createBlankTable());
			return;
		}

		TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).setCell(model.getCell())
				.setScale(GlobalOptions.getInstance().getScale()).setTarget(table)
				.setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new CellSignalColocalisationRenderer())
				.build();

		try {

			setTable(options);

		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error updating cell stats table");
			LOGGER.log(Loggable.STACK, "Error updating cell stats table", e);
		}
	}

	@Override
	public synchronized void setLoading() {

		table.setModel(AbstractTableCreator.createLoadingTable());
	}

	@Override
	protected void updateSingle() {
		update();
	}

	@Override
	protected void updateMultiple() {
		updateNull();
	}

	@Override
	protected void updateNull() {
		table.setModel(AbstractTableCreator.createBlankTable());

	}

	@Override
	protected TableModel createPanelTableType(@NonNull TableOptions options) {

		if (model.hasCell()) {
			return new CellTableDatasetCreator(options, model.getCell()).createPairwiseSignalDistanceTable();
		} else {
			return AbstractTableCreator.createBlankTable();
		}
	}

	/**
	 * Colour colocalising signal table. Self matches are greyed out.
	 */
	private class CellSignalColocalisationRenderer extends DefaultTableCellRenderer {

		@Override
		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {

			// Cells are by default rendered as a JLabel.
			super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

			Color bgColour = Color.WHITE;
			Color fgColour = Color.BLACK;

			if (row == column - 1) {
				bgColour = Color.LIGHT_GRAY;
				fgColour = Color.LIGHT_GRAY;
			}

			setBackground(bgColour);
			setForeground(fgColour);

			return this;
		}
	}

	@Override
	public void checkCellLock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBorderTagAction(@NonNull Landmark tag, int newTagIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSegmentStartIndexAction(@NonNull UUID id, int index) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public CellViewModel getCellModel() {
		return model;
	}

	@Override
	public void setCellModel(CellViewModel model) {
		this.model = model;
	}

	@Override
	public void clearCellCharts() {
		cache.clear(model.getCell());
	}

}
