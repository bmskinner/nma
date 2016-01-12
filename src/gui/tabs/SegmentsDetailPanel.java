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

		JPanel panel = new JPanel(new GridBagLayout());
		
		Dimension minimumChartSize = new Dimension(100, 100);
		segmentProfilePanel  = new SegmentProfilePanel();
		segmentProfilePanel.setMinimumSize(minimumChartSize);
		
		segmentBoxplotsPanel = new SegmentBoxplotsPanel();
		segmentBoxplotsPanel.setMinimumSize(minimumChartSize);
		tabPanel.addTab("Boxplots", segmentBoxplotsPanel);
		
		segmentHistogramsPanel = new SegmentHistogramsPanel(programLogger);
		segmentHistogramsPanel.setMinimumSize(minimumChartSize);
		tabPanel.addTab("Histograms", segmentHistogramsPanel);
		
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
		
		private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
		private JPanel buttonsPanel;
		private JButton mergeButton;
		private JButton unmergeButton;
		private JButton splitButton;
		
		protected SegmentProfilePanel(){
			
			this.setLayout(new BorderLayout());
			Dimension minimumChartSize = new Dimension(50, 100);
			Dimension preferredChartSize = new Dimension(400, 300);
			
			JFreeChart profileChart = MorphologyChartFactory.makeEmptyProfileChart();
			chartPanel = new DraggableOverlayChartPanel(profileChart, null);
//			chartPanel= MorphologyChartFactory.makeProfileChartPanel(profileChart);
			
			chartPanel.setMinimumSize(minimumChartSize);
			chartPanel.setPreferredSize(preferredChartSize);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			chartPanel.addSignalChangeListener(this);
			this.add(chartPanel, BorderLayout.CENTER);
			
			buttonsPanel = makeButtonPanel();
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
			mergeButton = new JButton("Merge segments");
			mergeButton.addActionListener(this);
			
			panel.add(mergeButton);
			
			unmergeButton = new JButton("Unmerge segments");
			unmergeButton.addActionListener(this);
			
			panel.add(unmergeButton);
			
			splitButton = new JButton("Split segment");
			splitButton.addActionListener(this);

			panel.add(splitButton);
			
			return panel;
			
			
		}
		
		private void mergeSegments(String segName1, String segName2) throws Exception {
			
			CellCollection collection = activeDataset().getCollection();
			
			SegmentedProfile medianProfile = collection.getProfileCollection(ProfileCollectionType.REGULAR)
					.getSegmentedProfile(BorderTag.ORIENTATION_POINT);
			
			// Get the segments to merge
			NucleusBorderSegment seg1 = medianProfile.getSegment(segName1);
			NucleusBorderSegment seg2 = medianProfile.getSegment(segName2);
			
			// check the boundaries of the segment - we do not want to merge across the BorderTags
			boolean ok = true;
			for(BorderTag tag : BorderTag.values(BorderTagType.CORE)){
				
				/*
				 * Find the position of the border tag in the median profile
				 * 
				 */
				int offsetForOp = collection.getProfileCollection(ProfileCollectionType.REGULAR).getOffset(BorderTag.ORIENTATION_POINT);
				
				int offset = collection.getProfileCollection(ProfileCollectionType.REGULAR).getOffset(tag);
				
				// this should be zero for the orientation point and  totalLength+difference for the reference point
				int difference = offset - offsetForOp;

				if(seg2.getStartIndex()==seg2.getTotalLength()+difference || seg2.getStartIndex()==difference){
					ok=false;
//					IJ.log("Fail on "+tag+": OP: "+offsetForOp+" ; "+offset+" ;  difference "+difference+" ; seg2 start"+seg2.getStartIndex()); 
				}


			}
			
			if(ok){

				// merge the two segments in the median - this is only a copy of the profile collection
				medianProfile.mergeSegments(seg1, seg2);

				// put the new segment pattern back with the appropriate offset
				collection.getProfileCollection(ProfileCollectionType.REGULAR).addSegments( BorderTag.ORIENTATION_POINT,  medianProfile.getSegments());

				/*
				 * With the median profile segments merged, also merge the segments
				 * in the individual nuclei
				 */
				for(Nucleus n : collection.getNuclei()){

					SegmentedProfile profile = n.getAngleProfile(BorderTag.ORIENTATION_POINT);
					NucleusBorderSegment nSeg1 = profile.getSegment(segName1);
					NucleusBorderSegment nSeg2 = profile.getSegment(segName2);
					profile.mergeSegments(nSeg1, nSeg2);
					n.setAngleProfile(profile, BorderTag.ORIENTATION_POINT);
				}

				/*
				 * Update the consensus if present
				 */
				if(collection.hasConsensusNucleus()){
					ConsensusNucleus n = collection.getConsensusNucleus();
					SegmentedProfile profile = n.getAngleProfile(BorderTag.ORIENTATION_POINT);
					NucleusBorderSegment nSeg1 = profile.getSegment(segName1);
					NucleusBorderSegment nSeg2 = profile.getSegment(segName2);
					profile.mergeSegments(nSeg1, nSeg2);
					n.setAngleProfile(profile, BorderTag.ORIENTATION_POINT);
				}
				
				fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, getDatasets());
			} else {
				JOptionPane.showMessageDialog(this, "Cannot merge segments across core border tags");
			}
		}
		
		private void unmergeSegments(String segName) throws Exception {
			CellCollection collection = activeDataset().getCollection();
			
			SegmentedProfile medianProfile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegmentedProfile(BorderTag.ORIENTATION_POINT);
			
			// Get the segments to merge
			NucleusBorderSegment seg = medianProfile.getSegment(segName);
			
			// merge the two segments in the median - this is only a copy of the profile collection
			medianProfile.unmergeSegment(seg);
			
			// put the new segment pattern back with the appropriate offset
			collection.getProfileCollection(ProfileCollectionType.REGULAR).addSegments( BorderTag.ORIENTATION_POINT,  medianProfile.getSegments());

			/*
			 * With the median profile segments unmerged, also unmerge the segments
			 * in the individual nuclei
			 */
			for(Nucleus n : collection.getNuclei()){

				SegmentedProfile profile = n.getAngleProfile(BorderTag.ORIENTATION_POINT);
				NucleusBorderSegment nSeg = profile.getSegment(segName);
				profile.unmergeSegment(nSeg);
				n.setAngleProfile(profile, BorderTag.ORIENTATION_POINT);
			}
			
			/*
			 * Update the consensus if present
			 */
			if(collection.hasConsensusNucleus()){
				ConsensusNucleus n = collection.getConsensusNucleus();
				SegmentedProfile profile = n.getAngleProfile(BorderTag.ORIENTATION_POINT);
				NucleusBorderSegment nSeg1 = profile.getSegment(segName);
				profile.unmergeSegment(nSeg1);
				n.setAngleProfile(profile, BorderTag.ORIENTATION_POINT);
			}
			fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, getDatasets());
		}
		
		private boolean splitSegment(String segName) throws Exception {
			
			boolean result = false;
			
			CellCollection collection = activeDataset().getCollection();
			
			SegmentedProfile medianProfile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegmentedProfile(BorderTag.ORIENTATION_POINT);
			
			// Get the segments to merge
			NucleusBorderSegment seg = medianProfile.getSegment(segName);
			
			SpinnerNumberModel sModel 
			= new SpinnerNumberModel(seg.getStartIndex(), 
					0, 
					medianProfile.size(),
					1);
			JSpinner spinner = new JSpinner(sModel);

			int option = JOptionPane.showOptionDialog(null, 
					spinner, 
					"Choose the split index", 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (option == JOptionPane.CANCEL_OPTION) {
				
				// user hit cancel
				
				
			} else if (option == JOptionPane.OK_OPTION)	{
				
				try{

					int index = (Integer) spinner.getModel().getValue();
					if(seg.contains(index)){


						double proportion = seg.getIndexProportion(index);

						// merge the two segments in the median - this is only a copy of the profile collection
						medianProfile.splitSegment(seg, index);

						// put the new segment pattern back with the appropriate offset
						collection.getProfileCollection(ProfileCollectionType.REGULAR).addSegments( BorderTag.ORIENTATION_POINT,  medianProfile.getSegments());

						/*
						 * With the median profile segments unmerged, also split the segments
						 * in the individual nuclei. Requires proportional alignment
						 */
						for(Nucleus n : collection.getNuclei()){

							SegmentedProfile profile = n.getAngleProfile(BorderTag.ORIENTATION_POINT);
							NucleusBorderSegment nSeg = profile.getSegment(segName);

							int targetIndex = nSeg.getProportionalIndex(proportion);
							profile.splitSegment(nSeg, targetIndex);
							n.setAngleProfile(profile, BorderTag.ORIENTATION_POINT);
						}

						/*
						 * Update the consensus if present
						 */
						if(collection.hasConsensusNucleus()){
							ConsensusNucleus n = collection.getConsensusNucleus();
							SegmentedProfile profile = n.getAngleProfile(BorderTag.ORIENTATION_POINT);
							NucleusBorderSegment nSeg1 = profile.getSegment(segName);
							int targetIndex = nSeg1.getProportionalIndex(proportion);
							profile.splitSegment(nSeg1, targetIndex);
							n.setAngleProfile(profile, BorderTag.ORIENTATION_POINT);
						}
						fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, getDatasets());
						result = true;
					} else {
						SegmentsDetailPanel.this.programLogger.log(Level.WARNING, "Unable to split segment: index not present");
					}
				} catch(Exception e){
					SegmentsDetailPanel.this.programLogger.log(Level.SEVERE, "Error splitting segment", e);
				}
			}
			
			return result;
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
					configureButtons(options);
					
					if(isSingleDataset()){
						profile = activeDataset().getCollection()
								.getProfileCollection(ProfileCollectionType.REGULAR)
								.getSegmentedProfile(BorderTag.REFERENCE_POINT)
								.interpolate((int) activeDataset().getCollection().getMedianArrayLength());
					}
				} 
				
				chartPanel.setChart(chart, profile);
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error in plotting segment profile", e);
				chartPanel.setChart(MorphologyChartFactory.makeEmptyProfileChart());
				unmergeButton.setEnabled(false);
				mergeButton.setEnabled(false);
			} 
		}
		
		/**
		 * Enable or disable buttons depending on datasets selected
		 * @param options
		 * @throws Exception
		 */
		private void configureButtons(ProfileChartOptions options) throws Exception {
			if(options.isSingleDataset()){
				
				buttonsPanel.setEnabled(true);
				CellCollection collection = options.firstDataset().getCollection();
				SegmentedProfile medianProfile = collection.getProfileCollection(ProfileCollectionType.REGULAR)
						.getSegmentedProfile(BorderTag.ORIENTATION_POINT);
				
				// Don't allow merging below 2 segments (causes errors)
				if(medianProfile.getSegmentCount()<=2){
					mergeButton.setEnabled(false);
				} else {
					mergeButton.setEnabled(true);
				}
				
				// Check if there are any merged segments
				boolean hasMerges = false;
				for(NucleusBorderSegment seg : medianProfile.getSegments()){
					if(seg.hasMergeSources()){
						hasMerges = true;
					}
				}
				
				// If there are no merged segments, don't allow unmerging 
				if(hasMerges){
					unmergeButton.setEnabled(true);
				} else {
					unmergeButton.setEnabled(false);
				}
				
			} else { // multiple collections
				buttonsPanel.setEnabled(false);
			}
		}

		@Override
		public void signalChangeReceived(SignalChangeEvent event) {
			if(event.type().contains("UpdateSegment")){

				try{

					String[] array = event.type().split("\\|");
					String segName = array[1];
					String index = array[2];
					int indexValue = Integer.valueOf(index);

					// Update the median profile
					updateMedianProfileSegmentIndex(true , segName, indexValue); // DraggablePanel always uses seg start index
					
					
					// Make a dialog - update the morphology of each nucleus?
					Object[] options = { "Update nuclei" , "Do not update", };
					int result = JOptionPane.showOptionDialog(null, "Update the nuclei to the new boundaries?", "Update nuclei",

							JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

							null, options, options[0]);
					
					if(result==0){ // OK
						
						// TODO: Update each nucleus profile
						fireDatasetEvent(DatasetMethod.REFRESH_MORPHOLOGY, getDatasets());
						
					}
					

				} catch(Exception e){
					programLogger.log(Level.SEVERE, "Error updating segment", e);
				}

			}
			
			
		}
		
		private void updateMedianProfileSegmentIndex(boolean start, String segName, int index) throws Exception {
			
			// Get the median profile from the reference point
			
			SegmentedProfile oldProfile = activeDataset()
					.getCollection()
					.getProfileCollection(ProfileCollectionType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT);


			NucleusBorderSegment seg = oldProfile.getSegment(segName);

			int newStart = start ? index : seg.getStartIndex();
			int newEnd = start ? seg.getEndIndex() : index;

			 // Move the appropriate segment endpoint
			if(oldProfile.update(seg, newStart, newEnd)){
				
				// Replace the old segments in the median
				
				activeDataset()
				.getCollection()
				.getProfileCollection(ProfileCollectionType.REGULAR)
				.addSegments(BorderTag.REFERENCE_POINT, oldProfile.getSegments());
				
				fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
				update(getDatasets());
				
			} else {
				programLogger.log(Level.WARNING, "Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
			}
			
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				CellCollection collection = activeDataset().getCollection();
				SegmentedProfile medianProfile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegmentedProfile(BorderTag.ORIENTATION_POINT);
				List<String> names = new ArrayList<String>();

				if(e.getSource().equals(mergeButton)){

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
						list.add(activeDataset());
						SegmentsDetailPanel.this.update(list);
						SegmentsDetailPanel.this.fireSignalChangeEvent("UpdatePanels");
					}


				}

				if(e.getSource().equals(unmergeButton)){
					// show a list of segments that can be unmerged, and merge the selected option

					// Put the names of the mergable segments into a list
					for(NucleusBorderSegment seg : medianProfile.getSegments()){
						if(seg.hasMergeSources()){
							names.add(seg.getName());
						}							
					}
					String[] nameArray = names.toArray(new String[0]);

					String mergeOption = (String) JOptionPane.showInputDialog(null, 
							"Choose segments to unmerge",
							"Unmerge",
							JOptionPane.QUESTION_MESSAGE, 
							null, 
							nameArray, 
							nameArray[0]);

					if(mergeOption!=null){
						// a choice was made


						unmergeSegments(mergeOption);

						List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
						list.add(activeDataset());
						SegmentsDetailPanel.this.update(list);
						SegmentsDetailPanel.this.fireSignalChangeEvent("UpdatePanels");
					}

					
				}
				
				if(e.getSource().equals(splitButton)){
					// show a list of segments that can be split, and merge the selected option

					// Put the names of the mergable segments into a list
					for(NucleusBorderSegment seg : medianProfile.getSegments()){
							names.add(seg.getName());						
					}
					
					String[] nameArray = names.toArray(new String[0]);

					String option = (String) JOptionPane.showInputDialog(null, 
							"Choose segment to split",
							"Split",
							JOptionPane.QUESTION_MESSAGE, 
							null, 
							nameArray, 
							nameArray[0]);

					if(option!=null){
						// a choice was made


						if(splitSegment(option)){
							SegmentsDetailPanel.this.update(SegmentsDetailPanel.this.activeDatasetToList());
							SegmentsDetailPanel.this.fireSignalChangeEvent("UpdatePanels");
						}
					}

					
				}
			} catch (Exception e1) {
				programLogger.log(Level.SEVERE, "Error merging segments", e1);
			}
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
	protected class SegmentHistogramsPanel extends HistogramsTabPanel implements SignalChangeListener {
		
		private Dimension preferredSize = new Dimension(200, 100);
		
		public SegmentHistogramsPanel(Logger programLogger){
			super(programLogger);
			
			JFreeChart chart = HistogramChartFactory.createHistogram(null, "Segment", "Length");		
			SelectableChartPanel panel = new SelectableChartPanel(chart, "null");
			panel.setPreferredSize(preferredSize);
			panel.addSignalChangeListener(this);
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
		
		@Override
		public void signalChangeReceived(SignalChangeEvent event) {

			//TODO
//			if(event.type().equals("MarkerPositionUpdated")){
//				
//				SelectableChartPanel panel = (SelectableChartPanel) event.getSource();
//				filterByChartSelection(panel);
//				
//			}

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
							
							CellCollection collection = activeDataset().getCollection();
							if(columnName.startsWith("Seg_")){
								
								try {
									SegmentedProfile profile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegmentedProfile(BorderTag.REFERENCE_POINT);
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
									programLogger.log(Level.SEVERE, "Error getting segment", e1);
								}
							}
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
