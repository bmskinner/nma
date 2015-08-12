package components;

import java.io.Serializable;
import java.util.List;

import no.components.XYPoint;

/**
 * This is a generic class of segment, made of border points.
 * It is subclassed as needed for nucleus border points, tail 
 * border points etc. It handles transisitons between physcial
 * segments as in a nucleus border, and index based segments,
 * as in a profile. The internal methods keep the two synchronised 
 */
public abstract class BorderSegment implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private int startIndex; // the starting index for index based segments
	private int length;	// the length of the segment for index based segments
	private List<XYPoint> points;
	private String name; // allow the segment to be named or tagged
	
	
	public BorderSegment(int startIndex, int length){
		this.startIndex = startIndex;
		this.length = length;
		this.points = null;
	}
	
	/**
	 * Create a new segment from a list of points
	 * @param points
	 */
	public BorderSegment(List<XYPoint> points){
		
		this.points = points;
		this.name = null;
	}
	
	/**
	 * Create a new segment from a single point
	 * @param point
	 */
	public BorderSegment(XYPoint point){
		
		this.points.add(point);
		this.name = null;
	}
	
	public BorderSegment(BorderSegment segment){
		
	}
	
	/**
	 * Get the length of the list of points
	 * Note that this is an array length, not
	 * a geometric length
	 * @return
	 */
	public int length(){
		return length;
//		return points.size();
	}
	
	/**
	 * Get the length physically represented
	 * by the segment
	 * @return
	 */
	public double physicalLength(){
		double result = 0;
		for (XYPoint p : points){
			
			if(!isLast(p)){
				result += p.getLengthTo( points.get(points.indexOf(p)+1));
			}
		}
		return result;
	}
	
	public void setName(String s){
		this.name = s;
	}
	
	public String getName(){
		return this.name;
	}
	
	public int getStartIndex(){
		return this.startIndex;
	}
	
	/**
	 * Update the segment position using index positions.
	 * If the segment contains XYPoints, the number of points
	 * must match the length input. If the points need to be changed,
	 * alter the XYPoints first, then update the indexes
	 * @param startIndex
	 * @param length
	 */
	public void update(int startIndex, int length){
		if(startIndex<0 || length < 0 ){
			throw new IllegalArgumentException("Index ("+startIndex+") or length ("+length+") is less than zero");
		}
		
		if(this.points!=null){
			if(points.size()!=length){
				throw new IllegalArgumentException("The length does not match the existing points");
			}
		}
		this.startIndex = startIndex;
		this.length = length;
	}
	
	public void update(int startIndex, List<XYPoint> points){
		if(startIndex<0){
			throw new IllegalArgumentException("Index ("+startIndex+") is less than zero ");
		}
		
		this.startIndex = startIndex;
		this.length = points.size();
		this.points = points;
		
	}
	
	/**
	 * Get the XYPoints in this segment
	 * @return
	 */
	public List<XYPoint> getPoints(){
		return this.points;
	}
	
	/**
	 * Add the given point to the end of the segment
	 * @param p
	 */
	public void addPoint(XYPoint p){
		if(p==null){
			throw new IllegalArgumentException("Point is null");
		}
		points.add(p);
		this.length = points.size();
	}
	
	/**
	 * Add a list of XYPoints to the segment.
	 * @param points
	 */
	public void addPoints(List<XYPoint> points){
		if(points==null){
			throw new IllegalArgumentException("Point list is null");
		}
		if(this.points==null){
			this.points = points;
		} else {
			this.points.addAll(points);
		}
		this.length = this.points.size();
	}
	
	/**
	 * Check if the given point exists in the segment
	 * If it does, remove it. This is private since it would be
	 * a problem to remove points in the middle of a segment
	 * @param p the point to remove
	 */
	private void removePoint(XYPoint p){
		
		if(p==null){
			throw new IllegalArgumentException("Point is null");
		}
		for(XYPoint exists : points){
			if(p.equals(exists)){
				points.remove(p);
			}
		}
		this.length = this.points.size();
	}
	
	/**
	 * Remove the point at the start of the segment
	 */
	public void removeFirstPoint(){
		points.remove(0);
		this.length = this.points.size();
	}
	
	
	/**
	 * Remove the point at the end of the segment  
	 */
	public void removeLastPoint(){
		points.remove(points.size()-1);
		this.length = this.points.size();
	}
	
	/**
	 * Check if the given point is the last in the segment
	 * @param p the point to check
	 * @return
	 */
	public boolean isLast(XYPoint p){
		if(p==null){
			throw new IllegalArgumentException("Point is null");
		}
		if(p.equals(points.get(points.size()-1))){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Check if the given point is the first in the segment
	 * @param p the point to check
	 * @return
	 */
	public boolean isFirst(XYPoint p){
		if(p==null){
			throw new IllegalArgumentException("Point is null");
		}
		if(p.equals(points.get(0))){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Check if the given point lies in the segment
	 * @param p
	 * @return
	 */
	public boolean contains(XYPoint p){
		if(p==null){
			throw new IllegalArgumentException("Point is null");
		}
		if(points.contains(p)){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Get the point at the centre of the segment
	 * @return
	 */
	public XYPoint getMidpoint(){
		int midpoint = (int) Math.floor(points.size()/2);
		return points.get(midpoint);
	}
	
	/**
	 * For debugging 
	 */
	public void print(){
		System.out.println("Segment: "+name+": Start: "+startIndex+" Length: "+length);
		for(XYPoint p : points){
			System.out.println("\t"+p.toString());
			
		}
	}
	

}
