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
/*
  -----------------------
  NUCLEUS BORDER SEGMENT
  -----------------------
  A segment is made of multiple NucleusBorderPoints.
  A Nucleus can contain many segments, which may overlap.
  The NucleusBorderPoints are not stored with a segment;
  the segment merely provides a way to interact with the points
  as a group.
*/  
package components.nuclear;

import ij.IJ;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import utility.ProfileException;
import logging.Loggable;
import components.AbstractCellularComponent;

public class NucleusBorderSegment  implements Serializable, Iterable<Integer>, Loggable{

	private static final long serialVersionUID = 1L;
	
	// the smallest number of values in a segment
	// Set to 3 (a start, midpoint and an end) so that the minimum length
	// in the ProfileSegmenter can be interpolated downwards without
	// causing errors when fitting segments to individual nuclei
	public static final int MINIMUM_SEGMENT_LENGTH       = 3; 
	public static final int INTERPOLATION_MINIMUM_LENGTH = 2;
	
	
	private int    startIndex;
	private int    endIndex;
//	private String name      = null;
	
	private int    totalLength; // the total length of the profile that this segment is a part of 
	
	private NucleusBorderSegment prevSegment = null; // track the previous segment in the profile
	private NucleusBorderSegment nextSegment = null; // track the next segment in the profile
	
	private String lastFailReason = "No fail";
	
	private List<NucleusBorderSegment> mergeSources = new ArrayList<NucleusBorderSegment>();
	
	private int positionInProfile = 0; // for future refactor
	
	private boolean startPositionLocked = false; // allow the start index to be fixed
	
	private UUID uuid; // allows keeping a consistent track of segment IDs with a profile

	/**
	 * Construct with an existing UUID. This allows nucleus segments to directly
	 * track median profile segments
	 * @param startIndex 
	 * @param endIndex
	 * @param total
	 * @param id
	 */
	public NucleusBorderSegment(int startIndex, int endIndex, int total, UUID id){
		
		// ensure that the segment meets minimum length requirements
		int testLength = 0;
		if(startIndex < endIndex){ // no wrap
			testLength = endIndex - startIndex;
		} else { // wrap
			testLength = endIndex + (total-startIndex);
		}
		
		if(testLength < MINIMUM_SEGMENT_LENGTH){
			throw new IllegalArgumentException("Cannot create segment of length "
						+ testLength
						+ " from " + startIndex + " to " + endIndex
						+ ": shorter than "+MINIMUM_SEGMENT_LENGTH);
		}
		this.startIndex   = startIndex;
		this.endIndex     = endIndex;
		this.totalLength  = total;
		this.mergeSources = new ArrayList<NucleusBorderSegment>();
		this.uuid         = id;
	}
	
	
	/**
	 * Construct with a default random id
	 * @param startIndex
	 * @param endIndex
	 * @param total
	 */
	public NucleusBorderSegment(int startIndex, int endIndex, int total){
		this( startIndex, endIndex, total, java.util.UUID.randomUUID());
		
	}

	/**
	 * Make a copy of the given segment, including the ID
	 * @param n
	 */
	public NucleusBorderSegment(NucleusBorderSegment n){
		this.uuid         = n.getID();
		this.startIndex   = n.getStartIndex();
		this.endIndex 	  = n.getEndIndex();
		this.totalLength  = n.getTotalLength();
		this.nextSegment  = n.nextSegment();
		this.prevSegment  = n.prevSegment();
		this.mergeSources = n.getMergeSources();
		this.startPositionLocked = n.isStartPositionLocked();
	}

	/*
		----------------
		Getters
		----------------
	*/
	
	public UUID getID(){
		return this.uuid;
	}
	
	
	/**
	 * Get a copy of the merge source segments
	 * @return
	 */
	public List<NucleusBorderSegment> getMergeSources(){
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>();
		for(NucleusBorderSegment seg : this.mergeSources){
			result.add( new NucleusBorderSegment(seg));
		}
		return result;
	}
	
	public void addMergeSource(NucleusBorderSegment seg){
		this.mergeSources.add(seg);
	}
	
