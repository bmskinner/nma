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

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.Serializable;

import logging.Loggable;
import components.nuclear.BorderPoint;

public class XYPoint  implements Serializable, Loggable {

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
  public XYPoint(final XYPoint p){
    this.x = p.getX();
    this.y = p.getY();
  }

  /**
  * Get the x-value 
  *
  * @return the x-value of the point
  */
  public double getX(){
    return this.x;
  }

  /**
  * Get the y-value 
  *
  * @return the y-value of the point
  */
  public double getY(){
    return this.y;
  }

  /**
  * Get the x-value as an integer
  *
  * @return the x-value of the point
  */
  public int getXAsInt(){
    return (int) Math.round(x);
  }

  /**
  * Get the y-value as an integer
  *
  * @return the y-value of the point
  */
  public int getYAsInt(){
    return (int)  Math.round(y);
  }

  /**
  * Set the x-value
  *
  * @param x the new x-value
  */
  public void setX(double x){
    this.x = x;
  }

  /**
  * Set the y-value
  *
  * @param y the new x-value
  */
  public void setY(double y){
    this.y = y;
  }

  /**
  * Find the distance between this point and
  * a given point
  *
  * @param a the point to measure to
  * @return the distance between the points
  */
  public double getLengthTo(final XYPoint a){
	  
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

  /**
   * Tests if the two points overlap with
   * integer precision
   *
   * @param a the point to test against
   * @return boolean whether they overlap as integers
   */
  public boolean overlaps(final XYPoint a){
	  
	  if(a==null){
		  throw new IllegalArgumentException("Destination point is null");
	  }
	  
	  if( this.getXAsInt() == a.getXAsInt() && this.getYAsInt() == a.getYAsInt()){
		  return true;
	  } else {
		  return false;
	  }
  }
  
  /**
   * Tests if the two points overlap with
   * double precision
   *
   * @param a the point to test against
   * @return boolean whether they overlap as doubles
   */
  public boolean overlapsPerfectly(final XYPoint a){
	  
	  if(a==null){
		  throw new IllegalArgumentException("Destination point is null");
	  }
	  
	  if( this.getX() == a.getX() && this.getY() == a.getY()){
		  return true;
	  } else {
		  return false;
	  }
  }

  /**
  * Writes the integer x and y values together in the format
  * "x,y"
  *
  * @return the string with the integer coordinates
  */
  public String toString(){
    return this.getXAsInt()+","+this.getYAsInt();
  }
  
  
  public Point2D asPoint(){
	  return new Point2D.Double(x, y);
  }

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
  
//  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("\t\tReading XYPoint");
//		in.defaultReadObject();
//		finest("\t\tRead XYPoint");
//	}
//
//	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\t\tWriting XYPoint");
//		out.defaultWriteObject();
//		finest("\t\tWrote XYPoint");
//	}

}