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

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Calculate the x and y components of a vector
 * 
 * @author ben
 *
 */
public class AngleTools {

    /**
     * Find the length on the x-axis of a line at a given angle
     * 
     * @param length
     *            the line length
     * @param angle
     *            the angle from 0 relative to positive x axis in degrees
     * @return the x distance
     */
    public static double getXComponentOfAngle(double length, double angle) {
        // cos(angle) = x / h
        // x = cos(a)*h
        double x = length * Math.cos(Math.toRadians(angle));
        return x;
    }

    /**
     * Find the length on the y-axis of a line at a given angle
     * 
     * @param length
     *            the line length
     * @param angle
     *            the angle from 0 relative to positive x axis in degrees
     * @return the y distance
     */
    public static double getYComponentOfAngle(double length, double angle) {
        double y = length * Math.sin(Math.toRadians(angle));
        return y;
    }

    /**
     * Rotate the given point about a centre
     * 
     * @param p
     *            the point to be moved
     * @param centre
     *            the centre of rotation
     * @param angle
     *            the angle to rotate in degrees
     * @return
     */
    public static IPoint rotateAboutPoint(IPoint p, IPoint centre, double angle) {
        // get the distance from the point to the centre of mass
        double distance = p.getLengthTo(centre);

        // get the angle between the centre of mass (C), the point (P) and a
        // point directly under the centre of mass (V)

        /*
         * C |\ V P
         * 
         */
        double oldAngle = centre.findAngle(p, IPoint.makeNew(centre.getX(), -10));

        if (p.getX() < centre.getX()) {
            oldAngle = 360 - oldAngle;
        }

        double newAngle = oldAngle + angle;
        double newX = AngleTools.getXComponentOfAngle(distance, newAngle) + centre.getX();
        double newY = AngleTools.getYComponentOfAngle(distance, newAngle) + centre.getY();
        return IPoint.makeNew(newX, newY);
    }

    /**
     * Calculate the angle between two lines in radians
     * 
     * @param line1Start
     * @param line1End
     * @param line2Start
     * @param line2End
     * @return
     */
    public static double angleBetweenLines(IPoint line1Start, IPoint line1End, IPoint line2Start, IPoint line2End) {
        double a = line1End.getX() - line1Start.getX();
        double b = line1End.getY() - line1Start.getY();
        double c = line2End.getX() - line2Start.getX();
        double d = line2End.getY() - line2Start.getY();

        double atanA = Math.atan2(a, b);
        double atanB = Math.atan2(c, d);

        return atanA - atanB;
    }

}
