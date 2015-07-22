package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Iterator;
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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.category.StatisticalBarRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;

import datasets.NucleusDatasetCreator;

public class SignalsDetailPanel extends JPanel implements ActionListener, SignalChangeListener {

	private static final long serialVersionUID = 1L;
	
	public static final String SOURCE_COMPONENT = "SignalsDetailPanel"; 
	
	private ChartPanel shellsChartPanel; 
	private JPanel shellsPanel;
	
	private ChartPanel signalsChartPanel; // consensus nucleus plus signals
	private JPanel signalsPanel;// signals container for chart and stats table
	private JTable signalStatsTable;
	private JPanel signalAnalysisSetupPanel;
	private JTable signalAnalysisSetupTable;
	private JPanel signalConsensusAndCheckboxPanel;
	private JPanel boxplotsPanel;
	private JPanel checkboxPanel;
	private ChartPanel areaBoxplotChartPanel;
	private JTabbedPane signalsTabPane;
	
	private static final int TAB_OVERVIEW 	= 0;
	private static final int TAB_HISTOGRAMS = 1;
	private static final int TAB_SHELLS 	= 2;
	private static final int TAB_SETTINGS 	= 3;
	private static final int TAB_BOXPLOTS 	= 4;
	
	
	private ChartPanel 	signalAngleChartPanel; 		// consensus nucleus plus signals
	private ChartPanel 	signalDistanceChartPanel; 	// consensus nucleus plus signals
	private JPanel 		signalHistogramPanel;		// signals container for chart and stats table
	
	private List<AnalysisDataset> list;
	private AnalysisDataset activeDataset;
	
	private List<Object> listeners = new ArrayList<Object>();

	/**
	 * Create the panel.
	 */
	public SignalsDetailPanel() {
		
		try{

//			IJ.log("Buildig signals panel");
			this.setLayout(new BorderLayout());

			signalsTabPane = new JTabbedPane(JTabbedPane.TOP);

			signalsPanel = createOverviewPanel();
			signalsTabPane.addTab("Overview", null, signalsPanel, null);
//			IJ.log("Made overview");
			//---------------
			// Distance and angle histograms charts
			//---------------
			signalHistogramPanel = createHistogramsPanel(); // main container in tab
			signalsTabPane.addTab("Signal histograms", null, signalHistogramPanel, null);
//			IJ.log("Made signals");
			//---------------
			// Create the shells panel
			//---------------
			JFreeChart shellsChart = ChartFactory.createBarChart(null, "Shell", "Percent", null);
			shellsChart.getCategoryPlot().setBackgroundPaint(Color.WHITE);
			shellsChart.getCategoryPlot().getRangeAxis().setRange(0,100);
			shellsChartPanel = new ChartPanel(shellsChart);

			signalsTabPane.addTab("Shells", null, shellsChartPanel, null);
//			IJ.log("Made shells");

			//---------------
			// Create the signal analysis settings panel
			//---------------
			signalAnalysisSetupPanel = new JPanel(new BorderLayout());

			signalAnalysisSetupTable  = new JTable(new DefaultTableModel());
			signalAnalysisSetupTable.setEnabled(false);
			JScrollPane signalAnalysisSetupScrollPane = new JScrollPane(signalAnalysisSetupTable);
			signalAnalysisSetupPanel.add(signalAnalysisSetupScrollPane, BorderLayout.CENTER);
			signalsTabPane.addTab("Detection settings", null, signalAnalysisSetupPanel, null);

			//---------------
			// Create the signal boxplots panel
			//---------------
			boxplotsPanel = new JPanel(new BorderLayout());

			JFreeChart areaBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			formatBoxplotChart(areaBoxplot);
			areaBoxplotChartPanel = new ChartPanel(areaBoxplot);
			boxplotsPanel.add(areaBoxplotChartPanel);
			signalsTabPane.addTab("Boxplots", null, boxplotsPanel, null);

			this.add(signalsTabPane, BorderLayout.CENTER);
			
		} catch (Exception e){
			IJ.log("Error making signal panel: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				IJ.log(e1.toString());
			}
		}
	}
	
	private JPanel createOverviewPanel(){
		JPanel panel = new JPanel(); // main container in tab
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

		//---------------
		// Stats panel
		//---------------
		DefaultTableModel signalsTableModel = new DefaultTableModel();
		signalsTableModel.addColumn("");
		signalsTableModel.addColumn("");
		signalStatsTable = new JTable(); // table  for basic stats
		signalStatsTable.setModel(signalsTableModel);
		signalStatsTable.setEnabled(false);
		
		signalStatsTable.addMouseListener(new MouseAdapter() {
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
							update(list);
							fireSignalChangeEvent("SignalColourUpdate");
						}
					}
						
				}

			}
		});
		
		JScrollPane signalStatsScrollPane = new JScrollPane(signalStatsTable);
		panel.add(signalStatsScrollPane);
		
		//---------------
		// Consensus chart
		//---------------
		signalConsensusAndCheckboxPanel = createConsensusPanel();
		panel.add(signalConsensusAndCheckboxPanel);
		return panel;
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
		signalsChartPanel = new ChartPanel(signalsChart);
		panel.add(signalsChartPanel, BorderLayout.CENTER);
		
