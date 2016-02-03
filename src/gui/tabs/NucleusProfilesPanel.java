/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
import gui.components.ExportableChartPanel;
import gui.components.panels.BorderTagOptionsPanel;
import gui.components.panels.ProfileCollectionTypeSettingsPanel;
import gui.components.panels.ProfileMarkersOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.tabs.profiles.ProfileDisplayPanel;
import gui.tabs.profiles.VariabilityDisplayPanel;
import stats.DipTester;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.ui.TextAnchor;

import utility.Constants;
import analysis.AnalysisDataset;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileType;

@SuppressWarnings("serial")
public class NucleusProfilesPanel extends DetailPanel {
	
	private Map<ProfileType, ProfileDisplayPanel> profilePanels = new HashMap<ProfileType, ProfileDisplayPanel>();
	
	VariabilityDisplayPanel	variabilityChartPanel;
	ModalityDisplayPanel 		modalityDisplayPanel;
	
	public NucleusProfilesPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		JTabbedPane profilesTabPanel = new JTabbedPane(JTabbedPane.TOP);
		
		for(ProfileType type : ProfileType.values()){
			ProfileDisplayPanel panel = new ProfileDisplayPanel(programLogger, type);
			profilePanels.put(type, panel);
			this.addSubPanel(panel);
			profilesTabPanel.addTab(type.toString(), null, panel, null);
		}
		
		/*
		 * Create the other profile panels
		 */
		
		modalityDisplayPanel  = new ModalityDisplayPanel();		
		variabilityChartPanel = new VariabilityDisplayPanel(programLogger);
		this.addSubPanel(variabilityChartPanel);
		
		profilesTabPanel.addTab("Variability", null, variabilityChartPanel, null);
		profilesTabPanel.addTab("Modality", null, modalityDisplayPanel, null);
		this.add(profilesTabPanel, BorderLayout.CENTER);

	}
	
	@Override
	protected void updateSingle() throws Exception {
		updateMultiple();
	}
	
	@Override
	protected void updateMultiple() throws Exception {
		
		for(ProfileType type : profilePanels.keySet()){
			profilePanels.get(type).update(getDatasets());
			programLogger.log(Level.FINEST, "Updated "+type.toString()+" profile panel");
		}
		
		variabilityChartPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updated variabililty panel");
		
		modalityDisplayPanel.update(getDatasets());
		programLogger.log(Level.FINEST, "Updated modality panel");
	}
	
	@Override
	protected void updateNull() throws Exception {
		updateMultiple();
	}
	
	private class ModalityDisplayPanel extends JPanel implements ActionListener {
		
		private JPanel mainPanel = new JPanel(new BorderLayout());
		private JList<String> pointList;
		private ExportableChartPanel chartPanel;
		private ExportableChartPanel modalityProfileChartPanel; // hold a chart showing p-values across the profile
		private ProfileCollectionTypeSettingsPanel profileCollectionTypeSettingsPanel = new ProfileCollectionTypeSettingsPanel();
		private JPanel buttonPanel = new JPanel(new FlowLayout());
		
		public ModalityDisplayPanel(){
			this.setLayout(new BorderLayout());
			
			profileCollectionTypeSettingsPanel.addActionListener(this);
			profileCollectionTypeSettingsPanel.setEnabled(false);;
			buttonPanel.add(profileCollectionTypeSettingsPanel);
			this.add(buttonPanel, BorderLayout.NORTH);
			
			chartPanel = createPositionChartPanel();
//			chartPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			modalityProfileChartPanel = createModalityProfileChartPanel();
			
			mainPanel.add(chartPanel, BorderLayout.WEST);
			mainPanel.add(modalityProfileChartPanel, BorderLayout.CENTER);
//			mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			
			this.add(mainPanel, BorderLayout.CENTER);
			
			DecimalFormat df = new DecimalFormat("#0.00");
			pointList = new JList<String>();
			DefaultListModel<String> model = new DefaultListModel<String>();
			for(Double d=0.0; d<=100; d+=0.5){
				model.addElement(df.format(d));
			}
			pointList.setModel(model);
			JScrollPane listPanel = new JScrollPane(pointList);
//			listPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
			this.add(listPanel, BorderLayout.WEST);
			pointList.addListSelectionListener(new ModalitySelectionListener());
			pointList.setEnabled(false);
			
		}
		
		
		public void setEnabled(boolean b){
			profileCollectionTypeSettingsPanel.setEnabled(b);
			pointList.setEnabled(b);
		}
		
		private ExportableChartPanel createPositionChartPanel(){
			
			JFreeChart chart = createPositionChart();
			ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			return chartPanel;
		}
		
		private JFreeChart createPositionChart(){
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					"Probability", "Angle", null);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.getDomainAxis().setRange(0,360);
			plot.addDomainMarker(new ValueMarker(180, Color.BLACK, new BasicStroke(2f)));
			return chart;
		}
		
		private ExportableChartPanel createModalityProfileChartPanel(){
			
			JFreeChart chart = createModalityProfileChart();
			ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			return chartPanel;
		}
		
		private JFreeChart createModalityProfileChart(){
			JFreeChart chart = ChartFactory.createXYLineChart(null,
					"Position", "Probability", null);
			XYPlot plot = chart.getXYPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.getDomainAxis().setRange(0,100);
			plot.getRangeAxis().setRange(0,1);
			return chart;
		}
		
		public void update(List<AnalysisDataset> list) throws Exception {

			try{
				if( list!=null && !list.isEmpty()){

					this.setEnabled(true);

					ProfileType type = profileCollectionTypeSettingsPanel.getSelected();

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
					modalityProfileChartPanel.setChart(createModalityProfileChart());
					chartPanel.setChart(createPositionChart());
					this.setEnabled(false);
				}

			} catch(Exception e){
				update( (List<AnalysisDataset>) null);
			}
		}

		public void updateModalityProfileChart(List<AnalysisDataset> list){
			
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();
			
			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(getDatasets())
				.setLogger(programLogger)
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(BorderTag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType(type)
				.build();
						
			JFreeChart chart = null;
			try {
				chart = MorphologyChartFactory.createModalityProfileChart(options);
				modalityProfileChartPanel.setChart(chart);
			} catch (Exception e){
				programLogger.log(Level.SEVERE, "Error updating modality profiles panel", e);
			}
		}
		
		public void updateChart(double xvalue){
			
			ProfileType type = profileCollectionTypeSettingsPanel.getSelected();
			
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

}
