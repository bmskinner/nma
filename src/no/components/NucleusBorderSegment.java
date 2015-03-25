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

import ij.IJ;

public class NucleusBorderSegment{

	private int startIndex;
	private int endIndex;
	private String segmentType;

	public NucleusBorderSegment(int startIndex, int endIndex){
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}

	public NucleusBorderSegment(NucleusBorderSegment n){
		this.startIndex = n.getStartIndex();
		this.endIndex = n.getEndIndex();
		this.segmentType = n.getSegmentType();
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
	
	public int length(int totalLength){
		if(endIndex<startIndex){
			return startIndex + (totalLength-endIndex);
		} else{
			return this.endIndex - this.startIndex;
		}
	}
	
	public boolean contains(int index){
		boolean result = false;
		if(endIndex<startIndex){ // wrapped
			if(index<endIndex || index>startIndex){
				result=true;
			}
		} else{ // regular
			if(index>startIndex && index<endIndex){
				result=true;
			}
		}
		return result;
	}
	
	public void update(int start, int end){
		this.startIndex = start;
		this.endIndex = end;
	}

	/*
		----------------
		Setters
		----------------
	*/

	public void setSegmentType(String s){
		this.segmentType = s;
	}
	
	public void print(){
		IJ.log("    Segment from "+this.startIndex+" to "+this.endIndex);
	}

}