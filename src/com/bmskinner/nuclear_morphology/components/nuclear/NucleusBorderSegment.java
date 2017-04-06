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
package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.AbstractCellularComponent;

@Deprecated
public class NucleusBorderSegment  implements IBorderSegment{

	private static final long serialVersionUID = 1L;
	
	private int    startIndex;
	private int    endIndex;
//	private String name      = null;
	
	private int    totalLength; // the total length of the profile that this segment is a part of 
	
	private IBorderSegment prevSegment = null; // track the previous segment in the profile
	private IBorderSegment nextSegment = null; // track the next segment in the profile
	
	private String lastFailReason = "No fail";
	
	private List<IBorderSegment> mergeSources = new ArrayList<IBorderSegment>(0);
	
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
		this.mergeSources = new ArrayList<IBorderSegment>(0);
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
	public NucleusBorderSegment(IBorderSegment n){
		this.uuid         = n.getID();
		this.startIndex   = n.getStartIndex();
		this.endIndex 	  = n.getEndIndex();
		this.totalLength  = n.getTotalLength();
		this.nextSegment  = n.nextSegment();
		this.prevSegment  = n.prevSegment();
		this.mergeSources = n.getMergeSources();
		this.startPositionLocked = n.isLocked();
	}

	/*
		----------------
		Getters
		----------------
	*/
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getID()
	 */
	@Override
	public UUID getID(){
		return this.uuid;
	}
	
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getMergeSources()
	 */
	@Override
	public List<IBorderSegment> getMergeSources(){
		List<IBorderSegment> result = new ArrayList<IBorderSegment>();
		for(IBorderSegment seg : this.mergeSources){
			result.add( new NucleusBorderSegment(seg));
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#addMergeSource(components.nuclear.NucleusBorderSegment)
	 */
	@Override
	public void addMergeSource(IBorderSegment seg){
		this.mergeSources.add(seg);
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#hasMergeSources()
	 */
	@Override
	public boolean hasMergeSources(){
		if(this.mergeSources.isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getLastFailReason()
	 */
//	@Override
//	public String getLastFailReason(){
//		return this.lastFailReason;
//	}
//	
//	/* (non-Javadoc)
//	 * @see components.nuclear.IBorderSegment#setLastFailReason(java.lang.String)
//	 */
//	@Override
//	public void setLastFailReason(String reason){
//		this.lastFailReason = reason;
//	}

	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getStartIndex()
	 */
	@Override
	public int getStartIndex(){
		return this.startIndex;
	}

	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getEndIndex()
	 */
	@Override
	public int getEndIndex(){
		return this.endIndex;
	}
	
	
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getProportionalIndex(double)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getIndexProportion(int)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getName()
	 */
	@Override
	public String getName(){
		return "Seg_"+this.positionInProfile;
	}

	// when using this, use wrapIndex()!
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getMidpointIndex()
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getDistanceToStart(int)
	 */
	@Override
	public int getDistanceToStart(int index){

		int startIndex 	= this.getStartIndex();

		int distForwards 	= Math.abs(index - startIndex);
		int distBackwards 	= this.length() - distForwards;
		
		int result = Math.min(distForwards, distBackwards);
		return result;	
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getDistanceToEnd(int)
	 */
	@Override
	public int getDistanceToEnd(int index){

		int endIndex 	= this.getEndIndex();

		int distForwards 	= Math.abs(index - endIndex);
		int distBackwards 	= this.length() - distForwards;
		
		int result = Math.min(distForwards, distBackwards);
		return result;	
	}
	
	
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#isStartPositionLocked()
	 */
	@Override
	public boolean isLocked() {
		return startPositionLocked;
	}

	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#setStartPositionLocked(boolean)
	 */
	@Override
	public void setLocked(boolean startPositionLocked) {
		this.startPositionLocked = startPositionLocked;
	}

	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getTotalLength()
	 */
	@Override
	public int getTotalLength(){
		return this.totalLength;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#nextSegment()
	 */
	@Override
	public IBorderSegment nextSegment(){
		return this.nextSegment;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#prevSegment()
	 */
	@Override
	public IBorderSegment prevSegment(){
		return this.prevSegment;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#shortenStart(int)
	 */
//	@Override
//	public boolean shortenStart(int value){
//		int newValue = AbstractCellularComponent.wrapIndex(this.getStartIndex()+value, this.getTotalLength());
//		return this.update(newValue, this.getEndIndex());
//	}
//	
//	/* (non-Javadoc)
//	 * @see components.nuclear.IBorderSegment#shortenEnd(int)
//	 */
//	@Override
//	public boolean shortenEnd(int value){
//		int newValue = AbstractCellularComponent.wrapIndex(this.getEndIndex()-value, this.getTotalLength());
//		return this.update(this.getStartIndex(), newValue);
//
//	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#lengthenStart(int)
	 */
//	@Override
//	public boolean lengthenStart(int value){
//		int newValue = AbstractCellularComponent.wrapIndex( this.getStartIndex()-value, this.getTotalLength());
//		return this.update(newValue, this.getEndIndex());
//	}
//	
//	/* (non-Javadoc)
//	 * @see components.nuclear.IBorderSegment#lengthenEnd(int)
//	 */
//	@Override
//	public boolean lengthenEnd(int value){
//		int newValue = AbstractCellularComponent.wrapIndex( this.getEndIndex()+value, this.getTotalLength());
//		return this.update(this.getStartIndex(), newValue);
//	}
		
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#length()
	 */
	@Override
	public int length(){
		return testLength(this.getStartIndex(), this.getEndIndex());
	}
	
	
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + endIndex;
		result = prime * result + startIndex;
		result = prime * result + totalLength;
		return result;
	}

	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#equals(java.lang.Object)
	 */
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
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#testLength(int, int)
	 */
	@Override
	public int testLength(int start, int end){
		if(wraps(start, end)){ // the segment wraps
			return end + (this.getTotalLength()-start);
		} else{
			return end - start;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#wraps(int, int)
	 */
	@Override
	public boolean wraps(int start, int end){
		if(end<=start){ // the segment wraps
			return true;
		} else{
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#wraps()
	 */
	@Override
	public boolean wraps(){
		return wraps(this.getStartIndex(), this.getEndIndex());
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#contains(int)
	 */
	@Override
	public boolean contains(int index){
		return testContains(this.getStartIndex() , this.getEndIndex(), index);
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#testContains(int, int, int)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#update(int, int)
	 */
	@Override
	public boolean update(int startIndex, int endIndex) throws SegmentUpdateException{
		
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
					IBorderSegment prev = this.prevSegment();
					prev.update(prev.getStartIndex(), startIndex);
				}
			}

			if(this.getEndIndex()!=endIndex){
				this.endIndex = endIndex;

				if(this.hasNextSegment()){
					IBorderSegment next = this.nextSegment();
					next.update(endIndex, next.getEndIndex());
				}
			}
			return true;
		} else {
			// update does not affect this segment
			return true;
		}
	}
	

	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#setNextSegment(components.nuclear.NucleusBorderSegment)
	 */
	@Override
	public void setNextSegment(IBorderSegment s){
		if(s.getTotalLength() != this.getTotalLength()){
			throw new IllegalArgumentException("Segment has a different total length");
		}
		if(s.getStartIndex() != this.getEndIndex()){
			throw new IllegalArgumentException("Segment start ("+s.getStartIndex()+") does not overlap the end of this segment: "+this.getEndIndex());
		}
		
		this.nextSegment = s;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#setPrevSegment(components.nuclear.IBorderSegment)
	 */
	@Override
	public void setPrevSegment(IBorderSegment s){
		if(s.getTotalLength() != this.getTotalLength()){
			throw new IllegalArgumentException("Segment has a different total length");
		}
		if(s.getEndIndex() != this.getStartIndex()){
			throw new IllegalArgumentException("Segment end ("+s.getEndIndex()+") does not overlap start: "+this.getStartIndex());
		}
		
		this.prevSegment = s;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#hasNextSegment()
	 */
	@Override
	public boolean hasNextSegment(){
		if(this.nextSegment()!=null){
			return true;
		} else {
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#hasPrevSegment()
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#setPosition(int)
	 */
	@Override
	public void setPosition(int i){
		this.positionInProfile = i;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#getPosition()
	 */
	@Override
	public int getPosition(){
		return this.positionInProfile;
	}
		
	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#toString()
	 */
	@Override
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
		builder.append(" - ");
		builder.append(this.endIndex);
		builder.append(" | ");
		builder.append(this.length());
		builder.append(" of ");
		builder.append(this.getTotalLength()-1);
		builder.append(" | ");
		builder.append(this.wraps());

		return builder.toString();
	}
	
	

	/* (non-Javadoc)
	 * @see components.nuclear.IBorderSegment#iterator()
	 */
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
	
	
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("\t\tReading nucleus border segment");
		in.defaultReadObject();
//		finest("\t\tRead nucleus border segment");
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\t\tWriting nucleus border segment");
		out.defaultWriteObject();
//		finest("\t\tWrote nucleus border segment");
	}


	@Override
	public void clearMergeSources() {
		mergeSources.clear();
		
	}

}