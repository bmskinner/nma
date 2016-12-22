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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearBoxplotsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearHistogramsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearOverlaysPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearScatterChartPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearStatsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NucleusMagnitudePanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.WilcoxonDetailPanel;

@SuppressWarnings("serial")
public class NuclearStatisticsPanel extends DetailPanel {
	
	private static final String OVERVIEW_TAB_LBL  = "Average stats";
	private static final String BOXPLOTS_TAB_LBL  = "Boxplots";
	private static final String HISTOGRAM_TAB_LBL = "Histograms";
	private static final String WILCOXON_TAB_LBL  = "Wilcoxon stats";
	private static final String MAGNITUDE_TAB_LBL = "Detection settings";
	private static final String OVERLAYS_TAB_LBL  = "Detection settings";
	private static final String SCATTER_TAB_LBL   = "Scatter";
	
	private JTabbedPane 	tabPane;

	public NuclearStatisticsPanel() throws Exception {
		super();
		
		
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		
		DetailPanel nuclearStatsPanel        = new NuclearStatsPanel();
		DetailPanel boxplotPanel             = new NuclearBoxplotsPanel();
		DetailPanel histogramsPanel          = new NuclearHistogramsPanel();
		DetailPanel wilcoxonPanel 	         = new WilcoxonDetailPanel();
		DetailPanel nucleusMagnitudePanel    = new NucleusMagnitudePanel();
		DetailPanel nuclearOverlaysPanel 	 = new NuclearOverlaysPanel();
		DetailPanel nuclearScatterChartPanel = new NuclearScatterChartPanel();
		
		this.addSubPanel(nuclearStatsPanel);
		this.addSubPanel(boxplotPanel);
		this.addSubPanel(histogramsPanel);
		this.addSubPanel(wilcoxonPanel);
		this.addSubPanel(nucleusMagnitudePanel);
		this.addSubPanel(nuclearOverlaysPanel);
		this.addSubPanel(nuclearScatterChartPanel);
		
		tabPane.addTab(OVERVIEW_TAB_LBL, nuclearStatsPanel);		
		tabPane.addTab(BOXPLOTS_TAB_LBL, boxplotPanel);
		tabPane.addTab(HISTOGRAM_TAB_LBL, histogramsPanel);
		tabPane.addTab(WILCOXON_TAB_LBL, wilcoxonPanel);
		tabPane.addTab(MAGNITUDE_TAB_LBL, nucleusMagnitudePanel);
		tabPane.addTab(OVERLAYS_TAB_LBL, nuclearOverlaysPanel);
		tabPane.addTab(SCATTER_TAB_LBL, nuclearScatterChartPanel);
		
		this.add(tabPane, BorderLayout.CENTER);
	}
		
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return null;
	}

}
