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
package gui.tabs.editing;

import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.DraggableOverlayChartPanel;
import gui.components.PositionSelectionChartPanel;
import gui.components.RectangleOverlayObject;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.dialogs.AngleWindowSizeExplorer;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import analysis.profiles.SegmentFitter;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import components.Cell;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class SegmentsEditingPanel extends DetailPanel implements ActionListener, SignalChangeListener {
	
//	private final SegmentProfilePanel		segmentProfilePanel;	// draw the segments on the median profile
//		
//	public SegmentsEditingPanel() {
//		
//		super();
//		
//		this.setLayout(new BorderLayout());
//		segmentProfilePanel  = new SegmentProfilePanel();
//		this.addSubPanel(segmentProfilePanel);
//        this.add(segmentProfilePanel, BorderLayout.CENTER);
//
//	}
//	
//	@Override
//	protected void updateSingle() {
//		updateMultiple();
//	}
//	
//
//	@Override
//	protected void updateMultiple() {
//		segmentProfilePanel.update(getDatasets());
//		
//		
//	}
//	
//	@Override
//	protected void updateNull() {
//		updateMultiple();
//	}
//	
//	@Override
//	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
//		return null;
//	}
//	
//	@Override
//	protected TableModel createPanelTableType(TableOptions options) throws Exception{
//		return null;
//	}
//		
//	@Override
//	public void signalChangeReceived(SignalChangeEvent event) {
//		fireSignalChangeEvent(event);
//	}
//			
//	public class SegmentProfilePanel extends DetailPanel implements ActionListener, SignalChangeListener {
		
		private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
		private PositionSelectionChartPanel rangePanel; // a small chart to show the entire profile
		
		private JPanel buttonsPanel;
		private JButton mergeButton;
		private JButton unmergeButton;
		private JButton splitButton;
		private JButton windowSizeButton;
		private JButton updatewindowButton;
		
		private static final String STR_MERGE_SEGMENT     = "Hide segment boundary";
		private static final String STR_UNMERGE_SEGMENT   = "Unhide segment boundary";
		private static final String STR_SPLIT_SEGMENT     = "Split segment";
		private static final String STR_SET_WINDOW_SIZE   = "Set window size";
		private static final String STR_SHOW_WINDOW_SIZES = "Window sizes";
				
		public SegmentsEditingPanel(){
			super();
			this.setLayout(new BorderLayout());
			Dimension minimumChartSize = new Dimension(50, 100);
			Dimension preferredChartSize = new Dimension(400, 300);
			
			JFreeChart profileChart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);
			chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);
			
			chartPanel.setMinimumSize(minimumChartSize);
			chartPanel.setPreferredSize(preferredChartSize);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			chartPanel.addSignalChangeListener(this);
			this.add(chartPanel, BorderLayout.CENTER);
			
			buttonsPanel = makeButtonPanel();
			this.add(buttonsPanel, BorderLayout.NORTH);
			
			
			/*
			 * TESTING: A second chart panel at the south
			 * with a domain overlay crosshair to define the 
			 * centre of the zoomed range on the 
			 * centre chart panel 
			 */
			JFreeChart rangeChart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);
			rangePanel = new PositionSelectionChartPanel(rangeChart);
			rangePanel.setPreferredSize(minimumChartSize);
			rangePanel.addSignalChangeListener(this);

			this.add(rangePanel, BorderLayout.SOUTH);
			updateChartPanelRange();
			
			setButtonsEnabled(false);
			
			
		}
		
		@Override
		public void setAnalysing(boolean b){
			super.setAnalysing(b);
			chartPanel.setAnalysing(b);
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
			
			mergeButton = new JButton(STR_MERGE_SEGMENT);
			mergeButton.addActionListener(this);
			panel.add(mergeButton);
			
			unmergeButton = new JButton(STR_UNMERGE_SEGMENT);
			unmergeButton.addActionListener(this);
			panel.add(unmergeButton);
			
			splitButton = new JButton(STR_SPLIT_SEGMENT);
			splitButton.addActionListener(this);
			panel.add(splitButton);

			windowSizeButton = new JButton(STR_SHOW_WINDOW_SIZES);
			windowSizeButton.addActionListener(this);
			panel.add(windowSizeButton);
			
			updatewindowButton = new JButton(STR_SET_WINDOW_SIZE);
			updatewindowButton.addActionListener(this);
			panel.add(updatewindowButton);
			
			return panel;
			
			
		}
						
		@Override
		protected void updateSingle() {
			
			SegmentedProfile profile = null;
			
			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(BorderTag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType( ProfileType.REGULAR)
				.setSwatch(activeDataset().getSwatch())
				.setShowPoints(true)
				.build();
			
			// Set the button configuration
			configureButtons(options);
						
			JFreeChart chart = getChart(options);
			
			profile = activeDataset().getCollection()
					.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT);
			
			chartPanel.setChart(chart, profile, true);
			updateChartPanelRange();
			
			
			/*
			 * Create the chart for the range panel
			 */
			
			ChartOptions rangeOptions = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(BorderTag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType( ProfileType.REGULAR)
				.setSwatch(activeDataset().getSwatch())
				.setShowPoints(false)
				.build();
			
			JFreeChart rangeChart = getChart(rangeOptions);
			
			rangePanel.setChart(rangeChart);
		}
		
		/**
		 * Set the main chart panel domain range to centre on the 
		 * position in the range panel, +- 10
		 */
		private void updateChartPanelRange(){
//			double xValue = rangePanel.getDomainCrosshairPosition();
//			finest("Range panel crosshair is at "+xValue);
//			
//			double min = xValue-RANGE_WINDOW;
//			double max = xValue+RANGE_WINDOW;
//			chartPanel.getChart().getXYPlot().getDomainAxis().setRange(min, max);
			
			RectangleOverlayObject ob = rangePanel.getDomainRectangleOverlay();
			double min = ob.getMinValue();
			double max = ob.getMaxValue();
			chartPanel.getChart().getXYPlot().getDomainAxis().setRange(min, max);
		}
		

		@Override
		protected void updateMultiple() {
			updateNull();
			
		}
		
		@Override
		protected void updateNull() {			
			chartPanel.setChart(MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR));
			rangePanel.setChart(MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR));
			setButtonsEnabled(false);
		}
		
		@Override
		protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
			return MorphologyChartFactory.makeMultiSegmentedProfileChart(options);
		}
		
		@Override
		protected TableModel createPanelTableType(TableOptions options) throws Exception{
			return null;
		}
		

				
		/**
		 * Enable or disable buttons depending on datasets selected
		 * @param options
		 * @throws Exception
		 */
		private void configureButtons(ChartOptions options) {
			if(options.isSingleDataset()){
				
				setButtonsEnabled(true);
				CellCollection collection = options.firstDataset().getCollection();
				SegmentedProfile medianProfile = collection.getProfileCollection(ProfileType.REGULAR)
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
				setButtonsEnabled(false);
			}
		}
		
		public void setButtonsEnabled(boolean b){
			unmergeButton.setEnabled(b);
			mergeButton.setEnabled(b);
			splitButton.setEnabled(b);
			windowSizeButton.setEnabled(b);
			updatewindowButton.setEnabled(b);
		}

		@Override
		public void signalChangeReceived(SignalChangeEvent event) {
			if(event.type().contains("UpdateSegment") && event.getSource().equals(chartPanel)){
				finest("Heard update segment request");
				try{

					
					SegmentsEditingPanel.this.setAnalysing(true);

					String[] array = event.type().split("\\|");
					int segMidpointIndex = Integer.valueOf(array[1]);
					int index = Integer.valueOf(array[2]);
					
					UUID segID = activeDataset()
							.getCollection()
							.getProfileCollection(ProfileType.REGULAR)
							.getSegmentedProfile(BorderTag.REFERENCE_POINT)
							.getSegmentContaining(segMidpointIndex)
							.getID();
					updateSegmentStartIndex(segID, index);

				} catch(Exception e){
					log(Level.SEVERE, "Error updating segment", e);
				} finally {
					SegmentsEditingPanel.this.setAnalysing(false);
				}

			}
			
			
			// Change the range of the main chart based on the lower chart  
			if(event.type().contains("UpdatePosition") && event.getSource().equals(rangePanel)){
				
				updateChartPanelRange();
				
			}

		}
		
		private void updateSegmentStartIndex(UUID id, int index) throws Exception{

			// Update the median profile
			activeDataset()
				.getCollection()
				.getProfileManager()
				.updateMedianProfileSegmentIndex(true, id, index); // DraggablePanel always uses seg start index

			// Lock all the segments except the one to change
			activeDataset()
			.getCollection()
			.getProfileManager()
			.setLockOnAllNucleusSegmentsExcept(id, true);
							
			/*
			 * Invalidate the chart cache for the active dataset.
			 * This will force the morphology refresh to create a new chart
			 */
			finest("Clearing chart cache for editing panel");
			fireDatasetEvent(DatasetMethod.CLEAR_CACHE, getDatasets());
//			this.clearChartCache();
			
			
			//  Update each nucleus profile
			finest("Firing refresh morphology action");
			fireDatasetEvent(DatasetMethod.REFRESH_MORPHOLOGY, getDatasets());

			
			
		}
				
		private void updateCollectionWindowSize() throws Exception{
			int windowSizeMin = 1;
			int windowSizeMax = (int) activeDataset().getCollection().getMedianArrayLength();
			int windowSizeActual = activeDataset().getAnalysisOptions().getAngleProfileWindowSize();
			
			SpinnerNumberModel spinnerModel = new SpinnerNumberModel(windowSizeActual,
					windowSizeMin,
					windowSizeMax,
					1);
			JSpinner windowSizeSpinner = new JSpinner(spinnerModel);
			
			
			int option = JOptionPane.showOptionDialog(null, 
					windowSizeSpinner, 
					"Select new window size", 
					JOptionPane.OK_CANCEL_OPTION, 
					JOptionPane.QUESTION_MESSAGE, null, null, null);
			if (option == JOptionPane.CANCEL_OPTION) {
				return;

			} else if (option == JOptionPane.OK_OPTION)	{
				this.setAnalysing(true);
				int windowSize = (Integer) windowSizeSpinner.getModel().getValue();
				setCollectionWindowSize(windowSize);
				this.refreshChartCache();
				fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
				this.setAnalysing(false);
			}
			
		}
		
		private void setCollectionWindowSize(int windowSize) throws Exception{
			
			// Update cells
			
			for(Cell c : activeDataset().getCollection().getCells()){
				c.getNucleus().setAngleProfileWindowSize(windowSize);
			}

			// recalc the aggregate
			
			ProfileCollection pc = activeDataset().getCollection().getProfileCollection(ProfileType.REGULAR);			
			pc.createProfileAggregate(activeDataset().getCollection(), ProfileType.REGULAR);
					
			
			SegmentedProfile medianProfile = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);	
			
			// Does nothing, but needed to access segment fitter
