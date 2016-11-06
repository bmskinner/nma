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
  XY POINT CLASS
  -----------------------
  This class contains the X and Y coordinates of a point as doubles.
  Also contains methods for determining distance and overlap with other points
 */

package components.generic;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

import java.awt.geom.Point2D;
import java.io.Serializable;

import logging.Loggable;

@Deprecated
public class XYPoint  implements Serializable, Loggable, IPoint {

	private static final long serialVersionUID = 1L;
	protected double x;
	protected double y;

	/**
	 * Constructor using doubles. 
	 *
	 * @param x the x-coordinate
	 * @param y the y-coordinate
	 * @return An XYPoint at these coordinates
	 */
	public XYPoint (final double x, final double y){
		this.x = x;
		this.y = y;
	}

	/**
	 * Constructor using XYPoint. Copies
	 * the x and y coordinates from the given
	 * point
	 *
	 * @param p the XYPoint
	 * @return An XYPoint at these coordinates
	 */
	public XYPoint(final IPoint p){
		this.x = p.getX();
		this.y = p.getY();
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#getX()
	 */
	@Override
	public double getX(){
		return this.x;
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#getY()
	 */
	@Override
	public double getY(){
		return this.y;
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#getXAsInt()
	 */
	@Override
	public int getXAsInt(){
		return (int) Math.round(x);
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#getYAsInt()
	 */
	@Override
	public int getYAsInt(){
		return (int)  Math.round(y);
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#setX(double)
	 */
	@Override
	public void setX(double x){
		this.x = x;
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#setY(double)
	 */
	@Override
	public void setY(double y){
		this.y = y;
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#set(components.generic.XYPoint)
	 */
	@Override
	public void set(IPoint p){
		this.x = p.getX();
		this.y = p.getY();
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#getLengthTo(components.generic.IPoint)
	 */
	@Override
	public double getLengthTo(final IPoint a){

		if(a==null){
			throw new IllegalArgumentException("Destination point is null");
		}

		// a2 = b2 + c2
		double dx = Math.abs(this.getX() - a.getX());
		double dy = Math.abs(this.getY() - a.getY());
		double dx2 = dx * dx;
		double dy2 = dy * dy;
		double length = Math.sqrt(dx2+dy2);
		return length;
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#overlaps(components.generic.IPoint)
	 */
	@Override
	public boolean overlaps(final IPoint a){

		if(a==null){
			throw new IllegalArgumentException("Destination point is null");
		}

		if( this.getXAsInt() == a.getXAsInt() && this.getYAsInt() == a.getYAsInt()){
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#isAbove(components.generic.XYPoint)
	 */
	@Override
	public boolean isAbove(IPoint p){
		return y>p.getY();
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#isBelow(components.generic.XYPoint)
	 */
	@Override
	public boolean isBelow(IPoint p){
		return y<p.getY();
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#isLeftOf(components.generic.XYPoint)
	 */
	@Override
	public boolean isLeftOf(IPoint p){
		return x<p.getX();
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#isRightOf(components.generic.XYPoint)
	 */
	@Override
	public boolean isRightOf(IPoint p){
		return x>p.getX();
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#offset(double, double)
	 */
	@Override
	public IPoint offset(double x, double y){
		return new XYPoint(this.x+x, this.y+y);
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#overlapsPerfectly(components.generic.IPoint)
	 */
	@Override
	public boolean overlapsPerfectly(final IPoint a){

		if(a==null){
			throw new IllegalArgumentException("Destination point is null");
		}

		if( this.getX() == a.getX() && this.getY() == a.getY()){
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#toString()
	 */
	@Override
	public String toString(){
		return this.getXAsInt()+","+this.getYAsInt();
	}


	/* (non-Javadoc)
	 * @see components.generic.IPoint#asPoint()
	 */
	@Override
	public Point2D toPoint2D(){
		return new Point2D.Double(x, y);
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#findAngle(components.generic.IPoint, components.generic.IPoint)
	 */
	@Override
	public double findAngle(IPoint a, IPoint b){

		// Use the cosine rule: a-b^2 = this-b^2 + this-a^2 - 2 * this-b * this-a * cos (theta)

		//	  double ab = a.getLengthTo(b);
		//	  double bc = getLengthTo(b);
		//	  double ac = getLengthTo(a);
		//	  
		//	  double ab2cosT = Math.pow(bc,2) + Math.pow(ac,2) - Math.pow(ab,2);
		//	  
		//	  double cosT = ab2cosT / (2 * ac * bc);
		//	  
		//	  double t = Math.acos(cosT);
		//	  return Math.toDegrees(t);

		float[] xpoints = { (float) a.getX(), (float) getX(), (float) b.getX()};
		float[] ypoints = { (float) a.getY(), (float) getY(), (float) b.getY()};
		PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
		return roi.getAngle();
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	/* (non-Javadoc)
	 * @see components.generic.IPoint#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XYPoint other = (XYPoint) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}

}