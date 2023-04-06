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
package com.bmskinner.nma.gui.tabs.comparisons;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nma.gui.components.ExportableTable;
import com.bmskinner.nma.gui.tabs.TableDetailPanel;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.options.TableOptions;
import com.bmskinner.nma.visualisation.options.TableOptionsBuilder;
import com.bmskinner.nma.visualisation.tables.AnalysisDatasetTableCreator;

@SuppressWarnings("serial")
public class VennDetailPanel extends TableDetailPanel {

	private static final Logger LOGGER = Logger.getLogger(VennDetailPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Venn";
	private static final String PANEL_DESC_LBL = "Pairwise comparison of cells shared between datasets";

	private static final String HEADER_LBL = "Shows the percentage of cells in each dataset column shared with each other dataset";
	private JPanel mainPanel = new JPanel();

	private ExportableTable vennTable;

	public VennDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		this.setLayout(new BorderLayout());
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		try {
			JPanel vennPanel = new JPanel(new BorderLayout());

			JPanel header = new JPanel();
			header.add(new JLabel(HEADER_LBL));

			JScrollPane scrollPane = new JScrollPane();
			scrollPane.setViewportView(mainPanel);

			this.add(header, BorderLayout.NORTH);
			this.add(scrollPane, BorderLayout.CENTER);

			vennTable = new ExportableTable(AnalysisDatasetTableCreator.createBlankTable());
			vennPanel.add(vennTable, BorderLayout.CENTER);
			vennPanel.add(vennTable.getTableHeader(), BorderLayout.NORTH);
			mainPanel.add(vennPanel);
			vennTable.setEnabled(false);

		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error creating venn panel", e);
		}

	}

	@Override
	public void setLoading() {
		vennTable.setModel(AnalysisDatasetTableCreator.createLoadingTable());
	}

	@Override
	protected void updateSingle() {
		updateNull();
	}

	@Override
	protected void updateMultiple() {
		TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
				.setTarget(vennTable)
				.setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN,
						new VennTableCellRenderer())
				.build();

		setTable(options);

	}

	@Override
	protected void updateNull() {
		vennTable.setModel(AnalysisDatasetTableCreator.createBlankTable());
	}

	@Override
	protected TableModel createPanelTableType(TableOptions options) {
		return new AnalysisDatasetTableCreator(options).createVennTable();
	}

	/**
	 * Colour table cell background to show pairwise comparisons. All cells are
	 * white, apart from the diagonal, which is made light grey
	 */
	class VennTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		@Override
		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table,
				java.lang.Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {

			Color backColour = Color.WHITE;
			Color foreColour = Color.BLACK;

			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
					hasFocus, row, column);

			String cellContents = l.getText();

			if (cellContents == null || cellContents.equals("")) {

				backColour = Color.LIGHT_GRAY;

			} else {

				String columnName = table.getColumnName(column);

				String pctString = cellContents.replace("%", "");
				double pct = 0;
				try {

					NumberFormat nf = NumberFormat.getInstance();
					pct = nf.parse(pctString).doubleValue();

				} catch (ParseException e) {
					LOGGER.fine("Error getting value: " + cellContents + " in column " + columnName
							+ ": "
							+ e.getMessage());
					pct = 0;
				}

				double colourIndex = 255 - ((pct / 100) * 255);

				colourIndex = colourIndex > 255 ? 255 : colourIndex;
				colourIndex = colourIndex < 0 ? 0 : colourIndex;

				backColour = new Color((int) colourIndex, (int) colourIndex, 255);

				if (pct > 60) {
					foreColour = Color.WHITE;
				}

			}

			l.setBackground(backColour);
			l.setForeground(foreColour);

			// Return the JLabel which renders the cell.
			return l;
		}
	}

}
