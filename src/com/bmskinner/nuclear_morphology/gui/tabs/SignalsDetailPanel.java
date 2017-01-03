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

import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalScatterChartPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalShellsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalsAnalysisPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalsBoxplotPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalsColocalisationPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalsHistogramPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalsOverviewPanel;

/**
 * The top level tab panel showing information on signals at the dataset level
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class SignalsDetailPanel extends DetailPanel implements SignalChangeListener {
		
	private static final String OVERVIEW_TAB_LBL = "Overview";
	private static final String BOXPLOTS_TAB_LBL = "Boxplots";
	private static final String HISTOGRAM_TAB_LBL= "Histograms";
	private static final String SHELLS_TAB_LBL   = "Shells";
	private static final String SETTINGS_TAB_LBL = "Detection settings";
	private static final String SCATTER_TAB_LBL  = "Scatter";
	private static final String COLOCAL_TAB_LBL  = "Colocalisation";

	private JTabbedPane signalsTabPane;

	/**
	 * Create the panel.
	 */
	public SignalsDetailPanel() {
		super();
		try{

			this.setLayout(new BorderLayout());

			signalsTabPane = new JTabbedPane(JTabbedPane.TOP);

			DetailPanel overviewPanel  = new SignalsOverviewPanel();			
			DetailPanel boxplotPanel   = new SignalsBoxplotPanel();
			DetailPanel histogramPanel = new SignalsHistogramPanel();
			DetailPanel shellsPanel    = new SignalShellsPanel();
			DetailPanel analysisPanel  = new SignalsAnalysisPanel();
			DetailPanel signalScatterChartPanel = new SignalScatterChartPanel();
			DetailPanel colocalistionPanel = new SignalsColocalisationPanel();
			
			signalsTabPane.addTab(OVERVIEW_TAB_LBL, overviewPanel);
			signalsTabPane.addTab(BOXPLOTS_TAB_LBL, boxplotPanel);
			signalsTabPane.addTab(HISTOGRAM_TAB_LBL, histogramPanel);
			signalsTabPane.addTab(SHELLS_TAB_LBL, shellsPanel);
			signalsTabPane.addTab(SETTINGS_TAB_LBL, analysisPanel);
			signalsTabPane.addTab(SCATTER_TAB_LBL, signalScatterChartPanel);
			signalsTabPane.addTab(COLOCAL_TAB_LBL, colocalistionPanel);

			this.addSubPanel(overviewPanel);
			this.addSubPanel(boxplotPanel);
			this.addSubPanel(histogramPanel);
			this.addSubPanel(shellsPanel);
			this.addSubPanel(analysisPanel);
			this.addSubPanel(signalScatterChartPanel);
			this.addSubPanel(colocalistionPanel);
			
			this.add(signalsTabPane, BorderLayout.CENTER);
			
		} catch (Exception e){
			error("Error making signal panel", e);
		}
	}

//	@Override
//	public void actionPerformed(ActionEvent e) {
//
//		if(e.getActionCommand().startsWith("GroupVisble_")){
//			
//			for(TabPanel p : this.getSubPanels()){
//				p.update(getDatasets());
//			}			
//		}
//		
//	}
	
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		super.signalChangeReceived(event);
		if(event.type().equals(SignalChangeEvent.SIGNAL_COLOUR_CHANGE)){
			update(getDatasets());
		}
		
		if(event.type().startsWith(SignalChangeEvent.GROUP_VISIBLE_PREFIX)){
			
			for(TabPanel p : this.getSubPanels()){
				p.update(getDatasets());
			}
		}
	}

}
