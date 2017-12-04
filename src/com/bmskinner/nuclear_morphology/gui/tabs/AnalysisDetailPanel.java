/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.util.logging.Level;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.gui.components.AnalysisTableCellRenderer;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;

/**
 * Holds the nuclear detection parameters
 *
 */
@SuppressWarnings("serial")
public class AnalysisDetailPanel extends DetailPanel {
    
    private static final String PANEL_TITLE_LBL = "Analysis info";
    private static final String HEADER_LBL      = "Green rows have the same value in all columns";
    private ExportableTable tableAnalysisParameters;

    public AnalysisDetailPanel() {

        super();

        this.setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.add(new JLabel(HEADER_LBL));
        
        JScrollPane parametersPanel = createAnalysisParametersPanel();

        this.add(header, BorderLayout.NORTH);
        this.add(parametersPanel, BorderLayout.CENTER);

    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return null;
    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        tableAnalysisParameters.setModel(AbstractTableCreator.createLoadingTable());
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return new AnalysisDatasetTableCreator(options).createAnalysisTable();
    }

    @Override
    protected void updateSingle() {
        updateMultiple();
    }

    @Override
    protected void updateMultiple() {
        updateAnalysisParametersPanel();
        finest("Updated analysis parameter panel");
    }

    @Override
    protected void updateNull() {
        tableAnalysisParameters.setModel(AbstractTableCreator.createBlankTable());
    }

    /**
     * Update the analysis panel with data from the given datasets
     * 
     * @param list
     *            the datasets
     * @throws Exception
     */
    private void updateAnalysisParametersPanel() {

        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
                .setType(TableType.ANALYSIS_PARAMETERS).setTarget(tableAnalysisParameters)
                .setRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new AnalysisTableCellRenderer()).build();

        setTable(options);

    }

    private JScrollPane createAnalysisParametersPanel() {
        JScrollPane scrollPane = new JScrollPane();

        try {

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout(0, 0));

            tableAnalysisParameters = new ExportableTable();
            panel.add(tableAnalysisParameters, BorderLayout.CENTER);
            tableAnalysisParameters.setEnabled(false);

            scrollPane.setViewportView(panel);
            scrollPane.setColumnHeaderView(tableAnalysisParameters.getTableHeader());

            TableOptions options = new TableOptionsBuilder().setDatasets(null).setType(TableType.ANALYSIS_PARAMETERS)
                    .build();

            TableModel model = new AnalysisDatasetTableCreator(options).createAnalysisTable();
            tableAnalysisParameters.setModel(model);

        } catch (Exception e) {
            log(Level.SEVERE, "Error creating stats panel", e);
        }
        return scrollPane;
    }
}
