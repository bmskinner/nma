package analysis.nucleus;

import java.util.ArrayList;
import java.util.List;
import analysis.AbstractProgressAction;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class SegmentAssignmentTask  extends AbstractProgressAction  {
	SegmentedProfile median;
	final int low, high;
	final Nucleus[] nuclei;
	private static final int THRESHOLD = 30;
	
	public SegmentAssignmentTask(SegmentedProfile medianProfile, Nucleus[] nuclei, int low, int high) throws Exception{
	
		this.low = low;
		this.high = high;
		this.nuclei = nuclei;
		this.median = medianProfile;
	}
	
	public SegmentAssignmentTask(SegmentedProfile medianProfile, Nucleus[] nuclei) throws Exception{
		this(medianProfile, nuclei, 0, nuclei.length);
	}

	protected void compute() {
	     if (high - low < THRESHOLD)
			try {
				processNuclei(low, high);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	     else {
	    	 int mid = (low + high) >>> 1;

	    	 List<SegmentAssignmentTask> tasks = new ArrayList<SegmentAssignmentTask>();
	    	 SegmentAssignmentTask task1;
	    	 try {
	    		 task1 = new SegmentAssignmentTask(median, nuclei, low, mid);

	    		 task1.addProgressListener(this);


	    		 SegmentAssignmentTask task2 = new SegmentAssignmentTask(median, nuclei, mid, high);
	    		 task2.addProgressListener(this);

	    		 tasks.add(task1);
	    		 tasks.add(task2);

	    		 this.invokeAll(tasks);
	    	 } catch (Exception e) {
	    		 // TODO Auto-generated catch block
	    		 e.printStackTrace();
	    	 }

	     }
	}
	
private void processNuclei(int lo, int hi) throws Exception {
		
		for(int i=low; i<high; i++){
			assignSegmentsToNucleus(nuclei[i]);
			fireProgressEvent();
		}
		
	}
	
	/**
	 * From the calculated median profile segments, assign segments to each nucleus
	 * based on the best offset fit of the start and end indexes 
	 * @param collection
	 * @param pointType
	 */
//	private void assignSegments(CellCollection collection){
//
//		try{
//			log(Level.FINER, "Assigning segments to nuclei...");
//
////			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);
////
////			// find the corresponding point in each Nucleus
////			SegmentedProfile median = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);
//
////			for(Nucleus n : collection.getNuclei()){
////				assignSegmentsToNucleus(n, median);
////				publish(progressCount++);
////			}
//			log(Level.FINER, "Segments assigned to nuclei");
//		} catch(Exception e){
//			logError("Error assigning segments", e);
//		}
//	}
	
	/**
	 * Assign the given segments to the nucleus, finding the best match of the nucleus
	 * profile to the median profile
	 * @param n the nucleus to segment
	 * @param median the segmented median profile
	 */
	private void assignSegmentsToNucleus(Nucleus n) throws Exception {
			
		// remove any existing segments in the nucleus
		SegmentedProfile nucleusProfile = n.getProfile(ProfileType.REGULAR);
		nucleusProfile.clearSegments();

		List<NucleusBorderSegment> nucleusSegments = new ArrayList<NucleusBorderSegment>();

		// go through each segment defined for the median curve
		NucleusBorderSegment prevSeg = null;

		for(NucleusBorderSegment segment : median.getSegments()){

			// get the positions the segment begins and ends in the median profile
			int startIndexInMedian 	= segment.getStartIndex();
			int endIndexInMedian 	= segment.getEndIndex();

			// find the positions these correspond to in the offset profiles

			// get the median profile, indexed to the start or end point
			Profile startOffsetMedian 	= median.offset(startIndexInMedian);
			Profile endOffsetMedian 	= median.offset(endIndexInMedian);

			// find the index at the point of the best fit
			int startIndex 	= n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(startOffsetMedian);
			int endIndex 	= n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(endOffsetMedian);

			// create a segment at these points
			// ensure that the segment meets length requirements
			NucleusBorderSegment seg = new NucleusBorderSegment(startIndex, endIndex, n.getBorderLength(), segment.getID());
			if(prevSeg != null){
				seg.setPrevSegment(prevSeg);
				prevSeg.setNextSegment(seg);
			}

//			seg.setName(segment.getName());
			nucleusSegments.add(seg);

			prevSeg = seg;
		}

		NucleusBorderSegment.linkSegments(nucleusSegments);
		nucleusProfile.setSegments(nucleusSegments);
		n.setProfile(ProfileType.REGULAR, nucleusProfile);

		
	}


}
