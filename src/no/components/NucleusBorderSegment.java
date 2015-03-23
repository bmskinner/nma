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

	/*
		----------------
		Setters
		----------------
	*/

	public void setSegmentType(String s){
		this.segmentType = s;
	}

}