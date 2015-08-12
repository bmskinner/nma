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

import utility.Utils;
import ij.IJ;

public class NucleusBorderSegment  implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int startIndex;
	private int endIndex;
	private String segmentType;
	
	private int totalLength; // the total length of the profile that this segment is a part of 
	
	private NucleusBorderSegment prevSegment; // track the previous segment in the profile
	private NucleusBorderSegment nextSegment; // track the next segment in the profile

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
	public void shortenStart(int value){
		int newValue = Utils.wrapIndex(this.getStartIndex()+value, this.getTotalLength());
		this.update(newValue, this.getEndIndex());
	}
	
	/**
	 * Make this segment shorter by the given amount.
	 * The end index is moved back. The next segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public void shortenEnd(int value){
		int newValue = Utils.wrapIndex(this.getEndIndex()-value, this.getTotalLength());
		this.update(this.getStartIndex(), newValue);

	}
	
	/**
	 * Make this segment longer by the given amount.
	 * The start index is moved back. The previous segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public void lengthenStart(int value){
		int newValue = Utils.wrapIndex( this.getStartIndex()-value, this.getTotalLength());
		this.update(newValue, this.getEndIndex());
	}
	
	/**
	 * Make this segment longer by the given amount.
	 * The end index is moved forward. The previous segment
	 * is adjusted to keep the segments in sync
	 * @param value the amount to shorten
	 */
	public void lengthenEnd(int value){
		int newValue = Utils.wrapIndex( this.getEndIndex()+value, this.getTotalLength());
		this.update(this.getStartIndex(), newValue);
	}
	
	/**
	 * Get the length of this segment. Accounts
	 * for array wrapping
	 * @return
	 */
	public int length(){
		if(this.getEndIndex()<this.getStartIndex()){ // the segment wraps
			return this.getEndIndex() + (this.getTotalLength()-this.getStartIndex());
		} else{
			return this.getEndIndex() - this.getStartIndex();
		}
	}
	
	/**
	 * Test if the segment contains the given index
	 * @param index the index to test
	 * @return
	 */
	public boolean contains(int index){
		
		if(index < 0 || index > this.getTotalLength()){
			throw new IllegalArgumentException(" Index is outsize the total profile length: "+index);
		}
		
		boolean result = false;
		if(this.getEndIndex()<this.getStartIndex()){ // wrapped
			if(index<=this.getEndIndex() || index>this.getStartIndex()){
				result=true;
			}
		} else{ // regular
			if(index>=this.getStartIndex() && index<this.getEndIndex()){
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
	public void update(int startIndex, int endIndex){
		
		if(startIndex < 0 || startIndex > this.getTotalLength() || endIndex < 0 || endIndex > this.getTotalLength()){
			throw new IllegalArgumentException(" Index is outside the total profile length");
		}
						
		NucleusBorderSegment prev = this.prevSegment();
		prev.update(prev.getStartIndex(), startIndex);
		
		NucleusBorderSegment next = this.nextSegment();
		next.update(endIndex, next.getEndIndex());
		
		this.startIndex = startIndex;
		this.endIndex = endIndex;
		//TODO: check that this does not infinitely recurse
		IJ.log("Updated");
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
			throw new IllegalArgumentException("Segment start does not overlap end");
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
			throw new IllegalArgumentException("Segment end does not overlap start");
		}
		
		this.prevSegment = s;
	}

	public void setSegmentType(String s){
		this.segmentType = s;
	}
	
	public void print(){
		IJ.log("    Segment from "+this.startIndex+" to "+this.endIndex);
	}

}