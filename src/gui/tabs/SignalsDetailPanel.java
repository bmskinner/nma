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
package gui.tabs;


import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.tabs.signals.SignalShellsPanel;
import gui.tabs.signals.SignalsAnalysisPanel;
import gui.tabs.signals.SignalsBoxplotPanel;
import gui.tabs.signals.SignalsHistogramPanel;
import gui.tabs.signals.SignalsOverviewPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JTabbedPane;

public class SignalsDetailPanel extends DetailPanel implements ActionListener, SignalChangeListener {

	private static final long serialVersionUID = 1L;
		
	private SignalsOverviewPanel	overviewPanel; 	//container for chart and stats table
	private SignalsHistogramPanel 	histogramPanel;
	private SignalsAnalysisPanel	analysisPanel;
	private SignalsBoxplotPanel	    boxplotPanel;
	private SignalShellsPanel		shellsPanel;

	private JTabbedPane signalsTabPane;

	/**
	 * Create the panel.
	 */
	public SignalsDetailPanel(Logger programLogger) {
		super(programLogger);
		try{

			this.setLayout(new BorderLayout());

			signalsTabPane = new JTabbedPane(JTabbedPane.TOP);

			overviewPanel  = new SignalsOverviewPanel(programLogger);			
			boxplotPanel   = new SignalsBoxplotPanel(programLogger);
			histogramPanel = new SignalsHistogramPanel(programLogger);
			shellsPanel    = new SignalShellsPanel(programLogger);
			analysisPanel  = new SignalsAnalysisPanel(programLogger);
			
			signalsTabPane.addTab("Overview", overviewPanel);
			signalsTabPane.addTab("Boxplots", boxplotPanel);
			signalsTabPane.addTab("Histograms", histogramPanel);
			signalsTabPane.addTab("Shells", shellsPanel);
			signalsTabPane.addTab("Detection settings", analysisPanel);

			this.addSubPanel(overviewPanel);
			this.addSubPanel(boxplotPanel);
			this.addSubPanel(histogramPanel);
			this.addSubPanel(shellsPanel);
			this.addSubPanel(analysisPanel);
			
			this.add(signalsTabPane, BorderLayout.CENTER);
			
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error making signal panel", e);
		}
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a single dataset is selected
	 */
	protected void updateSingle() throws Exception {
		updateMultiple();
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a multiple datasets are selected
	 */
	protected void updateMultiple() throws Exception {
		shellsPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updated shells panel");
		
		overviewPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updated signals overview panel");
		
		histogramPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updated signals histogram panel");
		
		analysisPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updated signals analysis panel");
		
		boxplotPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updated signals boxplot panel");
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a no datasets are selected
	 */
	protected void updateNull() throws Exception {
		updateMultiple();
	}


	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getActionCommand().startsWith("GroupVisble_")){
			overviewPanel.update(getDatasets());
			histogramPanel.update(getDatasets());
		}
		
	}
	
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals("SignalColourUpdate")){
			update(getDatasets());
		}
		
		if(event.type().startsWith("GroupVisble_")){
			overviewPanel.update(getDatasets());
			histogramPanel.update(getDatasets());
		}
		
		if(event.type().startsWith("RunShellAnalysis")){
			fireSignalChangeEvent("RunShellAnalysis");
		}
	}

}
