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

package no.components;

import java.io.Serializable;

public class XYPoint  implements Serializable{
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
private double x;
  private double y;
  
  /**
  * Constructor using doubles. 
  *
  * @param x the x-coordinate
  * @param y the y-coordinate
  * @return An XYPoint at these coordinates
  */
  public XYPoint (double x, double y){
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
  public XYPoint(XYPoint p){
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
    return (int)this.x;
  }

  /**
  * Get the y-value as an integer
  *
  * @return the y-value of the point
  */
  public int getYAsInt(){
    return (int)this.y;
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
  public double getLengthTo(XYPoint a){

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
  public boolean overlaps(XYPoint a){
    if( this.getXAsInt() == a.getXAsInt() && this.getYAsInt() == a.getYAsInt()){
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

}