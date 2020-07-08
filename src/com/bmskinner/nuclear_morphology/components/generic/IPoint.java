/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.generic;

import java.awt.geom.Point2D;
import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The interface for 2D points. It provides some extra methods beyond what
 * is in the Point2D classes.
 * @author bms41
 *
 */
public interface IPoint {

	/**
	 * Create a new point at the origin, 0,0
	 * @return
	 */
	static IPoint atOrigin() {
		return makeNew(0, 0);
	}
	
    /**
     * Create a new point of the default type
     * 
     * @param x the x position
     * @param y the y position
     * @return a point at the specified position
     */
    static IPoint makeNew(final float x, final float y) {
        return new FloatPoint(x, y);
    }

    /**
     * Create a new point of the default type
     * 
     * @param x the x position
     * @param y the y position
     * @return a point at the specified position
     */
    static IPoint makeNew(final double x, final double y) {
        return makeNew((float) x, (float) y);
    }

    /**
     * Create a new point of the default type based on the given point
     * 
     * @param x the x position
     * @param y the y position
     * @return a point at the specified position
     */
    static IPoint makeNew(final IPoint a) {
        return new FloatPoint(a);
    }

    /**
     * Create a new point of the default type based on the given point
     * 
     * @param x the x position
     * @param y the y position
     * @return a point at the specified position
     */
    static IPoint makeNew(final Point2D a) {
        return makeNew(a.getX(), a.getY());
    }
    
    /**
     * Create a duplicate of this point
     * @return a defensive duplicate
     */
    IPoint duplicate();

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
     * Find the distance between this point and a given point
     *
     * @param a the point to measure to
     * @return the distance between the points
     */
    double getLengthTo(@NonNull IPoint a);

    /**
     * Tests if the two points overlap with integer precision
     *
     * @param a the point to test against
     * @return boolean whether they overlap as integers
     */
    boolean overlaps(final @NonNull IPoint a);

    boolean isAbove(final @NonNull IPoint p);

    boolean isBelow(final @NonNull IPoint p);

    boolean isLeftOf(final @NonNull IPoint p);

    boolean isRightOf(final @NonNull IPoint p);

    /**
     * Tests if the two points overlap with double precision
     *
     * @param a
     *            the point to test against
     * @return boolean whether they overlap as doubles
     */
    boolean overlapsPerfectly(final @NonNull IPoint a);

    /**
     * Fetch the point as Point2D
     * 
     * @return
     */
    Point2D toPoint2D();

    /**
     * Measure the smallest angle between the two lines a-this and this-b
     * connecting the three points
     * 
     * @param a the first line endpoint
     * @param b the second line endpoint
     * @see findAbsoluteAngle
     * @return
     */
    double findSmallestAngle(final @NonNull IPoint a, final @NonNull IPoint b);
    
    /**
     * Measure the absolute angle between the two lines {@code a-this} and {@code this-b}
     * connecting the three points. The measurement is made clockwise from the
     * start point to the end point.
     * <p>
     * Contrast with {@link findSmallestAngle}:
     * 
     * <pre>    a<br>    |<br>    o  135<br> 225 \<br>       b<br></pre>
     * 
     * {@code o.findAbsoluteAngle(b, a)} will return 225<br>
     * {@code o.findAbsoluteAngle(a, b)} will return 135<br>
     * {@code o.findSmallestAngle(b, a)} will return 135<br>
     * {@code o.findSmallestAngle(a, b)} will return 135<br>
     * 
     * @param start the point from which measurement starts
     * @param end the point at which measurement ends
     * @return the angle measured clockwise from start to end about this point 
     */
    double findAbsoluteAngle(final @NonNull IPoint start, final @NonNull IPoint end);

    /**
     * Get the midpoint of the two points
     * 
     * @param a
     * @param b
     * @return
     */
    static IPoint getMidpoint(final IPoint a, final IPoint b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Points cannot be null");
        }

        double nx = (a.getX() + b.getX()) / 2;
        double ny = (a.getY() + b.getY()) / 2;
        return IPoint.makeNew(nx, ny);
    }
    
    
    /**
     * Subtract the given point from this point. Creates
     * a new point at x - p.x and y - p.y.
     * @param p
     * @return
     */
    IPoint minus(final @NonNull IPoint p);
    
    /**
     * Add the given point to this point. Creates
     * a new point at x + p.x and y + p.y.
     * @param p
     * @return
     */
    IPoint plus(final @NonNull IPoint p);
    
    /**
     * Add the given value to this point. Creates
     * a new point at x + value and y + value.
     * @param p
     * @return
     */
    IPoint plus(final double value);
    
    /**
     * Subtract the given value from this point. Creates
     * a new point at x - value and y - value.
     * @param p
     * @return
     */
    IPoint minus(final double value);
    
    /**
     * Set the x-value
     *
     * @param x  the new x-value
     */
    void setX(double x);

    /**
     * Set the y-value
     *
     * @param y the new y-value
     */
    void setY(double y);

    /**
     * Set the point to the position in the given point
     * 
     * @param p the position to move this point to
     */
    void set(@NonNull IPoint p);
    
    /**
     * Set the point to the position in the given point
     * 
     * @param p the position to move this point to
     */
    void set(@NonNull Point2D p);

    /**
     * Offset the point by the given amounts
     * 
     * @param x the x offset
     * @param y the y offset
     */
    void offset(double x, double y);
    
    /**
     * Find the mean x and y position of the points. Returns the origin (0,0)
     * if the provided points collection is null or empty
     * @param points the points to average
     * @return the point at the mean x and y position, or the  origin
     * if no points are provided
     */
    static IPoint average(Collection<IPoint> points) {
    	double x = 0;
    	double y = 0;
    	if(points==null || points.isEmpty())
    		return IPoint.makeNew(x, y);

    	for(IPoint p :points) {
    		x += p.getX();
    		y += p.getY();
    	}
    	x /= points.size();
    	y /= points.size();
    	return IPoint.makeNew(x, y);
    }
    
}
