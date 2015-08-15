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
package no.components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import utility.Utils;
import ij.IJ;

public class NucleusBorderSegment  implements Serializable{


	public static final int MINIMUM_SEGMENT_LENGTH = 10; // the smallest number of values in a segment
	
	private static final long serialVersionUID = 1L;
	private int startIndex;
	private int endIndex;
	private String segmentType;
	
	private int totalLength; // the total length of the profile that this segment is a part of 
	
	private NucleusBorderSegment prevSegment = null; // track the previous segment in the profile
	private NucleusBorderSegment nextSegment = null; // track the next segment in the profile

	public NucleusBorderSegment(int startIndex, int endIndex, int total){
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		this.totalLength = total;
	}

	public NucleusBorderSegment(NucleusBorderSegment n){
		this.startIndex = n.getStartIndex();
		this.endIndex = n.getEndIndex();
		this.segmentType = n.getSegmentType();
		this.totalLength = n.getTotalLength();
		this.nextSegment = n.nextSegment();
		this.prevSegment = n.prevSegment();
	}

	/*
		----------------
		Getters
		----------------
	*/

	public int getStartIndex(){
		return this.startIndex;
	}

	public int getEndIndex(){
		return this.endIndex;
	}

	public String getSegmentType(){
		return this.segmentType;
	}

	// when using this, use wrapIndex()!
	public int getMidpointIndex(){
		int midpoint = ((endIndex- startIndex)/2) + startIndex;
		return midpoint;
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
		int newValue = Utils.wrapIndex(this.getStartIndex()+value, this.getTotalLength());
		return this.update(newValue, this.getEndIndex());
	}
	
	/**
	 * Make this segment shorter by the given amount.
	 * The end index is moved back. The next segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public boolean shortenEnd(int value){
		int newValue = Utils.wrapIndex(this.getEndIndex()-value, this.getTotalLength());
		return this.update(this.getStartIndex(), newValue);

	}
	
	/**
	 * Make this segment longer by the given amount.
	 * The start index is moved back. The previous segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public boolean lengthenStart(int value){
		int newValue = Utils.wrapIndex( this.getStartIndex()-value, this.getTotalLength());
		return this.update(newValue, this.getEndIndex());
	}
	
	/**
	 * Make this segment longer by the given amount.
	 * The end index is moved forward. The previous segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public boolean lengthenEnd(int value){
		int newValue = Utils.wrapIndex( this.getEndIndex()+value, this.getTotalLength());
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
	
	/**
	 * Test the effect of new start and end indexes on the length
	 * of the segment. Use for validating updates. Also called by
	 * length() using real values
	 * @param start the new start index
	 * @param end the new end index
	 * @return the new segment length
	 */
	public int testLength(int start, int end){
		if(end<start){ // the segment wraps
			return end + (this.getTotalLength()-start);
		} else{
			return end - start;
		}
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
		
		boolean result = false;
		if(end<start){ // wrapped
			if(index<=end || index>start){
				result=true;
			}
		} else{ // regular
			if(index>=start && index<end){
				result=true;
			}
		}
		return result;
	}
	
