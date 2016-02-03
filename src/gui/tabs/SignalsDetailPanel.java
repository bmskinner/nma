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

	/**
	 * Get a series or dataset index for colour selection when drawing charts. The index
	 * is set in the DatasetCreator as part of the label. The format is Name_index_other
	 * @param label the label to extract the index from 
	 * @return the index found
	 */
	private int getIndexFromLabel(String label){
		String[] names = label.split("_");
		return Integer.parseInt(names[1]);
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
	}
                
//    protected class ShellsPanel extends JPanel{
//
//    	private static final long serialVersionUID = 1L;
//
//    	private ChartPanel 	chartPanel; 
//    	private JLabel 		statusLabel  = new JLabel();
//    	private JButton 	newAnalysis	 = new JButton("Run new shell analysis");
//
//    	protected ShellsPanel(){
//    		this.setLayout(new BorderLayout());
//    		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
//    		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
//    		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
//    		chartPanel = new ChartPanel(shellsChart);
//    		this.add(chartPanel, BorderLayout.CENTER);
//    		
//    		this.add(statusLabel, BorderLayout.NORTH);
//    		statusLabel.setVisible(false);
//    		
//    		newAnalysis.addMouseListener(new MouseAdapter() {
//    			@Override
//    			public void mouseClicked(MouseEvent arg0) {
//    				fireSignalChangeEvent("RunShellAnalysis");
//    			}
//    		});
//    		newAnalysis.setVisible(false);
//    		this.add(newAnalysis, BorderLayout.SOUTH);
//
//
//    	}
//    	
//    	/**
//    	 * Update the shells panel with data from the given datasets
//    	 * @param list the datasets
//    	 */
//    	protected void update(List<AnalysisDataset> list){
//
//    		if(isSingleDataset()){ // single collection is easy
//    			
////    			AnalysisDataset dataset = list.get(0);
//    			CellCollection collection = activeDataset().getCollection();
//
//    			if(activeDataset().hasShellResult()){ // only if there is something to display
//
//    				CategoryDataset ds = NuclearSignalDatasetCreator.createShellBarChartDataset(list);
//    				JFreeChart shellsChart = ChartFactory.createBarChart(null, "Outer <--- Shell ---> Interior", "Percent", ds);
//    				shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
//    				shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
//    				StatisticalBarRenderer rend = new StatisticalBarRenderer();
//    				rend.setBarPainter(new StandardBarPainter());
//    				rend.setShadowVisible(false);
//    				rend.setErrorIndicatorPaint(Color.black);
//    				rend.setErrorIndicatorStroke(new BasicStroke(2));
//    				shellsChart.getCategoryPlot().setRenderer(rend);
//
//    				for (int j = 0; j < ds.getRowCount(); j++) {
//    					rend.setSeriesVisibleInLegend(j, Boolean.FALSE);
//    					rend.setSeriesStroke(j, new BasicStroke(2));
//    					int index = getIndexFromLabel( (String) ds.getRowKey((j)));
//    					Color colour = activeDataset().getSignalGroupColour(index);
//    					rend.setSeriesPaint(j, colour);
//    				}	
//
//    				chartPanel.setChart(shellsChart);
//    				chartPanel.setVisible(true);
//    				
//    				statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
//    				String label = "";
//    				for(int i=1; i<=activeDataset().getHighestSignalGroup();i++){
//    					ShellResult r = activeDataset().getShellResult(i);
//    						label += "Group "+i+": p="+r.getChiSquare();
//    						String sig 	= r.getChiSquare() < Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL 
//    									? "Significantly different to random at 5% level"
//    									: "Not significantly different to random at 5% level";
//    						
//    						label += "; "+sig+"\n";
//    					
//    				}
//    				statusLabel.setText(label);
//    				statusLabel.setVisible(true);
//    				
//    				
//    				newAnalysis.setVisible(false);
//    				
//    			} else { // no shell analysis available
//
//    				if(collection.hasSignals()){
//    					// if signals, offer to run
//    					makeNoShellAnalysisAvailablePanel(true, collection, "No shell results available"); // allow option to run analysis
//    				} else {
//    					// otherwise don't show button
//    					makeNoShellAnalysisAvailablePanel(false, null, "No signals in population"); // container in tab if no shell chart
//    				}
//    			}
//    		} else {
//    			
//    			// Multiple populations. Do not display
//    			// container in tab if no shell chart
//    			makeNoShellAnalysisAvailablePanel(false, null, "Cannot display shell results for multiple populations");
//    		}
//    	}
//    	
//    	/**
//    	 * Create a panel to display when a shell analysis is not available
//    	 * @param showRunButton should there be an option to run a shell analysis on the dataset
//    	 * @param collection the nucleus collection from the dataset
//    	 * @param label the text to display on the panel
//    	 * @return a panel to put in the shell tab
//    	 */
//    	private void makeNoShellAnalysisAvailablePanel(boolean showRunButton, CellCollection collection, String label){
//    		chartPanel.setVisible(false);
//    		statusLabel.setText(label);
//    		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
//    		statusLabel.setVisible(true);
//
//    		newAnalysis.setVisible(showRunButton);
//
//    		this.revalidate();
//    		this.repaint();
//  
//    	}
//    }
}
