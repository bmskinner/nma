/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.text.NumberFormat;
import java.text.ParseException;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;

@SuppressWarnings("serial")
public class VennDetailPanel extends DetailPanel {

    private JPanel mainPanel = new JPanel();

    private ExportableTable vennTable;

    public VennDetailPanel() {
        super();
        this.setLayout(new BorderLayout());
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        try {
            JPanel vennPanel = new JPanel(new BorderLayout());

            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setViewportView(mainPanel);

            this.add(scrollPane, BorderLayout.CENTER);

            vennTable = new ExportableTable(AnalysisDatasetTableCreator.createBlankTable());
            vennPanel.add(vennTable, BorderLayout.CENTER);
            vennPanel.add(vennTable.getTableHeader(), BorderLayout.NORTH);
            mainPanel.add(vennPanel);
            vennTable.setEnabled(false);

        } catch (Exception e) {
            error("Error creating venn panel", e);
        }

    }

    @Override
    public void setChartsAndTablesLoading() {
        // log("Set venn to loading");
        vennTable.setModel(AnalysisDatasetTableCreator.createLoadingTable());
    }

    @Override
    protected void updateSingle() {
        // log("Setting venn to blank via single");
        updateNull();
    }

    @Override
    protected void updateMultiple() {
        // log("Updating venn panel with multiple");
        // vennTable.setModel(AnalysisDatasetTableCreator.createLoadingTable());
        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets()).setType(TableType.VENN)
                .setTarget(vennTable).setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new VennTableCellRenderer())
                .build();

        setTable(options);

    }

    @Override
    protected void updateNull() {
        // log("Set venn to blank via null");

        // Exception e = new Exception("Null update of Venn");
        // error("Venn: ",e);
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

        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Color backColour = Color.WHITE;
            Color foreColour = Color.BLACK;

            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String cellContents = l.getText();

            if (cellContents == null || cellContents.equals("")) {

                backColour = Color.LIGHT_GRAY;

            } else {

                String columnName = table.getColumnName(column);

                String pctString = cellContents.replace("%", "");
                // String[] array = cellContents.split("%");
                // String[] array2 = array[0].split("\\(");

                double pct = 0;
                try {

                    NumberFormat nf = NumberFormat.getInstance();
                    pct = nf.parse(pctString).doubleValue();

                } catch (ParseException e) {
                    fine("Error getting value: " + cellContents + " in column " + columnName, e);
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
