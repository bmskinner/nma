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

import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.DraggableOverlayChartPanel;
import gui.components.ExportableTable;
import gui.components.HistogramsTabPanel;
import gui.components.MeasurementUnitSettingsPanel;
import gui.components.SelectableChartPanel;
import gui.components.ProfileAlignmentOptionsPanel.ProfileAlignment;
import stats.SegmentStatistic;
import utility.Constants;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.charts.BoxplotChartFactory;
import charting.charts.HistogramChartFactory;
import charting.charts.HistogramChartOptions;
import charting.charts.MorphologyChartFactory;
import charting.charts.ProfileChartOptions;
import charting.datasets.NucleusTableDatasetCreator;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.BorderTag.BorderTagType;
import components.generic.MeasurementScale;
import components.generic.ProfileCollectionType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

public class SegmentsDetailPanel extends DetailPanel {

	private static final long serialVersionUID = 1L;
		
	private SegmentStatsPanel 		segmentStatsPanel;		// Hold the start and end points of each segment
	private SegmentProfilePanel		segmentProfilePanel;	// draw the segments on the median profile
	private SegmentBoxplotsPanel 	segmentBoxplotsPanel;	// draw boxplots of segment lengths
	private SegmentHistogramsPanel 	segmentHistogramsPanel;	// draw boxplots of segment lengths
	
	private JTabbedPane 			tabPanel;
	
