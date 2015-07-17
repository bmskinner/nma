package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;

import datasets.NucleusDatasetCreator;

public class SignalsDetailPanel extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;
	
	private ChartPanel shellsChartPanel; 
	private JPanel shellsPanel;
	
	private ChartPanel signalsChartPanel; // consensus nucleus plus signals
	private JPanel signalsPanel;// signals container for chart and stats table
	private JTable signalStatsTable;
	private JPanel signalAnalysisSetupPanel;
	private JTable signalAnalysisSetupTable;
	private JPanel signalSelectionVisiblePanel;
	private JPanel signalConsensusAndCheckboxPanel;
	private JTabbedPane signalsTabPane;
	
	private static final int TAB_OVERVIEW 	= 0;
	private static final int TAB_HISTOGRAMS = 1;
	private static final int TAB_SHELLS 	= 2;
	private static final int TAB_SETTINGS 	= 3;
	
	
	private ChartPanel signalAngleChartPanel; // consensus nucleus plus signals
	private ChartPanel signalDistanceChartPanel; // consensus nucleus plus signals
	private JPanel signalHistogramPanel;// signals container for chart and stats table
	
	private List<AnalysisDataset> list;

	/**
	 * Create the panel.
	 */
	public SignalsDetailPanel() {
		
		this.setLayout(new BorderLayout());
		
		signalsTabPane = new JTabbedPane(JTabbedPane.TOP);
		
		signalsPanel = new JPanel(); // main container in tab
		signalsPanel.setLayout(new BoxLayout(signalsPanel, BoxLayout.X_AXIS));

		//---------------
		// Stats panel
		//---------------
		DefaultTableModel signalsTableModel = new DefaultTableModel();
		signalsTableModel.addColumn("");
		signalsTableModel.addColumn("");
		signalStatsTable = new JTable(); // table  for basic stats
		signalStatsTable.setModel(signalsTableModel);
		signalStatsTable.setEnabled(false);
		
		JScrollPane signalStatsScrollPane = new JScrollPane(signalStatsTable);
		signalsPanel.add(signalStatsScrollPane);
		
		//---------------
		// Consensus chart
		//---------------
		
		// make a blank chart for signal locations on a consensus nucleus
		JFreeChart signalsChart = ChartFactory.createXYLineChart(null,  // chart for conseusns
				null, null, null);
		XYPlot signalsPlot = signalsChart.getXYPlot();
		
		signalsPlot.setBackgroundPaint(Color.WHITE);
		signalsPlot.getDomainAxis().setVisible(false);
		signalsPlot.getRangeAxis().setVisible(false);

		// the chart is inside a chartPanel; the chartPanel is inside a JPanel
		// this allows a checkbox panel to be added to the JPanel later
		signalsChartPanel = new ChartPanel(signalsChart);
		signalConsensusAndCheckboxPanel = new JPanel(new BorderLayout());
		signalConsensusAndCheckboxPanel.add(signalsChartPanel, BorderLayout.CENTER);
				
		signalsPanel.add(signalConsensusAndCheckboxPanel);
		
		signalsTabPane.addTab("Overview", null, signalsPanel, null);
		//---------------
		// Distance and angle histograms charts
		//---------------
		
		JFreeChart signalAngleChart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
		signalAngleChart.getPlot().setBackgroundPaint(Color.white);
		
		JFreeChart signalDistanceChart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
		signalDistanceChart.getPlot().setBackgroundPaint(Color.white);
		
		signalHistogramPanel = new JPanel(); // main container in tab
		signalHistogramPanel.setLayout(new BoxLayout(signalHistogramPanel, BoxLayout.Y_AXIS));
		signalAngleChartPanel = new ChartPanel(signalAngleChart);
		signalDistanceChartPanel = new ChartPanel(signalDistanceChart);

		signalHistogramPanel.add(signalAngleChartPanel);
		signalHistogramPanel.add(signalDistanceChartPanel);
		
		signalsTabPane.addTab("Signal histograms", null, signalHistogramPanel, null);
		
		//---------------
		// Create the shells panel
		//---------------
		JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
		shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
		shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
		shellsChartPanel = new ChartPanel(shellsChart);

		signalsTabPane.addTab("Shells", null, shellsChartPanel, null);
		
		
		//---------------
		// Create the signal analysis settings panel
		//---------------
		signalAnalysisSetupPanel = new JPanel(new BorderLayout());
				
		signalAnalysisSetupTable  = new JTable(new DefaultTableModel());
		signalAnalysisSetupTable.setEnabled(false);
		JScrollPane signalAnalysisSetupScrollPane = new JScrollPane(signalAnalysisSetupTable);
		signalAnalysisSetupPanel.add(signalAnalysisSetupScrollPane, BorderLayout.CENTER);
		signalsTabPane.addTab("Detection settings", null, signalAnalysisSetupPanel, null);
		this.add(signalsTabPane, BorderLayout.CENTER);
	}

	
	public void update(List<AnalysisDataset> list){
		this.list = list;
		updateShellPanel(list);
		updateSignalsPanel(list);
		updateSignalHistogramPanel(list);
		updateSignalAnalysisSetupPanel(list);
	}
	
	private void updateSignalsPanel(List<AnalysisDataset> list){
		
		updateSignalConsensusChart(list);
		updateSignalStatsPanel(list);
//		contentPane.revalidate();
//		contentPane.repaint();	
	}
	
	/**
	 * Update the signal stats with the given datasets
	 * @param list the datasets
	 */
	private void updateSignalStatsPanel(List<AnalysisDataset> list){
		try{
			TableModel model = NucleusDatasetCreator.createSignalStatsTable(list);
			signalStatsTable.setModel(model);
		} catch (Exception e){
			IJ.log("Error updating signal stats: "+e.getMessage());
		}
		int columns = signalStatsTable.getColumnModel().getColumnCount();
		for(int i=1;i<columns;i++){
			signalStatsTable.getColumnModel().getColumn(i).setCellRenderer(new StatsTableCellRenderer());
		}
	}
	
	/**
	 * Update the signal analysis detection settings with the given datasets
	 * @param list the datasets
	 */
	private void updateSignalAnalysisSetupPanel(List<AnalysisDataset> list){
		try{
			TableModel model = NucleusDatasetCreator.createSignalDetectionParametersTable(list);
			this.signalAnalysisSetupTable.setModel(model);
		} catch (Exception e){
			IJ.log("Error updating signal analysis: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
	}
	
	/**
	 * Create the checkboxes that set each signal channel visible or not
	 */
	private JPanel createSignalsVisiblePanel(AnalysisDataset d){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		try {

			for(int signalGroup : d.getCollection().getSignalGroups()){

				boolean visible = d.isSignalGroupVisible(signalGroup);
				
				String name = d.getCollection().getSignalGroupName(signalGroup);
				// make a checkbox for each signal group in the dataset
				JCheckBox box = new JCheckBox(name);
				
				// get the status within each dataset
				box.setSelected(visible);
				
				// apply the appropriate action 
				box.setActionCommand("GroupVisble_"+signalGroup);
				box.addActionListener(this);
				panel.add(box);

			}

		} catch(Exception e){
			IJ.log("Error creating signal checkboxes: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
		return panel;
	}
	
	/**
	 * Create a consenusus chart for the given nucleus collection
	 * @param collection the NucleusCollection to draw the consensus from
	 * @return the consensus chart
	 */
	public JFreeChart makeConsensusChart(CellCollection collection){
		XYDataset ds = NucleusDatasetCreator.createNucleusOutline(collection);
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
						null, null, null, PlotOrientation.VERTICAL, true, true,
						false);
		

		double maxX = Math.max( Math.abs(collection.getConsensusNucleus().getMinX()) , Math.abs(collection.getConsensusNucleus().getMaxX() ));
		double maxY = Math.max( Math.abs(collection.getConsensusNucleus().getMinY()) , Math.abs(collection.getConsensusNucleus().getMaxY() ));

		// ensure that the scales for each axis are the same
		double max = Math.max(maxX, maxY);

		// ensure there is room for expansion of the target nucleus due to IQR
		max *=  1.25;		

		XYPlot plot = chart.getXYPlot();
		plot.setDataset(0, ds);
		plot.getDomainAxis().setRange(-max,max);
		plot.getRangeAxis().setRange(-max,max);

		plot.getDomainAxis().setVisible(false);
		plot.getRangeAxis().setVisible(false);

		plot.setBackgroundPaint(Color.WHITE);
		plot.addRangeMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));
		plot.addDomainMarker(new ValueMarker(0, Color.LIGHT_GRAY, new BasicStroke(1.0f)));

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			String name = (String) ds.getSeriesKey(i);
			
			if(name.startsWith("Seg_")){
				int colourIndex = getIndexFromLabel(name);
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
				plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
			} 
			if(name.startsWith("Q")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
			} 

		}	
		return chart;
	}
	
	private void updateSignalHistogramPanel(List<AnalysisDataset> list){
		try {
			updateSignalAngleHistogram(list);
			updateSignalDistanceHistogram(list);
		} catch (Exception e) {
			IJ.log("Error updating signal histograms: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
	}
	
	private void updateSignalAngleHistogram(List<AnalysisDataset> list){
		try {
			HistogramDataset ds = NucleusDatasetCreator.createSignalAngleHistogramDataset(list);
			if(ds.getSeriesCount()>0){
				JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", ds, PlotOrientation.VERTICAL, true, true, true);
				XYPlot plot = chart.getXYPlot();
				plot.setBackgroundPaint(Color.white);
				XYBarRenderer rend = new XYBarRenderer();
				rend.setBarPainter(new StandardXYBarPainter());
				rend.setShadowVisible(false);
				plot.setRenderer(rend);
				plot.getDomainAxis().setRange(0,360);
				for (int j = 0; j < ds.getSeriesCount(); j++) {
					String name = (String) ds.getSeriesKey(j);
					int seriesGroup = getIndexFromLabel(name);
					plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));

					plot.getRenderer().setSeriesPaint(j, ColourSelecter.getSignalColour(seriesGroup-1, true, 128));
				}	
				signalAngleChartPanel.setChart(chart);
			} else {
				JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
				chart.getPlot().setBackgroundPaint(Color.white);
				signalAngleChartPanel.setChart(chart);
			}
		} catch (Exception e) {
			IJ.log("Error updating angle histograms: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
			JFreeChart chart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
			chart.getPlot().setBackgroundPaint(Color.white);
			signalAngleChartPanel.setChart(chart);
		}
		
		
	}

	private void updateSignalDistanceHistogram(List<AnalysisDataset> list){
		try {
			HistogramDataset ds = NucleusDatasetCreator.createSignalDistanceHistogramDataset(list);
			
			if(ds.getSeriesCount()>0){
				JFreeChart chart = ChartFactory.createHistogram(null, "Distance", "Count", ds, PlotOrientation.VERTICAL, true, true, true);
				XYPlot plot = chart.getXYPlot();
				plot.setBackgroundPaint(Color.white);
				XYBarRenderer rend = new XYBarRenderer();
				rend.setBarPainter(new StandardXYBarPainter());
				rend.setShadowVisible(false);
				plot.setRenderer(rend);
				plot.getDomainAxis().setRange(0,1);
				for (int j = 0; j < ds.getSeriesCount(); j++) {
					plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
					int index = getIndexFromLabel( (String) ds.getSeriesKey(j));
					plot.getRenderer().setSeriesPaint(j, ColourSelecter.getSignalColour(index-1, true, 128));
				}	
				signalDistanceChartPanel.setChart(chart);
			} else {
				JFreeChart chart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
				chart.getPlot().setBackgroundPaint(Color.white);
				signalDistanceChartPanel.setChart(chart);
			}
		} catch (Exception e) {
			IJ.log("Error updating distance histograms: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
			JFreeChart chart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
			chart.getPlot().setBackgroundPaint(Color.white);
			signalDistanceChartPanel.setChart(chart);
		}
	}
	
	private void updateSignalConsensusChart(List<AnalysisDataset> list){
		try {

			if(list.size()==1){
				
				AnalysisDataset dataset = list.get(0);
				
				signalSelectionVisiblePanel = createSignalsVisiblePanel(dataset);
				signalConsensusAndCheckboxPanel.add(signalSelectionVisiblePanel, BorderLayout.NORTH);
				signalConsensusAndCheckboxPanel.setVisible(true);

				CellCollection collection = list.get(0).getCollection();

				if(collection.hasConsensusNucleus()){ // if a refold is available
					
					XYDataset signalCoMs = NucleusDatasetCreator.createSignalCoMDataset(dataset);
					JFreeChart chart = makeConsensusChart(collection);

					XYPlot plot = chart.getXYPlot();
					plot.setDataset(1, signalCoMs);

					XYLineAndShapeRenderer  rend = new XYLineAndShapeRenderer();
					for(int series=0;series<signalCoMs.getSeriesCount();series++){
						String name = (String) signalCoMs.getSeriesKey(series);
						int seriesGroup = getIndexFromLabel(name);
						rend.setSeriesPaint(series, ColourSelecter.getSignalColour(seriesGroup-1, false));
						rend.setBaseLinesVisible(false);
						rend.setBaseShapesVisible(true);
						rend.setBaseSeriesVisibleInLegend(false);
					}
					plot.setRenderer(1, rend);

					for(int signalGroup : collection.getSignalGroups()){
						List<Shape> shapes = NucleusDatasetCreator.createSignalRadiusDataset(dataset, signalGroup);

						int signalCount = shapes.size();

						int alpha = (int) Math.floor( 255 / ((double) signalCount) );
						alpha = alpha < 5 ? 5 : alpha > 128 ? 128 : alpha;

						for(Shape s : shapes){
							XYShapeAnnotation an = new XYShapeAnnotation( s, null,
									null, ColourSelecter.getSignalColour(signalGroup-1, true, alpha)); // layer transparent signals
							plot.addAnnotation(an);
						}
					}
					signalsChartPanel.setChart(chart);
				} else { // no consensus to display
										
					signalConsensusAndCheckboxPanel.setVisible(false);
				}
			} else { // multiple populations
				
				signalConsensusAndCheckboxPanel.setVisible(false);
			}
		} catch(Exception e){
			IJ.log("Error updating signals: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
	}
	
	/**
	 * Update the shells panel with data from the given datasets
	 * @param list the datasets
	 */
	private void updateShellPanel(List<AnalysisDataset> list){

		if(list.size()==1){ // single collection is easy
			
			AnalysisDataset dataset = list.get(0);
			CellCollection collection = dataset.getCollection();

			if(dataset.hasShellResult()){ // only if there is something to display

				CategoryDataset ds = NucleusDatasetCreator.createShellBarChartDataset(list);
				JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", ds);
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
					rend.setSeriesPaint(j, ColourSelecter.getSignalColour(index));
				}	

				shellsChartPanel.setChart(shellsChart);
				

				
//				signalsTabPane.setComponentAt(2, shellsChartPanel);
			} else { // no shell analysis available

				if(collection.hasSignals()){
					// if signals, offer to run
					shellsPanel = makeNoShellAnalysisAvailablePanel(true, collection, "No shell results available"); // allow option to run analysis
					signalsTabPane.setComponentAt(TAB_SHELLS, shellsPanel);
				} else {
					// otherwise don't show button
					shellsPanel = makeNoShellAnalysisAvailablePanel(false, null, "No signals in population"); // container in tab if no shell chart
					signalsTabPane.setComponentAt(TAB_SHELLS, shellsPanel);
				}
			}
		} else {
			
//			shellsPanel.setVisible(false);

			// Multiple populations. Do not display
			// container in tab if no shell chart
			shellsPanel = makeNoShellAnalysisAvailablePanel(false, null, "Cannot display shell results for multiple populations");
			signalsTabPane.setComponentAt(TAB_SHELLS, shellsPanel);
		}
	}
	
	/**
	 * Create a panel to display when a shell analysis is not available
	 * @param showRunButton should there be an option to run a shell analysis on the dataset
	 * @param collection the nucleus collection from the dataset
	 * @param label the text to display on the panel
	 * @return a panel to put in the shell tab
	 */
	private JPanel makeNoShellAnalysisAvailablePanel(boolean showRunButton, CellCollection collection, String label){
		JPanel panel = new JPanel(); // container in tab if no shell chart
		
		panel.setLayout(new BorderLayout(0,0));
		JLabel lbl = new JLabel(label);
		lbl.setHorizontalAlignment(SwingConstants.CENTER);
		panel.add(lbl, BorderLayout.NORTH);
		return panel;
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
		// TODO Auto-generated method stub
		if(e.getActionCommand().startsWith("GroupVisble_")){
			
			int signalGroup = this.getIndexFromLabel(e.getActionCommand());
			JCheckBox box = (JCheckBox) e.getSource();
			AnalysisDataset d = list.get(0);
			d.setSignalGroupVisible(signalGroup, box.isSelected());
			updateSignalConsensusChart(list);
			updateSignalHistogramPanel(list);
		}
		
	}
	
	/**
	 * Allows for cell background to be coloured based on poition in a list. Used to colour
	 * the signal stats list
	 *
	 */
	private class StatsTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			// default cell colour is white
			Color colour = Color.WHITE;

			// get the value in the first column of the row below
			if(row<table.getModel().getRowCount()-1){
				String nextRowHeader = table.getModel().getValueAt(row+1, 0).toString();

				if(nextRowHeader.equals("Signal group")){
					// we want to colour this cell preemptively
					// get the signal group from the table
					String groupString = table.getModel().getValueAt(row+1, 1).toString();
					
					colour = ColourSelecter.getSignalColour(  Integer.valueOf(groupString)-1   ); 
				}
			}
			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}
}
