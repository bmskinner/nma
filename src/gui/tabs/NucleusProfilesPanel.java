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

import gui.components.BorderTagOptionsPanel;
import gui.components.ColourSelecter;
import gui.components.ProfileAlignmentOptionsPanel;
import gui.components.ProfileAlignmentOptionsPanel.ProfileAlignment;
import stats.DipTester;
import gui.components.ProfileCollectionTypeSettingsPanel;
import gui.components.ProfileMarkersOptionsPanel;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.TextAnchor;

import utility.Constants;
import analysis.AnalysisDataset;
import charting.charts.MorphologyChartFactory;
import charting.charts.ProfileChartOptions;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollectionType;

public class NucleusProfilesPanel extends DetailPanel {


	private static final long serialVersionUID = 1L;
		
	RegularProfileDisplayPanel 	profileDisplayPanel; // hold regular profiles
	FrankenProfileDisplayPanel 	frankenDisplayPanel; // hold franken profiles
	VariabililtyDisplayPanel	variabilityChartPanel;
	ModalityDisplayPanel 		modalityDisplayPanel;
	
	public NucleusProfilesPanel(Logger programLogger) {
		super(programLogger);
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
	
	@Override
	protected void updateDetail(){

		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				try {

					profileDisplayPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updated nuclear profiles panel");
					frankenDisplayPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updated franken profiles panel");
					variabilityChartPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updated variabililty panel");
					modalityDisplayPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updated modality panel");
				} catch  (Exception e){
					programLogger.log(Level.SEVERE, "Error updating profile panels", e);
					programLogger.log(Level.FINER, "Setting panels to null");

				} finally {
					setUpdating(false);
				}
			}});
	}

	@SuppressWarnings("serial")
	private class ModalityDisplayPanel extends JPanel implements ActionListener {
		
		private JPanel mainPanel = new JPanel(new BorderLayout());
		private JList<String> pointList;
		private ChartPanel chartPanel;
		private ChartPanel modalityProfileChartPanel; // hold a chart showing p-values across the profile
		private ProfileCollectionTypeSettingsPanel profileCollectionTypeSettingsPanel = new ProfileCollectionTypeSettingsPanel();
		private JPanel buttonPanel = new JPanel(new FlowLayout());
		
		public ModalityDisplayPanel(){
			this.setLayout(new BorderLayout());
			
			profileCollectionTypeSettingsPanel.addActionListener(this);
			profileCollectionTypeSettingsPanel.setEnabled(false);;
			buttonPanel.add(profileCollectionTypeSettingsPanel);
			this.add(buttonPanel, BorderLayout.NORTH);
			
			chartPanel = createPositionChartPanel();
			modalityProfileChartPanel = createModalityProfileChartPanel();
			
			mainPanel.add(chartPanel, BorderLayout.WEST);
			mainPanel.add(modalityProfileChartPanel, BorderLayout.CENTER);
			
			this.add(mainPanel, BorderLayout.CENTER);
			
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
			pointList.setEnabled(false);
			
		}
		
		private ChartPanel createPositionChartPanel(){
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					"Probability", "Angle", null);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.getDomainAxis().setRange(0,360);
			plot.addDomainMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2f)));
			
			ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			return chartPanel;
		}
		
		private ChartPanel createModalityProfileChartPanel(){
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					"Position", "Probability", null);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.getDomainAxis().setRange(0,100);
			plot.getRangeAxis().setRange(0,1);
			
			ChartPanel chartPanel = new ChartPanel(chart);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			return chartPanel;
		}
		
		public void update(List<AnalysisDataset> list) throws Exception {

			try{
				if( list!=null && !list.isEmpty()){

					profileCollectionTypeSettingsPanel.setEnabled(true);
					pointList.setEnabled(true);

					ProfileCollectionType type = profileCollectionTypeSettingsPanel.getSelected();

					DecimalFormat df = new DecimalFormat("#0.00");
					DefaultListModel<String> model = new DefaultListModel<String>();
					if(list.size()==1){ // use the actual x-positions
						List<Double> xvalues = list.get(0).getCollection().getProfileCollection(type).getAggregate().getXKeyset();

						for(Double d: xvalues){
							model.addElement(df.format(d));
						}


					} else {
						// use a standard 0.5 spacing
						for(Double d=0.0; d<=100; d+=0.5){
							model.addElement(df.format(d));
						}

					}
					pointList.setModel(model);

					String xString = pointList.getModel().getElementAt(0);
					double xvalue = Double.valueOf(xString);

					updateChart(xvalue);	
					updateModalityProfileChart(list);
				} else {
					profileCollectionTypeSettingsPanel.setEnabled(false);
					pointList.setEnabled(false);
				}

			} catch(Exception e){
				update( (List<AnalysisDataset>) null);
			}
		}

		public void updateModalityProfileChart(List<AnalysisDataset> list){
			
			ProfileCollectionType type = profileCollectionTypeSettingsPanel.getSelected();
			ProfileChartOptions options = new ProfileChartOptions(list, 
					true, 
					ProfileAlignment.LEFT, 
					BorderTag.REFERENCE_POINT, 
					false,
					type);
			
			JFreeChart chart = null;
			try {
				chart = MorphologyChartFactory.createModalityProfileChart(options);
				modalityProfileChartPanel.setChart(chart);
			} catch (Exception e){
				programLogger.log(Level.SEVERE, "Error updating modality profiles panel", e);
			}
		}
		
		public void updateChart(double xvalue){
			
			ProfileCollectionType type = profileCollectionTypeSettingsPanel.getSelected();
			
			JFreeChart chart = null;
			try {

				chart = MorphologyChartFactory.createModalityChart(xvalue, getDatasets(), type);
				XYPlot plot = chart.getXYPlot();

				double yMax = 0;
				DecimalFormat df = new DecimalFormat("#0.000");
								
				for(int i = 0; i<plot.getDatasetCount(); i++){

					// Ensure annotation is placed in the right y position
					double y = DatasetUtilities.findMaximumRangeValue(plot.getDataset(i)).doubleValue();
					yMax = y > yMax ? y : yMax;

				}
				
				int index = 0;
				for(AnalysisDataset dataset : getDatasets()){
					
					// Do the stats testing
					double pvalue = DipTester.getPValueForPositon(dataset.getCollection(), xvalue, type); 
//					Profile allValues = DipTester.testCollectionGetPValues(dataset.getCollection(), BorderTag.REFERENCE_POINT, type);
					
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
				programLogger.log(Level.SEVERE, "Error updating modality panel", e1);
			}
		}
		
		private class ModalitySelectionListener implements ListSelectionListener {
			public void valueChanged(ListSelectionEvent e) {
				updatePointSelection();
				
			}
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			updatePointSelection();
			updateModalityProfileChart(getDatasets());
		}
		
		private void updatePointSelection(){
			int row = pointList.getSelectedIndex();
			String xString = pointList.getModel().getElementAt(row);
			double xvalue = Double.valueOf(xString);
			
			int lastRow = pointList.getModel().getSize()-1;
			
			if(xvalue==100 || row==lastRow){
				xvalue=0; // wrap arrays
			}
			
			programLogger.log(Level.FINEST, "Selecting profile position "+xvalue +" at index "+row);
			updateChart(xvalue);
		}
	}
	
	@SuppressWarnings("serial")
	private class VariabililtyDisplayPanel extends JPanel implements ActionListener, ChangeListener {
		
//		
		private JPanel buttonPanel = new JPanel(new FlowLayout());
		protected ChartPanel chartPanel;
		private JSpinner pvalueSpinner;
//		
		private BorderTagOptionsPanel borderTagOptionsPanel = new BorderTagOptionsPanel();
		private ProfileCollectionTypeSettingsPanel profileCollectionTypeSettingsPanel = new ProfileCollectionTypeSettingsPanel();
		private ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();
//		protected ProfileAlignmentOptionsPanel profileAlignmentOptionsPanel = new ProfileAlignmentOptionsPanel();
		
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
			
//			buttonPanel.add(profileAlignmentOptionsPanel);
//			profileAlignmentOptionsPanel.addActionListener(this);
//			profileAlignmentOptionsPanel.setEnabled(false);
			
			buttonPanel.add(borderTagOptionsPanel);
			borderTagOptionsPanel.addActionListener(this);
			borderTagOptionsPanel.setEnabled(false);

			
			pvalueSpinner = new JSpinner(new SpinnerNumberModel(Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL,	0d, 1d, 0.001d));
			pvalueSpinner.setEnabled(false);
			pvalueSpinner.addChangeListener(this);
			JComponent field = ((JSpinner.DefaultEditor) pvalueSpinner.getEditor());
		      Dimension prefSize = field.getPreferredSize();
		      prefSize = new Dimension(50, prefSize.height);
		      field.setPreferredSize(prefSize);
		      
		     // add extra fields to the header panel
		      buttonPanel.add(new JLabel("Dip test p-value:"));
		      buttonPanel.add(pvalueSpinner);


			profileCollectionTypeSettingsPanel.addActionListener(this);
			profileCollectionTypeSettingsPanel.setEnabled(false);
			buttonPanel.add(profileCollectionTypeSettingsPanel);
			
			buttonPanel.revalidate();
			
			this.add(buttonPanel, BorderLayout.NORTH);
		}

		public void update(List<AnalysisDataset> list){

			if(!list.isEmpty()){

				borderTagOptionsPanel.setEnabled(true);
//				profileAlignmentOptionsPanel.setEnabled(true);
				profileCollectionTypeSettingsPanel.setEnabled(true);
				profileMarkersOptionsPanel.setEnabled(true);
				
				pvalueSpinner.setEnabled(true);

				if(list.size()>1){

					// Don't allow marker selection for multiple datasets
					profileMarkersOptionsPanel.setEnabled(false);
//					
					pvalueSpinner.setEnabled(false);
				}


			} else {

				// if the list is empty, do not enable controls
				borderTagOptionsPanel.setEnabled(false);
//				profileAlignmentOptionsPanel.setEnabled(false);
				profileCollectionTypeSettingsPanel.setEnabled(false);
				profileMarkersOptionsPanel.setEnabled(false);
//				
			}
			
			BorderTag tag = borderTagOptionsPanel.getSelected();
			boolean showMarkers = profileMarkersOptionsPanel.showMarkers();
			updateProfiles(list, true, ProfileAlignment.LEFT, tag, showMarkers);
		}
		
		/**
		 * Update the profile panel with data from the given datasets
		 * @param list the datasets
		 * @param normalised flag for raw or normalised lengths
		 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
		 */	
		private void updateProfiles(List<AnalysisDataset> list, boolean normalised, ProfileAlignment alignment, BorderTag tag, boolean showMarkers){
			
			ProfileCollectionType type = profileCollectionTypeSettingsPanel.getSelected();
			
			try {
				if(list.size()==1){
					JFreeChart chart = MorphologyChartFactory.makeSingleVariabilityChart(list, 100, tag, type);
					
					
					if(showMarkers){ // add the bimodal regions
						CellCollection collection = list.get(0).getCollection();
						
						// dip test the profiles
						
						double significance = (Double) pvalueSpinner.getValue();
						BooleanProfile modes  = DipTester.testCollectionIsUniModal(collection, tag, significance, type);


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
					JFreeChart chart = MorphologyChartFactory.makeMultiVariabilityChart(list, 100, tag, type);
					chartPanel.setChart(chart);
				}
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error in plotting variability chart", e);
			}	
		}


		@Override
		public void actionPerformed(ActionEvent e) {

			update(getDatasets());

		}

		@Override
		public void stateChanged(ChangeEvent arg0) {
			if(arg0.getSource()==pvalueSpinner){
				JSpinner j = (JSpinner) arg0.getSource();
				try {
					j.commitEdit();
				} catch (ParseException e) {
					programLogger.log(Level.SEVERE, "Error setting p-value spinner", e);
				}
			}
			update(getDatasets());
			
		}
	}
	
	@SuppressWarnings("serial")
	private abstract class ProfileDisplayPanel extends JPanel implements ActionListener {
		
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);

		protected JPanel buttonPanel = new JPanel(new FlowLayout());
		protected ChartPanel chartPanel;
		
		protected BorderTagOptionsPanel borderTagOptionsPanel = new BorderTagOptionsPanel();
		protected ProfileAlignmentOptionsPanel profileAlignmentOptionsPanel = new ProfileAlignmentOptionsPanel();
		protected ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();
		
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
			
			buttonPanel.add(profileAlignmentOptionsPanel);
			profileAlignmentOptionsPanel.addActionListener(this);
			profileAlignmentOptionsPanel.setEnabled(false);
			
			buttonPanel.add(borderTagOptionsPanel);
			borderTagOptionsPanel.addActionListener(this);
			borderTagOptionsPanel.setEnabled(false);
			
			buttonPanel.add(profileMarkersOptionsPanel);
			profileMarkersOptionsPanel.addActionListener(this);
			profileMarkersOptionsPanel.setEnabled(false);
						
			this.add(buttonPanel, BorderLayout.NORTH);
		}
		
		public void update(List<AnalysisDataset> list){
			
			if(!list.isEmpty()){
				
				profileAlignmentOptionsPanel.setEnabled(true);
				borderTagOptionsPanel.setEnabled(true);
				profileMarkersOptionsPanel.setEnabled(true);

				
				if(list.size()>1){
					
					// Don't allow marker selection for multiple datasets
					profileMarkersOptionsPanel.setEnabled(false);
				}
				
				
			} else {
				profileAlignmentOptionsPanel.setEnabled(false);
				borderTagOptionsPanel.setEnabled(false);
				profileMarkersOptionsPanel.setEnabled(false);
				
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			
			update(getDatasets());
		}
	}
	
	@SuppressWarnings("serial")
	private class RegularProfileDisplayPanel extends ProfileDisplayPanel {
		
		public RegularProfileDisplayPanel(){
			super();
		}
		
		public void update(List<AnalysisDataset> list){
			super.update(list);
			
			boolean normalised = profileAlignmentOptionsPanel.isNormalised();
			ProfileAlignment alignment = profileAlignmentOptionsPanel.getSelected();
			
			if(normalised){
				alignment = ProfileAlignment.LEFT;
			}
			
			BorderTag tag = borderTagOptionsPanel.getSelected();
			boolean showMarkers = profileMarkersOptionsPanel.showMarkers();
			
			ProfileChartOptions options = new ProfileChartOptions(list, normalised, alignment, tag, showMarkers, ProfileCollectionType.REGULAR);
			
			updateProfiles(options);
		}
		
		/**
		 * Update the profile panel with data from the given datasets
		 * @param list the datasets
		 * @param normalised flag for raw or normalised lengths
		 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
		 */	
		private void updateProfiles(ProfileChartOptions options){
			try {	
				JFreeChart chart = null;

				// Check for a cached chart
				if(getChartCache().hasChart(options)){
					
					programLogger.log(Level.FINEST, "Using cached profile chart");
					chart = getChartCache().getChart(options);

				} else { // No cache

					if(getDatasets().size()==1){

						// full segment colouring
						chart = MorphologyChartFactory.makeSingleProfileChart( options );
						chartPanel.setChart(chart);

					} else {

						chart = MorphologyChartFactory.makeMultiProfileChart( options );

						
					}
					getChartCache().addChart(options, chart);
					programLogger.log(Level.FINEST, "Added cached profile chart");
				}
				chartPanel.setChart(chart);
				
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error in plotting profile", e);			
			} 
		}
		
		
	}
	
	@SuppressWarnings("serial")
	private class FrankenProfileDisplayPanel extends ProfileDisplayPanel {
		
		public FrankenProfileDisplayPanel(){
			super();
			profileAlignmentOptionsPanel.setEnabled(false);
		}
		
		public void update(List<AnalysisDataset> list){
			super.update(list);
			profileAlignmentOptionsPanel.setEnabled(false);
			
			boolean normalised = true;
			ProfileAlignment alignment = ProfileAlignment.LEFT;
			BorderTag tag = borderTagOptionsPanel.getSelected();
			boolean showMarkers = profileMarkersOptionsPanel.showMarkers();
			
			ProfileChartOptions options = new ProfileChartOptions(list, normalised, alignment, tag, showMarkers, ProfileCollectionType.FRANKEN);
			
			
			updateProfiles(options);
		}
		
		/**
		 * Update the profile panel with data from the given datasets
		 * @param list the datasets
		 * @param normalised flag for raw or normalised lengths
		 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
		 */	
		private void updateProfiles(ProfileChartOptions options){
					
			try {
				JFreeChart chart = null;
			
				
				if(getChartCache().hasChart(options)){

					programLogger.log(Level.FINEST, "Using cached frankenprofile chart");
					chart = getChartCache().getChart(options);

				} else { // No cache


					if(getDatasets().size()==1){

						chart = MorphologyChartFactory.makeFrankenProfileChart(options);

					} else {

						chart = MorphologyChartFactory.makeMultiProfileChart(options);
					}
					getChartCache().addChart(options, chart);
					programLogger.log(Level.FINEST, "Added cached frankenprofile chart");
				}
				chartPanel.setChart(chart);

			} catch (Exception e) {
//				log("Error in plotting frankenprofile: "+e.getMessage());
//				for(AnalysisDataset d : list){
//					log(d.getName());
//					ProfileCollection f = d.getCollection().getProfileCollection(ProfileCollectionType.FRANKEN);
//					log(f.printKeys());
//				}
				programLogger.log(Level.SEVERE, "Error in plotting frankenprofile", e);
			} 
		}

	}

}