	/**
	 * Update the segment to the given position. Also updates the 
	 * previous and next segments. Error if the values cause any segment
	 * to become negative length
	 * @param start the new start index
	 * @param end the new end index
	 */
	public boolean update(int startIndex, int endIndex){
		
//		 Check the incoming data
		if(startIndex < 0 || startIndex > this.getTotalLength()){
			throw new IllegalArgumentException("Start index is outside the profile range: "+startIndex);
		}
		if(endIndex < 0 || endIndex > this.getTotalLength()){
			throw new IllegalArgumentException("End index is outside the profile range: "+endIndex);
		}

//		// Check that the new positions will not make this segment too small
		if(testLength(startIndex, endIndex) < MINIMUM_SEGMENT_LENGTH){
			return false;
//			throw new IllegalArgumentException("Segment length cannot be smaller than "+MINIMUM_SEGMENT_LENGTH);
		}
		
//		// don't update things that have not changed
//		if(this.getStartIndex()==startIndex && this.getEndIndex()==endIndex){
//			return;
//		}
		
		// Check that next and previous segments are not invalidated by length change
		// i.e the max length increase backwards is up to the MIN_SEG_LENGTH of the
		// previous segment, and the max length increase forwards is up to the 
		// MIN_SEG_LENGTH of the next segment
		
		if(this.hasPrevSegment()){
			if(this.prevSegment().testLength(this.prevSegment().getStartIndex(), startIndex) < MINIMUM_SEGMENT_LENGTH){
				return false;
				//			throw new IllegalArgumentException("Previous segment length cannot be smaller than "+MINIMUM_SEGMENT_LENGTH);
			}
		}
		if(this.hasNextSegment()){

			if(this.nextSegment().testLength(endIndex, this.nextSegment().getEndIndex()) < MINIMUM_SEGMENT_LENGTH){
				return false;
				//			throw new IllegalArgumentException("Previous segment length cannot be smaller than "+MINIMUM_SEGMENT_LENGTH);
			}
		}
		
		// check that updating will not cause segments to overlap or invert
		// i.e. where a start becomes greater than an end without begin part of
		// an array wrap
		if(startIndex > endIndex){
			
			if(!this.testContains(startIndex , endIndex, 0)){
//			if(!this.contains(0)){
				return false;
//				throw new IllegalArgumentException("Operation would cause this segment to invert");
			}
			
		}
		
		// also test the effect on the next and previous segments
		if(this.hasPrevSegment()){
			if(this.prevSegment().getStartIndex() > startIndex){

				if(!this.prevSegment().testContains(this.prevSegment().getStartIndex(), startIndex, 0)){
					return false;
//					throw new IllegalArgumentException("Operation would cause this segment to invert");
				}
			}
		}

		if(this.hasNextSegment()){
			if( endIndex > this.nextSegment().getEndIndex()){
				if(!this.nextSegment().testContains(endIndex, this.nextSegment().getEndIndex(), 0)){
					return false;
//					throw new IllegalArgumentException("Operation would cause this segment to invert");
				}
			}
		}
		
		// All checks have been passed; the update can proceed
		

//		 wrap in if to ensure we don't go in circles forever when testing a circular profile
		if(this.getStartIndex()!=startIndex){
			this.startIndex = startIndex;
//			IJ.log("Updating start: "+this.getSegmentType());
			if(this.hasPrevSegment()){
				NucleusBorderSegment prev = this.prevSegment();
				prev.update(prev.getStartIndex(), startIndex);
			}
		}
			
		if(this.getEndIndex()!=endIndex){
			this.endIndex = endIndex;
//			IJ.log("Updating end: "+this.getSegmentType());
			if(this.hasNextSegment()){
				NucleusBorderSegment next = this.nextSegment();
				next.update(endIndex, next.getEndIndex());
			}
		}
		return true;
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

	public void setSegmentType(String s){
		this.segmentType = s;
	}
	
	public void print(){
		IJ.log("    Segment "
				+this.getSegmentType()
				+": "+this.startIndex+" - "+this.endIndex+" of "
				+this.getTotalLength()
				+"; prev: "+this.hasPrevSegment()
				+"; next: "+this.hasNextSegment());
	}
	
	public String toString(){
		return new String("Segment "
				+this.getSegmentType()
				+": "+this.startIndex+" - "+this.endIndex+" of "
				+this.getTotalLength()
				+"; prev: "+this.hasPrevSegment()
				+"; next: "+this.hasNextSegment());
	}
	
	/**
	 * Given a list of segments, link them together into a circle.
	 * Links start and end properly.
	 * @param list
	 */
	public static void linkSegments(List<NucleusBorderSegment> list){
		if(list==null || list.isEmpty()){
			throw new IllegalArgumentException("List of segments is null or empty");
		}
		
		NucleusBorderSegment prevSeg = null;

		for(NucleusBorderSegment segment : list){

			if(prevSeg != null){
				segment.setPrevSegment(prevSeg);
				prevSeg.setNextSegment(segment);
			}

			prevSeg = segment;
		}
		NucleusBorderSegment firstSegment = list.get(0);
		boolean ok = firstSegment.update(prevSeg.getEndIndex(), firstSegment.getEndIndex());
		if(!ok){
			IJ.log("Error fitting final segment");
		}

		prevSeg.setNextSegment(firstSegment); // ensure they match up at the end
		firstSegment.setPrevSegment(prevSeg);
	}
	
	/**
	 * Move the segments by the given amount, without shrinking them.
	 * @param list the list of segments
	 * @param value the amount to nudge
	 * @return a new list of segments
	 */
	public static List<NucleusBorderSegment> nudge(List<NucleusBorderSegment> list, int value){
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>();
		
		for(NucleusBorderSegment segment : list){
			
			result.add( new NucleusBorderSegment(Utils.wrapIndex(segment.getStartIndex()+value, segment.getTotalLength()), 
												Utils.wrapIndex(segment.getEndIndex()+value, segment.getTotalLength()), 
												segment.getTotalLength() ));
		}
		
		linkSegments(result);
		
		return result;
	}

}