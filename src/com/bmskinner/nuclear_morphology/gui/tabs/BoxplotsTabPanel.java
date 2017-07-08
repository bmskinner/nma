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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.BoxplotChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;

/**
 * This class is extended for making a panel with multiple stats histograms
 * arranged vertically
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class BoxplotsTabPanel extends DetailPanel implements ActionListener {

    protected volatile Map<String, ExportableChartPanel> chartPanels = new HashMap<String, ExportableChartPanel>();

    protected JPanel mainPanel;   // hold the charts
    protected JPanel headerPanel; // hold buttons

    protected JScrollPane scrollPane; // hold the main panel

    protected String component;

    public BoxplotsTabPanel(String component) {
        super();
        this.component = component;
        this.setLayout(new BorderLayout());

        try {
            mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

            headerPanel = new JPanel(new FlowLayout());

            this.add(headerPanel, BorderLayout.NORTH);

            // add the scroll pane to the tab
            scrollPane = new JScrollPane(mainPanel);
            this.add(scrollPane, BorderLayout.CENTER);

            this.setEnabled(false);
        } catch (Exception e) {
            error("Error creating panel", e);
        }

    }

    @Override
    public synchronized void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        for (ExportableChartPanel p : chartPanels.values()) {
            p.setChart(AbstractChartFactory.createLoadingChart());
        }

    }

    @Override
    protected synchronized JFreeChart createPanelChartType(ChartOptions options) {
        if (GlobalOptions.getInstance().isViolinPlots()) {
            return new ViolinChartFactory(options).createStatisticPlot(component);
        } else {
            return new BoxplotChartFactory(options).createStatisticBoxplot(component);
        }
    }

    @Override
    protected synchronized TableModel createPanelTableType(TableOptions options) {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {
            finest("Updating abstract boxplot tab panel");
            this.update(getDatasets());
        } catch (Exception e1) {
            error("Error updating boxplot panel from action listener", e1);
        }

    }

}
