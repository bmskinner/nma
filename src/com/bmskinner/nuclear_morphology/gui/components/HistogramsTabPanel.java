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
package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.HistogramChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.charts.panels.SelectableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.gui.components.panels.GenericCheckboxPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * This class is extended for making a panel with multiple stats histograms
 * arranged vertically
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class HistogramsTabPanel extends DetailPanel implements ActionListener {

    protected Map<String, SelectableChartPanel> chartPanels = new HashMap<String, SelectableChartPanel>();

    protected JPanel               mainPanel;                                                                 // hold
                                                                                                              // the
                                                                                                              // charts
    protected JPanel               headerPanel;                                                               // hold
                                                                                                              // buttons
    protected GenericCheckboxPanel useDensityPanel = new GenericCheckboxPanel("Probability density function");

    protected JScrollPane scrollPane; // hold the main panel

    protected String component;

    public HistogramsTabPanel(String component) {
        super();
        this.component = component;
        this.setLayout(new BorderLayout());

        try {
            mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

            headerPanel = new JPanel(new FlowLayout());

            headerPanel.add(useDensityPanel);

            useDensityPanel.addActionListener(this);

            this.add(headerPanel, BorderLayout.NORTH);

            // add the scroll pane to the tab
            scrollPane = new JScrollPane(mainPanel);
            this.add(scrollPane, BorderLayout.CENTER);

            this.setEnabled(false);
        } catch (Exception e) {
            log(Level.SEVERE, "Error creating panel", e);
        }

    }

    @Override
    public void setChartsAndTablesLoading() {
        super.setChartsAndTablesLoading();
        for (ExportableChartPanel p : chartPanels.values()) {
            p.setChart(AbstractChartFactory.createLoadingChart());
        }

    }

    @Override
    protected JFreeChart createPanelChartType(ChartOptions options) {
        return new HistogramChartFactory(options).createStatisticHistogram(component);
    }

    @Override
    protected TableModel createPanelTableType(TableOptions options) {
        return null;
    }

    public void setEnabled(boolean b) {
        super.setEnabled(b);
        useDensityPanel.setEnabled(b);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        try {
            finest("Updating abstract histogram tab panel");
            this.update(getDatasets());
        } catch (Exception e1) {
            error("Error updating histogram panel from action listener", e1);
        }

    }

    protected int getFilterDialogResult(double lower, double upper) {
        DecimalFormat df = new DecimalFormat("#.##");
        Object[] options = { "Filter collection", "Cancel", };
        int result = JOptionPane.showOptionDialog(null,
                "Filter between " + df.format(lower) + "-" + df.format(upper) + "?", "Confirm filter",

                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

                null, options, options[0]);
        return result;
    }

}
