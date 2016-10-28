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

import gui.DatasetEvent;
import gui.GlobalOptions;
import gui.SegmentEvent;
import gui.SegmentEventListener;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.components.panels.SegmentationDualChartPanel;
import gui.dialogs.AngleWindowSizeExplorer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
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

import org.jfree.chart.JFreeChart;

import stats.Quartile;
import analysis.profiles.SegmentFitter;
import charting.charts.MorphologyChartFactory;
import charting.options.DefaultChartOptions;
import charting.options.ChartOptionsBuilder;
import components.ICell;
import components.ICellCollection;
import components.active.ChildAnalysisDataset;
import components.active.generic.SegmentedFloatProfile;
import components.generic.IProfile;
import components.generic.IProfileCollection;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderSegment;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class SegmentsEditingPanel extends AbstractEditingPanel implements ActionListener, SegmentEventListener {
					
	private SegmentationDualChartPanel dualPanel;
	
		private JPanel buttonsPanel;
		private JButton mergeButton;
		private JButton unmergeButton;
		private JButton splitButton;
		private JButton windowSizeButton;
		private JButton updatewindowButton;
		private JButton reprofileButton;
		
		
		private static final String STR_MERGE_SEGMENT     = "Hide segment boundary";
		private static final String STR_UNMERGE_SEGMENT   = "Unhide segment boundary";
		private static final String STR_SPLIT_SEGMENT     = "Split segment";
		private static final String STR_SET_WINDOW_SIZE   = "Set window size";
		private static final String STR_SHOW_WINDOW_SIZES = "Window sizes";
		private static final String STR_REPROFILE         = "Recalculate median profiles";
		
				
		public SegmentsEditingPanel(){
			super();
			this.setLayout(new BorderLayout());
			
			dualPanel = new SegmentationDualChartPanel();
			dualPanel.addSegmentEventListener(this);
			
			JPanel chartPanel = new JPanel();
			chartPanel.setLayout(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.EAST;
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth  = 1; 
			c.gridheight = 1;
			c.fill = GridBagConstraints.BOTH;      //reset to default
			c.weightx = 1.0; 
			c.weighty = 0.7;
			
			chartPanel.add(dualPanel.getMainPanel(), c);
			c.weighty = 0.3;
			c.gridx = 0;
			c.gridy = 1;
			chartPanel.add(dualPanel.getRangePanel(), c);
			
			this.add(chartPanel, BorderLayout.CENTER);

			
			buttonsPanel = makeButtonPanel();
			this.add(buttonsPanel, BorderLayout.NORTH);

			setButtonsEnabled(false);
			
			dualPanel.getMainPanel().getChart().getXYPlot().getDomainAxis().setVisible(false);
			dualPanel.getMainPanel().getChart().getXYPlot().getRangeAxis().setVisible(false);
			
		}
		
		@Override
		public void setAnalysing(boolean b){
			super.setAnalysing(b);
			dualPanel.setAnalysing(b);
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
			
//			reprofileButton = new JButton(STR_REPROFILE);
//			reprofileButton.addActionListener(this);
//			panel.add(reprofileButton);

			return panel;
			
			
		}
						
		@Override
		protected void updateSingle() {
			
			ISegmentedProfile profile = null;
			
			DefaultChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(Tag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType( ProfileType.ANGLE)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setShowPoints(true)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.setTarget(dualPanel.getMainPanel())
				.build();
			
			// Set the button configuration
			configureButtons(options);
						
			JFreeChart chart = getChart(options);
			
			profile = activeDataset().getCollection()
					.getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
			

			
			/*
			 * Create the chart for the range panel
			 */
			
			DefaultChartOptions rangeOptions = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setTag(Tag.REFERENCE_POINT)
				.setShowMarkers(false)
				.setProfileType( ProfileType.ANGLE)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setShowPoints(false)
				.setShowXAxis(false)
				.setShowYAxis(false)
				.setTarget(dualPanel.getRangePanel())
				.build();
			
			JFreeChart rangeChart = getChart(rangeOptions);

			dualPanel.setCharts(chart, profile, true, rangeChart);
		}

		@Override
		protected void updateMultiple() {
			updateNull();
			
		}
		
		@Override
		protected void updateNull() {	
			
			
			DefaultChartOptions options = new ChartOptionsBuilder()
				.setShowXAxis(false)
				.setShowYAxis(false)
				.build();
			
			
			JFreeChart mainChart = new MorphologyChartFactory(options).makeEmptyChart();
		
			JFreeChart rangeChart = new MorphologyChartFactory(options).makeEmptyChart();
			
			dualPanel.setCharts(mainChart, rangeChart);
			setButtonsEnabled(false);
		}
		
		@Override
		protected JFreeChart createPanelChartType(DefaultChartOptions options) throws Exception {
			return new MorphologyChartFactory(options).makeMultiSegmentedProfileChart();
		}
				
		/**
		 * Enable or disable buttons depending on datasets selected
		 * @param options
		 * @throws Exception
		 */
		private void configureButtons(DefaultChartOptions options) {
			if(options.isSingleDataset()){
				
				setButtonsEnabled(true);
				ICellCollection collection = options.firstDataset().getCollection();
				ISegmentedProfile medianProfile = collection.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
				
				// Don't allow merging below 2 segments (causes errors)
				if(medianProfile.getSegmentCount()<=2){
					mergeButton.setEnabled(false);
				} else {
					mergeButton.setEnabled(true);
				}
				
				
				// Check if there are any merged segments
				boolean hasMerges = false;
				for(IBorderSegment seg : medianProfile.getSegments()){
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
				
				// set child dataset options
				if(options.firstDataset() instanceof ChildAnalysisDataset){
					mergeButton.setEnabled(false);
					unmergeButton.setEnabled(false);
					splitButton.setEnabled(false);
					updatewindowButton.setEnabled(false);
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
//			reprofileButton.setEnabled(b);
			
		}
		
				
		private void updateCollectionWindowSize() throws Exception{
			double windowSizeMin = 0.01;
			double windowSizeMax = 0.1;
			double windowSizeActual = activeDataset().getAnalysisOptions().getAngleWindowProportion();
			
			SpinnerNumberModel spinnerModel = new SpinnerNumberModel(windowSizeActual,
					windowSizeMin,
					windowSizeMax,
					0.01);
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
				double windowSize = (double) windowSizeSpinner.getModel().getValue();
				setCollectionWindowSize(windowSize);
				this.refreshChartCache();
				fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
				this.setAnalysing(false);
			}
			
		}
		
		private void setCollectionWindowSize(double windowSize) throws Exception{
			
			// Update cells
			
			for(ICell c : activeDataset().getCollection().getCells()){
				c.getNucleus().setWindowProportion(ProfileType.ANGLE, windowSize);
			}

			// recalc the aggregate
			
			IProfileCollection pc = activeDataset().getCollection().getProfileCollection();	
			
			pc.createProfileAggregate(activeDataset().getCollection(), pc.length());
					
			
			ISegmentedProfile medianProfile = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);	
			
			// Does nothing, but needed to access segment fitter
//			DatasetSegmenter segmenter = new DatasetSegmenter(activeDataset(), MorphologyAnalysisMode.NEW, programLogger);
			
			// Make a fitter
			SegmentFitter fitter = new SegmentFitter(medianProfile);

			for(ICell c : activeDataset().getCollection().getCells()){

				// recombine the segments at the lengths of the median profile segments
				IProfile frankenProfile = fitter.recombine(c.getNucleus(), Tag.REFERENCE_POINT);

				c.getNucleus().setProfile(ProfileType.FRANKEN, new SegmentedFloatProfile(frankenProfile));

			}
			
			pc.createProfileAggregate(activeDataset().getCollection(), pc.length());

			fitter = null; // clean up
			
			activeDataset().getAnalysisOptions().setAngleWindowProportion(windowSize);

		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if(e.getSource()==windowSizeButton){
				new AngleWindowSizeExplorer(activeDataset());
			}
			

			
			try {
				ICellCollection collection = activeDataset().getCollection();
				ISegmentedProfile medianProfile = collection
						.getProfileCollection()
						.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
				

				SegmentsEditingPanel.this.setAnalysing(true);
				
				
				if(e.getSource().equals(mergeButton)){
					fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
					mergeAction(medianProfile);
					
				}

				if(e.getSource().equals(unmergeButton)){
					fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
					unmergeAction(medianProfile);
				}
				
				if(e.getSource().equals(splitButton)){
					fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
					splitAction(medianProfile);
					
				}
				
				if(e.getSource()==updatewindowButton){
					fireDatasetEvent(DatasetEvent.CLEAR_CACHE, getDatasets());
					updateCollectionWindowSize();
				}
				
				if(e.getSource()==reprofileButton){
					
					activeDataset().getCollection().getProfileManager().recalculateProfileAggregates();
					fireDatasetEvent(DatasetEvent.REFRESH_CACHE, getDatasets());
				}
				
			} catch (Exception e1) {
				
				log(Level.SEVERE, "Error in action", e1);
			} finally {
				SegmentsEditingPanel.this.setAnalysing(false);
			}
		}
		
		
		private void mergeAction(ISegmentedProfile medianProfile) throws Exception{
			
			List<SegMergeItem> names = new ArrayList<SegMergeItem>();
			
			
			// Put the names of the mergable segments into a list
			for(IBorderSegment seg : medianProfile.getOrderedSegments()){
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

				this.setAnalysing(true);

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
					fireDatasetEvent(DatasetEvent.REFRESH_CACHE, getDatasets());
				} else {
					JOptionPane.showMessageDialog(this, "Cannot merge segments: they would cross a core border tag");
				}
				this.setAnalysing(false);
			}
		}
		
		private class SegMergeItem{
			private IBorderSegment one, two;
			public SegMergeItem(IBorderSegment one, IBorderSegment two){
				this.one = one;
				this.two = two;
			}
			public String toString(){
				return one.getName()+" - "+two.getName();
			}
			public IBorderSegment getOne(){
				return one;
			}
			public IBorderSegment getTwo(){
				return two;
			}
		}
		
		private class SegSplitItem{
			private IBorderSegment seg;
			public SegSplitItem(IBorderSegment seg){
				this.seg = seg;
			}
			public String toString(){
				return seg.getName();
			}
			public IBorderSegment getSeg(){
				return seg;
			}
		}
		
		private void splitAction(ISegmentedProfile medianProfile) throws Exception{
			
			List<SegSplitItem> names = new ArrayList<SegSplitItem>();

			// Put the names of the mergable segments into a list
			for(IBorderSegment seg : medianProfile.getSegments()){
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
				
				this.setAnalysing(true);

				IBorderSegment seg = option.getSeg();

				if(activeDataset()
						.getCollection()
						.getProfileManager()
						.splitSegment(seg)){
					
					finest("Split segment "+option.toString());
					finest("Refreshing chart cache for editing panel");
					this.refreshChartCache();
					finest("Firing general refresh cache request for loaded datasets");
					fireDatasetEvent(DatasetEvent.REFRESH_CACHE, getDatasets());
				}
				this.setAnalysing(false);
			}
		}
		
		private void unmergeAction(ISegmentedProfile medianProfile) throws Exception{
						
			List<SegSplitItem> names = new ArrayList<SegSplitItem>();

			// Put the names of the mergable segments into a list
			for(IBorderSegment seg : medianProfile.getSegments()){
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
				this.setAnalysing(true);

				activeDataset()
				.getCollection()
				.getProfileManager()
				.unmergeSegments(mergeOption.getSeg());
				
				this.setAnalysing(false);
				
				finest("Unmerged segment "+mergeOption.toString());

				finest("Refreshing chart cache for editing panel");
				this.refreshChartCache();
				finest("Firing general refresh cache request for loaded datasets");
				fireDatasetEvent(DatasetEvent.REFRESH_CACHE, getDatasets());
			}
		}
//	}

		@Override
		public void segmentEventReceived(SegmentEvent event) {
			if(event.getType()==SegmentEvent.MOVE_START_INDEX){
				finest("Heard update segment request");
				try{

					
					setAnalysing(true);

					UUID segID = event.getId();
					
					int index = event.getIndex();

					updateSegmentStartIndexAction(segID, index);

				} catch(Exception e){
					error("Error updating segment", e);
				} finally {
					setAnalysing(false);
				}

			}
			
		}


}
