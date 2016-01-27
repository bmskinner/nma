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
import gui.components.ExportableTable;
import gui.components.HistogramsTabPanel;
import gui.components.SelectableChartPanel;
import gui.tabs.NuclearBoxplotsPanel.HistogramsPanel;
import gui.tabs.signals.SignalsOverviewPanel;
import stats.NucleusStatistic;
import stats.SignalStatistic;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;

import utility.Constants;
import analysis.AnalysisDataset;
import charting.charts.BoxplotChartFactory;
import charting.charts.HistogramChartFactory;
import charting.charts.HistogramChartOptions;
import charting.charts.SignalHistogramChartOptions;
import charting.datasets.NuclearSignalDatasetCreator;
import components.CellCollection;
import components.generic.MeasurementScale;
import components.nuclear.ShellResult;

public class SignalsDetailPanel extends DetailPanel implements ActionListener, SignalChangeListener {

	private static final long serialVersionUID = 1L;
		
	private SignalsOverviewPanel	overviewPanel; 	//container for chart and stats table
	private HistogramPanel 	histogramPanel;
	private AnalysisPanel	analysisPanel;
	private BoxplotPanel	boxplotPanel;
	private ShellsPanel		shellsPanel;

	private JTabbedPane signalsTabPane;

	/**
	 * Create the panel.
	 */
	public SignalsDetailPanel(Logger programLogger) {
		super(programLogger);
		try{

			this.setLayout(new BorderLayout());

			signalsTabPane = new JTabbedPane(JTabbedPane.TOP);

			overviewPanel = new SignalsOverviewPanel(programLogger);
			this.addSubPanel(overviewPanel);
			signalsTabPane.addTab("Overview", overviewPanel);

			histogramPanel = new HistogramPanel(programLogger);
			signalsTabPane.addTab("Signal histograms", histogramPanel);

			shellsPanel = new ShellsPanel();
			signalsTabPane.addTab("Shells", shellsPanel);

			analysisPanel = new AnalysisPanel();
			signalsTabPane.addTab("Detection settings", analysisPanel);

			boxplotPanel = new BoxplotPanel();
			signalsTabPane.addTab("Boxplots", boxplotPanel);

			this.add(signalsTabPane, BorderLayout.CENTER);
			
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error making signal panel", e);
		}
	}

	
//	private static void resizePreview(ChartPanel innerPanel, JPanel container) {
//        int w = container.getWidth();
//        int h = container.getHeight();
//        int size =  Math.min(w, h);
//        innerPanel.setPreferredSize(new Dimension(size, size));
//        container.revalidate();
//    }

	@Override
	public void updateDetail(){

		programLogger.log(Level.FINE, "Updating signals detail panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				try{
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
					
				} catch(Exception e){
					programLogger.log(Level.SEVERE, "Error updating signals detail panel" ,e);
					SignalsDetailPanel.this.update( (List<AnalysisDataset>) null);
				} finally {
					setUpdating(false);
				}
			
		}});

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
	
