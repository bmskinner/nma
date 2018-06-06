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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.TableModel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.BoxplotChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ViolinChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.main.GlobalOptions;
import com.bmskinner.nuclear_morphology.main.InputSupplier;

/**
 * This class is extended for making a panel with multiple stats histograms
 * arranged vertically
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class BoxplotsTabPanel extends DetailPanel implements ActionListener {

    private static final String PANEL_TITLE_LBL = "Violin plots";
    protected volatile Map<String, ExportableChartPanel> chartPanels = new HashMap<String, ExportableChartPanel>(8);

    protected JPanel mainPanel;   // hold the charts
    protected JPanel headerPanel; // hold buttons

    protected JScrollPane scrollPane; // hold the main panel

    protected String component;

    public BoxplotsTabPanel(@NonNull InputSupplier context, String component) {
        super(context, PANEL_TITLE_LBL);
        this.component = component;
        this.setLayout(new BorderLayout());

        try {
            mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

            headerPanel = new JPanel(new FlowLayout());

            this.add(headerPanel, BorderLayout.NORTH);

            // add the scroll pane to the tab
            scrollPane = new JScrollPane(mainPanel);
            
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension preferredFloatingDimension = new Dimension( (int) (screenSize.getWidth()*0.25), (int) (screenSize.getHeight()*0.25) );
            scrollPane.setPreferredSize(preferredFloatingDimension);
            
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
    protected synchronized JFreeChart createPanelChartType(@NonNull ChartOptions options) {
    	return new ViolinChartFactory(options).createStatisticPlot(component);
    }

    @Override
    protected synchronized TableModel createPanelTableType(@NonNull TableOptions options) {
        return null;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
    	this.update(getDatasets());
    }

}
