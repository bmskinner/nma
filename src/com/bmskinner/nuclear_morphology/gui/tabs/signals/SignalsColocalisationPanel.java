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
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.datasets.tables.NuclearSignalTableCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * Show the minimum distances between signals within a dataset
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalsColocalisationPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Colocalisation";
    private static final String HEADER_LBL    = "Pairwise distances between the closest signal pairs";

    private ExportableChartPanel violinChart;

    public SignalsColocalisationPanel(@NonNull InputSupplier context) {
        super(context);
        this.setLayout(new BorderLayout());

        JPanel header = createHeader();
        JPanel mainPanel = createMainPanel();

        this.add(header, BorderLayout.NORTH);
        this.add(mainPanel, BorderLayout.CENTER);
    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
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

    /**
     * Create the main panel with charts and tables
     * 
     * @return
     */
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        violinChart = new ExportableChartPanel(AbstractChartFactory.createEmptyChart());
        panel.add(violinChart, BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected synchronized void updateSingle() {
        ChartOptions chartOptions = new ChartOptionsBuilder().setDatasets(getDatasets())
                .setScale(GlobalOptions.getInstance().getScale()).setTarget(violinChart).build();

        setChart(chartOptions);

    }

    @Override
    protected synchronized void updateMultiple() {
        ChartOptions chartOptions = new ChartOptionsBuilder().setDatasets(getDatasets())
                .setScale(GlobalOptions.getInstance().getScale()).setTarget(violinChart).build();

        setChart(chartOptions);
    }

    @Override
    protected synchronized void updateNull() {
        violinChart.setChart(AbstractChartFactory.createEmptyChart());
    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        violinChart.setChart(AbstractChartFactory.createLoadingChart());

    }

    @Override
    protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
        return new ViolinChartFactory(options).createSignalColocalisationViolinChart();
    }

    @Override
    protected synchronized TableModel createPanelTableType(@NonNull TableOptions options) {
        return new NuclearSignalTableCreator(options).createSignalColocalisationTable();
    }
}
