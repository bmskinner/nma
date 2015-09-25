/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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

import gui.components.ProflleDisplaySettingsPanel;
import gui.components.ColourSelecter.ColourSwatch;

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

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import components.generic.ProfileCollection;

import analysis.AnalysisDataset;
import datasets.MorphologyChartFactory;
import datasets.NucleusDatasetCreator;

public class NucleusProfilesPanel extends DetailPanel implements ActionListener {


	private static final long serialVersionUID = 1L;

	private ChartPanel variabilityChartPanel; 
		
	RegularProfileDisplayPanel profileDisplayPanel; // hold regular profiles
	FrankenProfileDisplayPanel frankenDisplayPanel; // hold regular profiles
	
	private List<AnalysisDataset> list;
	
	public NucleusProfilesPanel() {
		
		this.setLayout(new BorderLayout());
		JTabbedPane profilesTabPanel = new JTabbedPane(JTabbedPane.TOP);
		
		profileDisplayPanel = new RegularProfileDisplayPanel();
		frankenDisplayPanel = new FrankenProfileDisplayPanel();
				
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
		profilesTabPanel.addTab("Profile", null, profileDisplayPanel, null);
		profilesTabPanel.addTab("FrankenProfile", null, frankenDisplayPanel, null);
		profilesTabPanel.addTab("Variability", null, variabilityChartPanel, null);
		this.add(profilesTabPanel, BorderLayout.CENTER);

	}
	
	public void update(List<AnalysisDataset> list){
		
		this.list = list;
		
		profileDisplayPanel.update(list);
		frankenDisplayPanel.update(list);
		
		if(!list.isEmpty()){
			updateVariabilityChart(list);
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

	}
	
	@SuppressWarnings("serial")
	private abstract class ProfileDisplayPanel extends JPanel implements ActionListener {
		
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		protected ProflleDisplaySettingsPanel profileDisplaySettingsPanel;
		protected ChartPanel chartPanel;
		
		public ProfileDisplayPanel(){
			this.setLayout(new BorderLayout());
			JFreeChart rawChart = MorphologyChartFactory.makeEmptyProfileChart();
			chartPanel = MorphologyChartFactory.makeProfileChartPanel(rawChart);
			
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			this.setMinimumSize(minimumChartSize);
			this.setPreferredSize(preferredChartSize);
			this.add(chartPanel, BorderLayout.CENTER);
					
			// add the alignments panel to the tab
			
			profileDisplaySettingsPanel = new ProflleDisplaySettingsPanel();
			profileDisplaySettingsPanel.normCheckBox.addActionListener(this);
			profileDisplaySettingsPanel.rawProfileLeftButton.addActionListener(this);
			profileDisplaySettingsPanel.rawProfileRightButton.addActionListener(this);
			profileDisplaySettingsPanel.referenceButton.addActionListener(this);
			profileDisplaySettingsPanel.orientationButton.addActionListener(this);
			profileDisplaySettingsPanel.showMarkersCheckBox.addActionListener(this);
			
			this.add(profileDisplaySettingsPanel, BorderLayout.NORTH);
		}
		
		public void update(List<AnalysisDataset> list){
			
			if(!list.isEmpty()){
				
				profileDisplaySettingsPanel.normCheckBox.setEnabled(true);
				profileDisplaySettingsPanel.referenceButton.setEnabled(true);
				profileDisplaySettingsPanel.orientationButton.setEnabled(true);
				profileDisplaySettingsPanel.showMarkersCheckBox.setEnabled(true);
				
				if(list.size()>1){
					
					// Don't allow marker selection for multiple datasets
					profileDisplaySettingsPanel.showMarkersCheckBox.setEnabled(false);
				}
				
				
			} else {
				
				// if the list is empty, do not enable controls
				profileDisplaySettingsPanel.setEnabled(false);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			
			update(list);
			
			if(e.getActionCommand().equals("NormalisedProfile")){

				if(  profileDisplaySettingsPanel.normCheckBox.isSelected()){
					profileDisplaySettingsPanel.rawProfileLeftButton.setEnabled(false);
					profileDisplaySettingsPanel.rawProfileRightButton.setEnabled(false);

				} else {
					profileDisplaySettingsPanel.rawProfileLeftButton.setEnabled(true);
					profileDisplaySettingsPanel.rawProfileRightButton.setEnabled(true);
				}
			}
		}
	}
	
	@SuppressWarnings("serial")
	private class RegularProfileDisplayPanel extends ProfileDisplayPanel {
		
		public RegularProfileDisplayPanel(){
			super();
		}
		
		public void update(List<AnalysisDataset> list){
			super.update(list);
			
			boolean normalised = profileDisplaySettingsPanel.normCheckBox.isSelected();
			boolean rightAlign = normalised ? false : profileDisplaySettingsPanel.rawProfileRightButton.isSelected();
			boolean fromReference = profileDisplaySettingsPanel.referenceButton.isSelected();
			boolean showMarkers = profileDisplaySettingsPanel.showMarkersCheckBox.isSelected();
			updateProfiles(list, normalised, rightAlign, fromReference, showMarkers);
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
					chartPanel.setChart(chart);
					
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
								
					chartPanel.setChart(chart);
				}
				
			} catch (Exception e) {
				error("Error in plotting profile", e);			
			} 
		}
		
		
	}
	
	@SuppressWarnings("serial")
	private class FrankenProfileDisplayPanel extends ProfileDisplayPanel {
		
		public FrankenProfileDisplayPanel(){
			super();
		}
		
		public void update(List<AnalysisDataset> list){
			super.update(list);
			
			boolean normalised = profileDisplaySettingsPanel.normCheckBox.isSelected();
			boolean rightAlign = normalised ? false : profileDisplaySettingsPanel.rawProfileRightButton.isSelected();
			boolean fromReference = profileDisplaySettingsPanel.referenceButton.isSelected();
			boolean showMarkers = profileDisplaySettingsPanel.showMarkersCheckBox.isSelected();
			updateProfiles(list, normalised, rightAlign, fromReference, showMarkers);
		}
		
		/**
		 * Update the profile panel with data from the given datasets
		 * @param list the datasets
		 * @param normalised flag for raw or normalised lengths
		 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
		 */	
		private void updateProfiles(List<AnalysisDataset> list, boolean normalised, boolean rightAlign, boolean fromReference, boolean showMarkers){

			String point 	= fromReference 
					? list.get(0).getCollection().getReferencePoint() 
					: list.get(0).getCollection().getOrientationPoint();
					
			try {
				if(list.size()==1){
					
					JFreeChart chart = MorphologyChartFactory.makeFrankenProfileChart(list.get(0), normalised, rightAlign, point, showMarkers);
					chartPanel.setChart(chart);
				} else {

					// many profiles, colour them all the same
					List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRFrankenDataset(list, normalised, rightAlign, point);				
					XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileFrankenDataset(	  list, normalised, rightAlign, point);
									
					
					JFreeChart chart = MorphologyChartFactory.makeMultiProfileChart(list, medianProfiles, iqrProfiles, 100);
					chartPanel.setChart(chart);
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

	}

}
