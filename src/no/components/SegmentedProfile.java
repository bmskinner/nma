package no.components;

import ij.IJ;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utility.Utils;

/**
 * This class provides consistency and error checking for segmnentation
 * applied to profiles.
 *
 */
public class SegmentedProfile extends Profile implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	// the segments
	protected List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
	
	/**
	 * Construct using a regular profile and a list of border segments
	 * @param p the profile
	 * @param segments the list of segments to use
	 */
	public SegmentedProfile(Profile p, List<NucleusBorderSegment> segments){		
		super(p);
		if(p.size() != segments.get(0).getTotalLength() ){
			throw new IllegalArgumentException("Segments do not fit profile");
		}
		
		this.segments = segments;
	}
	
	/**
	 * Construct using an existing profile. Copies the data
	 * and segments
	 * @param profile the segmented profile to copy
	 */
	public SegmentedProfile(SegmentedProfile profile){
		super(profile.array);
		this.segments = NucleusBorderSegment.copy(profile.getSegments());
	}
	
	/**
	 * Construct using a basic profile. Two segments are created 
	 * that span the entire profile, half each
	 * @param profile
	 */
	public SegmentedProfile(Profile profile){
		super(profile);
		int midpoint = profile.size()/2;
		NucleusBorderSegment segment1 = new NucleusBorderSegment(0, midpoint, profile.size());
		segment1.setName("Seg_1");
		NucleusBorderSegment segment2 = new NucleusBorderSegment(midpoint, 0, profile.size());
		segment2.setName("Seg_2");
		List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
		segments.add(segment1);
		segments.add(segment2);
		this.segments = segments;
	}
	
	/**
	 * Construct from an array of values
	 * @param values
	 */
	public SegmentedProfile(double[] values){
		super(values);
		int midpoint = values.length/2;
		NucleusBorderSegment segment1 = new NucleusBorderSegment(0, midpoint, values.length);
		NucleusBorderSegment segment2 = new NucleusBorderSegment(midpoint, 0, values.length);
		segment1.setName("Seg_1");
		segment2.setName("Seg_2");
		List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
		segments.add(segment1);
		segments.add(segment2);
		this.segments = segments;
	}
	
	/**
	 * Get a copy of the segments in this profile
	 * @return
	 */
	public List<NucleusBorderSegment> getSegments(){
		return NucleusBorderSegment.copy(this.segments);
	}
	
	/**
	 * Get the segment with the given name. Returns null if no segment
	 * is found. Gets the actual segment, not a copy
	 * @param name
	 * @return
	 */
	public NucleusBorderSegment getSegment(String name){
		if(name==null){
			throw new IllegalArgumentException("Requested segment name is null");
		}
		
		NucleusBorderSegment result = null;
		for(NucleusBorderSegment seg : this.segments){
			if(seg.getName().equals(name)){
				result = seg;
			}
		}
		return result;
	}
	
	/**
	 * Replace the segments in the profile with the given list
	 * @param segments
	 */
	public void setSegments(List<NucleusBorderSegment> segments){
		if(segments==null || segments.isEmpty()){
			throw new IllegalArgumentException("Segment list is null or empty");
		}
		
		if(segments.get(0).getTotalLength()!=this.size()){
			throw new IllegalArgumentException("Segment list is from a different total length");
		}
		
		this.segments = NucleusBorderSegment.copy(segments);
	}
	
	/**
	 * Remove the segments from this profile
	 */
	public void clearSegments(){
		this.segments = new ArrayList<NucleusBorderSegment>(0);
	}
	
	/**
	 * Get the names of the segments in the profile
	 * @return
	 */
	public List<String> getSegmentNames(){
		List<String> result = new ArrayList<String>();
		for(NucleusBorderSegment seg : this.segments){
			result.add(seg.getName());
		}
		return result;
	}
	
	/**
	 * Get the number of segments in the profile
	 * @return
	 */
	public int getSegmentCount(){
		return this.segments.size();
	}
	
	/**
	 * Test if the profile contains the given segment. Copies are ok,
	 * it checks position, length and name
	 * @param segment
	 * @return
	 */
	public boolean contains(NucleusBorderSegment segment){
		
		boolean result = false;
		for(NucleusBorderSegment seg : this.segments){
			if(seg.getName().equals(segment.getName())
					&& seg.getStartIndex()==segment.getStartIndex()
					&& seg.getEndIndex()==segment.getEndIndex()
					&& seg.getTotalLength()==this.size()
					
					){
				return true;
			}
		}
		return result;
	}
	
	/**
	 * Update the selected segment of the profile with the new start and end
	 * positions. Checks the validity of the operation, and returns false if
	 * it is not possible to perform the update
	 * @param segment the segment to update
	 * @param startIndex the new start
	 * @param endIndex the new end
	 * @return did the update succeed
	 */
	public boolean update(NucleusBorderSegment segment, int startIndex, int endIndex){
		
		// use this to wrap the NucleusBorderSegment update code. Test inversion effects
		
		if(!this.contains(segment)){
			throw new IllegalArgumentException("Segment is not part of this profile");
		}
		
		if( segment.update(startIndex, endIndex)){
			return true;
		} else{
//			
			if(testLinked()){
				return false;
			} else {
				IJ.log("Error updating SegmentedProfile: segments unlinked: "+segment.getLastFailReason());
				IJ.log(segment.toString());
				return false;
			}
		}
		
		// how to test wrapping
	}
	
	/**
	 * Test if the all the segments are currently linked
	 * @return
	 */
	private boolean testLinked(){
		boolean result = true;
		for(NucleusBorderSegment seg : this.segments){
			if(!seg.hasNextSegment()  || !seg.hasPrevSegment()){
				result = false;
			}
		}
		return result;
	}
	
	/**
	 * Adjust the start position of the given segment by the given amount.
	 * @param segment the segment to apply the change to
	 * @param amount the number of indexes to move
	 * @return did the update succeed
	 */
	public boolean adjustSegmentStart(String name, int amount){
		if(!this.getSegmentNames().contains(name)){
			throw new IllegalArgumentException("Segment is not part of this profile");
		}
		
		// get the segment within this profile, not a copy that looks the same
		NucleusBorderSegment segmentToUpdate = this.getSegment(name);
		
		int newValue = Utils.wrapIndex( segmentToUpdate.getStartIndex()+amount, segmentToUpdate.getTotalLength());
		return this.update(segmentToUpdate, newValue, segmentToUpdate.getEndIndex());
	}
	
	/**
	 * Adjust the end position of the given segment by the given amount.
	 * @param segment the segment to apply the change to
	 * @param amount the number of indexes to move
	 * @return did the update succeed
	 */
	public boolean adjustSegmentEnd(String name, int amount){
		if(!this.getSegmentNames().contains(name)){
			throw new IllegalArgumentException("Segment is not part of this profile");
		}
		
		// get the segment within this profile, not a copy that looks the same
		NucleusBorderSegment segmentToUpdate = this.getSegment(name);
				
		int newValue = Utils.wrapIndex( segmentToUpdate.getEndIndex()+amount, segmentToUpdate.getTotalLength());
		return this.update(segmentToUpdate, segmentToUpdate.getStartIndex(), newValue);
	}
	
	public void nudgeSegments(int amount){
		this.segments = NucleusBorderSegment.nudge(getSegments(), amount);
	}
	
	public SegmentedProfile offset(int amount){
	
		Profile offsetProfile = super.offset(amount);
		
		// offset the segments
		List<NucleusBorderSegment> segments = NucleusBorderSegment.nudge(getSegments(), -amount);
		SegmentedProfile copy = new SegmentedProfile(offsetProfile, segments);
		return copy;
	}
}
