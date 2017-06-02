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


package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.logging.Level;

import javax.swing.JScrollPane;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ViolinChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.tabs.BoxplotsTabPanel;

@SuppressWarnings("serial")
public class SignalsBoxplotPanel extends BoxplotsTabPanel {

    public SignalsBoxplotPanel() {
        super(CellularComponent.NUCLEAR_SIGNAL);
        createUI();
    }

    private void createUI() {

        this.setLayout(new BorderLayout());
        Dimension preferredSize = new Dimension(200, 300);

        for (PlottableStatistic stat : PlottableStatistic.getSignalStats()) {

            ChartOptions options = new ChartOptionsBuilder().addStatistic(stat)
                    .setScale(GlobalOptions.getInstance().getScale()).setSwatch(GlobalOptions.getInstance().getSwatch())
                    .build();

            JFreeChart chart = null;
            try {
                chart = new ViolinChartFactory(options).createStatisticPlot(CellularComponent.NUCLEAR_SIGNAL);
            } catch (Exception e) {
                log(Level.SEVERE, "Error creating boxplots panel", e);
            }

            ViolinChartPanel panel = new ViolinChartPanel(chart);
            panel.setPreferredSize(preferredSize);
            chartPanels.put(stat.toString(), panel);
            mainPanel.add(panel);
        }

        // add the scroll pane to the tab
        scrollPane = new JScrollPane(mainPanel);
        this.add(scrollPane, BorderLayout.CENTER);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        update(getDatasets());

    }

    @Override
    protected void updateSingle() {
        updateMultiple();

    }

    @Override
    protected void updateMultiple() {

        for (PlottableStatistic stat : PlottableStatistic.getSignalStats()) {

            ExportableChartPanel panel = chartPanels.get(stat.toString());

            ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).addStatistic(stat)
                    .setScale(GlobalOptions.getInstance().getScale()).setSwatch(GlobalOptions.getInstance().getSwatch())
                    .setTarget(panel).build();

            setChart(options);
        }

    }

    @Override
    protected void updateNull() {
        updateMultiple();
    }

}
