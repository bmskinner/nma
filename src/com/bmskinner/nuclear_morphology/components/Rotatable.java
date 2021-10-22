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
package com.bmskinner.nuclear_morphology.components;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Objects implementing this interface can be rotated by arbirtary amounts, and
 * can have specific border points positioned to the bottom.
 * 
 * @author bms41
 *
 */
public interface Rotatable {

    /**
     * Given two points, rotate the object so that they are vertical. It is not
     * necessary for the points to be within the object, only that they have the
     * same coordinate system
     * 
     * @param topPoint the point to have the higher Y value
     * @param bottomPoint the point to have the lower Y value
     */
    default void alignPointsOnVertical(final @NonNull IPoint topPoint, final @NonNull IPoint bottomPoint) {
    	double angle = getAngleToRotateVertical(topPoint, bottomPoint);
        rotate(angle);
    }
    
    /**
     * Given two points, rotate the object so that they are horizontal. It is not
     * necessary for the points to be within the object, only that they have the
     * same coordinate system
     * 
     * @param topPoint the point to have the higher Y value
     * @param bottomPoint the point to have the lower Y value
     */
    default void alignPointsOnHorizontal(final @NonNull IPoint leftPoint, final @NonNull IPoint rightPoint) {
    	double angle = getAngleToRotateHorizontal(leftPoint, rightPoint);
        rotate(angle);
    }
    
    /**
     * Find the clockwise angle in degrees required to place the top point above the bottom point
     * such that their x coordinates are zero, and the y value of topPoint is greater than the y value
     * of bottomPoint
     * @param topPoint
     * @param bottomPoint
     * @return
     */
    static double getAngleToRotateVertical(final @NonNull IPoint topPoint, final @NonNull IPoint bottomPoint) {
        // Take a vertical line from B to Bi. Rotate object by the absolute angle T-B-Bi 
        IPoint bi = IPoint.makeNew(bottomPoint.getX(), bottomPoint.getY()+10);
        return bottomPoint.findAbsoluteAngle(topPoint, bi);
    }
    
    /**
     * Find the clockwise angle in degrees required to place the left point left of the right point
     * such that their y coordinate difference is zero, and the x value of right is greater than the x value
     * of left
     * @param leftPoint
     * @param rightPoint
     * @return
     */
    static double getAngleToRotateHorizontal(final @NonNull IPoint leftPoint, final @NonNull IPoint rightPoint) {
        // Take a horizontal line from L to Li. Rotate object by the absolute angle R-L-Li 
    	
    	/*
    	 *      L - Li               L - Li
    	 *       \                 / 
    	 *        R               R 
    	 * 
    	 */
    	
        IPoint bi = IPoint.makeNew(leftPoint.getX()+10, leftPoint.getY());
        return leftPoint.findAbsoluteAngle(rightPoint, bi);
    }

    /**
     * Rotate the object so that the given point is directly below the centre of
     * mass
     * 
     * @param bottomPoint
     */
    void rotatePointToBottom(final @NonNull IPoint bottomPoint);
    
    /**
     * Rotate the object so that the given point is directly left of the centre of
     * mass
     * 
     * @param bottomPoint
     */
    void rotatePointToLeft(IPoint leftPoint);

    /**
     * Rotate the object by the given amount clockwise around the centre of mass
     * 
     * @param angle the angle in degrees
     */
    void rotate(double angle);
        
    /**
     * Flip the object horizontally, centred on the given point
     * @param centre the point about which to flip
     */
    void flipHorizontal(final @NonNull IPoint centre);
    
    /**
     * Flip the object horizontally about the centre of mass
     */
    void flipHorizontal();
    
    /**
     * Flip the object vertically, centred on the given point
     * @param centre the point about which to flip
     */
    void flipVertical(final @NonNull IPoint centre);

    /**
     * Flip the object vertically about the centre of mass
     */
	void flipVertical();

	

}
