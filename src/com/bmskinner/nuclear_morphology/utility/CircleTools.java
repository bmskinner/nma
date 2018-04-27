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


package com.bmskinner.nuclear_morphology.utility;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Utility functions involving circles
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
public class CircleTools {
	
    /**
     * Find if the given circles overlap
     * 
     * @param com1 the centre of the first circle
     * @param r1 the radius of the first circle
     * @param com2 the centre of the second circle
     * @param r2 the radius of the second circle
     * @return
     */
    public static boolean circlesOverlap(IPoint com1, double r1, IPoint com2, double r2) {
        double d = com1.getLengthTo(com2);
        double r1r2 = Math.abs(r1 + r2);
        return d < r1r2;
    }

    /**
     * Test if either circle is entirely contained within the other
     * 
     * @param com1 the centre of the first circle
     * @param r1 the radius of the first circle
     * @param com2 the centre of the second circle
     * @param r2 the radius of the second circle
     * @return true if either circle is contained within the other
     */
    public static boolean circleIsEnclosed(IPoint com1, double r1, IPoint com2, double r2) {
        double d = com1.getLengthTo(com2);
        return d < Math.abs(r1 - r2);
    }

    /**
     * Get the intersections of a circle with the given line segment
     * 
     * @param line the line segment
     * @param center the centre of the circle
     * @param radius the radius of the circle
     * @return
     */
    public static List<IPoint> getCircleLineIntersectionPoint(Line2D line, IPoint center, double radius) {

        Point2D pointA = line.getP1();
        Point2D pointB = line.getP2();

        double baX = pointB.getX() - pointA.getX();
        double baY = pointB.getY() - pointA.getY();
        double caX = center.getX() - pointA.getX();
        double caY = center.getY() - pointA.getY();

        double a = baX * baX + baY * baY;
        double bBy2 = baX * caX + baY * caY;
        double c = caX * caX + caY * caY - radius * radius;

        double pBy2 = bBy2 / a;
        double q = c / a;

        double disc = pBy2 * pBy2 - q;
        if (disc < 0) {
            return Collections.emptyList();
        }
        // if disc == 0 ... dealt with later
        double tmpSqrt = Math.sqrt(disc);
        double abScalingFactor1 = -pBy2 + tmpSqrt;
        double abScalingFactor2 = -pBy2 - tmpSqrt;

        IPoint p1 = IPoint.makeNew(pointA.getX() - baX * abScalingFactor1, pointA.getY() - baY * abScalingFactor1);
        if (disc == 0) { // abScalingFactor1 == abScalingFactor2
            return Collections.singletonList(p1);
        }
        IPoint p2 = IPoint.makeNew(pointA.getX() - baX * abScalingFactor2, pointA.getY() - baY * abScalingFactor2);
        return Arrays.asList(p1, p2);
    }

    /**
     * Test if the given line segment intersects the border of the circle
     * defined by the given radius and cenre point
     * 
     * @param com the centre of the circle
     * @param r the radius of the circle
     * @param line the line segment to test
     * @return true if the line crosses the border of the circle
     */
    public static boolean intersects(IPoint com, double r, Line2D line) {
        return !getCircleLineIntersectionPoint(line, com, r).isEmpty();

    }

    /**
     * Find the intersection points of two circles. Based on:
     * http://paulbourke.net/geometry/circlesphere/
     * 
     * @param com1 the centre of the first circle
     * @param r1 the radius of the first circle
     * @param com2 the centre of the second circle
     * @param r2 the radius of the second circle
     * @return
     */
    public static IPoint[] findIntersections(IPoint com0, double r0, IPoint com1, double r1) {

        double a, dx, dy, d, h, rx, ry;

        double x0 = com0.getX();
        double x1 = com1.getX();

        double y0 = com0.getY();
        double y1 = com1.getY();

        double x2, y2;

        /*
         * dx and dy are the vertical and horizontal distances between the
         * circle centers.
         */
        dx = x1 - x0;
        dy = y1 - y0;

        /* Determine the straight-line distance between the centers. */
        d = Math.sqrt((dy * dy) + (dx * dx));

        // d = com0.getLengthTo(com1);

        /* Check for solvability. */

        // If d > r0 + r1 then there are no solutions, the circles are separate.

        if (!circlesOverlap(com0, r0, com1, r1)) {
            throw new IllegalArgumentException("Circles do not overlap");
        }

        // If d < |r0 - r1| then there are no solutions because one circle is
        // contained within the other.
        if (circleIsEnclosed(com0, r0, com1, r1)) {
            throw new IllegalArgumentException("One circle is contained within the other");
        }

        // If d = 0 and r0 = r1 then the circles are coincident and there are an
        // infinite number of solutions.
        if (d == 0 && r0 == r1) {
            throw new IllegalArgumentException("Circles overlap perfectly");
        }

        double xi, yi, xi_prime, yi_prime;

        /*
         * 'point 2' is the point where the line through the circle intersection
         * points crosses the line between the circle centers.
         */

        /* Determine the distance from point 0 to point 2. */
        a = ((r0 * r0) - (r1 * r1) + (d * d)) / (2.0 * d);

        /* Determine the coordinates of point 2. */
        x2 = x0 + (dx * a / d);
        y2 = y0 + (dy * a / d);

        /*
         * Determine the distance from point 2 to either of the intersection
         * points.
         */
        h = Math.sqrt((r0 * r0) - (a * a));

        /*
         * Now determine the offsets of the intersection points from point 2.
         */
        rx = -dy * (h / d);
        ry = dx * (h / d);

        /* Determine the absolute intersection points. */
        xi = x2 + rx;
        xi_prime = x2 - rx;
        yi = y2 + ry;
        yi_prime = y2 - ry;

        IPoint first = IPoint.makeNew(xi, yi);
        IPoint second = IPoint.makeNew(xi_prime, yi_prime);

        IPoint[] result = { first, second };
        return result;
    }

}
