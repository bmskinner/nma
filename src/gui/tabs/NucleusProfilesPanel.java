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

import gui.components.ColourSelecter;
import gui.components.ProflleDisplaySettingsPanel;
import gui.components.ColourSelecter.ColourSwatch;
import ij.IJ;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.ListModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;

import jdistlib.disttest.DistributionTest;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

import utility.Constants.BorderTag;
import utility.Constants;
import utility.DipTester;
import charting.charts.MorphologyChartFactory;
import charting.datasets.NucleusDatasetCreator;
import components.CellCollection;
import components.CellCollection.ProfileCollectionType;
import components.generic.BooleanProfile;
import components.generic.Profile;
import components.generic.ProfileCollection;
import analysis.AnalysisDataset;

public class NucleusProfilesPanel extends DetailPanel {


	private static final long serialVersionUID = 1L;

//	private ChartPanel variabilityChartPanel; 
		
	RegularProfileDisplayPanel 	profileDisplayPanel; // hold regular profiles
	FrankenProfileDisplayPanel 	frankenDisplayPanel; // hold regular profiles
	VariabililtyDisplayPanel	variabilityChartPanel;
	ModalityDisplayPanel 		modalityDisplayPanel;
	
	private List<AnalysisDataset> list;
	
	public NucleusProfilesPanel() {
		
		this.setLayout(new BorderLayout());
		JTabbedPane profilesTabPanel = new JTabbedPane(JTabbedPane.TOP);
		
		profileDisplayPanel = new RegularProfileDisplayPanel();
		frankenDisplayPanel = new FrankenProfileDisplayPanel();
		modalityDisplayPanel = new ModalityDisplayPanel();		
		variabilityChartPanel = new VariabililtyDisplayPanel();
		//---------------
		// Add to the tabbed panel
		//---------------
		profilesTabPanel.addTab("Profile", null, profileDisplayPanel, null);
		profilesTabPanel.addTab("FrankenProfile", null, frankenDisplayPanel, null);
		profilesTabPanel.addTab("Variability", null, variabilityChartPanel, null);
		profilesTabPanel.addTab("Modality", null, modalityDisplayPanel, null);
		this.add(profilesTabPanel, BorderLayout.CENTER);

	}
	
	public void update(List<AnalysisDataset> list){
		
		this.list = list;
		
		try {
			profileDisplayPanel.update(list);
			frankenDisplayPanel.update(list);
			variabilityChartPanel.update(list);
			modalityDisplayPanel.update(list);
		} catch  (Exception e){
			error("Error updating profile panels", e);
		}
	}
	
	@SuppressWarnings("serial")
	private class ModalityDisplayPanel extends JPanel {
		
		private JList<String> pointList;
		private ChartPanel chartPanel;
//		private JPanel 
		