	private MeasurementUnitSettingsPanel measurementUnitSettingsPanel = new MeasurementUnitSettingsPanel() ;
	
	
	public SegmentsDetailPanel(Logger programLogger) {
		super(programLogger);
		this.setLayout(new BorderLayout());
		
		tabPanel = new JTabbedPane(JTabbedPane.TOP);
		tabPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel panel = new JPanel(new GridBagLayout());
		
		Dimension minimumChartSize = new Dimension(100, 100);
		segmentProfilePanel  = new SegmentProfilePanel();
		segmentProfilePanel.setMinimumSize(minimumChartSize);
		segmentProfilePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		segmentBoxplotsPanel = new SegmentBoxplotsPanel();
		segmentBoxplotsPanel.setMinimumSize(minimumChartSize);
		tabPanel.addTab("Boxplots", segmentBoxplotsPanel);
		
		
		segmentHistogramsPanel = new SegmentHistogramsPanel(programLogger);
		segmentHistogramsPanel.setMinimumSize(minimumChartSize);
		tabPanel.addTab("Histograms", segmentHistogramsPanel);
		
		segmentStatsPanel = new SegmentStatsPanel();
		segmentStatsPanel.setMinimumSize(minimumChartSize);
		segmentStatsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.weightx = 1;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.CENTER;
		
		panel.add(segmentStatsPanel, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.weighty = 1;
		panel.add(segmentProfilePanel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		panel.add(tabPanel, constraints);
		
		this.add(panel, BorderLayout.CENTER);
		
		
	}
		
	@Override
	public void updateDetail(){
//		this.list = list;
		programLogger.log(Level.FINE, "Updating segments detail panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(getDatasets()!=null && !getDatasets().isEmpty()){
					segmentBoxplotsPanel.update(getDatasets()); // get segname from panel
					programLogger.log(Level.FINEST, "Updated segments boxplot panel");
					
					segmentHistogramsPanel.update(getDatasets()); // get segname from panel
					programLogger.log(Level.FINEST, "Updated segments histogram panel");
					
					segmentProfilePanel.update(getDatasets()); // get segname from panel
					programLogger.log(Level.FINEST, "Updated segments profile panel");
					
					segmentStatsPanel.update(getDatasets());
					programLogger.log(Level.FINEST, "Updated segments stats panel");
				}
				setUpdating(false);
			}
		});
	}
			
	/**
	 * Allows for cell background to be coloured based on position in a list. Used to colour
	 * the segment stats list
	 *
	 */
	private class SegmentTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
		
		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			// default cell colour is white
			Color colour = Color.WHITE;
			
			final String colName = table.getColumnName(column); // will be Seg_x
			
			// only apply to first row, after the first column
			if(column>0 && row==0){
				
				int segment;
		        try {
		        	segment = Integer.valueOf(colName.replace("Seg_", ""));
		        } catch (Exception e){
		        	programLogger.log(Level.FINEST, "Error getting segment name: "+colName);
		        	segment = 0;
		        }

				ColourSwatch swatch = activeDataset().getSwatch() == null ? ColourSwatch.REGULAR_SWATCH : activeDataset().getSwatch();
				colour = swatch.color(segment);
				programLogger.log(Level.FINEST, "SegmentTableCellRenderer for segment "+segment+" uses color "+colour);

			}
			
			String rowName = (String) table.getModel().getValueAt(row, 0);
			if(rowName.equals("Length p(unimodal)") && column > 0){

				String cellContents = l.getText();
				
				double pval;
		        try {
		        	pval = Double.valueOf(cellContents);
		        } catch (Exception e){
		        	programLogger.log(Level.FINEST, "Error getting value: "+cellContents+" in column "+colName);
		        	pval = 1;
		        }
				
				if(  pval < Constants.FIVE_PERCENT_SIGNIFICANCE_LEVEL){
					colour = Color.YELLOW;
				}
				if(  pval < Constants.ONE_PERCENT_SIGNIFICANCE_LEVEL){
					colour = Color.GREEN;
				}
				
			}
						
			
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}
	
	@SuppressWarnings("serial")
	public class SegmentProfilePanel extends JPanel implements ActionListener, SignalChangeListener {
		
		private ChartPanel chartPanel; // for displaying the legnth of a given segment
		
		protected SegmentProfilePanel(){
			
			this.setLayout(new BorderLayout());
			Dimension minimumChartSize = new Dimension(50, 100);
			Dimension preferredChartSize = new Dimension(400, 300);
			
			JFreeChart profileChart = MorphologyChartFactory.makeEmptyProfileChart();
//			chartPanel = new DraggableOverlayChartPanel(profileChart, null);
			chartPanel= MorphologyChartFactory.makeProfileChartPanel(profileChart);
			
			chartPanel.setMinimumSize(minimumChartSize);
			chartPanel.setPreferredSize(preferredChartSize);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			this.add(chartPanel, BorderLayout.CENTER);

			
		}
		
		
		public void update(List<AnalysisDataset> list){
			
			try {
				JFreeChart chart = null;
				SegmentedProfile profile = null;
				if(list==null || list.isEmpty()){
					
					chart = MorphologyChartFactory.makeEmptyProfileChart();
					
					
				} else {
					
					ProfileChartOptions options = new ProfileChartOptions(list, true, ProfileAlignment.LEFT, BorderTag.REFERENCE_POINT, false, ProfileCollectionType.REGULAR);
					
					if(getChartCache().hasChart(options)){
						chart = getChartCache().getChart(options);
					} else {
						chart = MorphologyChartFactory.makeMultiSegmentedProfileChart(options);
						
						getChartCache().addChart(options, chart);
					}
					
					// Set the button configuration
//					configureButtons(options);
					
					if(isSingleDataset()){
						profile = activeDataset().getCollection()
								.getProfileCollection(ProfileCollectionType.REGULAR)
								.getSegmentedProfile(BorderTag.REFERENCE_POINT);
//								.interpolate((int) activeDataset().getCollection().getMedianArrayLength());
					}
				} 
				
//				chartPanel.setChart(chart, profile);
				chartPanel.setChart(chart);
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error in plotting segment profile", e);
				chartPanel.setChart(MorphologyChartFactory.makeEmptyProfileChart());
			} 
		}
		
		@Override
		public void signalChangeReceived(SignalChangeEvent event) {	
			
		}
		
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
		}
	}
	
	@SuppressWarnings("serial")
	protected class SegmentBoxplotsPanel extends JPanel implements ActionListener {
//		private ChartPanel chartPanel; // for displaying the legnth of a given segment
//		private JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP); // switch between chart types
		private JPanel 		mainPanel; // hold the charts
		private Dimension preferredSize = new Dimension(200, 300);
		private JScrollPane scrollPane;
		private JPanel 		buttonPanel;
				
		protected SegmentBoxplotsPanel(){
			
			this.setLayout(new BorderLayout());
//			this.add(tabPane, BorderLayout.CENTER);
			
			JFreeChart boxplot = BoxplotChartFactory.makeEmptyBoxplot();
			

			ChartPanel chartPanel = new ChartPanel(boxplot);
			chartPanel.setPreferredSize(preferredSize);
			
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
			mainPanel.add(chartPanel);
			
			scrollPane = new JScrollPane(mainPanel);
			
			this.add(scrollPane, BorderLayout.CENTER);
			
			buttonPanel = new JPanel(new FlowLayout());
			measurementUnitSettingsPanel.addActionListener(this);
			buttonPanel.add(measurementUnitSettingsPanel);
			
			this.add(buttonPanel, BorderLayout.NORTH);
			
		}
		
		
		public void update(List<AnalysisDataset> list){
			
			try{
				
				mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
				
				MeasurementScale scale = measurementUnitSettingsPanel.getSelected();
				
				if(!list.isEmpty()){
					
					programLogger.log(Level.FINEST, "Dataset list is not empty");
			
					// Check that all the datasets have the same number of segments
					if(checkSegmentCountsMatch(list)){ // make a boxplot for each segment
						
//						for(SegmentStatistic stat : SegmentStatistic.values()){
						
							CellCollection collection = list.get(0).getCollection();
							int segmentCount = collection.getProfileCollection(ProfileCollectionType.REGULAR)
									.getSegmentedProfile(BorderTag.ORIENTATION_POINT)
									.getSegmentCount();
							
							// Get each segment as a boxplot
							for( int i=0; i<segmentCount; i++){
								String segName = "Seg_"+i;
								JFreeChart boxplot = BoxplotChartFactory.makeSegmentBoxplot(segName, list, scale, SegmentStatistic.LENGTH);
								ChartPanel chartPanel = new ChartPanel(boxplot);
								chartPanel.setPreferredSize(preferredSize);
								mainPanel.add(chartPanel);							
							}
						
//						}
						
					} else { // different number of segments, blank chart
						mainPanel.add(new JLabel("Segment number is not consistent across datasets"));
					}
					mainPanel.revalidate();
					mainPanel.repaint();
					scrollPane.setViewportView(mainPanel);
					
				}

			} catch (Exception e){
				programLogger.log(Level.SEVERE, "Error updating segments boxplot", e);		
				mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
				mainPanel.add(new JLabel("Unable to display segment boxplots"));
				scrollPane.setViewportView(mainPanel);
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			update(getDatasets());
			
		}
	}
	
	/**
	 * Given a list of datasets, count the segments in the median profile of each, 
	 * and test if all datasets have the same number of segments.
	 * @param list
	 * @return
	 * @throws Exception
	 */
	private boolean checkSegmentCountsMatch(List<AnalysisDataset> list) throws Exception{
		int prevCount = 0;
		
		programLogger.log(Level.FINEST, "Counting segments in each dataset");
		// check that the datasets have the same number of segments
		for( AnalysisDataset dataset  : list){
			CellCollection collection = dataset.getCollection();
			int count = collection.getProfileCollection(ProfileCollectionType.REGULAR)
				.getSegmentedProfile(BorderTag.ORIENTATION_POINT)
				.getSegmentCount();
			
			programLogger.log(Level.FINEST, "\t"+dataset.getName()+": "+count+" segments");
			
			if(prevCount > 0 ){
				if(prevCount!=count){
					programLogger.log(Level.FINEST, "Segment count does not match");
					return false;
				}
			}
			prevCount = count;
		}
		return true;
	}
	
	@SuppressWarnings("serial")
	protected class SegmentHistogramsPanel extends HistogramsTabPanel  {
		
		private Dimension preferredSize = new Dimension(200, 100);
		
		public SegmentHistogramsPanel(Logger programLogger){
			super(programLogger);
			
			JFreeChart chart = HistogramChartFactory.createHistogram(null, "Segment", "Length");		
			SelectableChartPanel panel = new SelectableChartPanel(chart, "null");
			panel.setPreferredSize(preferredSize);
			SegmentHistogramsPanel.this.chartPanels.put("null", panel);
			SegmentHistogramsPanel.this.mainPanel.add(panel);
			
		}

		@Override
		public void updateDetail() {

			try{

				mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

				MeasurementScale scale = this.measurementUnitSettingsPanel.getSelected();
				boolean useDensity = this.useDensityPanel.isSelected();

				/*
				 * Do not use the chart cache, because the charts are created new every time
				 */
				
				if(!getDatasets().isEmpty()){

					programLogger.log(Level.FINEST, "Dataset list is not empty");

					// Check that all the datasets have the same number of segments
					if(checkSegmentCountsMatch(getDatasets())){ // make a histogram for each segment
						
//						JFreeChart chart = null;
						HistogramChartOptions options = new HistogramChartOptions(getDatasets(), null, scale, useDensity);
						options.setLogger(programLogger);

						CellCollection collection = activeDataset().getCollection();
						int segmentCount = collection.getProfileCollection(ProfileCollectionType.REGULAR)
								.getSegmentedProfile(BorderTag.ORIENTATION_POINT)
								.getSegmentCount();

						// Get each segment as a boxplot
						for( int i=0; i<segmentCount; i++){
							String segName = "Seg_"+i;
							JFreeChart chart = null;
							if(useDensity){
								chart = HistogramChartFactory.createSegmentLengthDensityChart(options, segName);	
							} else {
								chart = HistogramChartFactory.createSegmentLengthHistogram(options, segName);
							}
							ChartPanel chartPanel = new ChartPanel(chart);
							chartPanel.setPreferredSize(preferredSize);
							mainPanel.add(chartPanel);							
						}



					} else { // different number of segments, blank chart
						mainPanel.add(new JLabel("Segment number is not consistent across datasets"), JLabel.CENTER);
						scrollPane.setViewportView(mainPanel);
					}
					mainPanel.revalidate();
					mainPanel.repaint();
					scrollPane.setViewportView(mainPanel);
				}

			} catch (Exception e){
				programLogger.log(Level.SEVERE, "Error updating segments boxplot", e);		
				mainPanel = new JPanel();
				mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
				mainPanel.add(new JLabel("Unable to display segment boxplots"));
			} finally {
				SegmentHistogramsPanel.this.setUpdating(false);
			}
		}
		
		
	}
	
	protected class SegmentStatsPanel extends JPanel implements ActionListener {
		
		private static final long serialVersionUID = 1L;
		private ExportableTable table; // individual segment stats
				
		private JScrollPane scrollPane;
				
		protected SegmentStatsPanel(){
			
			this.setLayout(new BorderLayout());
			measurementUnitSettingsPanel.addActionListener(this);
			scrollPane = new JScrollPane();
						
			try {
				TableModel model = NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(null, MeasurementScale.PIXELS);
				table = new ExportableTable(model);
				table.addMouseListener(new MouseAdapter() {
					@Override
					public void mouseClicked(MouseEvent e) {
						
						JTable table = (JTable) e.getSource();
						int row = table.rowAtPoint(e.getPoint());
						String rowName = table.getModel().getValueAt(row, 0).toString();
						
						int column = table.columnAtPoint(e.getPoint());
						String columnName = table.getModel().getColumnName(column);
						
						// double click
						if (e.getClickCount() == 2) {
							
//							CellCollection collection = activeDataset().getCollection();
//							if(columnName.startsWith("Seg_")){
//								
//								try {
//									SegmentedProfile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegmentedProfile(BorderTag.REFERENCE_POINT);
//									NucleusBorderSegment seg = profile.getSegment(columnName);
//
//									
//								} catch (Exception e1) {
//									programLogger.log(Level.SEVERE, "Error getting segment", e1);
//								}
//							}
						}
					}
				});
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error in segment table", e);
			}
			table.setEnabled(false);
						
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			
			this.add(scrollPane, BorderLayout.CENTER);
		}

		protected void update(List<AnalysisDataset> list){

			MeasurementScale scale = measurementUnitSettingsPanel.getSelected();
			try {

				if(list.isEmpty()){
					programLogger.log(Level.FINEST, "Dataset list is empty: making null table");
					table.setToolTipText(null);
					table.setModel(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(null, scale));

				} else {

					if(list.size()==1){
						programLogger.log(Level.FINEST, "Single dataset selected");
						TableModel model = NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(activeDataset(), scale);
						table.setModel(model);
						table.setToolTipText(null);
						Enumeration<TableColumn> columns = table.getColumnModel().getColumns();

						while(columns.hasMoreElements()){
							TableColumn column = columns.nextElement();
							column.setCellRenderer(new SegmentTableCellRenderer());
						}

					} else {

						if(checkSegmentCountsMatch(list)){
							programLogger.log(Level.FINEST, "Multiple datasets selected");
							TableModel model = NucleusTableDatasetCreator.createMultiDatasetMedianProfileSegmentStatsTable(list, scale);
							table.setModel(model);
							table.setToolTipText("Mean and range for 95% confidence interval");
							Enumeration<TableColumn> columns = table.getColumnModel().getColumns();

							while(columns.hasMoreElements()){
								TableColumn column = columns.nextElement();
								column.setCellRenderer(new SegmentTableCellRenderer());
							}
						} else {
							programLogger.log(Level.FINEST, "Segment counts don't match");
							table.setToolTipText(null);
							table.setModel(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(null, scale));
						}
					}

				}

			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error updating segment stats panel", e);
				programLogger.log(Level.FINEST, "Error detected: making null table");
				try {
					table.setModel(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(null, scale));
				} catch (Exception e1) {
					programLogger.log(Level.SEVERE, "Error recovering from error in segment stats panel", e);
				}
			}
			

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			update(getDatasets());
			
		}
	}
}
