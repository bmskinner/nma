package no.gui;

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
import no.components.ProfileCollection;
import no.gui.ColourSelecter.ColourSwatch;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import datasets.MorphologyChartFactory;
import datasets.NucleusDatasetCreator;

public class NucleusProfilesPanel extends DetailPanel implements ActionListener {


	private static final long serialVersionUID = 1L;
	
	private ChartPanel frankenChartPanel;
	private ChartPanel profilesPanel;
	private ChartPanel variabilityChartPanel; 
	
	private ProflleDisplaySettingsPanel proflleDisplaySettingsPanel;
	
	private List<AnalysisDataset> list;
	
	public NucleusProfilesPanel() {
		
		this.setLayout(new BorderLayout());
		JTabbedPane profilesTabPanel = new JTabbedPane(JTabbedPane.TOP);
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		
		//---------------
		// Create the franken profile chart
		//---------------
		JFreeChart frankenChart = MorphologyChartFactory.makeEmptyProfileChart();
		frankenChartPanel  = MorphologyChartFactory.makeProfileChartPanel(frankenChart);
		frankenChartPanel.setMinimumSize(minimumChartSize);
		frankenChartPanel.setPreferredSize(preferredChartSize);
		frankenChartPanel.setMinimumDrawWidth( 0 );
		frankenChartPanel.setMinimumDrawHeight( 0 );
		
		
		//---------------
		// Create the raw profile chart
		//---------------
		JPanel rawPanel = new JPanel();
		rawPanel.setLayout(new BorderLayout());
		JFreeChart rawChart = MorphologyChartFactory.makeEmptyProfileChart();
		profilesPanel = MorphologyChartFactory.makeProfileChartPanel(rawChart);
		
		profilesPanel.setMinimumDrawWidth( 0 );
		profilesPanel.setMinimumDrawHeight( 0 );
		rawPanel.setMinimumSize(minimumChartSize);
		rawPanel.setPreferredSize(preferredChartSize);
		rawPanel.add(profilesPanel, BorderLayout.CENTER);
				
		// add the alignments panel to the tab
		
		proflleDisplaySettingsPanel = new ProflleDisplaySettingsPanel();
		proflleDisplaySettingsPanel.normCheckBox.addActionListener(this);
		proflleDisplaySettingsPanel.rawProfileLeftButton.addActionListener(this);
		proflleDisplaySettingsPanel.rawProfileRightButton.addActionListener(this);
		proflleDisplaySettingsPanel.referenceButton.addActionListener(this);
		proflleDisplaySettingsPanel.orientationButton.addActionListener(this);
		proflleDisplaySettingsPanel.showMarkersCheckBox.addActionListener(this);
		
		rawPanel.add(proflleDisplaySettingsPanel, BorderLayout.NORTH);
		
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
			
			proflleDisplaySettingsPanel.normCheckBox.setEnabled(true);
			proflleDisplaySettingsPanel.referenceButton.setEnabled(true);
			proflleDisplaySettingsPanel.orientationButton.setEnabled(true);
			proflleDisplaySettingsPanel.showMarkersCheckBox.setEnabled(true);
			
			boolean normalised = proflleDisplaySettingsPanel.normCheckBox.isSelected();
			
			// only allow right align if not normalised
			boolean rightAlign = normalised ? false : proflleDisplaySettingsPanel.rawProfileRightButton.isSelected();
			boolean fromReference = proflleDisplaySettingsPanel.referenceButton.isSelected();
			boolean showMarkers = proflleDisplaySettingsPanel.showMarkersCheckBox.isSelected();
			
			updateProfiles(list, normalised, rightAlign, fromReference, showMarkers);
			
			
//			if(  normCheckBox.isSelected()){
//				updateProfiles(list, true, false);
//			} else {			
//				if(  rawProfileLeftButton.isSelected()){
//					updateProfiles(list, false, false);
//				} else {
//					updateProfiles(list, false, true);
//				}
//			}

			updateFrankenProfileChart(list);
			updateVariabilityChart(list);
		} else {
			// if the list is empty, do not enable controls
			proflleDisplaySettingsPanel.setEnabled(false);
//			proflleDisplaySettingsPanel.normCheckBox.setEnabled(false);
//			proflleDisplaySettingsPanel.rawProfileLeftButton.setEnabled(false);
//			proflleDisplaySettingsPanel.rawProfileRightButton.setEnabled(false);
//			proflleDisplaySettingsPanel.referenceButton.setEnabled(false);
//			proflleDisplaySettingsPanel.orientationButton.setEnabled(false);
		}
	}
			
	/**
	 * Update the profile panel with data from the given datasets
	 * @param list the datasets
	 * @param normalised flag for raw or normalised lengths
	 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
	 */	
	private void updateProfiles(List<AnalysisDataset> list, boolean normalised, boolean rightAlign, boolean fromReference, boolean showMarkers){

		try {
			
			String point 	= fromReference 
					? list.get(0).getCollection().getReferencePoint() 
					: list.get(0).getCollection().getOrientationPoint();
					
			if(list.size()==1){
				
				
			
				// full segment colouring
				JFreeChart chart = MorphologyChartFactory.makeSingleProfileChart(list.get(0), normalised, rightAlign, point, showMarkers);
				profilesPanel.setChart(chart);
				
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRDataset(list, normalised, rightAlign, point);				
				XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileDataset(	  list, normalised, rightAlign, point);
								
				// find the maximum profile length - used when rendering raw profiles
				int length = 100;

				if(!normalised){
					for(AnalysisDataset d : list){
						length = (int) Math.max( d.getCollection().getMedianArrayLength(), length);
					}
				}
				
				JFreeChart chart = MorphologyChartFactory.makeMultiProfileChart(list, medianProfiles, iqrProfiles, length);
							
				profilesPanel.setChart(chart);
			}
			
		} catch (Exception e) {
			error("Error in plotting profile", e);			
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
				JFreeChart chart = MorphologyChartFactory.makeProfileChart(ds, 100, list.get(0).getSwatch());
				frankenChartPanel.setChart(chart);
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRFrankenDataset( list);				
				XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileFrankenDataset(	  list);
			
				JFreeChart chart = MorphologyChartFactory.makeMultiProfileChart(list, medianProfiles, iqrProfiles, 100);
				frankenChartPanel.setChart(chart);
			}
						
		} catch (Exception e) {
			log("Error in plotting frankenprofile: "+e.getMessage());
			for(AnalysisDataset d : list){
				log(d.getName());
				ProfileCollection f = d.getCollection().getFrankenCollection();
				log(f.printKeys());
			}
			error("Error in plotting fankenprofile", e);
		} 
	}
	
	public void updateVariabilityChart(List<AnalysisDataset> list){
		try {
			if(list.size()==1){
				JFreeChart chart = MorphologyChartFactory.makeSingleVariabilityChart(list, 100);
				variabilityChartPanel.setChart(chart);
			} else { // multiple nuclei
				JFreeChart chart = MorphologyChartFactory.makeMultiVariabilityChart(list, 100);
				variabilityChartPanel.setChart(chart);
			}
		} catch (Exception e) {
			error("Error in plotting variability chart", e);
		}	
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		boolean normalised = proflleDisplaySettingsPanel.normCheckBox.isSelected();
		
		// only allow right align if not normalised
		boolean rightAlign = normalised ? false : proflleDisplaySettingsPanel.rawProfileRightButton.isSelected();
		boolean fromReference = proflleDisplaySettingsPanel.referenceButton.isSelected();
		boolean showMarkers = proflleDisplaySettingsPanel.showMarkersCheckBox.isSelected();
		
		updateProfiles(list, normalised, rightAlign, fromReference, showMarkers);

		if(e.getActionCommand().equals("LeftAlignRawProfile")){
//			updateProfiles(list, false, false);
//			updateRawProfileImage(list, false);
		}
		
		if(e.getActionCommand().equals("RightAlignRawProfile")){
//			updateRawProfileImage(list, true);
//			updateProfiles(list, false, true);
		}
		
		if(e.getActionCommand().equals("NormalisedProfile")){

			if(  proflleDisplaySettingsPanel.normCheckBox.isSelected()){
				proflleDisplaySettingsPanel.rawProfileLeftButton.setEnabled(false);
				proflleDisplaySettingsPanel.rawProfileRightButton.setEnabled(false);
//				updateProfiles(list, true, false);
			} else {
				proflleDisplaySettingsPanel.rawProfileLeftButton.setEnabled(true);
				proflleDisplaySettingsPanel.rawProfileRightButton.setEnabled(true);
				
				if(  proflleDisplaySettingsPanel.rawProfileLeftButton.isSelected()){
//					updateProfiles(list, false, false);
				} else {
//					updateProfiles(list, false, true);
				}
			}
			
		}
		
	}

}
