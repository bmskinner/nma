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
package com.bmskinner.nuclear_morphology.gui.tabs.segments;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.text.NumberFormat;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.ColourSelecter;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.stats.SignificanceTest;
import com.bmskinner.nuclear_morphology.visualisation.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptionsBuilder;

public class SegmentStatsPanel extends DetailPanel {
    
    private static final String PANEL_TITLE_LBL = "Segment stats";

    private static final long serialVersionUID = 1L;
    private ExportableTable   table;                // individual segment stats

    private JScrollPane scrollPane;

    public SegmentStatsPanel(@NonNull InputSupplier context) {
        super(context);

        this.setLayout(new BorderLayout());

        scrollPane = new JScrollPane();
        TableModel model = AnalysisDatasetTableCreator.createBlankTable();
        table = new ExportableTable(model);

        table.setEnabled(false);

        scrollPane.setViewportView(table);
        scrollPane.setColumnHeaderView(table.getTableHeader());

        this.add(scrollPane, BorderLayout.CENTER);
    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    @Override
    protected synchronized void updateSingle() {
        TableOptions options = makeOptions();
        setTable(options);
    }

    @Override
    protected synchronized void updateMultiple() {
        TableOptions options = makeOptions();
        setTable(options);

        if (IProfileSegment.segmentCountsMatch(getDatasets())) {
            table.setToolTipText("Mean and range for 95% confidence interval");

        } else {
            table.setToolTipText(null);
        }
    }

    @Override
    protected synchronized void updateNull() {
        table.setModel(AbstractTableCreator.createBlankTable());
        table.setToolTipText(null);

    }

    @Override
    public synchronized void setChartsAndTablesLoading() {

        table.setModel(AbstractTableCreator.createLoadingTable());

    }

    private TableOptions makeOptions() {
    	return new TableOptionsBuilder().setDatasets(getDatasets())
                .setScale(GlobalOptions.getInstance().getScale())
                .setSwatch(GlobalOptions.getInstance().getSwatch())
                .setTarget(table)
                .setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new SegmentTableCellRenderer())
                .build();
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new AnalysisDatasetTableCreator(options).createMedianProfileStatisticTable();
    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return null;
    }

    private class SegmentTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            // Cells are by default rendered as a JLabel.
            Component l = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            // default cell colour is white
            Color colour = Color.WHITE;

            final String colName = table.getColumnName(column); // will be Seg_x

            // only apply to first row, after the first column
            if (column > 0 && row == 0) {

                int segment;
                try {
                    segment = Integer.valueOf(colName.replace("Seg_", ""));
                } catch (Exception e) {
                    segment = 0;
                }

                colour = ColourSelecter.getColor(segment);
            }

            String rowName = table.getModel().getValueAt(row, 0).toString();
            if (rowName.equals("Length p(unimodal)") && column > 0) {

                String cellContents = ((JLabel) l).getText();

                double pval;
                try {

                    NumberFormat nf = NumberFormat.getInstance();
                    pval = nf.parse(cellContents).doubleValue();
                } catch (Exception e) {
                    pval = 0;
                }

                if (pval < SignificanceTest.FIVE_PERCENT_SIGNIFICANCE_LEVEL) {
                    colour = Color.YELLOW;
                }
                if (pval < SignificanceTest.ONE_PERCENT_SIGNIFICANCE_LEVEL) {
                    colour = Color.GREEN;
                }

            }

            l.setBackground(colour);
            return l;
        }
    }

}
