/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.generic;

import java.awt.geom.Point2D;

/**
 * The interface for 2D points. It provides some extra methods beyond what
 * is in the Point2D classes.
 * @author bms41
 *
 */
public interface IPoint {

    /**
     * Create a new point of the default type
     * 
     * @param x
     *            the x position
     * @param y
     *            the y position
     * @return a point at the specified position
     */
    static IPoint makeNew(final float x, final float y) {
        return new FloatPoint(x, y);
    }

    /**
     * Create a new point of the default type
     * 
     * @param x
     *            the x position
     * @param y
     *            the y position
     * @return a point at the specified position
     */
    static IPoint makeNew(final double x, final double y) {
        return makeNew((float) x, (float) y);
    }

    /**
     * Create a new point of the default type based on the given point
     * 
     * @param x
     *            the x position
     * @param y
     *            the y position
     * @return a point at the specified position
     */
    static IPoint makeNew(final IPoint a) {
        return makeNew(a.getX(), a.getY());
    }

    /**
     * Create a new point of the default type based on the given point
     * 
     * @param x
     *            the x position
     * @param y
     *            the y position
     * @return a point at the specified position
     */
    static IPoint makeNew(final Point2D a) {
        return makeNew(a.getX(), a.getY());
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
     * Find the distance between this point and a given point
     *
     * @param a
     *            the point to measure to
     * @return the distance between the points
     */
    double getLengthTo(IPoint a);

    /**
     * Tests if the two points overlap with integer precision
     *
     * @param a
     *            the point to test against
     * @return boolean whether they overlap as integers
     */
    boolean overlaps(final IPoint a);

    boolean isAbove(final IPoint p);

    boolean isBelow(final IPoint p);

    boolean isLeftOf(final IPoint p);

    boolean isRightOf(final IPoint p);

    /**
     * Tests if the two points overlap with double precision
     *
     * @param a
     *            the point to test against
     * @return boolean whether they overlap as doubles
     */
    boolean overlapsPerfectly(final IPoint a);

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
     * @param a
     *            the first line endpoint
     * @param b
     *            the second line endpoint
     * @return
     */
    double findAngle(final IPoint a, final IPoint b);

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
    IPoint minus(final IPoint p);
    
    /**
     * Add the given point to this point. Creates
     * a new point at x + p.x and y + p.y.
     * @param p
     * @return
     */
    IPoint plus(final IPoint p);
    
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
     * @param x
     *            the new x-value
     */
    void setX(double x);

    /**
     * Set the y-value
     *
     * @param y
     *            the new x-value
     */
    void setY(double y);

    /**
     * Set the point to the position in the given point
     * 
     * @param p
     *            the position to move this point to
     */
    void set(IPoint p);

    /**
     * Offset the point by the given amounts
     * 
     * @param x
     *            the x offset
     * @param y
     *            the y offset
     */
    void offset(double x, double y);

}
