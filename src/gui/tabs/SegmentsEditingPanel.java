package gui.tabs;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;
import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.components.DraggableOverlayChartPanel;
import gui.components.ProfileAlignmentOptionsPanel.ProfileAlignment;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.jfree.chart.JFreeChart;

import analysis.AnalysisDataset;
import charting.charts.MorphologyChartFactory;
import charting.charts.ProfileChartOptions;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.ProfileCollectionType;
import components.generic.SegmentedProfile;
import components.generic.BorderTag.BorderTagType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class SegmentsEditingPanel extends DetailPanel implements SignalChangeListener, DatasetEventListener, InterfaceEventListener {
	
	private final SegmentProfilePanel		segmentProfilePanel;	// draw the segments on the median profile
	
	public SegmentsEditingPanel(Logger programLogger) {
		
		super(programLogger);
		
		this.setLayout(new BorderLayout());
		segmentProfilePanel  = new SegmentProfilePanel(programLogger);
		this.addSubPanel(segmentProfilePanel);
		/*
		 * Signals come from the segment panel to this container
		 */
		segmentProfilePanel.addSignalChangeListener(this);
		segmentProfilePanel.addInterfaceEventListener(this);
		segmentProfilePanel.addDatasetEventListener(this);
		this.add(segmentProfilePanel, BorderLayout.CENTER);

		
	}
	
	@Override
	public void updateDetail(){

		programLogger.log(Level.FINE, "Updating segments editing panel");
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(hasDatasets()){
					segmentProfilePanel.update(getDatasets());
					
				}
				setUpdating(false);
			}
		});
	}
	
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		fireSignalChangeEvent(event);
	}
	
	@Override
	public void interfaceEventReceived(InterfaceEvent event) {
		fireInterfaceEvent(event);
		
		if(event.method().equals(InterfaceMethod.RECACHE_CHARTS)){
			this.refreshChartCache();
			this.refreshTableCache();
		}
		
	}

	@Override
	public void datasetEventReceived(DatasetEvent event) {
		fireDatasetEvent(event);
	}
		
	public class SegmentProfilePanel extends DetailPanel implements ActionListener, SignalChangeListener {
		
		private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
		private JPanel buttonsPanel;
		private JButton mergeButton;
		private JButton unmergeButton;
		private JButton splitButton;
		
		protected SegmentProfilePanel(Logger programLogger){
			super(programLogger);
			this.setLayout(new BorderLayout());
			Dimension minimumChartSize = new Dimension(50, 100);
			Dimension preferredChartSize = new Dimension(400, 300);
			
			JFreeChart profileChart = MorphologyChartFactory.makeEmptyProfileChart();
			chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);
			
			chartPanel.setMinimumSize(minimumChartSize);
			chartPanel.setPreferredSize(preferredChartSize);
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			chartPanel.addSignalChangeListener(this);
			this.add(chartPanel, BorderLayout.CENTER);
			
			buttonsPanel = makeButtonPanel();
			this.add(buttonsPanel, BorderLayout.NORTH);
			
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
		
		/**
		 * Split the given segment into two segmnets. The split is made at the midpoint
		 * @param segName
		 * @return
		 * @throws Exception
		 */
		private boolean splitSegment(String segName) throws Exception {
			
			boolean result = false;
			
			CellCollection collection = activeDataset().getCollection();
			
			SegmentedProfile medianProfile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getSegmentedProfile(BorderTag.ORIENTATION_POINT);
			
			// Get the segments to merge
			NucleusBorderSegment seg = medianProfile.getSegment(segName);
			
//			SpinnerNumberModel sModel 
//			= new SpinnerNumberModel(seg.getMidpointIndex(), 
//					0, 
//					medianProfile.size(),
//					1);
//			JSpinner spinner = new JSpinner(sModel);
//
//			int option = JOptionPane.showOptionDialog(null, 
//					spinner, 
//					"Choose the split index", 
//					JOptionPane.OK_CANCEL_OPTION, 
//					JOptionPane.QUESTION_MESSAGE, null, null, null);
//			if (option == JOptionPane.CANCEL_OPTION) {
//				
//				// user hit cancel
//				
//				
//			} else if (option == JOptionPane.OK_OPTION)	{
				
				try{

					int index = seg.getMidpointIndex();
//					int index = (Integer) spinner.getModel().getValue();
					if(seg.contains(index)){

						fireInterfaceEvent(InterfaceMethod.UPDATE_IN_PROGRESS);
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
						fireInterfaceEvent(InterfaceMethod.UPDATE_COMPLETE);
						fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, getDatasets());
						result = true;
					} else {
						SegmentsEditingPanel.this.programLogger.log(Level.WARNING, "Unable to split segment: index not present");
					}
				} catch(Exception e){
					SegmentsEditingPanel.this.programLogger.log(Level.SEVERE, "Error splitting segment", e);
				}
//			}
			
			return result;
		}
		
		
		@Override
		public void updateDetail(){

			programLogger.log(Level.FINE, "Updating segments editing panel");
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					
					try {
						JFreeChart chart = null;
						SegmentedProfile profile = null;
						if(getDatasets()==null || getDatasets().isEmpty()){

							chart = MorphologyChartFactory.makeEmptyProfileChart();


						} else {

							ProfileChartOptions options = new ProfileChartOptions(getDatasets(), true, ProfileAlignment.LEFT, BorderTag.REFERENCE_POINT, false, ProfileCollectionType.REGULAR);

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
										.getSegmentedProfile(BorderTag.REFERENCE_POINT);
							}
						} 

						chartPanel.setChart(chart, profile, true);
					} catch (Exception e) {
						programLogger.log(Level.SEVERE, "Error in plotting segment profile", e);
						chartPanel.setChart(MorphologyChartFactory.makeEmptyProfileChart());
						unmergeButton.setEnabled(false);
						mergeButton.setEnabled(false);
					} 


					setUpdating(false);
				}
			});
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

					
					SegmentsEditingPanel.this.setAnalysing(true);

					String[] array = event.type().split("\\|");
					String segName = array[1];
					int index = Integer.valueOf(array[2]);
					updateSegmentStartIndex(segName, index);

				} catch(Exception e){
					programLogger.log(Level.SEVERE, "Error updating segment", e);
				} finally {
					SegmentsEditingPanel.this.setAnalysing(false);
				}

			}
			
			
		}
		
		private void updateSegmentStartIndex(String segName, int index) throws Exception{

			// Update the median profile
			updateMedianProfileSegmentIndex(true , segName, index); // DraggablePanel always uses seg start index
			
			
			// Make a dialog - update the morphology of each nucleus?
			Object[] options = { "Update nuclei" , "Do not update", };
			int result = JOptionPane.showOptionDialog(null, "Update the nuclei to the new boundaries?", "Update nuclei",

					JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,

					null, options, options[0]);
			
			if(result==0){ // OK
				
				// TODO: Update each nucleus profile
				fireDatasetEvent(DatasetMethod.REFRESH_MORPHOLOGY, getDatasets());
				
			} else {
				// Only run a panel update if the refresh was not called
				fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
				update(getDatasets());
			}
			
			
		}
		
		private void updateMedianProfileSegmentIndex(boolean start, String segName, int index) throws Exception {
			
			programLogger.log(Level.FINE, "Updating median profile segment: "+segName+" to index "+index);
			// Get the median profile from the reference point
			
			SegmentedProfile oldProfile = activeDataset()
					.getCollection()
					.getProfileCollection(ProfileCollectionType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT);
			
			programLogger.log(Level.FINEST, "Old profile: "+oldProfile.toString());
			


			NucleusBorderSegment seg = oldProfile.getSegment(segName);

			int newStart = start ? index : seg.getStartIndex();
			int newEnd = start ? seg.getEndIndex() : index;

			 // Move the appropriate segment endpoint
			if(oldProfile.update(seg, newStart, newEnd)){
				
				programLogger.log(Level.FINEST, "Segment position update succeeded");
				// Replace the old segments in the median
				programLogger.log(Level.FINEST, "Updated profile: "+oldProfile.toString());

				programLogger.log(Level.FINEST, "Adding segments to profile collection");
				
				activeDataset()
				.getCollection()
				.getProfileCollection(ProfileCollectionType.REGULAR)
				.addSegments(BorderTag.REFERENCE_POINT, oldProfile.getSegments());
				
				programLogger.log(Level.FINEST, "Segments added, refresh the charts");
				
//				fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
//				update(getDatasets());
				
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
				SegmentsEditingPanel.this.setAnalysing(true);
				
				if(e.getSource().equals(mergeButton)){
					
					mergeAction(medianProfile, names);
					
				}

				if(e.getSource().equals(unmergeButton)){

					unmergeAction(medianProfile, names);
				}
				
				if(e.getSource().equals(splitButton)){
					splitAction(medianProfile, names);
					
				}
				
			} catch (Exception e1) {
				
				programLogger.log(Level.SEVERE, "Error altering segments", e1);
			} finally {
				SegmentsEditingPanel.this.setAnalysing(false);
			}
		}
		
		private void mergeAction(SegmentedProfile medianProfile, List<String> names) throws Exception{
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
				SegmentsEditingPanel.this.update(list);
				SegmentsEditingPanel.this.fireSignalChangeEvent("UpdatePanels");
			}
		}
		
		private void splitAction(SegmentedProfile medianProfile, List<String> names) throws Exception{
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
					SegmentsEditingPanel.this.update(SegmentsEditingPanel.this.activeDatasetToList());
					SegmentsEditingPanel.this.fireSignalChangeEvent("UpdatePanels");
				}
			}
		}
		
		private void unmergeAction(SegmentedProfile medianProfile, List<String> names) throws Exception{
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
				SegmentsEditingPanel.this.update(list);
				SegmentsEditingPanel.this.fireSignalChangeEvent("UpdatePanels");
			}
		}
	}

}
