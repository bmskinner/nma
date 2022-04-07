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
package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.components.ExportableTable;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.visualisation.datasets.AnalysisDatasetTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.datasets.tables.AbstractTableCreator;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.TableOptionsBuilder;

@SuppressWarnings("serial")
public class NuclearStatsPanel extends DetailPanel {
	
	private static final Logger LOGGER = Logger.getLogger(NuclearStatsPanel.class.getName());

    private static final String PANEL_TITLE_LBL = "Average stats";
    private ExportableTable tablePopulationStats;

    public NuclearStatsPanel(@NonNull InputSupplier context) {
        super(context);

        this.setLayout(new BorderLayout());

        JScrollPane statsPanel = createStatsPanel();

        JPanel headerPanel = new JPanel(new FlowLayout());

        this.add(headerPanel, BorderLayout.NORTH);

        this.add(statsPanel, BorderLayout.CENTER);

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
    protected TableModel createPanelTableType(TableOptions options) {

        return new AnalysisDatasetTableCreator(options).createNucleusStatsTable();
    }

    @Override
    protected void updateSingle() {
        LOGGER.finest( "Passing to update multiple");
        updateMultiple();
    }

    @Override
    protected void updateMultiple() {
        super.updateMultiple();
        LOGGER.finest( "Updating analysis stats panel");
        updateStatsPanel();
        LOGGER.finest( "Updated analysis stats panel");
    }

    @Override
    protected void updateNull() {
        super.updateNull();
        LOGGER.finest( "Passing to update multiple");
        updateMultiple();
    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        tablePopulationStats.setModel(AbstractTableCreator.createLoadingTable());
    }

    /**
     * Update the stats panel with data from the given datasets
     * 
     * @param list the datasets
     */
    private void updateStatsPanel() {

        LOGGER.finest( "Updating stats panel");

        TableOptions options = new TableOptionsBuilder().setDatasets(getDatasets())
                .setScale(GlobalOptions.getInstance().getScale())
                .setTarget(tablePopulationStats)
                .build();

        setTable(options);
        LOGGER.finest( "Set table model");

    }

    private JScrollPane createStatsPanel() {
        JScrollPane scrollPane = new JScrollPane();
        try {

            JPanel panel = new JPanel();

            panel.setLayout(new BorderLayout(0, 0));

            tablePopulationStats = new ExportableTable();
            panel.add(tablePopulationStats, BorderLayout.CENTER);
            tablePopulationStats.setEnabled(false);

            scrollPane.setViewportView(panel);
            scrollPane.setColumnHeaderView(tablePopulationStats.getTableHeader());

            TableOptions options = new TableOptionsBuilder().setDatasets(null)
                    .build();

            TableModel model = new AnalysisDatasetTableCreator(options).createNucleusStatsTable();

            tablePopulationStats.setModel(model);

        } catch (Exception e) {
            LOGGER.warning("Error making nuclear stats panel");
            LOGGER.log(Loggable.STACK, "Error creating stats panel", e);
        }
        return scrollPane;
    }
}
