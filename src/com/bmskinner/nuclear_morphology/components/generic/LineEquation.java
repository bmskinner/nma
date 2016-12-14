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

package com.bmskinner.nuclear_morphology.components.generic;

public interface LineEquation {

	/**
	 *	Returns the x value for a given y value
	 *
	 * @param y the y value on the line
	 * @return The x value at the given y value
	 */
	double getX(double y);

	/**
	 *	Returns the y value for a given x value
	 *
	 * @param x the x value on the line
	 * @return The y value at the given x value
	 */
	double getY(double x);

	double getM();

	double getC();

	/**
	 * Test if the point fulfils the equation
	 * @param p
	 * @return
	 */
	boolean isOnLine(IPoint p);

	/**
	 *	Returns a point a given distance away from a given point
	 * on the line specified by this Equation.
	 *
	 * @param p	the reference point to measure from
	 * @param distance	the distance along the line from the point p
	 * @return The position <distance> away from <p>
	 */
	IPoint getPointOnLine(IPoint p, double distance);

	/**
	 * Finds the line equation perpendicular to this line,
	 * at the given point. The point p must lie on the line;
	 * if the y-value for XYPoint p's x does not lie on the line
	 * to int precision, an empty Equation will be returned.
	 *
	 * @param p	the XYPoint to measure from
	 * @return The Equation of the perpendicular line
	 */
	LineEquation getPerpendicular(IPoint p);

	/**
	 * Translates the line to run through the given point,
	 * keeping the gradient but moving the y intercept.
	 *
	 * @param p	the XYPoint to intercept
	 * @return The Equation of the translated line
	 */
	LineEquation translate(IPoint p);

	/**
	 * Find the intercept between this equation and another.
	 * @param eq
	 * @return
	 */
	IPoint getIntercept(LineEquation eq);

	/**
	 * Test if the two lines intersect. Effectively checks
	 * if the lines are parallel, and if so, whether they share
	 * and intercept.
	 * @param eq
	 * @return
	 */
	boolean intersects(DoubleEquation eq);

	/**
	 * Find the smallest distance from a given point to the line
	 * @param p
	 * @return
	 */
	double getClosestDistanceToPoint(IPoint p);

}