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
		
	private NuclearBoxplotsPanel   boxplotPanel;
	private NuclearHistogramsPanel histogramsPanel;
	private WilcoxonDetailPanel    wilcoxonPanel;
	private NucleusMagnitudePanel  nucleusMagnitudePanel;
	private NuclearOverlaysPanel   nuclearOverlaysPanel;
	private NuclearStatsPanel      nuclearStatsPanel;
	private NuclearScatterChartPanel nuclearScatterChartPanel;
	
	private JTabbedPane 	tabPane;

	public NuclearStatisticsPanel() throws Exception {
		super();
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		
		nuclearStatsPanel 	= new NuclearStatsPanel();
		this.addSubPanel(nuclearStatsPanel);
		tabPane.addTab("Average stats", null, nuclearStatsPanel, null);
		
		boxplotPanel = new NuclearBoxplotsPanel();
		this.addSubPanel(boxplotPanel);
		tabPane.addTab("Boxplots", boxplotPanel);
		
		histogramsPanel = new NuclearHistogramsPanel();
		this.addSubPanel(histogramsPanel);
		tabPane.addTab("Histograms", histogramsPanel);
		
		wilcoxonPanel 	= new WilcoxonDetailPanel();
		this.addSubPanel(wilcoxonPanel);
		tabPane.addTab("Wilcoxon stats", null, wilcoxonPanel, null);
		
		nucleusMagnitudePanel 	= new NucleusMagnitudePanel();
		this.addSubPanel(nucleusMagnitudePanel);
		tabPane.addTab("Magnitude", null, nucleusMagnitudePanel, null);
		
		nuclearOverlaysPanel 	= new NuclearOverlaysPanel();
		this.addSubPanel(nuclearOverlaysPanel);
		tabPane.addTab("Overlays", null, nuclearOverlaysPanel, null);
		
		nuclearScatterChartPanel 	= new NuclearScatterChartPanel();
		this.addSubPanel(nuclearScatterChartPanel);
		tabPane.addTab("Scatter", null, nuclearScatterChartPanel, null);
		
		
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
