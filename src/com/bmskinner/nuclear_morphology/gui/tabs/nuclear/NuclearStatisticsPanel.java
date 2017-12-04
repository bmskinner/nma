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


package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NuclearStatisticsPanel extends DetailPanel {

    private static final String PANEL_TITLE_LBL = "Nuclear charts";
//    private static final String OVERVIEW_TAB_LBL  = "Average stats";
//    private static final String BOXPLOTS_TAB_LBL  = "Boxplots";
//    private static final String HISTOGRAM_TAB_LBL = "Histograms";
//    private static final String WILCOXON_TAB_LBL  = "Wilcoxon stats";
//    private static final String MAGNITUDE_TAB_LBL = "Magnitude";
//    private static final String OVERLAYS_TAB_LBL  = "Overlays";
//    private static final String SCATTER_TAB_LBL   = "Scatter";
//    private static final String LOBES_TAB_LBL     = "Lobes";

    private JTabbedPane tabPane;

    public NuclearStatisticsPanel() {
        super();

        this.setLayout(new BorderLayout());
        tabPane = new JTabbedPane(JTabbedPane.TOP);

        DetailPanel nuclearStatsPanel = new NuclearStatsPanel();
        DetailPanel boxplotPanel = new NuclearBoxplotsPanel();
        DetailPanel histogramsPanel = new NuclearHistogramsPanel();
        DetailPanel wilcoxonPanel = new WilcoxonDetailPanel();
        DetailPanel nucleusMagnitudePanel = new NucleusMagnitudePanel();
        // DetailPanel nuclearOverlaysPanel = new NuclearOverlaysPanel();
        DetailPanel nuclearScatterChartPanel = new NuclearScatterChartPanel();
        DetailPanel nuclearLobesPanel = new NuclearLobesPanel();

        this.addSubPanel(nuclearStatsPanel);
        this.addSubPanel(boxplotPanel);
        this.addSubPanel(histogramsPanel);
        this.addSubPanel(wilcoxonPanel);
        this.addSubPanel(nucleusMagnitudePanel);
        // this.addSubPanel(nuclearOverlaysPanel);
        this.addSubPanel(nuclearScatterChartPanel);
        this.addSubPanel(nuclearLobesPanel);

        tabPane.addTab(nuclearStatsPanel.getPanelTitle(), nuclearStatsPanel);
        tabPane.addTab(boxplotPanel.getPanelTitle(), boxplotPanel);
        tabPane.addTab(histogramsPanel.getPanelTitle(), histogramsPanel);
        tabPane.addTab(wilcoxonPanel.getPanelTitle(), wilcoxonPanel);
        tabPane.addTab(nucleusMagnitudePanel.getPanelTitle(), null, nucleusMagnitudePanel, "Pop, pop");
        tabPane.addTab(nuclearScatterChartPanel.getPanelTitle(), nuclearScatterChartPanel);
        tabPane.addTab(nuclearLobesPanel.getPanelTitle(), nuclearLobesPanel);

        this.add(tabPane, BorderLayout.CENTER);
    }
    
    @Override
    public String getPanelTitle(){
        return PANEL_TITLE_LBL;
    }
    
}
