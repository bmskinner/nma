/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package components.generic;

import java.awt.geom.Point2D;

import components.active.generic.FloatPoint;

public interface IPoint {

	/**
	 * Create a new point of the default type
	 * @param x
	 * @param y
	 * @return
	 */
	static IPoint makeNew(float x, float y){
		return new FloatPoint(x,y);
	}
	
	/**
	 * Create a new point of the default type
	 * @param x
	 * @param y
	 * @return
	 */
	static IPoint makeNew(double x, double y){
		return makeNew( (float) x, (float) y);
	}
	
	/**
	 * Create a new point of the default type
	 * based on the given point
	 * @param x
	 * @param y
	 * @return
	 */
	static IPoint makeNew(IPoint a){
		return makeNew( a.getX(), a.getY());
	}
	
	/**
	 * Get the x-value 
	 *
	 * @return the x-value of the point
	 */
	double getX();

	/**
	 * Get the y-value 
	 *
	 * @return the y-value of the point
	 */
	double getY();

	/**
	 * Get the x-value as an integer
	 *
	 * @return the x-value of the point
	 */
	int getXAsInt();

	/**
	 * Get the y-value as an integer
	 *
	 * @return the y-value of the point
	 */
	int getYAsInt();

	/**
	 * Set the x-value
	 *
	 * @param x the new x-value
	 */
	void setX(double x);

	/**
	 * Set the y-value
	 *
	 * @param y the new x-value
	 */
	void setY(double y);

	void set(IPoint p);

	/**
	 * Find the distance between this point and
	 * a given point
	 *
	 * @param a the point to measure to
	 * @return the distance between the points
	 */
	double getLengthTo(IPoint a);

	/**
	 * Tests if the two points overlap with
	 * integer precision
	 *
	 * @param a the point to test against
	 * @return boolean whether they overlap as integers
	 */
	boolean overlaps(IPoint a);

	boolean isAbove(IPoint p);

	boolean isBelow(IPoint p);

	boolean isLeftOf(IPoint p);

	boolean isRightOf(IPoint p);

	IPoint offset(double x, double y);

	/**
	 * Tests if the two points overlap with
	 * double precision
	 *
	 * @param a the point to test against
	 * @return boolean whether they overlap as doubles
	 */
	boolean overlapsPerfectly(IPoint a);

	/**
	 * Writes the integer x and y values together in the format
	 * "x,y"
	 *
	 * @return the string with the integer coordinates
	 */
	String toString();

	Point2D toPoint2D();

	/**
	 * Measure the smallest angle between the two lines a-this and this-b connecting
	 * the three points
	 * @param a the first line endpoint
	 * @param b the second line endpoint
	 * @return
	 */
	double findAngle(IPoint a, IPoint b);

	int hashCode();

	boolean equals(Object obj);

}