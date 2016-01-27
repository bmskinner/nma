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
package components.generic;

import ij.IJ;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import utility.Utils;
import components.nuclear.NucleusBorderSegment;

/**
 * This class provides consistency and error checking for segmnentation
 * applied to profiles.
 *
 */
public class SegmentedProfile extends Profile implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	// the segments
	protected List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
	
	private transient NucleusBorderSegment firstSegment = null;
	
	/**
	 * Construct using a regular profile and a list of border segments
	 * @param p the profile
	 * @param segments the list of segments to use
	 */
	public SegmentedProfile(Profile p, List<NucleusBorderSegment> segments) throws Exception{		
		super(p);
		
		if(segments==null || segments.isEmpty()){
			throw new IllegalArgumentException("Segment list is null or empty in profile contructor");
		}
		if(p.size() != segments.get(0).getTotalLength() ){
			throw new IllegalArgumentException("Segments total length ("
							+segments.get(0).getTotalLength()
							+") does not fit profile ("+
							+p.size()
							+")");
		}
		
		NucleusBorderSegment.linkSegments(segments);

		this.segments = segments;
		this.firstSegment = segments.get(0);
	}
	
	/**
	 * Construct using an existing profile. Copies the data
	 * and segments
	 * @param profile the segmented profile to copy
	 */
	public SegmentedProfile(SegmentedProfile profile) throws Exception {
		this(profile, NucleusBorderSegment.copy(profile.getSegments()));
	}
	
	/**
	 * Construct using a basic profile. Two segments are created 
	 * that span the entire profile, half each
	 * @param profile
	 */
	public SegmentedProfile(Profile profile) throws Exception {
		super(profile);
		int midpoint = profile.size()/2;
		NucleusBorderSegment segment1 = new NucleusBorderSegment(0, midpoint, profile.size());

		segment1.setPosition(0);
		NucleusBorderSegment segment2 = new NucleusBorderSegment(midpoint, 0, profile.size());

		segment2.setPosition(1);
		List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
		segments.add(segment1);
		segments.add(segment2);
		
		NucleusBorderSegment.linkSegments(segments);

		this.segments = segments;
		this.firstSegment = segments.get(0);
	}
	
	/**
	 * Construct from an array of values
	 * @param values
	 * @throws Exception 
	 */
	public SegmentedProfile(double[] values) throws Exception{
		this( new Profile(values));
	}
	
	/**
	 * Check if this profile contains segments
	 * @return
	 */
	public boolean hasSegments(){
		if(this.segments==null || this.segments.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Get a copy of the segments in this profile. The order will be the same
	 * as the original segment list. No point type sepecifiers are available;
	 * if you need to offset a profile, do it by a profile offset 
	 * @return
	 */
	public List<NucleusBorderSegment> getSegments() throws Exception {
		return NucleusBorderSegment.copy(this.segments);
	}
	

	/**
	 * Get an iterator that begins with the segment at position zero in the profile
	 * @return
	 * @throws Exception
	 */
	public Iterator<NucleusBorderSegment> segmentIterator() throws Exception {

		List<NucleusBorderSegment> list = new ArrayList<NucleusBorderSegment>();
		
		// find the first segment
		NucleusBorderSegment first = null;
		
		for(NucleusBorderSegment seg : this.segments){
			if(seg.getPosition()==0){
				first = seg;
			}
		}
		
		list = getSegmentsFrom(first);
		return list.iterator();
	}
	
	/**
	 * Get the segments in order from the given segment
	 * @param firstSeg
	 * @return
	 * @throws Exception
	 */
	private List<NucleusBorderSegment> getSegmentsFrom(NucleusBorderSegment firstSeg) throws Exception {
		
		if(firstSeg==null){
			throw new IllegalArgumentException("Requested first segment is null");
		}
		
		
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>();
		int i = segments.size()-1; // the number of segments 
		result.add(firstSeg);
		while(i>0){
			
			if(firstSeg.hasNextSegment()){
				firstSeg = firstSeg.nextSegment();
				result.add(firstSeg);
				i--;
			} else {
				throw new Exception(i+": No next segment in "+firstSeg.toString());
			}
		}
		return NucleusBorderSegment.copy(result);
	}
	
	
	/**
	 * Get a copy of the segments in this profile, ordered 
	 * from the zero index of the profile
	 * @return
	 */
	public List<NucleusBorderSegment> getOrderedSegments() throws Exception {
		
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>();
		NucleusBorderSegment firstSeg = null;
		
		/*
		 * Choose the first segment of the profile to be the segment
		 * starting at the zero index
		 */
		for(NucleusBorderSegment seg : this.getSegments()){
			
			if(seg.getStartIndex()==0){
				firstSeg = seg;
			}
		}
		
		result = getSegmentsFrom(firstSeg);
		
		return NucleusBorderSegment.copy(result);
	}
	
	/**
	 * Get the segment assigned to be the first in the list
	 * @return
	 */
	public NucleusBorderSegment getFirstSegment(){
		if(this.firstSegment != null){
			return new NucleusBorderSegment(this.firstSegment);
		} else {
			return null;
		}
	}
	
	public List<NucleusBorderSegment> getSegmentsFromFirst() throws Exception {
		return getSegmentsFrom(getFirstSegment());
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
	 * Get the given segment. Returns null if no segment
	 * is found. Gets the actual segment, not a copy
	 * @param name
	 * @return
	 */
	public NucleusBorderSegment getSegment(NucleusBorderSegment segment){
		if(! this.contains(segment)){
			throw new IllegalArgumentException("Requested segment name is not present");
		}
		
		NucleusBorderSegment result = null;
		for(NucleusBorderSegment seg : this.segments){
			if(seg.equals(segment)){
				result = seg;
			}
		}
		return result;
	}
	
	/**
	 * Get the segment at the given position in the profile.
	 * is found. Gets the actual segment, not a copy
	 * @param name
	 * @return
	 */
	public NucleusBorderSegment getSegmentAt(int position){
		
		
		NucleusBorderSegment result = null;
		for(NucleusBorderSegment seg : this.segments){
			if(seg.getPosition()==position){
				result = seg;
			}
		}
		return result;
	}
	
	/**
	 * Get the segment containing the given index
	 * @param index
	 * @return
	 */
	public NucleusBorderSegment getSegmentContaining(int index){

		NucleusBorderSegment result = null;
		for(NucleusBorderSegment seg : this.segments){
			if(seg.contains(index)){
				result = seg;
			}
		}
		return result;
	}
	
	/**
	 * Replace the segments in the profile with the given list
	 * @param segments
	 */
	public void setSegments(List<NucleusBorderSegment> segments) throws Exception {
		if(segments==null || segments.isEmpty()){
			throw new IllegalArgumentException("Segment list is null or empty");
		}
		
		if(segments.get(0).getTotalLength()!=this.size()){
			throw new IllegalArgumentException("Segment list is from a different total length");
		}
		
		this.segments = NucleusBorderSegment.copy(segments);
		this.firstSegment = new NucleusBorderSegment(segments.get(0));
	}
	
	/**
	 * Remove the segments from this profile
	 */
	public void clearSegments(){
		this.segments = new ArrayList<NucleusBorderSegment>(0);
		this.firstSegment = null;
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
	 * Find the value displacement of the given segment in the profile.
	 * i.e the difference between the start value and the end value
	 * @param segment the segment to measure
	 * @return the displacement, or 0 if the segment was not found
	 */
	public double getDisplacement(NucleusBorderSegment segment){
		if(this.contains(segment)){
			
			double start = this.get(segment.getStartIndex());
			double end   = this.get(segment.getEndIndex());
			
			double min = Math.min(start, end);
			double max = Math.max(start, end);

			return max-min;
			
		} else {
			return 0;
		}
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
			if(	seg.getStartIndex()==segment.getStartIndex()
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
				
		if(!this.contains(segment)){
			throw new IllegalArgumentException("Segment is not part of this profile");
		}
		
		// test effect on all segments in list: the update should
		// not allow the endpoints to move within a segment other than
		// next or prev
		NucleusBorderSegment nextSegment = segment.nextSegment();
		NucleusBorderSegment prevSegment = segment.prevSegment();
		
		for(NucleusBorderSegment testSeg : this.segments){
			
			// if the proposed start or end index is found in another segment
			// that is not next or prev, do not proceed
			if(testSeg.contains(startIndex) || testSeg.contains(endIndex)){
								
				if(!testSeg.getName().equals(segment.getName())
						&& !testSeg.getName().equals(nextSegment.getName())
						&& !testSeg.getName().equals(prevSegment.getName())){
					segment.setLastFailReason("Index out of bounds of next and prev");
					return false;
				}
			}
		}
		
		// the basic checks have been passed; the update will not damage linkage
		// Allow the segment to determine if the update is valid and apply it
		
		if( segment.update(startIndex, endIndex)){
			return true;
		} else{
//			// If something is wrong, linkage may have been disrupted. Check for debugging
			if(testLinked()){
				return false;
			} else {
				
				IJ.log("Error updating SegmentedProfile: segments unlinked: "+segment.getLastFailReason());
				IJ.log(segment.toString());
				return false;
			}
		}
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
	public boolean adjustSegmentStart(NucleusBorderSegment segment, int amount){
		if(!this.contains(segment)){
			throw new IllegalArgumentException("Segment is not part of this profile");
		}
		
		// get the segment within this profile, not a copy that looks the same
		NucleusBorderSegment segmentToUpdate = this.getSegment(segment);
		
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
		
		// get the segment within this profile, not a copy
		NucleusBorderSegment segmentToUpdate = this.getSegment(name);
				
		int newValue = Utils.wrapIndex( segmentToUpdate.getEndIndex()+amount, segmentToUpdate.getTotalLength());
		return this.update(segmentToUpdate, segmentToUpdate.getStartIndex(), newValue);
	}
	
	public void nudgeSegments(int amount) throws Exception {
		this.segments = NucleusBorderSegment.nudge(getSegments(), amount);
	}
	
	/* (non-Javadoc)
	 * @see no.components.Profile#offset(int)
	 * Offset the segment by the given amount. Returns a copy
	 * of the profile.
	 */
	public SegmentedProfile offset(int newStartIndex) throws Exception {
	
		// get the basic profile with the offset applied
		Profile offsetProfile = super.offset(newStartIndex);
		
		/*
		The segmented profile starts like this:
		
		0     5          15                   35
		|-----|----------|--------------------|
		
		After applying newStartIndex=5, the profile looks like this:
		
		0          10                   30    35
		|----------|--------------------|-----|
		
		The new profile starts at index 'newStartIndex' in the original profile
		This means that we must subtract 'newStartIndex' from the segment positions
		to make them line up.
		
		The nudge function in NucleusBorderSegment moves endpoints by a specified amount
		
		*/
//		IJ.log("Offsetting segments to begin at "+newStartIndex );
//		IJ.log(NucleusBorderSegment.toString(getSegments()));
		
		List<NucleusBorderSegment> segments = NucleusBorderSegment.nudge(getSegments(), -newStartIndex);
		
		/*
		 * Ensure that the first segment in the list is at index zero
		 */
		
		return new SegmentedProfile(offsetProfile, segments);
	}
	
	
	
	/**
	 * Interpolate the segments of this profile to the proportional lengths of the
	 * segments in the template. The template must have the same number of segments.
	 * Both this and the template must be already offset to start at equivalent positions
	 * @param template the profile with segments to copy.
	 * @return
	 * @throws Exception
	 */
	public SegmentedProfile frankenNormaliseToProfile(SegmentedProfile template) throws Exception {
		
		if(this.getSegmentCount()!=template.getSegmentCount()){
			throw new IllegalArgumentException("Segment counts are different in profile and template");
		}
		
		/*
		 * The final frankenprofile is made of stitched together profiles from each segment
		 */
		List<Profile> finalSegmentProfiles = new ArrayList<Profile>(0);
				
		
		List<NucleusBorderSegment> tempList = template.getOrderedSegments();
		
		List<NucleusBorderSegment> testList = this.getOrderedSegments();
		
		
		int counter = 0;
		for(NucleusBorderSegment templateSeg : tempList){
			// Get the corresponding segment in this profile, by segment position
			NucleusBorderSegment testSeg = testList.get(counter++);

			// Interpolate the segment region to the new length
			Profile revisedProfile = interpolateSegment(testSeg, templateSeg.length());
			finalSegmentProfiles.add(revisedProfile);
		}
		
		
//		Recombine the segment profiles
		Profile mergedProfile = new Profile( Profile.merge(finalSegmentProfiles));
		
		SegmentedProfile result = new SegmentedProfile(mergedProfile, template.getSegments());
		return result;
	}
	
	/**
	 * The interpolation step of frankenprofile creation. The segment in this profile,
	 * with the same name as the template segment is interpolated to the length of the template,
	 * and returned as a new Profile.
	 * @param templateSegment the segment to interpolate
	 * @param newLength the new length of the segment profile
	 * @return the interpolated profile
	 * @throws Exception 
	 */
	private Profile interpolateSegment(NucleusBorderSegment testSeg, int newLength) throws Exception{
		
		// get the region within the segment as a new profile
		// Exclude the last index of each segment to avoid duplication
		// the first index is kept, because the first index is used for border tags
		int lastIndex = Utils.wrapIndex( testSeg.getEndIndex()-1, testSeg.getTotalLength());
		
		Profile testSegProfile = this.getSubregion(testSeg.getStartIndex(), lastIndex);

		// interpolate the test segments to the length of the median segments
		Profile revisedProfile = testSegProfile.interpolate(newLength);
		return revisedProfile;
	}

	
	/**
	 * Test if the given profile values are the same as in
	 * this profile.
	 * @param profile
	 * @return
	 */
	public boolean equals(SegmentedProfile profile){
		if(!super.equals(profile)){
			return false;
		}
		// check the segments
		for(String name : this.getSegmentNames()){
			if(!this.getSegment(name).equals(profile.getSegment(name))){
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void reverse() throws Exception {
		super.reverse();
		
		// reverse the segments
		// in a profile of 100
		// if a segment began at 10 and ended at 20, it should begin at 80 and end at 90
		
		// if is begins at 90 and ends at 10, it should begin at 10 and end at 90
		List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>();
		for(NucleusBorderSegment seg : this.getSegments()){
			
			// invert the segment by swapping start and end
			int newStart = (this.size()-1) - seg.getEndIndex();
			int newEnd   = Utils.wrapIndex(newStart+seg.length(), this.size());
			NucleusBorderSegment newSeg = new NucleusBorderSegment(newStart, newEnd, this.size());
			newSeg.setName(seg.getName());
			// since the order is reversed, add them to the top of the new list
			segments.add(0,newSeg);
		}
		NucleusBorderSegment.linkSegments(segments);
		this.setSegments(segments);
		
	}
	
	/**
	 * Attempt to merge the given segments into one segment. The segments must
	 * belong to the profile, and be adjacent
	 * @param segment1
	 * @param segment2
	 * @return
	 */
	public void mergeSegments(NucleusBorderSegment segment1, NucleusBorderSegment segment2) throws Exception {
		
		// Check the segments belong to the profile
		if(!this.contains(segment1) || !this.contains(segment2)){
			throw new IllegalArgumentException("An input segment is not part of this profile");
		}
		
		// Check the segments are linked
		if(!segment1.nextSegment().equals(segment2) && !segment1.prevSegment().equals(segment2)){
			throw new IllegalArgumentException("Input segments are not linked");
		}
		
		// Ensure we have the segments in the correct order
		NucleusBorderSegment firstSegment  = segment1.nextSegment().equals(segment2) ? segment1 : segment2;
		NucleusBorderSegment secondSegment = segment2.nextSegment().equals(segment1) ? segment1 : segment2;
		
		// Create the new segment
		int startIndex = firstSegment.getStartIndex();
		int endIndex = secondSegment.getEndIndex();
		NucleusBorderSegment mergedSegment = new NucleusBorderSegment(startIndex, endIndex, this.size());
		mergedSegment.setName(firstSegment.getName());
		
		mergedSegment.addMergeSource(firstSegment);
		mergedSegment.addMergeSource(secondSegment);
		
		// Replace the two segments in this profile
		List<NucleusBorderSegment> oldSegs = this.getSegments();
		List<NucleusBorderSegment> newSegs = new ArrayList<NucleusBorderSegment>();
		
		int position = 0;
		for(NucleusBorderSegment oldSegment : oldSegs){
			
			if(oldSegment.equals(firstSegment)){
				// add the merge instead
				mergedSegment.setPosition(position);
				newSegs.add(mergedSegment);
			} else if(oldSegment.equals(secondSegment)){
				// do nothing
			} else {
				// add the original segments
				oldSegment.setPosition(position);
				newSegs.add(oldSegment);
			}
			position++;
		}

		NucleusBorderSegment.linkSegments(newSegs);

		this.setSegments(newSegs);
	}
	
	/**
	 * Reverse a merge operation on a segment
	 * @param segment
	 */
	public void unmergeSegment(NucleusBorderSegment segment) throws Exception {
		// Check the segments belong to the profile
		if(!this.contains(segment) ){
			throw new IllegalArgumentException("Input segment is not part of this profile");
		}
		
		if(!segment.hasMergeSources()){
			throw new IllegalArgumentException("Segment does not have merge sources");
		}
		
		// Replace the two segments in this profile
		List<NucleusBorderSegment> oldSegs = this.getSegments();
		List<NucleusBorderSegment> newSegs = new ArrayList<NucleusBorderSegment>();
		
		int position = 0;
		for(NucleusBorderSegment oldSegment : oldSegs){

			if(oldSegment.equals(segment)){
				
				// add each of the old segments
				for(NucleusBorderSegment mergedSegment : segment.getMergeSources()){
					mergedSegment.setPosition(position);
					newSegs.add(mergedSegment);
					position++;
				}
				
			} else {
				
				// add the original segments
				oldSegment.setPosition(position);
				newSegs.add(oldSegment);
			}
			position++;
		}
		NucleusBorderSegment.linkSegments(newSegs);
		this.setSegments(newSegs);

	}
	
	/**
	 * Split a segment at the given index into two new segments
	 * @param segment the segment to split
	 * @param splitIndex the index to split at
	 * @throws Exception
	 */
	public void splitSegment(NucleusBorderSegment segment, int splitIndex) throws Exception {
		// Check the segments belong to the profile
		if(!this.contains(segment) ){
			throw new IllegalArgumentException("Input segment is not part of this profile");
		}
		
		if(!segment.contains(splitIndex)){
			throw new IllegalArgumentException("Splitting index is not within the segment");
		}
		
		// Replace the two segments in this profile
		List<NucleusBorderSegment> oldSegs = this.getSegments();
		List<NucleusBorderSegment> newSegs = new ArrayList<NucleusBorderSegment>();
		
		// Add the new segments to a list
		List<NucleusBorderSegment> splitSegments = new ArrayList<NucleusBorderSegment>();
		splitSegments.add(new NucleusBorderSegment(segment.getStartIndex(), splitIndex, segment.getTotalLength()));
		splitSegments.add(new NucleusBorderSegment(splitIndex, segment.getEndIndex(), segment.getTotalLength()));
		

		int position = 0;
		for(NucleusBorderSegment oldSegment : oldSegs){

			if(oldSegment.equals(segment)){
				
				// add each of the old segments
				for(NucleusBorderSegment mergedSegment : splitSegments){
					mergedSegment.setPosition(position);
					newSegs.add(mergedSegment);
					position++;
				}
				
			} else {
				
				// add the original segments
				oldSegment.setPosition(position);
				newSegs.add(oldSegment);
			}
			position++;
		}
		NucleusBorderSegment.linkSegments(newSegs);
		this.setSegments(newSegs);
		
	}
	

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 * Returns the segments in this profile as a string
	 */
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		for(String name : this.getSegmentNames()){
			builder.append(this.getSegment(name).toString()+"\n");
		}
		return builder.toString();
	}
	
	  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		    in.defaultReadObject();
		    this.firstSegment = segments.get(0);
		}
}
