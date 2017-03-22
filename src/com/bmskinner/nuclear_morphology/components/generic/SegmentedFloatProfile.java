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

package com.bmskinner.nuclear_morphology.components.generic;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.DefaultCellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;

/**
 * The default implementation of a segmented profile.
 * @author ben
 * @since 1.13.3
 *
 */
public class SegmentedFloatProfile extends FloatProfile implements ISegmentedProfile {
	private static final long serialVersionUID = 1L;

	// the segments
	protected IBorderSegment[] segments = new IBorderSegment[0];

	/**
	 * Construct using a regular profile and a list of border segments
	 * @param p the profile
	 * @param segments the list of segments to use
	 * @throws ProfileException 
	 */
	public SegmentedFloatProfile(final IProfile p, final List<IBorderSegment> segments) throws IndexOutOfBoundsException, ProfileException {		
		super(p);

		if(segments==null || segments.isEmpty()){
			throw new IllegalArgumentException("Segment list is null or empty in segmented profile contructor");
		}

		if(p.size() != segments.get(0).getTotalLength() ){
			throw new IndexOutOfBoundsException("Segments total length ("
					+segments.get(0).getTotalLength()
					+") does not fit profile ("+
					+p.size()
					+")");
		}
		


//		try {
			IBorderSegment.linkSegments(segments);
//		} catch (ProfileException e) {
//			warn("Error linking segments in profile constructor");
//			stack("Profile error linking segments", e);
//		}

		this.segments = new IBorderSegment[segments.size()];
		for(int i=0; i<segments.size(); i++){
			this.segments[i] =  segments.get(i);
		}
		
	}

	/**
	 * Construct using an existing profile. Copies the data
	 * and segments
	 * @param profile the segmented profile to copy
	 * @throws ProfileException 
	 * @throws IndexOutOfBoundsException 
	 */
	public SegmentedFloatProfile(final ISegmentedProfile profile) throws IndexOutOfBoundsException, ProfileException {
		this(profile, profile.getSegments());
	}

	/**
	 * Construct using a basic profile. Two segments are created 
	 * that span the entire profile, half each
	 * @param profile
	 */
	public SegmentedFloatProfile(IProfile profile) {
		super(profile);
		
		int midpoint = profile.size()/2;
		IBorderSegment segment1 = IBorderSegment.newSegment(0, midpoint, profile.size());

		segment1.setPosition(0);
		IBorderSegment segment2 = IBorderSegment.newSegment(midpoint, 0, profile.size());

		segment2.setPosition(1);
		List<IBorderSegment> segments = new ArrayList<IBorderSegment>();
		segments.add(segment1);
		segments.add(segment2);


		try {
			IBorderSegment.linkSegments(segments);
		} catch (ProfileException e) {
			warn("Error linking segments");
		}

		this.segments = new IBorderSegment[segments.size()];
		for(int i=0; i<segments.size(); i++){
			this.segments[i] =  segments.get(i);
		}
//		this.segments = segments;
	}

