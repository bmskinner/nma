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
import gui.tabs.signals.SignalScatterChartPanel;
import gui.tabs.signals.SignalShellsPanel;
import gui.tabs.signals.SignalsAnalysisPanel;
import gui.tabs.signals.SignalsBoxplotPanel;
import gui.tabs.signals.SignalsHistogramPanel;
import gui.tabs.signals.SignalsOverviewPanel;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JTabbedPane;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import charting.datasets.AbstractDatasetCreator;
import charting.options.ChartOptions;
import charting.options.TableOptions;

@SuppressWarnings("serial")
public class SignalsDetailPanel extends DetailPanel implements ActionListener, SignalChangeListener {
		
	private SignalsOverviewPanel	overviewPanel; 	//container for chart and stats table
	private SignalsHistogramPanel 	histogramPanel;
	private SignalsAnalysisPanel	analysisPanel;
	private SignalsBoxplotPanel	    boxplotPanel;
	private SignalShellsPanel		shellsPanel;
	private SignalScatterChartPanel signalScatterChartPanel;

	private JTabbedPane signalsTabPane;

	/**
	 * Create the panel.
	 */
	public SignalsDetailPanel() {
		super();
		try{

			this.setLayout(new BorderLayout());

			signalsTabPane = new JTabbedPane(JTabbedPane.TOP);

			overviewPanel  = new SignalsOverviewPanel();			
			boxplotPanel   = new SignalsBoxplotPanel();
			histogramPanel = new SignalsHistogramPanel();
			shellsPanel    = new SignalShellsPanel();
			analysisPanel  = new SignalsAnalysisPanel();
			signalScatterChartPanel = new SignalScatterChartPanel();
			
			signalsTabPane.addTab("Overview", overviewPanel);
			signalsTabPane.addTab("Boxplots", boxplotPanel);
			signalsTabPane.addTab("Histograms", histogramPanel);
			signalsTabPane.addTab("Shells", shellsPanel);
			signalsTabPane.addTab("Detection settings", analysisPanel);
			signalsTabPane.addTab("Scatter", signalScatterChartPanel);

			this.addSubPanel(overviewPanel);
			this.addSubPanel(boxplotPanel);
			this.addSubPanel(histogramPanel);
			this.addSubPanel(shellsPanel);
			this.addSubPanel(analysisPanel);
			this.addSubPanel(signalScatterChartPanel);
			
			this.add(signalsTabPane, BorderLayout.CENTER);
			
		} catch (Exception e){
			log(Level.SEVERE, "Error making signal panel", e);
		}
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a single dataset is selected
	 */
	protected void updateSingle() {
		updateMultiple();
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a multiple datasets are selected
	 */
	protected void updateMultiple() {
		
		finest("Updating shells panel: "+this.getClass().getName());
		shellsPanel.update(getDatasets());
		finest("Updated shells panel: "+this.getClass().getName());
		
		finest("Updating signals overview panel: "+this.getClass().getName());
		overviewPanel.update(getDatasets());
		finest("Updated signals overview panel: "+this.getClass().getName());
		
		finest("Updating signals histogram panel: "+this.getClass().getName());
		histogramPanel.update(getDatasets());
		finest("Updated signals histogram panel: "+this.getClass().getName());
		
		finest("Updating signals analysis panel: "+this.getClass().getName());
		analysisPanel.update(getDatasets());
		finest("Updated signals analysis panel: "+this.getClass().getName());
		
		finest("Updating signals boxplot panel: "+this.getClass().getName());
		boxplotPanel.update(getDatasets());
		finest("Updated signals boxplot panel: "+this.getClass().getName());
		
		finest("Updating signals scatter panel: "+this.getClass().getName());
		signalScatterChartPanel.update(getDatasets());
		finest("Updated signals scatter panel: "+this.getClass().getName());
	}
	
	/**
	 * This method must be overridden by the extending class
	 * to perform the actual update when a no datasets are selected
	 */
	protected void updateNull() {
		updateMultiple();
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
		return null;
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return null;
	}


	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getActionCommand().startsWith("GroupVisble_")){
			overviewPanel.update(getDatasets());
			histogramPanel.update(getDatasets());
			boxplotPanel.update(getDatasets());
			shellsPanel.update(getDatasets());
			
		}
		
	}
	
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		super.signalChangeReceived(event);
		if(event.type().equals("SignalColourUpdate")){
			update(getDatasets());
		}
		
		if(event.type().startsWith("GroupVisble_")){
			overviewPanel.update(getDatasets());
			histogramPanel.update(getDatasets());
			boxplotPanel.update(getDatasets());
			shellsPanel.update(getDatasets());
		}
	}

}