	/**
	 * Test if this segment is a merge of other segments
	 * @return
	 */
	public boolean hasMergeSources(){
		if(this.mergeSources.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	public String getLastFailReason(){
		return this.lastFailReason;
	}
	
	public void setLastFailReason(String reason){
		this.lastFailReason = reason;
	}

	public int getStartIndex(){
		return this.startIndex;
	}

	public int getEndIndex(){
		return this.endIndex;
	}
	
	
	
	/**
	 * Get the index closest to the fraction
	 * of the way through the segment
	 * @param d a fraction between 0 (start) and 1 (end)
	 * @return the nearest index, or -1 on error
	 */
	public int getProportionalIndex(double d){
		if(d<0 || d > 1){
			return -1;
		}
		
		double desiredDistanceFromStart = (double) this.length() * d;
		
		int target = (int) desiredDistanceFromStart;
		
		int counter = 0;
		Iterator<Integer> it = this.iterator();
		while(it.hasNext()){
			int index = it.next();
			
			if(counter==target){
				return index;
			}
			counter++;
		}
		return -1;
		
	}
	
	/**
	 * Get the proportion of the given index along the segment
	 * from zero to one. Returns -1 if the index was not found
	 * @param index the index to test
	 * @return
	 */
	public double getIndexProportion(int index){
		if(!this.contains(index)){
			finest("Segment does not contain index "+index);
			return -1;
		}
		
		int counter = 0;
		Iterator<Integer> it = this.iterator();
		while(it.hasNext()){
			int test = it.next();
			
			if(index==test){
				return (double) ( (double) counter / (double) this.length());
			}
			counter++;
		}
		finest("Error finding position of index "+index+", returning -1");
		finest("Listing indexes within segment");
		it = this.iterator();
		while(it.hasNext()){
			int test = it.next();
			finest("Segment contains index "+test);
			counter++;
		}
		return -1;
	}
	
//	public String getOldName(){
////		if(this.name==null){
////			IJ.log("Name is null on segment getName()");
////			return null;
////		}
//		return this.name;
//	}
	
	/**
	 * Get the name of the segment in the form "Seg_n"
	 * where n is the position in the profile
	 * @return
	 */
	public String getName(){
		return "Seg_"+this.positionInProfile;
	}

	// when using this, use wrapIndex()!
	public int getMidpointIndex(){
		if(this.wraps()){
			
			int midLength = this.length() >> 1 ;
			if( midLength+startIndex < this.getTotalLength()){
				return midLength+startIndex;
			} else {
				return endIndex - midLength;
			}
			
		} else {
			return ((endIndex- startIndex)/2) + startIndex;
		}
	}
	
	/**
	 * Get the shortest distance of the given index to the start
	 * of the segment
	 * @param index
	 * @return
	 */
	public int getDistanceToStart(int index){

		int startIndex 	= this.getStartIndex();

		int distForwards 	= Math.abs(index - startIndex);
		int distBackwards 	= this.length() - distForwards;
		
		int result = Math.min(distForwards, distBackwards);
		return result;	
	}
	
	/**
	 * Get the shortest distance of the given index to the end
	 * of the segment
	 * @param index
	 * @return
	 */
	public int getDistanceToEnd(int index){

		int endIndex 	= this.getEndIndex();

		int distForwards 	= Math.abs(index - endIndex);
		int distBackwards 	= this.length() - distForwards;
		
		int result = Math.min(distForwards, distBackwards);
		return result;	
	}
	
	
	
	public boolean isStartPositionLocked() {
		return startPositionLocked;
	}

	public void setStartPositionLocked(boolean startPositionLocked) {
		this.startPositionLocked = startPositionLocked;
	}

	/**
	 * get the total length of the profile that this segment is a part of 
	 * @return
	 */
	public int getTotalLength(){
		return this.totalLength;
	}
	
	/**
	 * Get the next segment. That is, the segment whose
	 * start index is the end index of this segment
	 * @return the segment
	 */
	public NucleusBorderSegment nextSegment(){
		return this.nextSegment;
	}
	
	/**
	 * Get the previous segment. That is, the segment whose
	 * end index is the start index of this segment
	 * @return the segment
	 */
	public NucleusBorderSegment prevSegment(){
		return this.prevSegment;
	}
	
	/**
	 * Make this segment shorter by the given amount.
	 * The start index is moved forward. The previous segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public boolean shortenStart(int value){
		int newValue = AbstractCellularComponent.wrapIndex(this.getStartIndex()+value, this.getTotalLength());
		return this.update(newValue, this.getEndIndex());
	}
	
	/**
	 * Make this segment shorter by the given amount.
	 * The end index is moved back. The next segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public boolean shortenEnd(int value){
		int newValue = AbstractCellularComponent.wrapIndex(this.getEndIndex()-value, this.getTotalLength());
		return this.update(this.getStartIndex(), newValue);

	}
	
	/**
	 * Make this segment longer by the given amount.
	 * The start index is moved back. The previous segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public boolean lengthenStart(int value){
		int newValue = AbstractCellularComponent.wrapIndex( this.getStartIndex()-value, this.getTotalLength());
		return this.update(newValue, this.getEndIndex());
	}
	
	/**
	 * Make this segment longer by the given amount.
	 * The end index is moved forward. The previous segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public boolean lengthenEnd(int value){
		int newValue = AbstractCellularComponent.wrapIndex( this.getEndIndex()+value, this.getTotalLength());
		return this.update(this.getStartIndex(), newValue);
	}
		
	/**
	 * Get the length of this segment. Accounts
	 * for array wrapping
	 * @return
	 */
	public int length(){
		return testLength(this.getStartIndex(), this.getEndIndex());
	}
	
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endIndex;
		result = prime * result + startIndex;
		result = prime * result + totalLength;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NucleusBorderSegment other = (NucleusBorderSegment) obj;
		if (endIndex != other.endIndex)
			return false;
		if (startIndex != other.startIndex)
			return false;
		if (totalLength != other.totalLength)
			return false;
		return true;
	}

//	/**
//	 * Test if the given segment matches this segment in position, length
//	 * and name
//	 * @param test
//	 * @return
//	 */
//	public boolean equals(NucleusBorderSegment test){
//		
//		if(test==null){
//			return false;
//		}
//		
//		if(test.getTotalLength()!=this.getTotalLength()){
//			return false;
//		}
//
//		if(test.getStartIndex()!=this.getStartIndex() 
//				|| test.getEndIndex()!=this.getEndIndex()){
//			return false;
//		}
//		return true;
//	}
	
	/**
	 * Test the effect of new start and end indexes on the length
	 * of the segment. Use for validating updates. Also called by
	 * length() using real values
	 * @param start the new start index
	 * @param end the new end index
	 * @return the new segment length
	 */
	public int testLength(int start, int end){
		if(wraps(start, end)){ // the segment wraps
			return end + (this.getTotalLength()-start);
		} else{
			return end - start;
		}
	}
	
	/**
	 * Check if the segment would wrap with the given start
	 * and end points (i.e contains 0)
	 * @param start the start index
	 * @param end the end index
	 * @return
	 */
	public boolean wraps(int start, int end){
		if(end<=start){ // the segment wraps
			return true;
		} else{
			return false;
		}
	}
	
	/**
	 * Test if the segment currently wraps
	 * @return
	 */
	public boolean wraps(){
		return wraps(this.getStartIndex(), this.getEndIndex());
	}
	
	/**
	 * Test if the segment contains the given index
	 * @param index the index to test
	 * @return
	 */
	public boolean contains(int index){
		return testContains(this.getStartIndex() , this.getEndIndex(), index);
	}
	
	/**
	 * Test if the segment would contain the given index if it had
	 * the specified start and end indexes. Acts as a wrapper for the real
	 * contains()
	 * @param start the start to test
	 * @param end the end to test
	 * @param index the index to test
	 * @return 
	 */
	public boolean testContains(int start, int end, int index){
		if(index < 0 || index > this.getTotalLength()){
			throw new IllegalArgumentException("Index is outside the total profile length: "+index);
		}
		
		if(wraps(start, end)){ // wrapped
			if(index<=end || index>start){
				return true;
			}
		} else{ // regular
			if(index>=start && index<end){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Test if a proposed update affects this segment
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	private boolean updateAffectsThisSegment(int startIndex, int endIndex){
		if(startIndex != this.getStartIndex() || endIndex != this.getEndIndex()){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Update the segment to the given position. Also updates the 
	 * previous and next segments. Error if the values cause any segment
	 * to become negative length
	 * @param start the new start index
	 * @param end the new end index
	 */
	public boolean update(int startIndex, int endIndex){
		
		if(this.startPositionLocked){ // don't allow locked segments to update
			this.lastFailReason = "Segment locked";
			return false;
		}
		
		this.lastFailReason = "No fail";
//		 Check the incoming data
		if(startIndex < 0 || startIndex > this.getTotalLength()){
			throw new IllegalArgumentException("Start index is outside the profile range: "+startIndex);
		}
		if(endIndex < 0 || endIndex > this.getTotalLength()){
			throw new IllegalArgumentException("End index is outside the profile range: "+endIndex);
		}
		
		// only run an update and checks if the update will actually
		// cause changes to the segment. If not, return true so as not
		// to interfere with other linked segments
		if(updateAffectsThisSegment(startIndex, endIndex)){

			// Check that the new positions will not make this segment too small
			int testLength = testLength(startIndex, endIndex);
			if(testLength < MINIMUM_SEGMENT_LENGTH){
				this.lastFailReason = startIndex+"-"+endIndex+": segment length ("+testLength+") cannot be smaller than "+MINIMUM_SEGMENT_LENGTH;
				return false;
			}

			// Check that next and previous segments are not invalidated by length change
			// i.e the max length increase backwards is up to the MIN_SEG_LENGTH of the
			// previous segment, and the max length increase forwards is up to the 
			// MIN_SEG_LENGTH of the next segment

			if(this.hasPrevSegment()){
				int prevTestLength = this.prevSegment().testLength(this.prevSegment().getStartIndex(), startIndex);
				if( prevTestLength < MINIMUM_SEGMENT_LENGTH){
					this.lastFailReason = startIndex
							+"-"+endIndex
							+": Previous segment length cannot be smaller than "
							+MINIMUM_SEGMENT_LENGTH
							+"; would be "
							+this.prevSegment().getStartIndex()+"-"
							+startIndex
							+"("+prevTestLength+")";
					return false;
				}
			}
			if(this.hasNextSegment()){
				int nextTestLength = this.nextSegment().testLength(endIndex, this.nextSegment().getEndIndex());
				if( nextTestLength < MINIMUM_SEGMENT_LENGTH){
					this.lastFailReason = startIndex
							+"-"+endIndex
							+": Next segment length cannot be smaller than "
							+MINIMUM_SEGMENT_LENGTH
							+"; would be "
							+endIndex+"-"
							+this.nextSegment().getEndIndex()
							+"("+nextTestLength+")";
					return false;
				}
			}

			// check that updating will not cause segments to overlap or invert
			// i.e. where a start becomes greater than an end without begin part of
			// an array wrap
			if(startIndex > endIndex){

				if(!this.testContains(startIndex , endIndex, 0)){
					this.lastFailReason = startIndex+"-"+endIndex+": Operation would cause this segment to invert";
					return false;
				}

			}

			// also test the effect on the next and previous segments
			if(this.hasPrevSegment()){
				if(this.prevSegment().getStartIndex() > startIndex){

					if(!this.prevSegment().wraps() && this.prevSegment().wraps(startIndex, endIndex)){
						this.lastFailReason = startIndex+"-"+endIndex+": Operation would cause prev segment to invert";
						return false;
					}

					// another wrapping test - if the new positions induce a wrap, the segment should contain 0
					if(this.prevSegment().wraps(startIndex, endIndex) && !this.prevSegment().testContains(startIndex, endIndex, 0)){
						this.lastFailReason = startIndex+"-"+endIndex+": Operation would cause prev segment to invert";
						return false;
					}
				}
			}

			if(this.hasNextSegment()){
				if( endIndex > this.nextSegment().getEndIndex()){

					// if the next segment goes from not wrapping to wrapping when this segment is altered,
					// an inversion must have occurred. Prevent.
					if(!this.nextSegment().wraps() && this.nextSegment().wraps(startIndex, endIndex)){
						this.lastFailReason = startIndex+"-"+endIndex+": Operation would cause next segment to invert";
						return false;
					}

					// another wrapping test - if the new positions induce a wrap, the segment should contain 0
					if(this.nextSegment().wraps(startIndex, endIndex) && !this.nextSegment().testContains(startIndex, endIndex, 0)){
						this.lastFailReason = startIndex+"-"+endIndex+": Operation would cause next segment to invert";
						return false;
					}
				}
			}

			// All checks have been passed; the update can proceed


			//		 wrap in if to ensure we don't go in circles forever when testing a circular profile
			if(this.getStartIndex()!=startIndex){
				this.startIndex = startIndex;
				if(this.hasPrevSegment()){
					NucleusBorderSegment prev = this.prevSegment();
					prev.update(prev.getStartIndex(), startIndex);
				}
			}

			if(this.getEndIndex()!=endIndex){
				this.endIndex = endIndex;

				if(this.hasNextSegment()){
					NucleusBorderSegment next = this.nextSegment();
					next.update(endIndex, next.getEndIndex());
				}
			}
			return true;
		} else {
			// update does not affect this segment
			return true;
		}
	}
	

	/**
	 * Set the next segment in the profile from this
	 * @param s
	 */
	public void setNextSegment(NucleusBorderSegment s){
		if(s.getTotalLength() != this.getTotalLength()){
			throw new IllegalArgumentException("Segment has a different total length");
		}
		if(s.getStartIndex() != this.getEndIndex()){
			throw new IllegalArgumentException("Segment start ("+s.getStartIndex()+") does not overlap this end: "+this.getEndIndex());
		}
		
		this.nextSegment = s;
	}
	
	/**
	 * Set the previous segment in the profile from this
	 * @param s
	 */
	public void setPrevSegment(NucleusBorderSegment s){
		if(s.getTotalLength() != this.getTotalLength()){
			throw new IllegalArgumentException("Segment has a different total length");
		}
		if(s.getEndIndex() != this.getStartIndex()){
			throw new IllegalArgumentException("Segment end ("+s.getEndIndex()+") does not overlap start: "+this.getStartIndex());
		}
		
		this.prevSegment = s;
	}
	
	/**
	 * Check if a next segment has been added
	 * @return
	 */
	public boolean hasNextSegment(){
		if(this.nextSegment()!=null){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Check if a previous segment has been added
	 * @return
	 */
	public boolean hasPrevSegment(){
		if(this.prevSegment()!=null){
			return true;
		} else {
			return false;
		}
	}

//	public void setName(String s){
//		this.name = s;
//	}
	
	/**
	 * Set the position in the segmented profile.
	 * Should be clockwise from the reference point.
	 * @param i
	 */
	public void setPosition(int i){
		this.positionInProfile = i;
	}
	
	/**
	 * Get the position in the segmented profile.
	 * Should be clockwise from the reference point.
	 */
	public int getPosition(){
		return this.positionInProfile;
	}
	
	/**
	 * Write toString() onto the ImageJ log window 
	 */
	public void print(){
		IJ.log(this.toString());
	}
	
	public String toString(){
		
		StringBuilder builder = new StringBuilder();

		builder.append("Segment ");
		builder.append(this.getName());
		builder.append(" | ");
		builder.append(this.getID());
		builder.append(" | ");
		builder.append(this.getPosition());
		builder.append(" | ");
		builder.append(this.startIndex);
		builder.append(" | ");
		builder.append(this.endIndex);
		builder.append(" | ");
		builder.append(this.length());
		builder.append(" | ");
		builder.append(this.getTotalLength()-1);

		return builder.toString();
	}
	
	/**
	 * Given a list of segments, link them together into a circle.
	 * Links start and end properly.
	 * @param list
	 * @throws Exception 
	 */
	public static void linkSegments(List<NucleusBorderSegment> list) throws ProfileException {
		if(list==null || list.isEmpty() ){ // || list.size()==1
			throw new IllegalArgumentException("List of segments is null, empty or one");
		}
				
		NucleusBorderSegment prevSeg = null;
		
//		IJ.log("Linking list:");
//		IJ.log(NucleusBorderSegment.toString(list));

		int position = 0;
		for(NucleusBorderSegment segment : list){

			if(prevSeg != null){
				segment.setPrevSegment(prevSeg);
				prevSeg.setNextSegment(segment);
			}
			segment.setPosition(position);

			prevSeg = segment;
			position++;
		}
		
		if(list.size()>1){
		
			NucleusBorderSegment firstSegment = list.get(0);
			NucleusBorderSegment lastSegment  = list.get(list.size()-1);

			/*
			 * Ensure the end of the final segment is the same index as the start of the first segment.
			 * 
			 * Unlock the first segment while the update proceeds
			 */

			boolean lockState = firstSegment.isStartPositionLocked();
			firstSegment.setStartPositionLocked(false);

			boolean ok = firstSegment.update(lastSegment.getEndIndex(), firstSegment.getEndIndex());
			if(!ok){
				throw new ProfileException("Error fitting final segment: "+firstSegment.getLastFailReason());
			}

			lastSegment.setNextSegment(firstSegment); // ensure they match up at the end
			firstSegment.setPrevSegment(lastSegment);

			// if the first segment is starting at the last index of the profile, correct
			// it to start at 0
			if(firstSegment.getStartIndex()==firstSegment.getTotalLength()-1){
				firstSegment.update(0, firstSegment.getEndIndex());
			}	
			/*
			 * Relock the segment if it was previously locked
			 */
			firstSegment.setStartPositionLocked(lockState);
		} else {
			NucleusBorderSegment firstSegment = list.get(0);
			firstSegment.setNextSegment(firstSegment);
			firstSegment.setPrevSegment(firstSegment);
		}
		
	}
	
	/**
	 * Move the segments by the given amount, without shrinking them.
	 * @param list the list of segments
	 * @param value the amount to nudge
	 * @return a new list of segments
	 * @throws Exception 
	 */
	public static List<NucleusBorderSegment> nudge(List<NucleusBorderSegment> list, int value)  {
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>();
		
		for(NucleusBorderSegment segment : list){
			
			NucleusBorderSegment newSeg = new NucleusBorderSegment(
					
					AbstractCellularComponent.wrapIndex(segment.getStartIndex()+value,
									segment.getTotalLength()), 
									
					AbstractCellularComponent.wrapIndex(segment.getEndIndex()+value,
									segment.getTotalLength()), 
					
					segment.getTotalLength() ,
					
					segment.getID()
			);
			
//			newSeg.setName(segment.getName());
			newSeg.setPosition(segment.getPosition());
			
			
			// adjust merge sources also and read
			if(segment.hasMergeSources()){
				
//				
				List<NucleusBorderSegment> adjustedMergeSources = nudgeUnlinked(segment.getMergeSources(), value);
				for(NucleusBorderSegment newMergeSource : adjustedMergeSources){
					newSeg.addMergeSource(newMergeSource);
				}				
			}
			
			result.add( newSeg );
		}
		
		try {
			linkSegments(result);
		} catch (ProfileException e) {
			IJ.log("Error linking segments");
		}
		
		return result;
	}
		
	/**
	 * Nudge segments that are not linked together into a complete profile. Used in merging and unmerging
	 * segments recursively.
	 * @param list
	 * @param value
	 * @return
	 * @throws Exception
	 */
	private static List<NucleusBorderSegment> nudgeUnlinked(List<NucleusBorderSegment> list, int value) {
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>();
		
		for(NucleusBorderSegment segment : list){
			
			NucleusBorderSegment newSeg = new NucleusBorderSegment(AbstractCellularComponent.wrapIndex(segment.getStartIndex()+value, segment.getTotalLength()), 
					AbstractCellularComponent.wrapIndex(segment.getEndIndex()+value, segment.getTotalLength()), 
					segment.getTotalLength(),
					segment.getID());
						
			
			// adjust merge sources also and readd
			if(segment.hasMergeSources()){
				

				List<NucleusBorderSegment> adjustedMergeSources = nudgeUnlinked(segment.getMergeSources(), value);
				for(NucleusBorderSegment newMergeSource : adjustedMergeSources){
					newSeg.addMergeSource(newMergeSource);
				}
				

			}
			
			result.add( newSeg );
		}
		return result;
	}
	
	/**
	 * Make a copy of the given list of linked segments
	 * @param list the segments to copy
	 * @return a new list
	 * @throws Exception 
	 */
	public static List<NucleusBorderSegment> copy(List<NucleusBorderSegment> list) throws ProfileException{
		
		if(list==null || list.isEmpty()){
			throw new IllegalArgumentException("Cannot copy segments: segment list is null or empty");
		}
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>();
		
		for(NucleusBorderSegment segment : list){

			result.add( new NucleusBorderSegment(segment));
		}
		
		linkSegments(result);
		return result;
	}
	
	/**
	 * Given a list of ordered segments, fetch the segment with the given name
	 * @param list the linked order list
	 * @param segName the segment name to find ('Seg_n')
	 * @return the segment or null
	 * @throws Exception
	 */
	public static NucleusBorderSegment getSegment(List<NucleusBorderSegment> list, String segName) throws Exception{

		for(NucleusBorderSegment segment : list){

			if(segment.getName().equals(segName)){
				return segment;
			}
		}
		return null;
	}
	
	public static String toString(List<NucleusBorderSegment> list){
		StringBuilder builder = new StringBuilder();
		builder.append("List of segments:\n");
		for(NucleusBorderSegment segment : list){

			builder.append("\t"+segment.toString()+"\n");
		}
		return builder.toString();
	}

	@Override
	public Iterator<Integer> iterator() {

		List<Integer> indexes = new ArrayList<Integer>();
		
		if(this.wraps()){
			
			for(int i = this.getStartIndex(); i<this.getTotalLength(); i++){
				indexes.add(i);
			}
			for(int i = 0; i<this.getEndIndex(); i++){
				indexes.add(i);
			}
			
			
		} else {
			
			for(int i = this.getStartIndex(); i<=this.getEndIndex(); i++){
				indexes.add(i);
			}
			
		}
		
		return indexes.iterator();
	}

}