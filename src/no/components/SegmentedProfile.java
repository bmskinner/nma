package no.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides consistency and error checking for segmnentation
 * applied to profiles.
 *
 */
public class SegmentedProfile extends Profile implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();

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
	 * Get a copy of the segments in this profile
	 * @return
	 */
	public List<NucleusBorderSegment> getSegments(){
		return NucleusBorderSegment.copy(this.segments);
	}
	
	/**
	 * Get the segment with the given name. Returns null if no segment
	 * is found
	 * @param name
	 * @return
	 */
	public NucleusBorderSegment getSegment(String name){
		if(name==null){
			throw new IllegalArgumentException("Requested segment name is null");
		}
		
		NucleusBorderSegment result = null;
		for(NucleusBorderSegment seg : this.segments){
			if(seg.getSegmentType().equals(name)){
				result = seg;
			}
		}
		return result;
	}
	
	/**
	 * Get the names of the segments in the profile
	 * @return
	 */
	public List<String> getSegmentNames(){
		List<String> result = new ArrayList<String>();
		for(NucleusBorderSegment seg : this.segments){
			result.add(seg.getSegmentType());
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
			if(seg.getSegmentType().equals(segment.getSegmentType())
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
		
		// how to test wrapping
		
		return false;
	}
}
