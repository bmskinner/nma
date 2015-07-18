package no.gui;

import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;

import no.analysis.AnalysisDataset;
import no.collections.CellCollection;
import no.components.Profile;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import datasets.NucleusDatasetCreator;

public class NucleusProfilesPanel extends JPanel implements ActionListener {


	private static final long serialVersionUID = 1L;
	
	private ChartPanel profileChartPanel;
	private ChartPanel frankenChartPanel;
	private ChartPanel rawChartPanel;
	private ChartPanel variabilityChartPanel; 
	
	private JRadioButton rawProfileLeftButton  = new JRadioButton("Left"); // left align raw profiles in rawChartPanel
	private JRadioButton rawProfileRightButton = new JRadioButton("Right"); // right align raw profiles in rawChartPan
	
	private List<AnalysisDataset> list;


	public NucleusProfilesPanel() {
		
		this.setLayout(new BorderLayout());
		JTabbedPane profilesTabPanel = new JTabbedPane(JTabbedPane.TOP);
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		//---------------
		// Create the regular profile chart
		//---------------
		JFreeChart profileChart = ChartFactory.createXYLineChart(null,
	            "Position", "Angle", null);
		XYPlot plot = profileChart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		profileChartPanel = new ChartPanel(profileChart);
		profileChartPanel.setMinimumSize(minimumChartSize);
		profileChartPanel.setPreferredSize(preferredChartSize);
		profileChartPanel.setMinimumDrawWidth( 0 );
		profileChartPanel.setMinimumDrawHeight( 0 );
		
		//---------------
		// Create the franken profile chart
		//---------------
		JFreeChart frankenChart = ChartFactory.createXYLineChart(null,
				"Position", "Angle", null);
		XYPlot frankenPlot = frankenChart.getXYPlot();
		frankenPlot.getDomainAxis().setRange(0,100);
		frankenPlot.getRangeAxis().setRange(0,360);
		frankenPlot.setBackgroundPaint(Color.WHITE);
		frankenChartPanel = new ChartPanel(frankenChart);
		frankenChartPanel.setMinimumSize(minimumChartSize);
		frankenChartPanel.setPreferredSize(preferredChartSize);
		frankenChartPanel.setMinimumDrawWidth( 0 );
		frankenChartPanel.setMinimumDrawHeight( 0 );
		
		
		//---------------
		// Create the raw profile chart
		//---------------
		JPanel rawPanel = new JPanel();
		rawPanel.setLayout(new BorderLayout());
		JFreeChart rawChart = ChartFactory.createXYLineChart(null,
				"Position", "Angle", null);
		XYPlot rawPlot = rawChart.getXYPlot();
		rawPlot.getDomainAxis().setRange(0,100);
		rawPlot.getRangeAxis().setRange(0,360);
		rawPlot.setBackgroundPaint(Color.WHITE);
		rawChartPanel = new ChartPanel(rawChart);
		rawChartPanel.setMinimumDrawWidth( 0 );
		rawChartPanel.setMinimumDrawHeight( 0 );
		rawPanel.setMinimumSize(minimumChartSize);
		rawPanel.setPreferredSize(preferredChartSize);
		rawPanel.add(rawChartPanel, BorderLayout.CENTER);
		
		rawProfileLeftButton.setSelected(true);
		
		rawProfileLeftButton.setActionCommand("LeftAlignRawProfile");
		rawProfileRightButton.setActionCommand("RightAlignRawProfile");
		rawProfileLeftButton.addActionListener(this);
		rawProfileRightButton.addActionListener(this);
		

		//Group the radio buttons.
		final ButtonGroup alignGroup = new ButtonGroup();
		alignGroup.add(rawProfileLeftButton);
		alignGroup.add(rawProfileRightButton);
		
		JPanel alignPanel = new JPanel();
		alignPanel.setLayout(new BoxLayout(alignPanel, BoxLayout.X_AXIS));

		
		alignPanel.add(rawProfileLeftButton);
		alignPanel.add(rawProfileRightButton);
		rawPanel.add(alignPanel, BorderLayout.NORTH);
		
		//---------------
		// Create the variability chart
		//---------------
		JFreeChart variablityChart = ChartFactory.createXYLineChart(null,
				"Position", "IQR", null);
		XYPlot variabilityPlot = variablityChart.getXYPlot();
		variabilityPlot.setBackgroundPaint(Color.WHITE);
		variabilityPlot.getDomainAxis().setRange(0,100);
		variabilityChartPanel = new ChartPanel(variablityChart);
		
		//---------------
		// Add to the tabbed panel
		//---------------
		profilesTabPanel.addTab("Normalised", null, profileChartPanel, null);
		profilesTabPanel.addTab("Raw", null, rawPanel, null);
		profilesTabPanel.addTab("FrankenProfile", null, frankenChartPanel, null);
		profilesTabPanel.addTab("Variability", null, variabilityChartPanel, null);
		this.add(profilesTabPanel, BorderLayout.CENTER);

	}
	