	/**
	 * Construct from an array of values
	 * @param values
	 * @throws Exception 
	 */
	public SegmentedFloatProfile(float[] values) {
		this( new FloatProfile(values));
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#hasSegments()
	 */
	@Override
	public boolean hasSegments(){
		if(this.segments==null || this.segments.length==0){
			return false;
		} else {
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegments()
	 */
	@Override
	public List<IBorderSegment> getSegments() {
		List<IBorderSegment> result = null;
		List<IBorderSegment> temp = new ArrayList<IBorderSegment>();
		for(IBorderSegment seg : segments){
			temp.add(seg);
		}
		
		try {
			result = IBorderSegment.copy(temp);
		} catch (ProfileException e) {
			error("Error copying segments", e);
		}
		return result;
	}


	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#segmentIterator()
	 */
	@Override
	public Iterator<IBorderSegment> segmentIterator() throws Exception {

		List<IBorderSegment> list = new ArrayList<IBorderSegment>();

		// find the first segment
		IBorderSegment first = null;

		for(IBorderSegment seg : this.segments){
			if(seg.getPosition()==ZERO_INDEX){
				first = seg;
			}
		}

		list = getSegmentsFrom(first);
		return list.iterator();
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegment(java.util.UUID)
	 */
	@Override
	public IBorderSegment getSegment(UUID id){
		for(IBorderSegment seg : this.segments){
			if(seg.getID().equals(id)){
				return seg;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#hasSegment(java.util.UUID)
	 */
	@Override
	public boolean hasSegment(UUID id){
		for(IBorderSegment seg : this.segments){
			if(seg.getID().equals(id)){
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegmentsFrom(java.util.UUID)
	 */
	@Override
	public List<IBorderSegment> getSegmentsFrom(UUID id) throws Exception {
		return getSegmentsFrom(getSegment(id));
	}

	/**
	 * Get the segments in order from the given segment
	 * @param firstSeg
	 * @return
	 * @throws Exception
	 */
	private List<IBorderSegment> getSegmentsFrom(IBorderSegment firstSeg) throws ProfileException{

		if(firstSeg==null){
			throw new IllegalArgumentException("Requested first segment is null");
		}


		List<IBorderSegment> result = new ArrayList<IBorderSegment>();
		int i = segments.length-1; // the number of segments 
		result.add(firstSeg);
		while(i>0){

			if(firstSeg.hasNextSegment()){
				firstSeg = firstSeg.nextSegment();
				result.add(firstSeg);
				i--;
			} else {
				throw new ProfileException(i+": No next segment in "+firstSeg.toString());
			}
		}
		return IBorderSegment.copy(result);
	}


	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getOrderedSegments()
	 */
	@Override
	public List<IBorderSegment> getOrderedSegments() {

		IBorderSegment firstSeg = null; // default to the first segment in the profile

		/*
		 * Choose the first segment of the profile to be the segment
		 * starting at the zero index
		 */
		for(IBorderSegment seg : segments){

			if(seg.getStartIndex()==ZERO_INDEX){
				firstSeg = seg;
			}
		}

		if(firstSeg==null){

			/*
			 * A subset of nuclei do not produce segment boundaries
			 */
			fine("Cannot get ordered segments");
			fine("Profile is "+this.toString());
			//			log(Level.WARNING, this.toString());
			fine("Using the first segment in the profile");
			firstSeg = this.getSegments().get(0); // default to the first segment in the profile
		}

		List<IBorderSegment> result;
		try {
			result = getSegmentsFrom(firstSeg);
		} catch (ProfileException e) {
			warn("Profile error getting segments");
			log(Level.FINE, "Profile error getting segments", e);
			result = new ArrayList<IBorderSegment>();
		}

		return result;

	}


	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegment(java.lang.String)
	 */
	@Override
	public IBorderSegment getSegment(String name){
		if(name==null){
			throw new IllegalArgumentException("Requested segment name is null");
		}

		for(IBorderSegment seg : this.segments){
			if(seg.getName().equals(name)){
				return seg;
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegment(components.nuclear.IBorderSegment)
	 */
	@Override
	public IBorderSegment getSegment(IBorderSegment segment){
		if(! this.contains(segment)){
			throw new IllegalArgumentException("Requested segment name is not present");
		}

		IBorderSegment result = null;
		for(IBorderSegment seg : this.segments){
			if(seg.equals(segment)){
				result = seg;
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegmentAt(int)
	 */
	@Override
	public IBorderSegment getSegmentAt(int position){


		IBorderSegment result = null;
		for(IBorderSegment seg : this.segments){
			if(seg.getPosition()==position){
				result = seg;
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegmentContaining(int)
	 */
	@Override
	public IBorderSegment getSegmentContaining(int index){

		IBorderSegment result = null;
		for(IBorderSegment seg : this.segments){
			if(seg.contains(index)){
				result = seg;
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#setSegments(java.util.List)
	 */
	@Override
	public void setSegments(List<IBorderSegment> segments) {
		if(segments==null || segments.isEmpty()){
			throw new IllegalArgumentException("Segment list is null or empty");
		}

		if(segments.get(0).getTotalLength()!=this.size()){
			throw new IllegalArgumentException("Segment list is from a different total length");
		}

		try {
			segments = IBorderSegment.copy(segments);
			
			this.segments = new IBorderSegment[segments.size()];
			for(int i=0; i<segments.size(); i++){
				this.segments[i] =  segments.get(i);
			}
			
		} catch (ProfileException e) {
			warn("Cannot copy segments");
		}
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#clearSegments()
	 */
	@Override
	public void clearSegments(){
		this.segments = new IBorderSegment[0];

	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegmentNames()
	 */
	@Override
	public List<String> getSegmentNames(){
		List<String> result = new ArrayList<String>();
		for(IBorderSegment seg : this.segments){
			result.add(seg.getName());
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegmentIDs()
	 */
	@Override
	public List<UUID> getSegmentIDs(){
		List<UUID> result = new ArrayList<UUID>();
		for(IBorderSegment seg : this.segments){
			result.add(seg.getID());
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getSegmentCount()
	 */
	@Override
	public int getSegmentCount(){
		return this.segments.length;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#getDisplacement(components.nuclear.IBorderSegment)
	 */
	@Override
	public double getDisplacement(IBorderSegment segment){
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

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#contains(components.nuclear.IBorderSegment)
	 */
	@Override
	public boolean contains(IBorderSegment segment){
		if(segment==null){
			return false;
		}
		boolean result = false;
		for(IBorderSegment seg : this.segments){
			if(	seg.getStartIndex()==segment.getStartIndex()
					&& seg.getEndIndex()==segment.getEndIndex()
					&& seg.getTotalLength()==this.size()

					){
				return true;
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#update(components.nuclear.IBorderSegment, int, int)
	 */
	@Override
	public boolean update(IBorderSegment segment, int startIndex, int endIndex){

		if(!this.contains(segment)){
			throw new IllegalArgumentException("Segment is not part of this profile");
		}

		// test effect on all segments in list: the update should
		// not allow the endpoints to move within a segment other than
		// next or prev
		IBorderSegment nextSegment = segment.nextSegment();
		IBorderSegment prevSegment = segment.prevSegment();

		for(IBorderSegment testSeg : this.segments){

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
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#adjustSegmentStart(java.util.UUID, int)
	 */
	@Override
	public boolean adjustSegmentStart(UUID id, int amount){
		if(!this.getSegmentIDs().contains(id)){
			throw new IllegalArgumentException("Segment is not part of this profile");
		}

		// get the segment within this profile, not a copy
		IBorderSegment segmentToUpdate = this.getSegment(id);

		int newValue = DefaultCellularComponent.wrapIndex( segmentToUpdate.getStartIndex()+amount, segmentToUpdate.getTotalLength());
		return this.update(segmentToUpdate, newValue, segmentToUpdate.getEndIndex());
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#adjustSegmentEnd(java.util.UUID, int)
	 */
	@Override
	public boolean adjustSegmentEnd(UUID id, int amount){

		if(!this.getSegmentIDs().contains(id)){
			throw new IllegalArgumentException("Segment is not part of this profile");
		}

		// get the segment within this profile, not a copy
		IBorderSegment segmentToUpdate = this.getSegment(id);

		int newValue = DefaultCellularComponent.wrapIndex( segmentToUpdate.getEndIndex()+amount, segmentToUpdate.getTotalLength());
		return this.update(segmentToUpdate, segmentToUpdate.getStartIndex(), newValue);
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#nudgeSegments(int)
	 */
	@Override
	public void nudgeSegments(int amount) {
		
		List<IBorderSegment> result;
		try {
			result = IBorderSegment.nudge(getSegments(), amount);
		} catch (ProfileException e) {
			fine("Error offsetting segments", e);
			return;
		}
		this.segments = new IBorderSegment[segments.length];
		for(int i=0; i<segments.length; i++){
			this.segments[i] =  result.get(i);
		}
		
	}


	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#offset(int)
	 */
	@Override
	public ISegmentedProfile offset(int offset) throws ProfileException {
		
		
		
		// get the basic profile with the offset applied
		IProfile offsetProfile = super.offset(offset);

		/*
		The segmented profile starts like this:

		0     5          15                   35
		|-----|----------|--------------------|

		After applying offset=5, the profile looks like this:

		0          10                   30    35
		|----------|--------------------|-----|

		The new profile starts at index 'offset' in the original profile
		This means that we must subtract 'offset' from the segment positions
		to make them line up.

		The nudge function in IBorderSegment moves endpoints by a specified amount

		 */
//		fine("Offsetting segments in profile by "+offset );
		
//		fine("Profile length: "+size()+"; segment total: "+segments[0].getTotalLength());

		List<IBorderSegment> segments = IBorderSegment.nudge(getSegments(), -offset);

		/*
		 * Ensure that the first segment in the list is at index zero
		 */

		return new SegmentedFloatProfile(offsetProfile, segments);
	}


	@Override
	public ISegmentedProfile interpolateSegments(int length) throws ProfileException{
		
		// get the proportions of the existing segments
//		log("Segment interpolation begun: "+segments.length);
		
		double[] props = new double[segments.length];
		
		for(int i=0; i<segments.length; i++){
			props[i] = this.getIndexProportion(segments[i].getStartIndex());
		}
		
		// get the target start indexes of the new segments
		
		int[] newStarts = new int[segments.length];
		
		for(int i=0; i<segments.length; i++){
			newStarts[i] =  (int) (props[i] * (double) length );
		}
		
		// Make the new segments
		List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>(segments.length);
		for(int i=0; i<segments.length-1; i++){
			
			int testStart = newStarts[i];
			int testEnd   = newStarts[i+1];
			
			if(testEnd - testStart < IBorderSegment.MINIMUM_SEGMENT_LENGTH){
				newStarts[i+1] = newStarts[i+1]+1;
			}
			
			IBorderSegment seg = new DefaultBorderSegment(newStarts[i], newStarts[i+1], length, segments[i].getID());
			newSegs.add(seg);
		}
		
		// Add final segment
		// We need to correct start and end positions appropriately.
		// Since the start position may already have been set, we need to adjust the end,
		// i.e the start position of the first segment.
		
		int firstStart = newStarts[0];
		int lastStart  = newStarts[segments.length-1];
		if(segments[0].wraps(newStarts[segments.length-1], newStarts[0])){
			// wrapping final segment
			if(firstStart + (length-lastStart) < IBorderSegment.MINIMUM_SEGMENT_LENGTH){
				newStarts[0] = firstStart+1; // update the start in the array
				newSegs.get(0).update(firstStart, newSegs.get(1).getStartIndex()); // update the new segment
			}
			
		} else {
			// non-wrapping final segment
			if( firstStart - lastStart < IBorderSegment.MINIMUM_SEGMENT_LENGTH){
				newStarts[0] = firstStart+1; // update the start in the array
				newSegs.get(0).update(firstStart, newSegs.get(1).getStartIndex()); // update the new segment

			}
		}
		
		IBorderSegment lastSeg = new DefaultBorderSegment(newStarts[segments.length-1], newStarts[0], length, segments[segments.length-1].getID());
		newSegs.add(lastSeg);
		

		if(newSegs.size() != segments.length){
			throw new ProfileException("Error interpolating segments");
		}
		
		// interpolate the IProfile
		
		IProfile newProfile = super.interpolate(length);
		
		// assign new segments
		IBorderSegment.linkSegments(newSegs);	
		
//		log("Segment interpolation complete "+newSegs.size());
		
		return new SegmentedFloatProfile(newProfile, newSegs);
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#frankenNormaliseToProfile(components.generic.ISegmentedProfile)
	 */
	@Override
	public ISegmentedProfile frankenNormaliseToProfile(ISegmentedProfile template) throws ProfileException {

		if(this.getSegmentCount()!=template.getSegmentCount()){
			throw new IllegalArgumentException("Segment counts are different in profile and template");
		}

		/*
		 * The final frankenprofile is made of stitched together profiles from each segment
		 */
		List<IProfile> finalSegmentProfiles = new ArrayList<IProfile>(segments.length);



		for(UUID segID : template.getSegmentIDs()){
			// Get the corresponding segment in this profile, by segment position
			IBorderSegment testSeg     = this.getSegment(segID);
			IBorderSegment templateSeg = template.getSegment(segID);

			if(testSeg==null){
				throw new ProfileException("Cannot find segment "+segID+" in test profile");
			}

			if(templateSeg==null){
				throw new ProfileException("Cannot find segment "+segID+" in template profile");
			}

			// Interpolate the segment region to the new length
			IProfile revisedProfile = interpolateSegment(testSeg, templateSeg.length());
			finalSegmentProfiles.add(revisedProfile);
		}


		//		Recombine the segment profiles
		IProfile mergedProfile = new FloatProfile( FloatProfile.merge(finalSegmentProfiles));

		ISegmentedProfile result = new SegmentedFloatProfile(mergedProfile, template.getSegments());
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
	private IProfile interpolateSegment(IBorderSegment testSeg, int newLength) throws ProfileException {

		// get the region within the segment as a new profile
		// Exclude the last index of each segment to avoid duplication
		// the first index is kept, because the first index is used for border tags
		int lastIndex = DefaultCellularComponent.wrapIndex( testSeg.getEndIndex()-1, testSeg.getTotalLength());

		IProfile testSegProfile = this.getSubregion(testSeg.getStartIndex(), lastIndex);

		// interpolate the test segments to the length of the median segments
		IProfile revisedProfile = testSegProfile.interpolate(newLength);
		return revisedProfile;
	}


	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#equals(components.generic.ISegmentedProfile)
	 */
	@Override
	public boolean equals(ISegmentedProfile profile){
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

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#reverse()
	 */
	@Override
	public void reverse() {
		super.reverse();

		// reverse the segments
		// in a profile of 100
		// if a segment began at 10 and ended at 20, it should begin at 80 and end at 90

		// if is begins at 90 and ends at 10, it should begin at 10 and end at 90
		List<IBorderSegment> segments = new ArrayList<IBorderSegment>();
		for(IBorderSegment seg : this.getSegments()){

			// invert the segment by swapping start and end
			int newStart = (this.size()-1) - seg.getEndIndex();
			int newEnd   = DefaultCellularComponent.wrapIndex(newStart+seg.length(), this.size());
			IBorderSegment newSeg = IBorderSegment.newSegment(newStart, newEnd, this.size(), seg.getID());
			//			newSeg.setName(seg.getName());
			// since the order is reversed, add them to the top of the new list
			segments.add(0,newSeg);
		}
		try {
			IBorderSegment.linkSegments(segments);
		} catch (ProfileException e) {
			warn("Error linking segments");
			stack("Cannot link segments in reversed profile", e);
		}
		this.setSegments(segments);

	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#mergeSegments(components.nuclear.IBorderSegment, components.nuclear.IBorderSegment, java.util.UUID)
	 */
	@Override
	public void mergeSegments(IBorderSegment segment1, IBorderSegment segment2, UUID id) throws ProfileException {

		// Check the segments belong to the profile
		if(!this.contains(segment1) || !this.contains(segment2)){
			throw new IllegalArgumentException("An input segment is not part of this profile");
		}

		// Check the segments are linked
		if(!segment1.nextSegment().equals(segment2) && !segment1.prevSegment().equals(segment2)){
			throw new IllegalArgumentException("Input segments are not linked");
		}

		// Ensure we have the segments in the correct order
		IBorderSegment firstSegment  = segment1.nextSegment().equals(segment2) ? segment1 : segment2;
		IBorderSegment secondSegment = segment2.nextSegment().equals(segment1) ? segment1 : segment2;

		// Create the new segment
		int startIndex = firstSegment.getStartIndex();
		int endIndex = secondSegment.getEndIndex();
		IBorderSegment mergedSegment = IBorderSegment.newSegment(startIndex, endIndex, this.size(), id);

		mergedSegment.addMergeSource(firstSegment);
		mergedSegment.addMergeSource(secondSegment);

		// Replace the two segments in this profile
		List<IBorderSegment> oldSegs = this.getSegments();
		List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();

		int position = 0;
		for(IBorderSegment oldSegment : oldSegs){

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

		IBorderSegment.linkSegments(newSegs);

		this.setSegments(newSegs);
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#unmergeSegment(components.nuclear.IBorderSegment)
	 */
	@Override
	public void unmergeSegment(IBorderSegment segment) throws ProfileException {
		// Check the segments belong to the profile
		if(!this.contains(segment) ){
			throw new IllegalArgumentException("Input segment is not part of this profile");
		}

		if(!segment.hasMergeSources()){
			throw new IllegalArgumentException("Segment does not have merge sources");
		}

		// Replace the two segments in this profile
		List<IBorderSegment> oldSegs = this.getSegments();
		List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();

		int position = 0;
		for(IBorderSegment oldSegment : oldSegs){

			if(oldSegment.equals(segment)){

				// add each of the old segments
				for(IBorderSegment mergedSegment : segment.getMergeSources()){
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
		IBorderSegment.linkSegments(newSegs);
		this.setSegments(newSegs);

	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#splitSegment(components.nuclear.IBorderSegment, int, java.util.UUID, java.util.UUID)
	 */
	@Override
	public void splitSegment(IBorderSegment segment, int splitIndex, UUID id1, UUID id2) throws ProfileException {
		// Check the segments belong to the profile
		if(!this.contains(segment) ){
			throw new IllegalArgumentException("Input segment is not part of this profile");
		}

		if(!segment.contains(splitIndex)){
			throw new IllegalArgumentException("Splitting index is not within the segment");
		}

		// Replace the two segments in this profile
		List<IBorderSegment> oldSegs = this.getSegments();
		List<IBorderSegment> newSegs = new ArrayList<IBorderSegment>();

		// Add the new segments to a list
		List<IBorderSegment> splitSegments = new ArrayList<IBorderSegment>();
		splitSegments.add(IBorderSegment.newSegment(segment.getStartIndex(), splitIndex, segment.getTotalLength(), id1));
		splitSegments.add(IBorderSegment.newSegment(splitIndex, segment.getEndIndex(), segment.getTotalLength(), id2));

		segment.addMergeSource(splitSegments.get(0));
		segment.addMergeSource(splitSegments.get(1));

		int position = 0;
		for(IBorderSegment oldSegment : oldSegs){

			if(oldSegment.equals(segment)){

				// add each of the old segments
				//				for(IBorderSegment mergedSegment : splitSegments){
				//					mergedSegment.setPosition(position);
				newSegs.add(segment);
				//					position++;
				//				}

			} else {

				// add the original segments
				oldSegment.setPosition(position);
				newSegs.add(oldSegment);
			}
			position++;
		}
		IBorderSegment.linkSegments(newSegs);
		this.setSegments(newSegs);
		this.unmergeSegment(segment);

	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#toString()
	 */
	@Override
	public String toString(){
		StringBuilder builder = new StringBuilder();
		for(IBorderSegment seg : this.segments){
			builder.append(seg.toString()+System.getProperty("line.separator"));
		}
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#valueString()
	 */
	@Override
	public String valueString(){
		return super.toString();
	}




	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((segments == null) ? 0 : Arrays.hashCode(segments));
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.ISegmentedProfile#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SegmentedFloatProfile other = (SegmentedFloatProfile) obj;
		if (segments == null) {
			if (other.segments != null)
				return false;
		} else if (!Arrays.equals(segments,  other.segments))
			return false;
		return true;
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		//		finest("\tWriting segmented profile");
		out.defaultWriteObject();
		//		finest("\tWrote segmented profile");
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		//		finest("\tReading segmented profile");
		in.defaultReadObject();
		//		finest("\tRead segmented profile");
		if(size() != segments[0].getTotalLength()){
			log("Error reading segments: "+" segment length "+segments[0].getTotalLength()+" different to profile "+size() );
		}
	}
}