		public ModalityDisplayPanel(){
			this.setLayout(new BorderLayout());
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					"Probability", "Angle", null);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.getDomainAxis().setRange(0,360);
			plot.addDomainMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2f)));
			
			chartPanel = new ChartPanel(chart);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			this.add(chartPanel, BorderLayout.CENTER);
			
			DecimalFormat df = new DecimalFormat("#0.00");
			pointList = new JList<String>();
			DefaultListModel<String> model = new DefaultListModel<String>();
			for(Double d=0.0; d<=100; d+=0.5){
				model.addElement(df.format(d));
			}
			pointList.setModel(model);
			JScrollPane listPanel = new JScrollPane(pointList);
			this.add(listPanel, BorderLayout.WEST);
			pointList.addListSelectionListener(new ModalitySelectionListener());
			
		}
		
		public void update(List<AnalysisDataset> list) throws Exception {

			if(!list.isEmpty()){
				
				DecimalFormat df = new DecimalFormat("#0.00");
				if(list.size()==1){ // use the actual x-positions
					List<Double> xvalues = list.get(0).getCollection().getProfileCollection(ProfileCollectionType.FRANKEN).getAggregate().getXKeyset();
					DefaultListModel<String> model = new DefaultListModel<String>();
					
					for(Double d: xvalues){
						model.addElement(df.format(d));
					}
					pointList.setModel(model);
				} else {
					// use a standard 0.5 spacing
					DefaultListModel<String> model = new DefaultListModel<String>();
					for(Double d=0.0; d<=100; d+=0.5){
						model.addElement(df.format(d));
					}
					pointList.setModel(model);
				}
				
				String xString = pointList.getModel().getElementAt(0);
				double xvalue = Double.valueOf(xString);
				
				updateChart(xvalue);				
			}
		}
		
		public void updateChart(double xvalue){
			JFreeChart chart = null;
			try {

				chart = MorphologyChartFactory.createModalityChart(xvalue, list);
				XYPlot plot = chart.getXYPlot();

				double yMax = 0;
				DecimalFormat df = new DecimalFormat("#0.000");
								
				for(int i = 0; i<plot.getDatasetCount(); i++){

					// Ensure annotation is placed in the right y position
					double y = DatasetUtilities.findMaximumRangeValue(plot.getDataset(i)).doubleValue();
					yMax = y > yMax ? y : yMax;

				}
				
				int index = 0;
				for(AnalysisDataset dataset : list){
					
					// Do the stats testing
					double pvalue = DipTester.getPValueForPositon(dataset.getCollection(), xvalue); 
					
					// Add the annotation
					double yPos = yMax - ( index * (yMax / 20));
					String statisticalTesting = "p(unimodal) = "+df.format(pvalue);
					if(pvalue<Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL){
						statisticalTesting = "* " + statisticalTesting;
					}
					XYTextAnnotation annotation = new XYTextAnnotation(statisticalTesting,355, yPos);

					// Set the text colour
					Color colour = dataset.getDatasetColour() == null 
							? ColourSelecter.getSegmentColor(index)
									: dataset.getDatasetColour();
					annotation.setPaint(colour);
					annotation.setTextAnchor(TextAnchor.TOP_RIGHT);
					plot.addAnnotation(annotation);
					index++;
				}

				chartPanel.setChart(chart);
			} catch (Exception e1) {
				error("Error updating modality panel", e1);
			}
		}
		
		private class ModalitySelectionListener implements ListSelectionListener {
			public void valueChanged(ListSelectionEvent e) {
				int row = e.getFirstIndex();
				String xString = pointList.getModel().getElementAt(row);
				double xvalue = Double.valueOf(xString);
//				double xvalue = pointList.getModel().getElementAt(row);
				updateChart(xvalue);
				
			}
		}
	}
	
	@SuppressWarnings("serial")
	private class VariabililtyDisplayPanel extends JPanel implements ActionListener, ChangeListener {
		
		protected ProflleDisplaySettingsPanel profileDisplaySettingsPanel;
		protected ChartPanel chartPanel;
		private JSpinner pvalueSpinner;
		
		public VariabililtyDisplayPanel(){
			this.setLayout(new BorderLayout());
			JFreeChart variablityChart = ChartFactory.createXYLineChart(null,
					"Position", "IQR", null);
			XYPlot variabilityPlot = variablityChart.getXYPlot();
			variabilityPlot.setBackgroundPaint(Color.WHITE);
			variabilityPlot.getDomainAxis().setRange(0,100);
			chartPanel = new ChartPanel(variablityChart);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			this.add(chartPanel, BorderLayout.CENTER);
			
			// add the alignments panel to the tab
			profileDisplaySettingsPanel = new ProflleDisplaySettingsPanel();
			profileDisplaySettingsPanel.referenceButton.addActionListener(this);
			profileDisplaySettingsPanel.orientationButton.addActionListener(this);
			profileDisplaySettingsPanel.showMarkersCheckBox.addActionListener(this);
			profileDisplaySettingsPanel.referenceButton.setSelected(true);
			
			// disable unused settings
			profileDisplaySettingsPanel.normCheckBox.setEnabled(false);
			profileDisplaySettingsPanel.rawProfileLeftButton.setEnabled(false);
			profileDisplaySettingsPanel.rawProfileRightButton.setEnabled(false);
			
			pvalueSpinner = new JSpinner(new SpinnerNumberModel(Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL,	0d, 1d, 0.001d));
			pvalueSpinner.setEnabled(false);
			pvalueSpinner.addChangeListener(this);
			JComponent field = ((JSpinner.DefaultEditor) pvalueSpinner.getEditor());
		      Dimension prefSize = field.getPreferredSize();
		      prefSize = new Dimension(50, prefSize.height);
		      field.setPreferredSize(prefSize);
		      
		      
		    profileDisplaySettingsPanel.add(new JLabel("Dip test p-value:"));
			profileDisplaySettingsPanel.add(pvalueSpinner);
			profileDisplaySettingsPanel.revalidate();
			
			
			this.add(profileDisplaySettingsPanel, BorderLayout.NORTH);
		}

		public void update(List<AnalysisDataset> list){

			if(!list.isEmpty()){

				profileDisplaySettingsPanel.referenceButton.setEnabled(true);
				profileDisplaySettingsPanel.orientationButton.setEnabled(true);
				profileDisplaySettingsPanel.showMarkersCheckBox.setEnabled(true);
				pvalueSpinner.setEnabled(true);

				if(list.size()>1){

					// Don't allow marker selection for multiple datasets
					profileDisplaySettingsPanel.showMarkersCheckBox.setEnabled(false);
					pvalueSpinner.setEnabled(false);
				}


			} else {

				// if the list is empty, do not enable controls
				profileDisplaySettingsPanel.setEnabled(false);
			}
			
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
			
			BorderTag tag = fromReference
					? BorderTag.REFERENCE_POINT
					: BorderTag.ORIENTATION_POINT;
			try {
				if(list.size()==1){
					JFreeChart chart = MorphologyChartFactory.makeSingleVariabilityChart(list, 100, tag);
					
					
					if(showMarkers){ // add the bimodal regions
						CellCollection collection = list.get(0).getCollection();
						
						// dip test the profiles
						
						double significance = (Double) pvalueSpinner.getValue();
						BooleanProfile modes  = DipTester.testCollectionGetIsNotUniModal(collection, tag, significance);


						// add any regions with bimodal distribution to the chart
						XYPlot plot = chart.getXYPlot();

						Profile xPositions = modes.getPositions(100);

						for(int i=0; i<modes.size(); i++){
							double x = xPositions.get(i);
							if(modes.get(i)==true){
								ValueMarker marker = new ValueMarker(x, Color.black, new BasicStroke(2f));
								plot.addDomainMarker(marker);
							}
						}
	
						double ymax = DatasetUtilities.findMaximumRangeValue(plot.getDataset()).doubleValue();
						DecimalFormat df = new DecimalFormat("#0.000"); 
						XYTextAnnotation annotation = new XYTextAnnotation("Markers for non-unimodal positions (p<"+df.format(significance)+")",1, ymax);
						annotation.setTextAnchor(TextAnchor.TOP_LEFT);
						plot.addAnnotation(annotation);
					}
					
					chartPanel.setChart(chart);
				} else { // multiple nuclei
					JFreeChart chart = MorphologyChartFactory.makeMultiVariabilityChart(list, 100, tag);
					chartPanel.setChart(chart);
				}
			} catch (Exception e) {
				error("Error in plotting variability chart", e);
			}	
		}


		@Override
		public void actionPerformed(ActionEvent e) {

			update(list);

		}

		@Override
		public void stateChanged(ChangeEvent arg0) {
			if(arg0.getSource()==pvalueSpinner){
				JSpinner j = (JSpinner) arg0.getSource();
				try {
					j.commitEdit();
				} catch (ParseException e) {
					error("Error setting p-value spinner", e);
				}
			}
			update(list);
			
		}
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
				
				BorderTag tag = fromReference
						? BorderTag.REFERENCE_POINT
						: BorderTag.ORIENTATION_POINT;
				
						
				if(list.size()==1){

					// full segment colouring
					JFreeChart chart = MorphologyChartFactory.makeSingleProfileChart(list.get(0), normalised, rightAlign, tag, showMarkers);
					chartPanel.setChart(chart);
					
				} else {
					// many profiles, colour them all the same
					List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRDataset(list, normalised, rightAlign, tag);				
					XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileDataset(	  list, normalised, rightAlign, tag);
									
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

			BorderTag tag = fromReference
					? BorderTag.REFERENCE_POINT
					: BorderTag.ORIENTATION_POINT;
					
			try {
				if(list.size()==1){
					
					JFreeChart chart = MorphologyChartFactory.makeFrankenProfileChart(list.get(0), normalised, rightAlign, tag, showMarkers);
					chartPanel.setChart(chart);
				} else {

					// many profiles, colour them all the same
					List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRFrankenDataset(list, normalised, rightAlign, tag);				
					XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileFrankenDataset(	  list, normalised, rightAlign, tag);
									
					
					JFreeChart chart = MorphologyChartFactory.makeMultiProfileChart(list, medianProfiles, iqrProfiles, 100);
					chartPanel.setChart(chart);
				}

			} catch (Exception e) {
				log("Error in plotting frankenprofile: "+e.getMessage());
				for(AnalysisDataset d : list){
					log(d.getName());
					ProfileCollection f = d.getCollection().getProfileCollection(ProfileCollectionType.FRANKEN);
					log(f.printKeys());
				}
				error("Error in plotting fankenprofile", e);
			} 
		}

	}

}
