/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinTask;
import java.util.logging.Level;

import analysis.AbstractProgressAction;
import components.AbstractCellularComponent;
import components.generic.IProfile;
import components.generic.Profile;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

@SuppressWarnings("serial")
public class SegmentAssignmentTask  extends AbstractProgressAction  {
	
	final SegmentedProfile median;
	final int low, high;
	final Nucleus[] nuclei;
	private static final int THRESHOLD = 30;
	
	protected SegmentAssignmentTask(SegmentedProfile medianProfile, Nucleus[] nuclei, int low, int high) throws ProfileException{
	
		this.low    = low;
		this.high   = high;
		this.nuclei = nuclei;
		this.median = medianProfile;
	}
	
	public SegmentAssignmentTask(SegmentedProfile medianProfile, Nucleus[] nuclei) throws ProfileException {
		this(medianProfile, nuclei, 0, nuclei.length);
	}

	protected void compute() {
	     if (high - low < THRESHOLD)
			try {
				processNuclei();
			} catch (ProfileException e) {
	    		 warn("Error assigning segments to nuclei");
	    		 fine("Error processing nuclei", e);
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

	    		 ForkJoinTask.invokeAll(tasks);
	    	 } catch (ProfileException e) {
	    		 warn("Error assigning segments to nucleus");
	    		 fine("Error processing nuclei", e);
	    	 }

	     }
	}
	
	/**
	 * From the calculated median profile segments, assign segments to each nucleus
	 * based on the best offset fit of the start and end indexes 
	 */
	private void processNuclei() throws ProfileException {

		for(int i=low; i<high; i++){
			assignSegmentsToNucleus(nuclei[i]);
			fireProgressEvent();
		}

	}

	/**
	 * Assign the median segments to the nucleus, finding the best match of the nucleus
	 * profile to the median profile
	 * @param n the nucleus to segment
	 * @param median the segmented median profile
	 */
	private void assignSegmentsToNucleus(Nucleus n) throws ProfileException {

		if(n.isLocked()){
			finest(n.getNameAndNumber()+" is locked, skipping");
			return;
		} else {
			finest("Assigning segments to "+n.getNameAndNumber());
		}
		
		// remove any existing segments in the nucleus
		SegmentedProfile nucleusProfile = n.getProfile(ProfileType.ANGLE);
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
			int startIndex 	= n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(startOffsetMedian);
			int endIndex 	= n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(endOffsetMedian);

			// create a segment at these points
			// ensure that the segment meets length requirements
			
//			
//			if( Math.abs(endIndex-startIndex) < NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH ){
//				
//				while(Math.abs(endIndex-startIndex)<NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH){
//					endIndex = n.wrapIndex(++endIndex);
//					prevSeg.update(prevSeg.getStartIndex(), endIndex);
//					fine("Segment too short. Lengthening to end index "+endIndex);
//				}
//			}
			
			try {
				NucleusBorderSegment seg = new NucleusBorderSegment(startIndex, endIndex, n.getBorderLength(), segment.getID());
				if(prevSeg != null){
					seg.setPrevSegment(prevSeg);
					prevSeg.setNextSegment(seg);
				}

				nucleusSegments.add(seg);

				prevSeg = seg;
			
			} catch(IllegalArgumentException e){
				fine("Error making segment for nucleus "+n.getNameAndNumber(), e);
				break;
				
			}

		}

		NucleusBorderSegment.linkSegments(nucleusSegments);
		
		nucleusProfile.setSegments(nucleusSegments);

		n.setProfile(ProfileType.ANGLE, nucleusProfile);
		log(Level.FINEST, "Assigned segments to nucleus "+n.getNameAndNumber()+":");
		log(Level.FINEST, nucleusProfile.toString());
		
		finest("Assigned segments to "+n.getNameAndNumber());
		
	}


}
