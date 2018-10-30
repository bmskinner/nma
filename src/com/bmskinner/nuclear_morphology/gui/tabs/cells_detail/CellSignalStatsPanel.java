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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.CellTableDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;

@SuppressWarnings("serial")
public class CellSignalStatsPanel extends AbstractCellDetailPanel {

    private static final String PANEL_TITLE_LBL = "Signals";
    private static final String HEADER_LBL    = "Pairwise distances between the centres of mass of all signals";
    private static final String TABLE_TOOLTIP = "Shows the distances between the centres of mass of signals";

    private ExportableTable table; // individual cell stats

    private JScrollPane scrollPane;

    public CellSignalStatsPanel(@NonNull InputSupplier context, CellViewModel model) {
        super(context, model, PANEL_TITLE_LBL);
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

    public synchronized void update() {

        if (this.isMultipleDatasets() || !this.hasDatasets()) {
            table.setModel(AbstractTableCreator.createBlankTable());
            return;
        }

        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
                .setCell(this.getCellModel().getCell()).setScale(GlobalOptions.getInstance().getScale())
                .setTarget(table)
                .setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new CellSignalColocalisationRenderer()).build();

        try {

            setTable(options);

        } catch (Exception e) {
            warn("Error updating cell stats table");
            stack("Error updating cell stats table", e);
        }
    }

    @Override
    public synchronized void setChartsAndTablesLoading() {

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

        if (getCellModel().hasCell()) {
            return new CellTableDatasetCreator(options, getCellModel().getCell()).createPairwiseSignalDistanceTable();
        } else {
            return AbstractTableCreator.createBlankTable();
        }
    }

    /**
     * Colour colocalising signal table. Self matches are greyed out.
     */
    private class CellSignalColocalisationRenderer extends DefaultTableCellRenderer {

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

}
