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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.charting.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.DefaultTableOptions.TableType;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptionsBuilder;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.components.renderers.JTextAreaCellRenderer;

/**
 * Holds the nuclear detection parameters
 *
 */
@SuppressWarnings("serial")
public class AnalysisDetailPanel extends DetailPanel {
    
    private static final String PANEL_TITLE_LBL = "Analysis info";
    private static final String HEADER_LBL      = "Green rows have the same value in all columns";
    private ExportableTable tableAnalysisParameters;

    public AnalysisDetailPanel(@NonNull InputSupplier context) {
        super(context, PANEL_TITLE_LBL);

        this.setLayout(new BorderLayout());

        JPanel header = new JPanel();
        header.add(new JLabel(HEADER_LBL));
        
        this.add(header, BorderLayout.NORTH);
        this.add(createTablePanel(), BorderLayout.CENTER);

    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        tableAnalysisParameters.setModel(AbstractTableCreator.createLoadingTable());
    }

    @Override
    protected TableModel createPanelTableType(@NonNull TableOptions options) {
        return new AnalysisDatasetTableCreator(options).createAnalysisTable();
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
        tableAnalysisParameters.setModel(AbstractTableCreator.createBlankTable());
    }

    /**
     * Update the analysis panel with data from the given datasets
     * 
     */
    private void updateAnalysisParametersPanel() {

        TableOptions options = new TableOptionsBuilder()
        		.setDatasets(getDatasets())
                .setType(TableType.ANALYSIS_PARAMETERS)
                .setTarget(tableAnalysisParameters)
//                .setColumnRenderer(TableOptions.ALL_EXCEPT_FIRST_COLUMN, new JTextAreaCellRenderer())
                .build();

        setTable(options);

    }

    private JPanel createTablePanel() {
    	JPanel panel = new JPanel();
    	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        tableAnalysisParameters = new ExportableTable();
        tableAnalysisParameters.setModel(AbstractTableCreator.createBlankTable());

        tableAnalysisParameters.setEnabled(false);
        tableAnalysisParameters.setDefaultRenderer(Object.class, new JTextAreaCellRenderer());
        JScrollPane scrollPane = new JScrollPane(tableAnalysisParameters);

        JPanel tablePanel = new JPanel(new BorderLayout());

        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.add(tableAnalysisParameters.getTableHeader(), BorderLayout.NORTH);
        
        panel.add(tablePanel);
        return panel;
    }
}
