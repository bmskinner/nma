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
import gui.tabs.signals.SignalsOverviewPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
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
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import utility.Constants;
import analysis.AnalysisDataset;
import charting.charts.BoxplotChartFactory;
import charting.charts.HistogramChartFactory;
import charting.charts.HistogramChartOptions;
import charting.datasets.NuclearSignalDatasetCreator;
import components.CellCollection;
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

			histogramPanel = new HistogramPanel();
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
			
			int signalGroup = this.getIndexFromLabel(e.getActionCommand());
			JCheckBox box = (JCheckBox) e.getSource();
			AnalysisDataset d = this.activeDataset();
			d.setSignalGroupVisible(signalGroup, box.isSelected());
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
	}
    
//    protected class OverviewPanel extends JPanel{
//    	
//    	private static final long serialVersionUID = 1L;
//    	
//    	private ConsensusNucleusChartPanel 	chartPanel; 		// consensus nucleus plus signals
//    	private ExportableTable 		statsTable;					// table for signal stats
//    	private JPanel 		consensusAndCheckboxPanel;	// holds the consensus chart and the checkbox
//    	private JPanel		checkboxPanel;
//    	
//    	
//    	protected OverviewPanel(){
//    		
//    		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
//
//    		JScrollPane scrollPane = createStatsPane();
//    		this.add(scrollPane);
//    		
//    	
//    		consensusAndCheckboxPanel = createConsensusPanel();
//    		this.add(consensusAndCheckboxPanel);
//    		
//    	}
//    	
//    	private JScrollPane createStatsPane(){
//    		DefaultTableModel tableModel = new DefaultTableModel();
//    		tableModel.addColumn("");
//    		tableModel.addColumn("");
//    		statsTable = new ExportableTable(); // table  for basic stats
//    		statsTable.setModel(tableModel);
//    		statsTable.setEnabled(false);
//    		
//    		statsTable.addMouseListener(new MouseAdapter() {
//    			@Override
//    			public void mouseClicked(MouseEvent e) {
//    				
//    				JTable table = (JTable) e.getSource();
//    				
//    				int row = table.rowAtPoint(e.getPoint());
//    				int column = table.columnAtPoint(e.getPoint());
//    				
//    				int signalGroupRow = 0;
//    				int signalGroup = 0;
//    				int rowsPerSignalGroup = 11;
//    				if(row>0){
//    					signalGroupRow = row - (row % rowsPerSignalGroup);
//    					signalGroup = (Integer) table.getModel().getValueAt(signalGroupRow, column);
//    				}
//    				
//    				// double click
//    				if (e.getClickCount() == 2) {
//
//    					String rowName = table.getModel().getValueAt(row, 0).toString();
//    					String nextRowName = table.getModel().getValueAt(row+1, 0).toString();
//    					if(nextRowName.equals("Signal group")){
//    						updateSignalColour( signalGroup );
//    					}
//    					
//    					if(rowName.equals("Source")){
//    						updateSignalSource( signalGroup );
//    					}
//    						
//    				}
//
//    			}
//    		});
//    		
//    		JScrollPane scrollPane = new JScrollPane(statsTable);
//    		return scrollPane;
//    	}
//    	
//    	private void updateSignalSource(int signalGroup){
//    		if(isSingleDataset()){
//    			programLogger.log(Level.FINEST, "Updating signal source for signal group "+signalGroup);
//
//    			DirectoryChooser openDialog = new DirectoryChooser("Select directory of signal images...");
//    			String folderName = openDialog.getDirectory();
//
//    			if(folderName==null){
//    				programLogger.log(Level.FINEST, "Folder name null");
//    				return;
//    			}
//
//    			File folder =  new File(folderName);
//
//    			if(!folder.isDirectory() ){
//    				programLogger.log(Level.FINEST, "Folder is not directory");
//    				return;
//    			}
//    			if(!folder.exists()){
//    				programLogger.log(Level.FINEST, "Folder does not exist");
//    				return;
//    			}
//
//    			activeDataset().getCollection().updateSignalSourceFolder(signalGroup, folder);
////    			SignalsDetailPanel.this.update(getDatasets());
//    			refreshTableCache();
//    			programLogger.log(Level.FINEST, "Updated signal source for signal group "+signalGroup+" to "+folder.getAbsolutePath() );
//    		}
//    	}
//    	
//    	/**
//    	 * Update the colour of the clicked signal group
//    	 * @param row the row selected (the colour bar, one above the group name)
//    	 */
//    	private void updateSignalColour(int signalGroup){
//			
//			Color oldColour = ColourSelecter.getSignalColour( signalGroup-1 );
//			
//			Color newColor = JColorChooser.showDialog(
//                     SignalsDetailPanel.this,
//                     "Choose signal Color",
//                     oldColour);
//			
//			if(newColor != null){
//				activeDataset().setSignalGroupColour(signalGroup, newColor);
//				SignalsDetailPanel.this.update(getDatasets());
////				fireSignalChangeEvent("SignalColourUpdate");
//				fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
//			}
//    	}
//    	
//    	private JPanel createConsensusPanel(){
//    		
//    		final JPanel panel = new JPanel(new BorderLayout());
//    		// make a blank chart for signal locations on a consensus nucleus
//    		JFreeChart signalsChart = ChartFactory.createXYLineChart(null,  // chart for conseusns
//    				null, null, null);
//    		XYPlot signalsPlot = signalsChart.getXYPlot();
//
//    		signalsPlot.setBackgroundPaint(Color.WHITE);
//    		signalsPlot.getDomainAxis().setVisible(false);
//    		signalsPlot.getRangeAxis().setVisible(false);
//    				
//    		// the chart is inside a chartPanel; the chartPanel is inside a JPanel
//    		// this allows a checkbox panel to be added to the JPanel later
//    		chartPanel = new ConsensusNucleusChartPanel(signalsChart);// {
////    			@Override
////				public void restoreAutoBounds() {
////					XYPlot plot = (XYPlot) this.getChart().getPlot();
////					
////					double chartWidth = this.getWidth();
////					double chartHeight = this.getHeight();
////					double aspectRatio = chartWidth / chartHeight;
////					
////					// start with impossible values
////					double xMin = chartWidth;
////					double yMin = chartHeight;
//////					
////					double xMax = 0;
////					double yMax = 0;
////					
////					// get the max and min values of the chart
////					for(int i = 0; i<plot.getDatasetCount();i++){
////						XYDataset dataset = plot.getDataset(i);
////
////						if(DatasetUtilities.findMaximumDomainValue(dataset)!=null){
////
////							xMax = DatasetUtilities.findMaximumDomainValue(dataset).doubleValue() > xMax
////									? DatasetUtilities.findMaximumDomainValue(dataset).doubleValue()
////											: xMax;
////
////							xMin = DatasetUtilities.findMinimumDomainValue(dataset).doubleValue() < xMin
////									? DatasetUtilities.findMinimumDomainValue(dataset).doubleValue()
////											: xMin;
////
////							yMax = DatasetUtilities.findMaximumRangeValue(dataset).doubleValue() > yMax
////									? DatasetUtilities.findMaximumRangeValue(dataset).doubleValue()
////											: yMax;
////
////							yMin = DatasetUtilities.findMinimumRangeValue(dataset).doubleValue() < yMin
////									? DatasetUtilities.findMinimumRangeValue(dataset).doubleValue()
////											: yMin;
////						}
////					}
////					
////
////					// find the ranges they cover
////					double xRange = xMax - xMin;
////					double yRange = yMax - yMin;
////					
//////					double aspectRatio = xRange / yRange;
////
////					double newXRange = xRange;
////					double newYRange = yRange;
////
////					// test the aspect ratio
//////					IJ.log("Old range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);
////					if( (xRange / yRange) > aspectRatio){
////						// width is not enough
//////						IJ.log("Too narrow: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
////						newXRange = xRange * 1.1;
////						newYRange = newXRange / aspectRatio;
////					} else {
////						// height is not enough
//////						IJ.log("Too short: "+xRange+", "+yRange+":  aspect ratio "+aspectRatio);
////						newYRange = yRange * 1.1; // add some extra x space
////						newXRange = newYRange * aspectRatio; // get the new Y range
////					}
////					
////
////					// with the new ranges, find the best min and max values to use
////					double xDiff = (newXRange - xRange)/2;
////					double yDiff = (newYRange - yRange)/2;
////
////					xMin -= xDiff;
////					xMax += xDiff;
////					yMin -= yDiff;
////					yMax += yDiff;
//////					IJ.log("New range: "+xMax+"-"+xMin+", "+yMax+"-"+yMin);
////
////					plot.getRangeAxis().setRange(yMin, yMax);
////					plot.getDomainAxis().setRange(xMin, xMax);				
////				} 
////    		};
//    		panel.add(chartPanel, BorderLayout.CENTER);
//    		
//    		
//    		chartPanel.addComponentListener(new ComponentAdapter() {
//    			@Override
//    			public void componentResized(ComponentEvent e) {
//    				resizePreview(chartPanel, panel);
//    				chartPanel.restoreAutoBounds();
//    			}
//    		});
//    		
//    		panel.addComponentListener(new ComponentAdapter() {
//    			@Override
//    			public void componentResized(ComponentEvent e) {
//    				resizePreview(chartPanel, panel);
//    			}
//    		});
//    		
//    		
//    		checkboxPanel = createSignalCheckboxPanel(null);
//    		
//    		panel.add(checkboxPanel, BorderLayout.NORTH);
//
//    		return panel;
//    	}
//    	
//    	/**
//    	 * Create the checkboxes that set each signal channel visible or not
//    	 */
//    	private JPanel createSignalCheckboxPanel(AnalysisDataset d){
//    		JPanel panel = new JPanel();
//    		
//    		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
//    		
//    		if(d!=null){
//    			try {
//
//    				for(int signalGroup : d.getCollection().getSignalGroups()){
//
//    					boolean visible = d.isSignalGroupVisible(signalGroup);
//
//    					String name = d.getCollection().getSignalGroupName(signalGroup);
//    					// make a checkbox for each signal group in the dataset
//    					JCheckBox box = new JCheckBox(name);
//
//    					// get the status within each dataset
//    					box.setSelected(visible);
//
//    					// apply the appropriate action 
//    					box.setActionCommand("GroupVisble_"+signalGroup);
//    					box.addActionListener(SignalsDetailPanel.this);
//    					panel.add(box);
//
//    				}
//
//    			} catch(Exception e){
//    				programLogger.log(Level.SEVERE, "Error creating signal checkboxes", e);
//    			}
//    		}
//    		return panel;
//    	}
//    	
//    	protected void update(List<AnalysisDataset> list){
//    		updateCheckboxPanel(list);
//    		updateSignalConsensusChart(list);
//    		updateSignalStatsPanel(list);
//    	}
//    	
//    	/**
//    	 * Update the signal stats with the given datasets
//    	 * @param list the datasets
//    	 */
//    	private void updateSignalStatsPanel(List<AnalysisDataset> list){
//    		
//    		TableModel model = NuclearSignalDatasetCreator.createSignalStatsTable(null);
//    		
//    		DefaultTableOptions options = new DefaultTableOptions(list, TableType.SIGNAL_STATS_TABLE, programLogger);
//    		
//    		if(getTableCache().hasTable(options)){
//    			model = getTableCache().getTable(options);
//    			programLogger.log(Level.FINEST, "Fetched cached signal stats table");
//    		} else {
//    			model = NuclearSignalDatasetCreator.createSignalStatsTable(options); 
//    			getTableCache().addTable(options, model);
//    			programLogger.log(Level.FINEST, "Added cached signal stats table");
//    		}
//    		statsTable.setModel(model);
//
//    		// Add the signal group colours
//    		if(list!=null && !list.isEmpty()){
//    			int columns = statsTable.getColumnModel().getColumnCount();
//    			if(columns>1){
//    				for(int i=1;i<columns;i++){
//    					statsTable.getColumnModel().getColumn(i).setCellRenderer(new StatsTableCellRenderer());
//    				}
//    			}
//    		}
//    			
//    		
//    	}
//    	
//    	private void updateCheckboxPanel(List<AnalysisDataset> list){
//    		if(list.size()==1){
//								
//				// make a new panel for the active dataset
//				consensusAndCheckboxPanel.remove(checkboxPanel);
//				checkboxPanel = createSignalCheckboxPanel(activeDataset());
//	
//				// add this new panel
//				consensusAndCheckboxPanel.add(checkboxPanel, BorderLayout.NORTH);
//				consensusAndCheckboxPanel.revalidate();
//				consensusAndCheckboxPanel.repaint();
//				consensusAndCheckboxPanel.setVisible(true);
//    		}
//    	}
//    	
//    	
//    	private void updateSignalConsensusChart(List<AnalysisDataset> list){
//    		try {
//
//    			if(list.size()==1){
//    								
//    				CellCollection collection = activeDataset().getCollection();
//
//    				if(collection.hasConsensusNucleus()){ // if a refold is available
//    					
//    					XYDataset signalCoMs = NuclearSignalDatasetCreator.createSignalCoMDataset(activeDataset());
//    					JFreeChart chart = MorphologyChartFactory.makeSignalCoMNucleusOutlineChart(activeDataset(), signalCoMs);
//    					chartPanel.setChart(chart);
//    					chartPanel.restoreAutoBounds();
//    				} else { // no consensus to display
//    							
//    					JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
//    					chartPanel.setChart(chart);
//    				}
//    				
//    			} else { // multiple populations
//    				
//    				JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
//					chartPanel.setChart(chart);
//					
////    				consensusAndCheckboxPanel.setVisible(false);
//    			}
//    		} catch(Exception e){
//    			programLogger.log(Level.SEVERE, "Error updating signals", e);
//    		}
//    	}
//    }
    
    protected class HistogramPanel extends JPanel{
    	
    	private static final long serialVersionUID = 1L;
    	
    	private ChartPanel 	angleChartPanel; 		// 
    	private ChartPanel 	distanceChartPanel; 	// 
  	    	
    	protected HistogramPanel(){
    		
    		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    		HistogramChartOptions options = new HistogramChartOptions(null, null, null, false);
    		JFreeChart signalAngleChart 	= HistogramChartFactory.createSignalAngleHistogram(options);
    		JFreeChart signalDistanceChart 	= HistogramChartFactory.createSignalDistanceHistogram(options);
    		    		
    		angleChartPanel 	= new ChartPanel(signalAngleChart);
    		distanceChartPanel 	= new ChartPanel(signalDistanceChart);

    		this.add(angleChartPanel);
    		this.add(distanceChartPanel);
    	}
    	
    	protected void update(List<AnalysisDataset> list){
    	
    		try {
    			updateSignalAngleHistogram(list);
    			updateSignalDistanceHistogram(list);
    		} catch (Exception e) {
    			programLogger.log(Level.SEVERE, "Error updating signal histograms", e);
    		}
    	}
    	
    	private void updateSignalAngleHistogram(List<AnalysisDataset> list){
    		
    		HistogramChartOptions options = new HistogramChartOptions(list, null, null, false);
    		JFreeChart chart = HistogramChartFactory.createSignalAngleHistogram(options);
    		angleChartPanel.setChart(chart);
    	}

    	private void updateSignalDistanceHistogram(List<AnalysisDataset> list){
    		HistogramChartOptions options = new HistogramChartOptions(list, null, null, false);
    		JFreeChart chart = HistogramChartFactory.createSignalDistanceHistogram(options);
//    		try {
//    			HistogramDataset ds = NuclearSignalDatasetCreator.createSignalDistanceHistogramDataset(list);
//
//    			if(ds.getSeriesCount()>0){
//    				chart = HistogramChartFactory.createSignalDistanceHistogram(ds, activeDataset());
//    			}
//
//    		} catch (Exception e) {
//    			programLogger.log(Level.SEVERE, "Error updating distance histograms", e);
//    		}
    		distanceChartPanel.setChart(chart);
    	}
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