//		signalsChartPanel.addComponentListener(new ComponentAdapter() {
//			@Override
//			public void componentResized(ComponentEvent e) {
//				resizePreview(signalsChartPanel, panel);
//			}
//		});
//		
//		this.addComponentListener(new ComponentAdapter() {
//			@Override
//			public void componentResized(ComponentEvent e) {
//				resizePreview(signalsChartPanel, panel);
//			}
//		});
		checkboxPanel = createSignalCheckboxPanel(null);
		
		panel.add(checkboxPanel, BorderLayout.NORTH);

		return panel;
	}
	
	private JPanel createHistogramsPanel(){
		JFreeChart signalAngleChart = ChartFactory.createHistogram(null, "Angle", "Count", null, PlotOrientation.VERTICAL, true, true, true);
		signalAngleChart.getPlot().setBackgroundPaint(Color.white);
		
		JFreeChart signalDistanceChart = ChartFactory.createHistogram(null, "Distance", "Count", null, PlotOrientation.VERTICAL, true, true, true);
		signalDistanceChart.getPlot().setBackgroundPaint(Color.white);
		
		JPanel panel = new JPanel(); // main container in tab
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		signalAngleChartPanel = new ChartPanel(signalAngleChart);
		signalDistanceChartPanel = new ChartPanel(signalDistanceChart);

		panel.add(signalAngleChartPanel);
		panel.add(signalDistanceChartPanel);
		return panel;
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
		updateShellPanel(list);
		updateSignalConsensusChart(list);
		updateSignalStatsPanel(list);
		updateSignalHistogramPanel(list);
		updateSignalAnalysisSetupPanel(list);
		updateAreaBoxplot(list);
	}
	
	/**
	 * Update the boxplot panel for areas with a list of NucleusCollections
	 * @param list
	 */
	private void updateAreaBoxplot(List<AnalysisDataset> list){
		if(list.size()==1){
			BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSignalAreaBoxplotDataset(list.get(0));
			JFreeChart boxplotChart = ChartFactory.createBoxAndWhiskerChart(null, null, null, ds, false); 
			formatBoxplotChart(boxplotChart, list);
			areaBoxplotChartPanel.setChart(boxplotChart);
		} else {
			JFreeChart areaBoxplot = ChartFactory.createBoxAndWhiskerChart(null, null, null, new DefaultBoxAndWhiskerCategoryDataset(), false);	        
			formatBoxplotChart(areaBoxplot);
			areaBoxplotChartPanel.setChart(areaBoxplot);
		}
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
			fireSignalChangeEvent("Log_"+"Error updating signal stats: "+e.getMessage());
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
			fireSignalChangeEvent("Log_"+"Error updating signal analysis: "+e.getMessage());
			for(StackTraceElement e1 : e.getStackTrace()){
				fireSignalChangeEvent("Log_"+e1.toString());
			}
		}
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
					box.addActionListener(this);
					panel.add(box);

				}

			} catch(Exception e){
				IJ.log("Error creating signal checkboxes: "+e.getMessage());
				for(StackTraceElement e1 : e.getStackTrace()){
					IJ.log(e1.toString());
				}
			}
		}
		return panel;
	}
	
	/**
	 * Create a consenusus chart for the given nucleus collection
	 * @param collection the NucleusCollection to draw the consensus from
	 * @return the consensus chart
	 */
	public JFreeChart makeConsensusChart(AnalysisDataset dataset){
		CellCollection collection = dataset.getCollection();
		XYDataset ds = NucleusDatasetCreator.createBareNucleusOutline(dataset);
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
			Shape circle = new Ellipse2D.Double(0, 0, 2, 2);
			plot.getRenderer().setSeriesShape(i, circle);
			plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
			plot.getRenderer().setSeriesPaint(i, Color.BLACK);
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
					Color colour = activeDataset.getSignalGroupColour(seriesGroup);
					plot.getRenderer().setSeriesPaint(j, ColourSelecter.getTransparentColour(colour, true, 128));
//					plot.getRenderer().setSeriesPaint(j, ColourSelecter.getSignalColour(seriesGroup-1, true, 128));
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
					Color colour = activeDataset.getSignalGroupColour(index);
					plot.getRenderer().setSeriesPaint(j, ColourSelecter.getTransparentColour(colour, true, 128));
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
				this.activeDataset = list.get(0);
				
				// get rid of any checkboxes present
//				signalConsensusAndCheckboxPanel.remove(checkboxPanel);
				
				// make a new panel for the active dataset
				checkboxPanel = createSignalCheckboxPanel(activeDataset);
				
				// add this new panel
				signalConsensusAndCheckboxPanel.add(checkboxPanel, BorderLayout.NORTH);
				signalConsensusAndCheckboxPanel.revalidate();
				signalConsensusAndCheckboxPanel.repaint();
				signalConsensusAndCheckboxPanel.setVisible(true);

				CellCollection collection = list.get(0).getCollection();

				if(collection.hasConsensusNucleus()){ // if a refold is available
					
					XYDataset signalCoMs = NucleusDatasetCreator.createSignalCoMDataset(activeDataset);
					JFreeChart chart = makeConsensusChart(activeDataset);

					XYPlot plot = chart.getXYPlot();
					plot.setDataset(1, signalCoMs);

					XYLineAndShapeRenderer  rend = new XYLineAndShapeRenderer();
					for(int series=0;series<signalCoMs.getSeriesCount();series++){
						
						Shape circle = new Ellipse2D.Double(0, 0, 4, 4);
						rend.setSeriesShape(series, circle);
						
						String name = (String) signalCoMs.getSeriesKey(series);
						int seriesGroup = getIndexFromLabel(name);
						Color colour = activeDataset.getSignalGroupColour(seriesGroup);
						rend.setSeriesPaint(series, colour);
//						rend.setSeriesPaint(series, ColourSelecter.getSignalColour(seriesGroup-1, false));
						rend.setBaseLinesVisible(false);
						rend.setBaseShapesVisible(true);
						rend.setBaseSeriesVisibleInLegend(false);
					}
					plot.setRenderer(1, rend);

					for(int signalGroup : collection.getSignalGroups()){
						List<Shape> shapes = NucleusDatasetCreator.createSignalRadiusDataset(activeDataset, signalGroup);

						int signalCount = shapes.size();

						int alpha = (int) Math.floor( 255 / ((double) signalCount) )+20;
						alpha = alpha < 10 ? 10 : alpha > 156 ? 156 : alpha;
						
						Color colour = activeDataset.getSignalGroupColour(signalGroup);

						for(Shape s : shapes){
							XYShapeAnnotation an = new XYShapeAnnotation( s, null,
									null, ColourSelecter.getTransparentColour(colour, true, alpha)); // layer transparent signals
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
		
		JButton button = new JButton();
		button.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				fireSignalChangeEvent("RunShellAnalysis");
			}
		});
		
		if(showRunButton){
			panel.add(button, BorderLayout.SOUTH);
		}
		
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
	
	/**
	 * Apply the default formatting to a boxplot with list
	 * @param boxplot
	 */
	private void formatBoxplotChart(JFreeChart boxplot, List<AnalysisDataset> list){
		formatBoxplotChart(boxplot);
		CategoryPlot plot = boxplot.getCategoryPlot();
		BoxAndWhiskerRenderer renderer = (BoxAndWhiskerRenderer) plot.getRenderer();

		CategoryDataset ds = plot.getDataset(0);

		for(int series=0;series<ds.getRowCount();series++){
			String name = (String) ds.getRowKey(series);
			int seriesGroup = getIndexFromLabel(name);

			Color color = activeDataset.getSignalGroupColour(seriesGroup) == null 
					? ColourSelecter.getSegmentColor(series)
							: activeDataset.getSignalGroupColour(seriesGroup);

					renderer.setSeriesPaint(series, color);


		}

		renderer.setMeanVisible(false);
	}
	
	/**
	 * Apply basic formatting to the charts, without any series added
	 * @param boxplot
	 */
	private void formatBoxplotChart(JFreeChart boxplot){
		CategoryPlot plot = boxplot.getCategoryPlot();
		plot.setBackgroundPaint(Color.WHITE);
		BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
		plot.setRenderer(renderer);
		renderer.setUseOutlinePaintForWhiskers(true);   
		renderer.setBaseOutlinePaint(Color.BLACK);
		renderer.setBaseFillPaint(Color.LIGHT_GRAY);
	}


	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		if(event.type().equals("SignalColourUpdate")){
			update(list);
		}
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
     
    private synchronized void fireSignalChangeEvent(String message) {
        SignalChangeEvent event = new SignalChangeEvent( this, message, SOURCE_COMPONENT );
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }
    

}
