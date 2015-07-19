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
import javax.swing.JCheckBox;
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
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import datasets.NucleusDatasetCreator;

public class NucleusProfilesPanel extends JPanel implements ActionListener {


	private static final long serialVersionUID = 1L;
	
//	private ChartPanel profileChartPanel;
	private ChartPanel frankenChartPanel;
	private ChartPanel profilesPanel;
	private ChartPanel variabilityChartPanel; 
	
	private JRadioButton rawProfileLeftButton  = new JRadioButton("Left"); // left align raw profiles in rawChartPanel
	private JRadioButton rawProfileRightButton = new JRadioButton("Right"); // right align raw profiles in rawChartPan
	private JCheckBox    normCheckBox 	= new JCheckBox("Normalised");	// to toggle raw or normalised segment profiles in segmentsProfileChartPanel
	
	private List<AnalysisDataset> list;


	public NucleusProfilesPanel() {
		
		this.setLayout(new BorderLayout());
		JTabbedPane profilesTabPanel = new JTabbedPane(JTabbedPane.TOP);
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		
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
		profilesPanel = new ChartPanel(rawChart);
		profilesPanel.setMinimumDrawWidth( 0 );
		profilesPanel.setMinimumDrawHeight( 0 );
		rawPanel.setMinimumSize(minimumChartSize);
		rawPanel.setPreferredSize(preferredChartSize);
		rawPanel.add(profilesPanel, BorderLayout.CENTER);
		
		rawProfileLeftButton.setSelected(true);
		
		rawProfileLeftButton.setActionCommand("LeftAlignRawProfile");
		rawProfileRightButton.setActionCommand("RightAlignRawProfile");
		
		rawProfileLeftButton.addActionListener(this);
		rawProfileRightButton.addActionListener(this);
		
		rawProfileLeftButton.setEnabled(false);
		rawProfileRightButton.setEnabled(false);
		
		// checkbox to select raw or normalised profiles
		normCheckBox.setSelected(true);
		normCheckBox.setEnabled(false);
		normCheckBox.setActionCommand("NormalisedProfile");
		normCheckBox.addActionListener(this);
		

		//Group the radio buttons.
		final ButtonGroup alignGroup = new ButtonGroup();
		alignGroup.add(rawProfileLeftButton);
		alignGroup.add(rawProfileRightButton);
		
		JPanel alignPanel = new JPanel();
		alignPanel.setLayout(new BoxLayout(alignPanel, BoxLayout.X_AXIS));

		alignPanel.add(normCheckBox);
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
//		profilesTabPanel.addTab("Normalised", null, profileChartPanel, null);
		profilesTabPanel.addTab("Profiles", null, rawPanel, null);
		profilesTabPanel.addTab("FrankenProfile", null, frankenChartPanel, null);
		profilesTabPanel.addTab("Variability", null, variabilityChartPanel, null);
		this.add(profilesTabPanel, BorderLayout.CENTER);

	}
	
	public void update(List<AnalysisDataset> list){
		
		this.list = list;
		
		if(!list.isEmpty()){
			normCheckBox.setEnabled(true);
			if(  normCheckBox.isSelected()){
				updateProfiles(list, true, false);
			} else {			
				if(  rawProfileLeftButton.isSelected()){
					updateProfiles(list, false, false);
				} else {
					updateProfiles(list, false, true);
				}
			}

			updateFrankenProfileChart(list);
			updateVariabilityChart(list);
		} else {
			// if the list is empty, do not enable controls
			normCheckBox.setEnabled(false);
			rawProfileLeftButton.setEnabled(false);
			rawProfileRightButton.setEnabled(false);
		}
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
	 * @param normalised flag for raw or normalised lengths
	 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
	 */	
	private void updateProfiles(List<AnalysisDataset> list, boolean normalised, boolean rightAlign){

		try {
			if(list.size()==1){
				
				XYDataset ds = NucleusDatasetCreator.createSegmentedProfileDataset(list.get(0).getCollection(), normalised);
				
				// full segment colouring
				JFreeChart chart = makeProfileChart(ds);
				XYPlot plot = chart.getXYPlot();
				
				// length of chart depends on whether the profile is normalised
				if(normalised){
					plot.getDomainAxis().setRange(0,100);
				} else {
					plot.getDomainAxis().setRange(0,list.get(0).getCollection().getMedianArrayLength());
				}
				profilesPanel.setChart(chart);
				
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRDataset(list, normalised, rightAlign);				
				XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileDataset(	  list, normalised, rightAlign);
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
						                false);
				
				// find the maximum profile length - used when rendering raw profiles
				int length = 100;

				if(!normalised){
					for(AnalysisDataset d : list){
						length = (int) Math.max( d.getCollection().getMedianArrayLength(), length);
					}
				}
				
				formatMultiProfileChart(chart, iqrProfiles, medianProfiles, length);				
				profilesPanel.setChart(chart);
			}
			
		} catch (Exception e) {
			IJ.log("Error in plotting profile");
		} 
	}
	
	private void formatMultiProfileChart(JFreeChart chart, List<XYSeriesCollection> iqrProfiles, XYDataset medianProfiles, int length){

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
			Color profileColour = list.get(index).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(i)
					: list.get(index).getDatasetColour();

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
			
			Color profileColour = list.get(index).getDatasetColour() == null 
					? ColourSelecter.getSegmentColor(j)
					: list.get(index).getDatasetColour();
					
			medianRenderer.setSeriesPaint(j, profileColour.darker());
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
				List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRFrankenDataset( list);				
				XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileFrankenDataset(	  list);
				
				JFreeChart chart = 
						ChartFactory.createXYLineChart(null,
						                "Position", "Angle", null, PlotOrientation.VERTICAL, true, true,
						                false);
				
				formatMultiProfileChart(chart, iqrProfiles, medianProfiles, 100);
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
					Color profileColour = list.get(index).getDatasetColour() == null 
							? ColourSelecter.getSegmentColor(index)
							: list.get(index).getDatasetColour();
					
//					Color profileColour = list.get(index).getDatasetColour();
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
			updateProfiles(list, false, false);
//			updateRawProfileImage(list, false);
		}
		
		if(e.getActionCommand().equals("RightAlignRawProfile")){
//			updateRawProfileImage(list, true);
			updateProfiles(list, false, true);
		}
		
		if(e.getActionCommand().equals("NormalisedProfile")){

			if(  normCheckBox.isSelected()){
				rawProfileLeftButton.setEnabled(false);
				rawProfileRightButton.setEnabled(false);
				updateProfiles(list, true, false);
			} else {
				rawProfileLeftButton.setEnabled(true);
				rawProfileRightButton.setEnabled(true);
				
				if(  rawProfileLeftButton.isSelected()){
					updateProfiles(list, false, false);
				} else {
					updateProfiles(list, false, true);
				}
			}
			
		}
		
	}

}
