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

import ij.IJ;

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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.BoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import utility.Constants.BorderTag;
import charting.charts.MorphologyChartFactory;
import charting.datasets.NucleusDatasetCreator;
import charting.datasets.NucleusTableDatasetCreator;
import components.CellCollection;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;
import analysis.AnalysisDataset;

public class SegmentsDetailPanel extends DetailPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

//	private List<AnalysisDataset> list;
	protected AnalysisDataset activeDataset;
		
	private SegmentStatsPanel 		segmentStatsPanel;		// Hold the start and end points of each segment
	private SegmentProfilePanel		segmentProfilePanel;	// draw the segments on the median profile
	private SegmentBoxplotsPanel 	segmentBoxplotsPanel;	// draw boxplots of segment lengths
	
	
	public SegmentsDetailPanel() {
			
		this.setLayout(new BorderLayout());
		
		JPanel panel = new JPanel(new GridBagLayout());
		
		Dimension minimumChartSize = new Dimension(100, 100);
		segmentProfilePanel  = new SegmentProfilePanel();
		segmentProfilePanel.setMinimumSize(minimumChartSize);
		
		segmentBoxplotsPanel = new SegmentBoxplotsPanel();
		segmentBoxplotsPanel.setMinimumSize(minimumChartSize);
		
		segmentStatsPanel = new SegmentStatsPanel();
		segmentStatsPanel.setMinimumSize(minimumChartSize);
		
		
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
		panel.add(segmentBoxplotsPanel, constraints);
		
		this.add(panel, BorderLayout.CENTER);
		
		
	}
		
	public void update(List<AnalysisDataset> list){
		
		if(list!=null && !list.isEmpty()){
			segmentBoxplotsPanel.update(list); // get segname from panel
			segmentProfilePanel.update(list, null, false, false); // get segname from panel
			segmentStatsPanel.update(list);
		}
	}
		
	@Override
	public void actionPerformed(ActionEvent arg0) {
		
	}
	
	/**
	 * Allows for cell background to be coloured based on position in a list. Used to colour
	 * the segment stats list
	 *
	 */
	private class SegmentTableCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
		
		private static final long serialVersionUID = 1L;

		public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, java.lang.Object value, boolean isSelected, boolean hasFocus, int row, int column) {

			// default cell colour is white
			Color colour = Color.WHITE;
			
			final String colName = table.getColumnName(column); // will be Seg_x
			
			// only apply to first row, after the first column
			if(column>0 && row==0){
//				String colName = table.getColumnName(column); // will be Seg_x

				int segment = Integer.valueOf(colName.replace("Seg_", ""));

				colour = activeDataset.getSwatch().color(segment);
//				colour = ColourSelecter.getOptimisedColor(segment);
			}
						
			//Cells are by default rendered as a JLabel.
			JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			l.setBackground(colour);

			//Return the JLabel which renders the cell.
			return l;
		}
	}
	
	@SuppressWarnings("serial")
	protected class SegmentProfilePanel extends JPanel {
		
		private ChartPanel chartPanel; // for displaying the legnth of a given segment
		private JPanel buttonsPanel;
		
		protected SegmentProfilePanel(){
			
			this.setLayout(new BorderLayout());
			Dimension minimumChartSize = new Dimension(50, 100);
			Dimension preferredChartSize = new Dimension(400, 300);
			
			JFreeChart profileChart = MorphologyChartFactory.makeEmptyProfileChart();
			chartPanel= MorphologyChartFactory.makeProfileChartPanel(profileChart);
			
			chartPanel.setMinimumSize(minimumChartSize);
			chartPanel.setPreferredSize(preferredChartSize);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			this.add(chartPanel, BorderLayout.CENTER);
			
			buttonsPanel = makeButtonPanel();
			buttonsPanel.setEnabled(false);
			buttonsPanel.setVisible(false);
			this.add(buttonsPanel, BorderLayout.NORTH);
			
		}
		
		private JPanel makeButtonPanel(){
			
			JPanel panel = new JPanel(new FlowLayout()){
				@Override
				public void setEnabled(boolean b){
					super.setEnabled(b);
					for(Component c : this.getComponents()){
						c.setEnabled(b);
					}
				}
			};
			JButton mergeButton = new JButton("Merge segments");
			
			mergeButton.addActionListener( new ActionListener(){

				@Override
				public void actionPerformed(ActionEvent e) {
					
					// Goal: show a list of segments that can be merged, and merge the selected option
//					IJ.log("Merge clicked");
					CellCollection collection = activeDataset.getCollection();

					try {
						SegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(collection.getOrientationPoint());
						List<String> names = new ArrayList<String>();
						
						// Put the names of the mergable segments into a list
						for(NucleusBorderSegment seg : medianProfile.getSegments()){
							String mergeName = seg.getName()+" - "+seg.nextSegment().getName();
							names.add(mergeName);
						}
						String[] nameArray = names.toArray(new String[0]);
						
						String mergeOption = (String) JOptionPane.showInputDialog(null, 
								"Choose segments to merge",
								"Merge",
								JOptionPane.QUESTION_MESSAGE, 
								null, 
								nameArray, 
								nameArray[0]);
						
						if(mergeOption!=null){
							// a choice was made
							String[] segs = mergeOption.split(" - "); // split back up to seg names
							
							mergeSegments(segs[0], segs[1]);
							
							List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
							list.add(activeDataset);
							SegmentsDetailPanel.this.update(list);
							SegmentsDetailPanel.this.fireSignalChangeEvent("UpdatePanels");
						}
						
					} catch (Exception e1) {
						error("Error merging segments", e1);
					}
					
				}
			});
			
			panel.add(mergeButton);
			return panel;
			
			
		}
		
		private void mergeSegments(String segName1, String segName2) throws Exception {
			
			CellCollection collection = activeDataset.getCollection();
			
			SegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(collection.getOrientationPoint());
			
			// Get the segments to merge
			NucleusBorderSegment seg1 = medianProfile.getSegment(segName1);
			NucleusBorderSegment seg2 = medianProfile.getSegment(segName2);
			
			// merge the two segments in the median - this is only a copy of the profile collection
			medianProfile.mergeSegments(seg1, seg2);
			
			// put the new segment pattern back with the appropriate offset
			collection.getProfileCollection().addSegments( collection.getOrientationPoint(),  medianProfile.getSegments());

			/*
			 * With the median profile segments merged, also merge the segments
			 * in the individual nuclei
			 */
			for(Nucleus n : collection.getNuclei()){

				SegmentedProfile profile = n.getAngleProfile(n.getOrientationPoint());
				NucleusBorderSegment nSeg1 = profile.getSegment(segName1);
				NucleusBorderSegment nSeg2 = profile.getSegment(segName2);
				profile.mergeSegments(nSeg1, nSeg2);
				n.setAngleProfile(profile, n.getOrientationPoint());
			}
			
			/*
			 * Update the consensus if present
			 */
			if(collection.hasConsensusNucleus()){
				ConsensusNucleus n = collection.getConsensusNucleus();
				SegmentedProfile profile = n.getAngleProfile(collection.getOrientationPoint());
				NucleusBorderSegment nSeg1 = profile.getSegment(segName1);
				NucleusBorderSegment nSeg2 = profile.getSegment(segName2);
				profile.mergeSegments(nSeg1, nSeg2);
				n.setAngleProfile(profile, n.getOrientationPoint());
			}
		}
		
		public void update(List<AnalysisDataset> list, String segName, boolean normalised, boolean rightAlign){
			
			DefaultXYDataset ds = null;
			
			try {
				
				if(list==null || list.isEmpty()){
					
					buttonsPanel.setEnabled(false);
					buttonsPanel.setVisible(false);
					
				} else if(list.size()==1){
				
					buttonsPanel.setEnabled(true);
					buttonsPanel.setVisible(true);

					if(normalised){
						ds = NucleusDatasetCreator.createMultiProfileSegmentDataset(list, segName);
					} else {
						ds = NucleusDatasetCreator.createRawMultiProfileSegmentDataset(list, segName, rightAlign);
					}

					JFreeChart chart = null;
					if(normalised){
						chart = MorphologyChartFactory.makeProfileChart(ds, 100, list.get(0).getSwatch());
					} else {
						int length = 100;
						for(AnalysisDataset d : list){
							if(   (int) d.getCollection().getMedianArrayLength()>length){
								length = (int) d.getCollection().getMedianArrayLength();
							}
						}
						chart = MorphologyChartFactory.makeProfileChart(ds, length, list.get(0).getSwatch());
					}								
					chartPanel.setChart(chart);
				} else {
					
					// Multiple datasets
					buttonsPanel.setEnabled(false);
					buttonsPanel.setVisible(false);
					
//					String point = list.get(0).getCollection().getPoint(BorderTag.ORIENTATION_POINT);
					// many profiles, colour them all the same
					List<XYSeriesCollection> iqrProfiles = NucleusDatasetCreator.createMultiProfileIQRDataset(list, normalised, rightAlign, BorderTag.ORIENTATION_POINT);				
					XYDataset medianProfiles			 = NucleusDatasetCreator.createMultiProfileDataset(	  list, normalised, rightAlign, BorderTag.ORIENTATION_POINT);
									
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
				error("Error in plotting segment profile", e);
			} 
		}
	}
	
	@SuppressWarnings("serial")
	protected class SegmentBoxplotsPanel extends JPanel {
		private ChartPanel chartPanel; // for displaying the legnth of a given segment
		
		protected SegmentBoxplotsPanel(){
			
			this.setLayout(new BorderLayout());
			
			JFreeChart boxplot = MorphologyChartFactory.makeEmptyBoxplot();

			chartPanel = new ChartPanel(boxplot);
			this.add(chartPanel, BorderLayout.CENTER);
			
		}
		
		
		public void update(List<AnalysisDataset> list){
			try{

				if(list.size()==1){

					BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSegmentVariabillityDataset(list);
					JFreeChart boxplotChart = MorphologyChartFactory.makeSegmentBoxplot(ds, list);
					chartPanel.setChart(boxplotChart);

				} else {
					

					BoxAndWhiskerCategoryDataset ds = NucleusDatasetCreator.createSegmentVariabillityDataset(list);
					JFreeChart boxplotChart = MorphologyChartFactory.makeSegmentBoxplot(ds, list);
					chartPanel.setChart(boxplotChart);

				}
			} catch (Exception e){
				error("Error updating segments boxplot", e);
				JFreeChart boxplotChart = MorphologyChartFactory.makeEmptyBoxplot();
				chartPanel.setChart(boxplotChart);
				
			}
		}
	}
	
	protected class SegmentStatsPanel extends JPanel {
		
		private static final long serialVersionUID = 1L;
		private JTable table; // individual cell stats
		
		private JScrollPane scrollPane;
				
		protected SegmentStatsPanel(){
			
			this.setLayout(new BorderLayout());
			
			scrollPane = new JScrollPane();
						
			try {
				table = new JTable(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(null));
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
							
							CellCollection collection = activeDataset.getCollection();
							if(columnName.startsWith("Seg_")){
								
								try {
									SegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(collection.getReferencePoint());
									NucleusBorderSegment seg = profile.getSegment(columnName);
									
//									if(rowName.equals("Start index")){
//										SpinnerNumberModel sModel 
//										= new SpinnerNumberModel(seg.getStartIndex(), 
//												0, 
//												profile.size(),
//												1);
//										JSpinner spinner = new JSpinner(sModel);
//
//										int option = JOptionPane.showOptionDialog(null, 
//												spinner, 
//												"Choose the new segment start index", 
//												JOptionPane.OK_CANCEL_OPTION, 
//												JOptionPane.QUESTION_MESSAGE, null, null, null);
//										if (option == JOptionPane.CANCEL_OPTION) {
//											// user hit cancel
//										} else if (option == JOptionPane.OK_OPTION)	{
//											
//											int index = (Integer) spinner.getModel().getValue();
//											if(profile.update(seg, index, seg.getEndIndex())){
////											if(seg.update(index, seg.getEndIndex())){
//												collection.getProfileCollection().addSegments(collection.getReferencePoint(), profile.getSegments());
//												update(activeDataset);
//											} else {
//												log("Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
//											}
//											
//
//										}
//									}
									
//									if(rowName.equals("End index")){
////										SpinnerNumberModel sModel 
////										= new SpinnerNumberModel(seg.getEndIndex(), 
////												seg.getStartIndex()+NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH, 
////												seg.nextSegment().getEndIndex()-NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH,
////												1);
//										SpinnerNumberModel sModel 
//										= new SpinnerNumberModel(seg.getEndIndex(), 
//												0, 
//												profile.size(),
//												1);
//										JSpinner spinner = new JSpinner(sModel);
//
//										int option = JOptionPane.showOptionDialog(null, 
//												spinner, 
//												"Choose the new segment end index", 
//												JOptionPane.OK_CANCEL_OPTION, 
//												JOptionPane.QUESTION_MESSAGE, null, null, null);
//										if (option == JOptionPane.CANCEL_OPTION) {
//											// user hit cancel
//										} else if (option == JOptionPane.OK_OPTION)	{
//											
//											
//											
//											int index = (Integer) spinner.getModel().getValue();
//											if(profile.update(seg, seg.getStartIndex(), index)){
//												collection.getProfileCollection().addSegments(collection.getReferencePoint(), profile.getSegments());
//												update(activeDataset);
//											} else {
//												log("Updating "+seg.getEndIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
//											}
//											
//
//										}
//									}
									
									
									
								} catch (Exception e1) {
									error("Error getting segment", e1);
								}
							}
						}
					}
				});
			} catch (Exception e) {
				error("Error in segment table", e);
			}
			table.setEnabled(false);
						
			scrollPane.setViewportView(table);
			scrollPane.setColumnHeaderView(table.getTableHeader());
			
			this.add(scrollPane, BorderLayout.CENTER);
		}
		
		protected void update(List<AnalysisDataset> list){
			
			if(list!=null){

				if(list.isEmpty()){

					try {
						table.setModel(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(null));
					} catch (Exception e) {
						error("Error updating segment stats panel", e);
					}
				} else {
					
					if(list.size()==1){
						this.setVisible(true);
						activeDataset = list.get(0);
						try {
							table.setModel(NucleusTableDatasetCreator.createMedianProfileSegmentStatsTable(activeDataset));
						} catch (Exception e) {
							error("Error updating segment stats panel", e);
						}

						Enumeration<TableColumn> columns = table.getColumnModel().getColumns();

						while(columns.hasMoreElements()){
							TableColumn column = columns.nextElement();
							column.setCellRenderer(new SegmentTableCellRenderer());
						}
					} else {
						this.setVisible(false);
					}
					
				}
			}
		}
	}
}