	public void update(List<AnalysisDataset> list){
		this.list = list;
		updateProfileImage(list);
		
		if(rawProfileRightButton.isSelected()){
			updateRawProfileImage(list, true);
		} else {
			updateRawProfileImage(list, false);
		}
		
		updateFrankenProfileChart(list);
		updateVariabilityChart(list);
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
	
	private JFreeChart makeProfileChart(XYDataset ds){
		JFreeChart chart = 
				ChartFactory.createXYLineChart(null,
				                "Position", "Angle", ds, PlotOrientation.VERTICAL, true, true,
				                false);
		
		
		XYPlot plot = chart.getXYPlot();
		plot.getDomainAxis().setRange(0,100);
		plot.getRangeAxis().setRange(0,360);
		plot.setBackgroundPaint(Color.WHITE);
		plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));

		int seriesCount = plot.getSeriesCount();

		for (int i = 0; i < seriesCount; i++) {
			plot.getRenderer().setSeriesVisibleInLegend(i, Boolean.FALSE);
			String name = (String) ds.getSeriesKey(i);
			if(name.startsWith("Seg_")){
				int colourIndex = getIndexFromLabel(name);
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(3));
				plot.getRenderer().setSeriesPaint(i, ColourSelecter.getSegmentColor(colourIndex));
			} 
			if(name.startsWith("Nucleus_")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(1));
				plot.getRenderer().setSeriesPaint(i, Color.LIGHT_GRAY);
			} 
			if(name.startsWith("Q")){
				plot.getRenderer().setSeriesStroke(i, new BasicStroke(2));
				plot.getRenderer().setSeriesPaint(i, Color.DARK_GRAY);
			} 
			
		}	
		return chart;
	}
	
	/**
	 * Update the profile panel with data from the given datasets
	 * @param list the datasets
	 */	
	private void updateProfileImage(List<AnalysisDataset> list){
		
		try {
			if(list.size()==1){

				// full segment colouring
				XYDataset ds = NucleusDatasetCreator.createSegmentedProfileDataset(list.get(0).getCollection(), true);
				JFreeChart chart = makeProfileChart(ds);
				profileChartPanel.setChart(chart);
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> ds = NucleusDatasetCreator.createMultiProfileIQRDataset(list, true, false);				
				
				XYDataset profileDS = NucleusDatasetCreator.createMultiProfileDataset(list, true, false);
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));
				
				int i=0;
				for(XYSeriesCollection c : ds){

					// find the series index
					String name = (String) c.getSeriesKey(0);
					String[] names = name.split("_");
					int index = Integer.parseInt(names[1]);
					
					// add to dataset
					plot.setDataset(i, c);
					
					// make a transparent color based on teh profile segmenter system
					Color profileColour = list.get(index).getDatasetColour();
					Color iqrColour		= ColourSelecter.getTransparentColour(profileColour, true, 128);
					
					XYDifferenceRenderer xydr = new XYDifferenceRenderer(iqrColour, iqrColour, false);
					
					// go through each series in the collection, and set the line colour
					for(int series=0;series<c.getSeriesCount();series++){
						xydr.setSeriesPaint(series, iqrColour);
						xydr.setSeriesVisibleInLegend(series, false);
						
					}
					plot.setRenderer(i, xydr);
					
					
					i++;
				}

				plot.setDataset(i, profileDS);
				plot.setRenderer(i, new StandardXYItemRenderer());

				for (int j = 0; j < profileDS.getSeriesCount(); j++) {
					plot.getRenderer(i).setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer(i).setSeriesStroke(j, new BasicStroke(2));
					String name = (String) profileDS.getSeriesKey(j);
					
					int index = getIndexFromLabel(name); 
					Color profileColour = list.get(index).getDatasetColour();
					
					plot.getRenderer(i).setSeriesPaint(j, profileColour.darker());
				}	
				
				profileChartPanel.setChart(chart);
			}
			
		} catch (Exception e) {
			IJ.log("Error in plotting profile");
		} 
	}
	
	private void updateRawProfileImage(List<AnalysisDataset> list, boolean rightAlign){
		
		try {
			if(list.size()==1){

				// full segment colouring
				XYDataset ds = NucleusDatasetCreator.createSegmentedProfileDataset(list.get(0).getCollection(), false);
				JFreeChart chart = makeProfileChart(ds);
				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,list.get(0).getCollection().getMedianArrayLength());
				rawChartPanel.setChart(chart);
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRDataset(list, false, rightAlign);				
				XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileDataset(	  list, false, rightAlign);
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
						                false);
				
				// find the maximum profile length - used when rendering raw profiles
				int length = 100;
				for(AnalysisDataset d : list){
					length = (int) Math.max( d.getCollection().getMedianArrayLength(), length);

				}
				
				// set basic range and colour
				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,length);
				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				
				// add 180 degree horizontal line
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));
				
				int lastSeries = 0;

				for(int i=0;i<iqrProfiles.size();i++){
					XYSeriesCollection seriesCollection = iqrProfiles.get(i);

					// add to dataset
					plot.setDataset(i, seriesCollection);
					
					
					// find the series index
					String name = (String) seriesCollection.getSeriesKey(0);
					
					// index should be the position in the AnalysisDatase list
					// see construction in NucleusDatasetCreator
					int index = getIndexFromLabel(name); 
					
					// make a transparent color based on teh profile segmenter system
					Color profileColour = list.get(index).getDatasetColour();
					Color iqrColour		= ColourSelecter.getTransparentColour(profileColour, true, 128);
					
					// fill beteween the upper and lower IQR with single colour; do not show shapes
					XYDifferenceRenderer differenceRenderer = new XYDifferenceRenderer(iqrColour, iqrColour, false);
					
					// go through each series in the collection, and set the line colour
					for(int series=0;series<seriesCollection.getSeriesCount();series++){
						differenceRenderer.setSeriesPaint(series, iqrColour);
						differenceRenderer.setSeriesVisibleInLegend(series, false);
						
					}
					plot.setRenderer(i, differenceRenderer);

					lastSeries++; // track the count of series
				}

				plot.setDataset(lastSeries, medianProfiles);
				StandardXYItemRenderer medianRenderer = new StandardXYItemRenderer();
				plot.setRenderer(lastSeries, medianRenderer);

				for (int j = 0; j < medianProfiles.getSeriesCount(); j++) {
					medianRenderer.setSeriesVisibleInLegend(j, Boolean.FALSE);
					medianRenderer.setSeriesStroke(j, new BasicStroke(2));
					String name = (String) medianProfiles.getSeriesKey(j);
					int index = getIndexFromLabel(name); 
					Color profileColour = list.get(index).getDatasetColour();

					medianRenderer.setSeriesPaint(j, profileColour.darker());
				}	
				
				rawChartPanel.setChart(chart);
			}
			
		} catch (Exception e) {
			IJ.log("Error in plotting profile");
		} 
	}
	
	/**
	 * Update the frankenprofile panel with data from the given datasets
	 * @param list the datasets
	 */	
	public void updateFrankenProfileChart(List<AnalysisDataset> list){
		
		try {
			if(list.size()==1){

				// full segment colouring
				XYDataset ds = NucleusDatasetCreator.createFrankenSegmentDataset(list.get(0).getCollection());
				JFreeChart chart = makeProfileChart(ds);
				frankenChartPanel.setChart(chart);
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> ds = NucleusDatasetCreator.createMultiProfileIQRFrankenDataset(list);				
				
				XYDataset profileDS = NucleusDatasetCreator.createMultiProfileFrankenDataset(list);
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setRange(0,360);
				plot.setBackgroundPaint(Color.WHITE);
				plot.addRangeMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2.0f)));
				
				int i=0;
				for(XYSeriesCollection c : ds){

					// find the series index
					String name = (String) c.getSeriesKey(0);
					int index = getIndexFromLabel(name); 
					
					// add to dataset
					plot.setDataset(i, c);
					
					// make a transparent color based on teh profile segmenter system
					Color profileColour = list.get(index).getDatasetColour();
					Color iqrColour		= ColourSelecter.getTransparentColour(profileColour, true, 128);
					
					
					XYDifferenceRenderer xydr = new XYDifferenceRenderer(iqrColour, iqrColour, false);
					
					// go through each series in the collection, and set the line colour
					for(int series=0;series<c.getSeriesCount();series++){
						xydr.setSeriesPaint(series, iqrColour);
						xydr.setSeriesVisibleInLegend(series, false);
						
					}
					plot.setRenderer(i, xydr);
					
					
					i++;
				}

				plot.setDataset(i, profileDS);
				plot.setRenderer(i, new StandardXYItemRenderer());

				for (int j = 0; j < profileDS.getSeriesCount(); j++) {
					plot.getRenderer(i).setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer(i).setSeriesStroke(j, new BasicStroke(2));
					String name = (String) profileDS.getSeriesKey(j);
					int index = getIndexFromLabel(name); 
					Color profileColour = list.get(index).getDatasetColour();
					plot.getRenderer(i).setSeriesPaint(j, profileColour.darker());
				}	
				
				frankenChartPanel.setChart(chart);
			}
						
		} catch (Exception e) {
			IJ.log("Error in plotting frankenprofile: "+e.getMessage());
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		} 
	}
	
	public void updateVariabilityChart(List<AnalysisDataset> list){
		try {
			XYDataset ds = NucleusDatasetCreator.createIQRVariabilityDataset(list);
			if(list.size()==1){
				CellCollection n = list.get(0).getCollection();
				JFreeChart chart = makeProfileChart(ds);
				XYPlot plot = chart.getXYPlot();
				plot.setBackgroundPaint(Color.WHITE);
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setLabel("IQR");
				plot.getRangeAxis().setAutoRange(true);
				List<Integer> maxima = n.getProfileCollection().findMostVariableRegions(n.getOrientationPoint());
				Profile xpoints = n.getProfileCollection().getProfile(n.getOrientationPoint()).getPositions(100);
				for(Integer i : maxima){

					plot.addDomainMarker(new ValueMarker(xpoints.get(i), Color.BLACK, new BasicStroke(1.0f)));
				}

				variabilityChartPanel.setChart(chart);
			} else { // multiple nuclei
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "IQR", ds, PlotOrientation.VERTICAL, true, true,
						                false);

				XYPlot plot = chart.getXYPlot();
				plot.getDomainAxis().setRange(0,100);
				plot.getRangeAxis().setAutoRange(true);
				plot.setBackgroundPaint(Color.WHITE);

				for (int j = 0; j < ds.getSeriesCount(); j++) {
					plot.getRenderer().setSeriesVisibleInLegend(j, Boolean.FALSE);
					plot.getRenderer().setSeriesStroke(j, new BasicStroke(2));
					int index = getIndexFromLabel( (String) ds.getSeriesKey(j));
					Color profileColour = list.get(index).getDatasetColour();
					plot.getRenderer().setSeriesPaint(j, profileColour);
				}	
				variabilityChartPanel.setChart(chart);
			}
		} catch (Exception e) {
			IJ.log("Error drawing variability chart: "+e.getMessage());
		}	
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getActionCommand().equals("LeftAlignRawProfile")){
			updateRawProfileImage(list, false);
		}
		
		if(e.getActionCommand().equals("RightAlignRawProfile")){
			updateRawProfileImage(list, true);
		}
		
	}

}