//	/**
//	 * Allows for cell background to be coloured based on poition in a list. Used to colour
//	 * the signal stats list
//	 *
//	 */
//	private class StatsTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
//
//		private static final long serialVersionUID = 1L;
//
//		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {
//
//			// default cell colour is white
//			Color colour = Color.WHITE;
//
//			// get the value in the first column of the row below
//			if(row<table.getModel().getRowCount()-1){
//				String nextRowHeader = table.getModel().getValueAt(row+1, 0).toString();
//
//				if(nextRowHeader.equals("Signal group")){
//					// we want to colour this cell preemptively
//					// get the signal group from the table
//					String groupString = table.getModel().getValueAt(row+1, 1).toString();
//					colour = activeDataset().getSignalGroupColour(Integer.valueOf(groupString));
////					colour = ColourSelecter.getSignalColour(  Integer.valueOf(groupString)-1   ); 
//				}
//			}
//			//Cells are by default rendered as a JLabel.
//			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
//			l.setBackground(colour);
//
//			//Return the JLabel which renders the cell.
//			return l;
//		}
//	}
	
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
        
    @SuppressWarnings("serial")
	protected class HistogramPanel extends HistogramsTabPanel {
        	  	    	
    	protected HistogramPanel(Logger programLogger) throws Exception{
    		super(programLogger);
    		
    		try {

				MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();
				Dimension preferredSize = new Dimension(400, 150);
				for(SignalStatistic stat : SignalStatistic.values()){

					SignalHistogramChartOptions options = new SignalHistogramChartOptions(null, stat, scale, false, 0);
					SelectableChartPanel panel = new SelectableChartPanel(HistogramChartFactory.createSignalStatisticHistogram(options), stat.toString());
					panel.setPreferredSize(preferredSize);
					panel.addSignalChangeListener(this);
					HistogramPanel.this.chartPanels.put(stat.toString(), panel);
					HistogramPanel.this.mainPanel.add(panel);

				}

			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error creating histogram panel", e);
			}
    		
    	}
    	
    	protected void updateDetail(){
    		
    		if(hasDatasets()){
				this.setEnabled(true);
			} else {
				this.setEnabled(false);
			}
    		
    		MeasurementScale scale  = HistogramPanel.this.measurementUnitSettingsPanel.getSelected();
			boolean useDensity = HistogramPanel.this.useDensityPanel.isSelected();

			try{

				
				int signalGroup = 1; //TODO - get the number  of signal groups in the selected datasets, and iterate 
				
				for(SignalStatistic stat : SignalStatistic.values()){
					SelectableChartPanel panel = HistogramPanel.this.chartPanels.get(stat.toString());

					JFreeChart chart = null;
					SignalHistogramChartOptions options = new SignalHistogramChartOptions(getDatasets(), stat, scale, useDensity, signalGroup);
					options.setLogger(programLogger);

					if(this.getChartCache().hasChart(options)){
						programLogger.log(Level.FINEST, "Using cached histogram: "+stat.toString());
						chart = HistogramPanel.this.getChartCache().getChart(options);

					} else { // No cache


						if(useDensity){
							//TODO - make the density chart
							chart = HistogramChartFactory.createSignalDensityStatsChart(options);
							HistogramPanel.this.getChartCache().addChart(options, chart);

						} else {
							chart = HistogramChartFactory.createSignalStatisticHistogram(options);
							HistogramPanel.this.getChartCache().addChart(options, chart);

						}
						programLogger.log(Level.FINEST, "Added cached histogram chart: "+stat);
					}

					XYPlot plot = (XYPlot) chart.getPlot();
					plot.setDomainPannable(true);
					plot.setRangePannable(true);

					panel.setChart(chart);
				}
			} catch(Exception e){
				programLogger.log(Level.SEVERE, "Error updating histogram panel", e);
			} finally {
				HistogramPanel.this.setUpdating(false);
			}
    	
//    		try {
//    			updateSignalAngleHistogram(list);
//    			updateSignalDistanceHistogram(list);
//    		} catch (Exception e) {
//    			programLogger.log(Level.SEVERE, "Error updating signal histograms", e);
//    		}
    	}
    	
//    	private void updateSignalAngleHistogram(List<AnalysisDataset> list) throws Exception{
//    		
//    		HistogramChartOptions options = new HistogramChartOptions(list, null, null, false);
//    		JFreeChart chart = HistogramChartFactory.createSignalAngleHistogram(options);
//    		angleChartPanel.setChart(chart);
//    	}
//
//    	private void updateSignalDistanceHistogram(List<AnalysisDataset> list) throws Exception{
//    		HistogramChartOptions options = new HistogramChartOptions(list, null, null, false);
//    		JFreeChart chart = HistogramChartFactory.createSignalDistanceHistogram(options);
////    		try {
////    			HistogramDataset ds = NuclearSignalDatasetCreator.createSignalDistanceHistogramDataset(list);
////
////    			if(ds.getSeriesCount()>0){
////    				chart = HistogramChartFactory.createSignalDistanceHistogram(ds, activeDataset());
////    			}
////
////    		} catch (Exception e) {
////    			programLogger.log(Level.SEVERE, "Error updating distance histograms", e);
////    		}
//    		distanceChartPanel.setChart(chart);
//    	}
    }

    protected class AnalysisPanel extends JPanel{

    	private static final long serialVersionUID = 1L;

    	private ExportableTable 		table;			// table for analysis parameters
    	private JScrollPane scrollPane;


    	protected AnalysisPanel(){

    		this.setLayout(new BorderLayout());

    		table  = new ExportableTable(new DefaultTableModel());
    		table.setAutoCreateColumnsFromModel(false);
    		table.setEnabled(false);
    		scrollPane = new JScrollPane(table);
    		this.add(scrollPane, BorderLayout.CENTER);
    	}
    	
    	/**
    	 * Update the signal analysis detection settings with the given datasets
    	 * @param list the datasets
    	 */
    	protected void update(List<AnalysisDataset> list){
    		try{
    			TableModel model;
    			if(hasDatasets()){

    				model = NuclearSignalDatasetCreator.createSignalDetectionParametersTable(list);
    			} else {
    				model = NuclearSignalDatasetCreator.createSignalDetectionParametersTable(null);
    			}

    			table.setModel(model);
    			table.createDefaultColumnsFromModel();
    		} catch (Exception e){
    			programLogger.log(Level.SEVERE, "Error updating signal analysis", e);
    		}
    	}

    }
    
    protected class BoxplotPanel extends JPanel{

    	private static final long serialVersionUID = 1L;

    	private ChartPanel 	chartPanel;


    	protected BoxplotPanel(){

    		this.setLayout(new BorderLayout());
    		
    		JFreeChart areaBoxplot = BoxplotChartFactory.makeEmptyBoxplot();
			chartPanel = new ChartPanel(areaBoxplot);
			this.add(chartPanel);
    	}
    	
    	/**
    	 * Update the boxplot panel for areas with a list of NucleusCollections
    	 * @param list
    	 */
    	protected void update(List<AnalysisDataset> list){
    		
    		try{
    			
    			JFreeChart boxplotChart;
    			if(isSingleDataset()){

    				BoxAndWhiskerCategoryDataset ds = NuclearSignalDatasetCreator.createSignalAreaBoxplotDataset(activeDataset());
    				boxplotChart = BoxplotChartFactory.makeSignalAreaBoxplot(ds, activeDataset());
    				
    			} else {
    				boxplotChart = BoxplotChartFactory.makeEmptyBoxplot();
    			}
    			chartPanel.setChart(boxplotChart);
    		}	catch(Exception e){
    			programLogger.log(Level.SEVERE, "Error updating boxplots", e);
    		}
    	}

    }
    
    protected class ShellsPanel extends JPanel{

    	private static final long serialVersionUID = 1L;

    	private ChartPanel 	chartPanel; 
    	private JLabel 		statusLabel  = new JLabel();
    	private JButton 	newAnalysis	 = new JButton("Run new shell analysis");

    	protected ShellsPanel(){
    		this.setLayout(new BorderLayout());
    		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
    		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
    		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
    		chartPanel = new ChartPanel(shellsChart);
    		this.add(chartPanel, BorderLayout.CENTER);
    		
    		this.add(statusLabel, BorderLayout.NORTH);
    		statusLabel.setVisible(false);
    		
    		newAnalysis.addMouseListener(new MouseAdapter() {
    			@Override
    			public void mouseClicked(MouseEvent arg0) {
    				fireSignalChangeEvent("RunShellAnalysis");
    			}
    		});
    		newAnalysis.setVisible(false);
    		this.add(newAnalysis, BorderLayout.SOUTH);


    	}
    	
    	/**
    	 * Update the shells panel with data from the given datasets
    	 * @param list the datasets
    	 */
    	protected void update(List<AnalysisDataset> list){

    		if(isSingleDataset()){ // single collection is easy
    			
//    			AnalysisDataset dataset = list.get(0);
    			CellCollection collection = activeDataset().getCollection();

    			if(activeDataset().hasShellResult()){ // only if there is something to display

    				CategoryDataset ds = NuclearSignalDatasetCreator.createShellBarChartDataset(list);
    				JFreeChart shellsChart = ChartFactory.createBarChart(null, "Outer <--- Shell ---> Interior", "Percent", ds);
    				shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
    				shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
    				StatisticalBarRenderer rend = new StatisticalBarRenderer();
    				rend.setBarPainter(new StandardBarPainter());
    				rend.setShadowVisible(false);
    				rend.setErrorIndicatorPaint(Color.black);
    				rend.setErrorIndicatorStroke(new BasicStroke(2));
    				shellsChart.getCategoryPlot().setRenderer(rend);

    				for (int j = 0; j < ds.getRowCount(); j++) {
    					rend.setSeriesVisibleInLegend(j, Boolean.FALSE);
    					rend.setSeriesStroke(j, new BasicStroke(2));
    					int index = getIndexFromLabel( (String) ds.getRowKey((j)));
    					Color colour = activeDataset().getSignalGroupColour(index);
    					rend.setSeriesPaint(j, colour);
    				}	

    				chartPanel.setChart(shellsChart);
    				chartPanel.setVisible(true);
    				
    				statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
    				String label = "";
    				for(int i=1; i<=activeDataset().getHighestSignalGroup();i++){
    					ShellResult r = activeDataset().getShellResult(i);
    						label += "Group "+i+": p="+r.getChiSquare();
    						String sig 	= r.getChiSquare() < Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL 
    									? "Significantly different to random at 5% level"
    									: "Not significantly different to random at 5% level";
    						
    						label += "; "+sig+"\n";
    					
    				}
    				statusLabel.setText(label);
    				statusLabel.setVisible(true);
    				
    				
    				newAnalysis.setVisible(false);
    				
    			} else { // no shell analysis available

    				if(collection.hasSignals()){
    					// if signals, offer to run
    					makeNoShellAnalysisAvailablePanel(true, collection, "No shell results available"); // allow option to run analysis
    				} else {
    					// otherwise don't show button
    					makeNoShellAnalysisAvailablePanel(false, null, "No signals in population"); // container in tab if no shell chart
    				}
    			}
    		} else {
    			
    			// Multiple populations. Do not display
    			// container in tab if no shell chart
    			makeNoShellAnalysisAvailablePanel(false, null, "Cannot display shell results for multiple populations");
    		}
    	}
    	
    	/**
    	 * Create a panel to display when a shell analysis is not available
    	 * @param showRunButton should there be an option to run a shell analysis on the dataset
    	 * @param collection the nucleus collection from the dataset
    	 * @param label the text to display on the panel
    	 * @return a panel to put in the shell tab
    	 */
    	private void makeNoShellAnalysisAvailablePanel(boolean showRunButton, CellCollection collection, String label){
    		chartPanel.setVisible(false);
    		statusLabel.setText(label);
    		statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
    		statusLabel.setVisible(true);

    		newAnalysis.setVisible(showRunButton);

    		this.revalidate();
    		this.repaint();
  
    	}
    }
}
