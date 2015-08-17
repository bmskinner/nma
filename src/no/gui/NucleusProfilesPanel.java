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
import no.components.ProfileCollection;
import no.nuclei.Nucleus;

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

import datasets.MorphologyChartFactory;
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
	 * Update the profile panel with data from the given datasets
	 * @param list the datasets
	 * @param normalised flag for raw or normalised lengths
	 * @param rightAlign flag for left or right alignment (no effect if normalised is true)
	 */	
	private void updateProfiles(List<AnalysisDataset> list, boolean normalised, boolean rightAlign){

		try {
			if(list.size()==1){
				
				XYDataset ds = NucleusDatasetCreator.createSegmentedProfileDataset(list.get(0).getCollection(), normalised);
				
				int length = 100 ; // default if normalised

				// if we set raw values, get the maximum nucleus length
				if(!normalised){
					for(Nucleus n : list.get(0).getCollection().getNuclei()){
						length = (int) Math.max( n.getLength(), length);
					}
				}
								
				// full segment colouring
				JFreeChart chart = MorphologyChartFactory.makeProfileChart(ds, length);
				profilesPanel.setChart(chart);
				
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRDataset(list, normalised, rightAlign);				
				XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileDataset(	  list, normalised, rightAlign);
								
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
				JFreeChart chart = MorphologyChartFactory.makeProfileChart(ds, 100);
				frankenChartPanel.setChart(chart);
			} else {
				// many profiles, colour them all the same
				List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRFrankenDataset( list);				
				XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileFrankenDataset(	  list);
			
				JFreeChart chart = MorphologyChartFactory.makeMultiProfileChart(list, medianProfiles, iqrProfiles, 100);
				frankenChartPanel.setChart(chart);
			}
						
		} catch (Exception e) {
			IJ.log("Error in plotting frankenprofile: "+e.getMessage());
			for(AnalysisDataset d : list){
				IJ.log(d.getName());
				ProfileCollection f = d.getCollection().getFrankenCollection();
				IJ.log(f.printKeys());
			}
			for(StackTraceElement el : e.getStackTrace()){
				IJ.log(el.toString());
			}
		} 
	}
	
	public void updateVariabilityChart(List<AnalysisDataset> list){
		try {
			XYDataset ds = NucleusDatasetCreator.createIQRVariabilityDataset(list);
			if(list.size()==1){
				JFreeChart chart = MorphologyChartFactory.makeSingleVariabilityChart(list, ds, 100);
				variabilityChartPanel.setChart(chart);
			} else { // multiple nuclei
				JFreeChart chart = MorphologyChartFactory.makeMultiVariabilityChart(list, ds, 100);
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
