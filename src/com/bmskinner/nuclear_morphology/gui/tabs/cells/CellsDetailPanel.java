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

package com.bmskinner.nuclear_morphology.gui.tabs.cells;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearBoxplotsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearHistogramsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearLobesPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearOverlaysPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearScatterChartPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearStatsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NucleusMagnitudePanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.WilcoxonDetailPanel;

/**
 * Shows aggregate stats for the cells in datasets
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CellsDetailPanel extends DetailPanel {
	
//	private static final String OVERVIEW_TAB_LBL  = "Average stats";
	private static final String BOXPLOTS_TAB_LBL  = "Boxplots";
//	private static final String HISTOGRAM_TAB_LBL = "Histograms";
//	private static final String WILCOXON_TAB_LBL  = "Wilcoxon stats";
//	private static final String MAGNITUDE_TAB_LBL = "Magnitude";
//	private static final String OVERLAYS_TAB_LBL  = "Overlays";
//	private static final String SCATTER_TAB_LBL   = "Scatter";
//	private static final String LOBES_TAB_LBL     = "Lobes";
	
	private JTabbedPane 	tabPane;

	public CellsDetailPanel()  {
		super();
		
		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		
//		DetailPanel nuclearStatsPanel        = new NuclearStatsPanel();
		DetailPanel boxplotPanel             = new CellsBoxplotsPanel();
//		DetailPanel histogramsPanel          = new NuclearHistogramsPanel();
//		DetailPanel wilcoxonPanel 	         = new WilcoxonDetailPanel();
//		DetailPanel nucleusMagnitudePanel    = new NucleusMagnitudePanel();
//		DetailPanel nuclearOverlaysPanel 	 = new NuclearOverlaysPanel();
//		DetailPanel nuclearScatterChartPanel = new NuclearScatterChartPanel();
//		DetailPanel nuclearLobesPanel        = new NuclearLobesPanel();
		
//		this.addSubPanel(nuclearStatsPanel);
		this.addSubPanel(boxplotPanel);
//		this.addSubPanel(histogramsPanel);
//		this.addSubPanel(wilcoxonPanel);
//		this.addSubPanel(nucleusMagnitudePanel);
//		this.addSubPanel(nuclearOverlaysPanel);
//		this.addSubPanel(nuclearScatterChartPanel);
//		this.addSubPanel(nuclearLobesPanel);
//		
//		tabPane.addTab(OVERVIEW_TAB_LBL, nuclearStatsPanel);		
		tabPane.addTab(BOXPLOTS_TAB_LBL, boxplotPanel);
//		tabPane.addTab(HISTOGRAM_TAB_LBL, histogramsPanel);
//		tabPane.addTab(WILCOXON_TAB_LBL, wilcoxonPanel);
//		tabPane.addTab(MAGNITUDE_TAB_LBL, null, nucleusMagnitudePanel, "Pop, pop");
//		tabPane.addTab(OVERLAYS_TAB_LBL, nuclearOverlaysPanel);
//		tabPane.addTab(SCATTER_TAB_LBL, nuclearScatterChartPanel);
//		tabPane.addTab(LOBES_TAB_LBL, nuclearLobesPanel);
		
		this.add(tabPane, BorderLayout.CENTER);
	}
}