//			DatasetSegmenter segmenter = new DatasetSegmenter(activeDataset(), MorphologyAnalysisMode.NEW, programLogger);
			
			// Make a fitter
			SegmentFitter fitter = new SegmentFitter(medianProfile);

			for(Cell c : activeDataset().getCollection().getCells()){

				// recombine the segments at the lengths of the median profile segments
				Profile frankenProfile = fitter.recombine(c.getNucleus(), BorderTag.REFERENCE_POINT);

				c.getNucleus().setProfile(ProfileType.FRANKEN, new SegmentedProfile(frankenProfile));

			}
			
			activeDataset().getCollection()
				.getProfileCollection(ProfileType.FRANKEN)
				.createProfileAggregate(activeDataset().getCollection(), ProfileType.FRANKEN);

			fitter = null; // clean up
			
			activeDataset().getAnalysisOptions().setAngleProfileWindowSize(windowSize);

		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(e.getSource()==windowSizeButton){
				new AngleWindowSizeExplorer(activeDataset());
			}
			
			
			
			try {
				CellCollection collection = activeDataset().getCollection();
				SegmentedProfile medianProfile = collection
						.getProfileCollection(ProfileType.REGULAR)
						.getSegmentedProfile(BorderTag.REFERENCE_POINT);
				
//				List<NucleusBorderSegment> list =collection.getProfileCollection(ProfileCollectionType.REGULAR)
//						.getSegmentedProfile(BorderTag.REFERENCE_POINT).getOrderedSegments();
				
				
				// Update the segment pattern to the same ordered pattern seen in the profile chart
//				medianProfile.setSegments(list);
				
//				List<String> names = new ArrayList<String>();
				SegmentsEditingPanel.this.setAnalysing(true);
				
				
				if(e.getSource().equals(mergeButton)){
					fireDatasetEvent(DatasetMethod.CLEAR_CACHE, getDatasets());
					mergeAction(medianProfile);
					
				}

				if(e.getSource().equals(unmergeButton)){
					fireDatasetEvent(DatasetMethod.CLEAR_CACHE, getDatasets());
					unmergeAction(medianProfile);
				}
				
				if(e.getSource().equals(splitButton)){
					fireDatasetEvent(DatasetMethod.CLEAR_CACHE, getDatasets());
					splitAction(medianProfile);
					
				}
				
				if(e.getSource()==updatewindowButton){
					fireDatasetEvent(DatasetMethod.CLEAR_CACHE, getDatasets());
					updateCollectionWindowSize();
				}
				
			} catch (Exception e1) {
				
				log(Level.SEVERE, "Error in action", e1);
			} finally {
				SegmentsEditingPanel.this.setAnalysing(false);
			}
		}
		
		private void mergeAction(SegmentedProfile medianProfile) throws Exception{
			
			List<SegMergeItem> names = new ArrayList<SegMergeItem>();
			
			
			// Put the names of the mergable segments into a list
			for(NucleusBorderSegment seg : medianProfile.getOrderedSegments()){
				SegMergeItem item = new SegMergeItem(seg, seg.nextSegment());
				names.add(item);
			}
			SegMergeItem[] nameArray = names.toArray(new SegMergeItem[0]);

			SegMergeItem mergeOption = (SegMergeItem) JOptionPane.showInputDialog(null, 
					"Choose segments to merge",
					"Merge",
					JOptionPane.QUESTION_MESSAGE, 
					null, 
					nameArray, 
					nameArray[0]);

			if(mergeOption!=null){


				if(activeDataset()
						.getCollection()
						.getProfileManager().testSegmentsMergeable(mergeOption.getOne(), mergeOption.getTwo())){
					activeDataset()
						.getCollection()
						.getProfileManager()
						.mergeSegments(mergeOption.getOne(), mergeOption.getTwo());
					
					finest("Merged segments: "+mergeOption.toString());
					finest("Refreshing chart cache for editing panel");
					this.refreshChartCache();
					finest("Firing general refresh cache request for loaded datasets");
					fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
				} else {
					JOptionPane.showMessageDialog(this, "Cannot merge segments: they would cross a core border tag");
				}

			}
		}
		
		private class SegMergeItem{
			private NucleusBorderSegment one, two;
			public SegMergeItem(NucleusBorderSegment one, NucleusBorderSegment two){
				this.one = one;
				this.two = two;
			}
			public String toString(){
				return one.getName()+" - "+two.getName();
			}
			public NucleusBorderSegment getOne(){
				return one;
			}
			public NucleusBorderSegment getTwo(){
				return two;
			}
		}
		
		private class SegSplitItem{
			private NucleusBorderSegment seg;
			public SegSplitItem(NucleusBorderSegment seg){
				this.seg = seg;
			}
			public String toString(){
				return seg.getName();
			}
			public NucleusBorderSegment getSeg(){
				return seg;
			}
		}
		
		private void splitAction(SegmentedProfile medianProfile) throws Exception{
			
			List<SegSplitItem> names = new ArrayList<SegSplitItem>();

			// Put the names of the mergable segments into a list
			for(NucleusBorderSegment seg : medianProfile.getSegments()){
					names.add(new SegSplitItem(seg));						
			}
			
			SegSplitItem[] nameArray = names.toArray(new SegSplitItem[0]);

			SegSplitItem option = (SegSplitItem) JOptionPane.showInputDialog(null, 
					"Choose segment to split",
					"Split",
					JOptionPane.QUESTION_MESSAGE, 
					null, 
					nameArray, 
					nameArray[0]);

			if(option!=null){

				NucleusBorderSegment seg = option.getSeg();

				if(activeDataset()
						.getCollection()
						.getProfileManager()
						.splitSegment(seg)){
					
					finest("Split segment "+option.toString());
					finest("Refreshing chart cache for editing panel");
					this.refreshChartCache();
					finest("Firing general refresh cache request for loaded datasets");
					fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
				}
			}
		}
		
		private void unmergeAction(SegmentedProfile medianProfile) throws Exception{
						
			List<SegSplitItem> names = new ArrayList<SegSplitItem>();

			// Put the names of the mergable segments into a list
			for(NucleusBorderSegment seg : medianProfile.getSegments()){
				if(seg.hasMergeSources()){
					names.add(new SegSplitItem(seg));		
				}	

			}
			
			SegSplitItem[] nameArray = names.toArray(new SegSplitItem[0]);
			
			// show a list of segments that can be unmerged, and merge the selected option

			// Put the names of the mergable segments into a list


			SegSplitItem mergeOption = (SegSplitItem) JOptionPane.showInputDialog(null, 
					"Choose segments to unmerge",
					"Unmerge",
					JOptionPane.QUESTION_MESSAGE, 
					null, 
					nameArray, 
					nameArray[0]);

			if(mergeOption!=null){
				// a choice was made

				activeDataset()
				.getCollection()
				.getProfileManager()
				.unmergeSegments(mergeOption.getSeg());
				
				finest("Unmerged segment "+mergeOption.toString());

				finest("Refreshing chart cache for editing panel");
				this.refreshChartCache();
				finest("Firing general refresh cache request for loaded datasets");
				fireDatasetEvent(DatasetMethod.REFRESH_CACHE, getDatasets());
			}
		}
//	}


}
