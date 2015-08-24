package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
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
import no.components.ShellResult;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;

import utility.Constants;
import datasets.ConsensusNucleusChartFactory;
import datasets.HistogramChartFactory;
import datasets.MorphologyChartFactory;
import datasets.NucleusDatasetCreator;

public class SignalsDetailPanel extends DetailPanel implements ActionListener, SignalChangeListener {

	private static final long serialVersionUID = 1L;
		
	private OverviewPanel	overviewPanel; 	//container for chart and stats table
	private HistogramPanel 	histogramPanel;
	private AnalysisPanel	analysisPanel;
	private BoxplotPanel	boxplotPanel;
	private ShellsPanel		shellsPanel;

	private JTabbedPane signalsTabPane;
	
	private List<AnalysisDataset> list;
	private AnalysisDataset activeDataset;

	/**
	 * Create the panel.
	 */
	public SignalsDetailPanel() {
		
		try{

			this.setLayout(new BorderLayout());

			signalsTabPane = new JTabbedPane(JTabbedPane.TOP);

			overviewPanel = new OverviewPanel();
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
			error("Error making signal panel", e);
		}
	}

	
	private static void resizePreview(ChartPanel innerPanel, JPanel container) {
        int w = container.getWidth();
        int h = container.getHeight();
        int size =  Math.min(w, h);
        innerPanel.setPreferredSize(new Dimension(size, size));
        container.revalidate();
    }

	
	public void update(List<AnalysisDataset> list){
		this.list = list;
		if(list.size()==1){
			this.activeDataset = list.get(0);
		}
		shellsPanel.update(list);
		overviewPanel.update(list);
		histogramPanel.update(list);
		analysisPanel.update(list);
		boxplotPanel.update(list);
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
			AnalysisDataset d = list.get(0);
			d.setSignalGroupVisible(signalGroup, box.isSelected());
			overviewPanel.update(list);
			histogramPanel.update(list);
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
					colour = activeDataset.getSignalGroupColour(Integer.valueOf(groupString));
//					colour = ColourSelecter.getSignalColour(  Integer.valueOf(groupString)-1   ); 
				}
			}
			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals("SignalColourUpdate")){
			update(list);
		}
	}
    
    protected class OverviewPanel extends JPanel{
    	
    	private static final long serialVersionUID = 1L;
    	
    	private ChartPanel 	chartPanel; 		// consensus nucleus plus signals
    	private JTable 		statsTable;					// table for signal stats
    	private JPanel 		consensusAndCheckboxPanel;	// holds the consensus chart and the checkbox
    	private JPanel		checkboxPanel;
    	
    	
    	protected OverviewPanel(){
    		
    		this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    		JScrollPane scrollPane = createStatsPane();
    		this.add(scrollPane);
    		
    	
    		consensusAndCheckboxPanel = createConsensusPanel();
    		this.add(consensusAndCheckboxPanel);
    		
    	}
    	
    	private JScrollPane createStatsPane(){
    		DefaultTableModel tableModel = new DefaultTableModel();
    		tableModel.addColumn("");
    		tableModel.addColumn("");
    		statsTable = new JTable(); // table  for basic stats
    		statsTable.setModel(tableModel);
    		statsTable.setEnabled(false);
    		
    		statsTable.addMouseListener(new MouseAdapter() {
    			@Override
    			public void mouseClicked(MouseEvent e) {
    				
    				JTable table = (JTable) e.getSource();
    				
    				// double click
    				if (e.getClickCount() == 2) {
    					int row = table.rowAtPoint((e.getPoint()));

    					String value = table.getModel().getValueAt(row+1, 0).toString();
    					if(value.equals("Signal group")){
    						String groupString = table.getModel().getValueAt(row+1, 1).toString();
    						int signalGroup = Integer.valueOf(groupString);
    						
    						Color oldColour = ColourSelecter.getSignalColour( signalGroup-1 );
    						
    						Color newColor = JColorChooser.showDialog(
    			                     SignalsDetailPanel.this,
    			                     "Choose signal Color",
    			                     oldColour);
    						
    						if(newColor != null){
    							activeDataset.setSignalGroupColour(signalGroup, newColor);
    							SignalsDetailPanel.this.update(list);
    							fireSignalChangeEvent("SignalColourUpdate");
    						}
    					}
    						
    				}

    			}
    		});
    		
    		JScrollPane scrollPane = new JScrollPane(statsTable);
    		return scrollPane;
    	}
    	
    	private JPanel createConsensusPanel(){
    		
    		final JPanel panel = new JPanel(new BorderLayout());
    		// make a blank chart for signal locations on a consensus nucleus
    		JFreeChart signalsChart = ChartFactory.createXYLineChart(null,  // chart for conseusns
    				null, null, null);
    		XYPlot signalsPlot = signalsChart.getXYPlot();

    		signalsPlot.setBackgroundPaint(Color.WHITE);
    		signalsPlot.getDomainAxis().setVisible(false);
    		signalsPlot.getRangeAxis().setVisible(false);
    				
    		// the chart is inside a chartPanel; the chartPanel is inside a JPanel
    		// this allows a checkbox panel to be added to the JPanel later
    		chartPanel = new ChartPanel(signalsChart);
    		panel.add(chartPanel, BorderLayout.CENTER);
    		
    		chartPanel.addComponentListener(new ComponentAdapter() {
    			@Override
    			public void componentResized(ComponentEvent e) {
    				resizePreview(chartPanel, panel);
    			}
    		});
    		
    		panel.addComponentListener(new ComponentAdapter() {
    			@Override
    			public void componentResized(ComponentEvent e) {
    				resizePreview(chartPanel, panel);
    			}
    		});
    		
    		
    		checkboxPanel = createSignalCheckboxPanel(null);
    		
    		panel.add(checkboxPanel, BorderLayout.NORTH);

    		return panel;
    	}
    	
    	/**
    	 * Create the checkboxes that set each signal channel visible or not
    	 */
    	private JPanel createSignalCheckboxPanel(AnalysisDataset d){
    		JPanel panel = new JPanel();
    		
    		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    		
    		if(d!=null){
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
    					box.addActionListener(SignalsDetailPanel.this);
    					panel.add(box);

    				}

    			} catch(Exception e){
    				error("Error creating signal checkboxes", e);
    			}
    		}
    		return panel;
    	}
    	
    	protected void update(List<AnalysisDataset> list){
    		updateCheckboxPanel(list);
    		updateSignalConsensusChart(list);
    		updateSignalStatsPanel(list);
    	}
    	
    	/**
    	 * Update the signal stats with the given datasets
    	 * @param list the datasets
    	 */
    	private void updateSignalStatsPanel(List<AnalysisDataset> list){
    		try{
    			TableModel model = NucleusDatasetCreator.createSignalStatsTable(list);
    			statsTable.setModel(model);
    		} catch (Exception e){
    			log("Error updating signal stats: "+e.getMessage());
    		}
    		int columns = statsTable.getColumnModel().getColumnCount();
    		for(int i=1;i<columns;i++){
    			statsTable.getColumnModel().getColumn(i).setCellRenderer(new StatsTableCellRenderer());
    		}
    	}
    	
    	private void updateCheckboxPanel(List<AnalysisDataset> list){
    		if(list.size()==1){
								
				// make a new panel for the active dataset
				consensusAndCheckboxPanel.remove(checkboxPanel);
				checkboxPanel = createSignalCheckboxPanel(activeDataset);
	
				// add this new panel
				consensusAndCheckboxPanel.add(checkboxPanel, BorderLayout.NORTH);
				consensusAndCheckboxPanel.revalidate();
				consensusAndCheckboxPanel.repaint();
				consensusAndCheckboxPanel.setVisible(true);
    		}
    	}
    	
    	
    	private void updateSignalConsensusChart(List<AnalysisDataset> list){
    		try {

    			if(list.size()==1){
    								
    				CellCollection collection = activeDataset.getCollection();

    				if(collection.hasConsensusNucleus()){ // if a refold is available
    					
    					XYDataset signalCoMs = NucleusDatasetCreator.createSignalCoMDataset(activeDataset);
    					JFreeChart chart = MorphologyChartFactory.makeSignalCoMNucleusOutlineChart(activeDataset, signalCoMs);
    					chartPanel.setChart(chart);
    				} else { // no consensus to display
    							
    					JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
    					chartPanel.setChart(chart);
    				}
    			} else { // multiple populations
    				
    				JFreeChart chart = ConsensusNucleusChartFactory.makeEmptyNucleusOutlineChart();
					chartPanel.setChart(chart);
					
//    				consensusAndCheckboxPanel.setVisible(false);
    			}
    		} catch(Exception e){
    			error("Error updating signals", e);
    		}
    	}
    }
    
    protected class HistogramPanel extends JPanel{
    	
    	private static final long serialVersionUID = 1L;
    	
    	private ChartPanel 	angleChartPanel; 		// 
    	private ChartPanel 	distanceChartPanel; 	// 
  	    	
    	protected HistogramPanel(){
    		
    		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    		JFreeChart signalAngleChart 	= HistogramChartFactory.createSignalAngleHistogram(null, null);
    		JFreeChart signalDistanceChart 	= HistogramChartFactory.createSignalDistanceHistogram(null, null);
    		    		
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
    			error("Error updating signal histograms", e);
    		}
    	}
    	
    	private void updateSignalAngleHistogram(List<AnalysisDataset> list){
    		
    		JFreeChart chart = HistogramChartFactory.createSignalAngleHistogram(null, activeDataset);
    		try {
    			HistogramDataset ds = NucleusDatasetCreator.createSignalAngleHistogramDataset(list);

    			if(ds.getSeriesCount()>0){
    				chart = HistogramChartFactory.createSignalAngleHistogram(ds, activeDataset);
    			}

    		} catch (Exception e) {
    			error("Error updating angle histograms", e);
    		}
    		angleChartPanel.setChart(chart);
    	}

    	private void updateSignalDistanceHistogram(List<AnalysisDataset> list){
    		JFreeChart chart = HistogramChartFactory.createSignalDistanceHistogram(null, activeDataset);
    		try {
    			HistogramDataset ds = NucleusDatasetCreator.createSignalDistanceHistogramDataset(list);

    			if(ds.getSeriesCount()>0){
    				chart = HistogramChartFactory.createSignalDistanceHistogram(ds, activeDataset);
    			}

    		} catch (Exception e) {
    			error("Error updating distance histograms", e);
    		}
    		distanceChartPanel.setChart(chart);
    	}
    }

    protected class AnalysisPanel extends JPanel{

    	private static final long serialVersionUID = 1L;

    	private JTable 		table;			// table for analysis parameters
    	private JScrollPane scrollPane;


    	protected AnalysisPanel(){

    		this.setLayout(new BorderLayout());

    		table  = new JTable(new DefaultTableModel());
    		table.setEnabled(false);
    		scrollPane = new JScrollPane(table);
    		this.add(scrollPane, BorderLayout.CENTER);
    	}
    	
    	/**
    	 * Update the signal analysis detection settings with the given datasets
    	 * @param list the datasets
    	 */
    	protected void update(List<AnalysisDataset> list){
    		
    		TableModel model = NucleusDatasetCreator.createSignalDetectionParametersTable(null);
    		if(list!=null && !list.isEmpty()){
    			try{
    				model = NucleusDatasetCreator.createSignalDetectionParametersTable(list);
    			} catch (Exception e){
    				error("Error updating signal analysis", e);
    			}
    		}
    		table.setModel(model);
    	}

    }
    
    protected class BoxplotPanel extends JPanel{

    	private static final long serialVersionUID = 1L;

    	private ChartPanel 	chartPanel;


    	protected BoxplotPanel(){

    		this.setLayout(new BorderLayout());
    		
    		JFreeChart areaBoxplot = MorphologyChartFactory.makeEmptyBoxplot();
			chartPanel = new ChartPanel(areaBoxplot);
			this.add(chartPanel);
    	}
    	
    	/**
    	 * Update the boxplot panel for areas with a list of NucleusCollections
    	 * @param list
    	 */
    	protected void update(List<AnalysisDataset> list){
    		if(list.size()==1){
    			BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSignalAreaBoxplotDataset(list.get(0));
    			JFreeChart boxplotChart = MorphologyChartFactory.makeSignalAreaBoxplot(ds, list.get(0));
    			chartPanel.setChart(boxplotChart);
    		} else {
    			JFreeChart areaBoxplot = MorphologyChartFactory.makeEmptyBoxplot();
    			chartPanel.setChart(areaBoxplot);
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

    		if(list.size()==1){ // single collection is easy
    			
    			AnalysisDataset dataset = list.get(0);
    			CellCollection collection = dataset.getCollection();

    			if(dataset.hasShellResult()){ // only if there is something to display

    				CategoryDataset ds = NucleusDatasetCreator.createShellBarChartDataset(list);
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
    					Color colour = dataset.getSignalGroupColour(index);
    					rend.setSeriesPaint(j, colour);
    				}	

    				chartPanel.setChart(shellsChart);
    				chartPanel.setVisible(true);
    				
    				statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
    				String label = "";
    				for(int i=1; i<=dataset.getHighestSignalGroup();i++){
    					ShellResult r = dataset.getShellResult(i);
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
