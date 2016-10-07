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

package components;

import logging.Loggable;
import components.generic.XYPoint;

/**
 * Objects implementing this interface can be rotated
 * by arbirtary amounts, and can have specific border points
 * positioned to the bottom.
 * @author bms41
 *
 */
public interface Rotatable extends Loggable {
	

	/**
	 * Align the object to vertical using the preferred method. If 
	 * TOP_VERTICAL and BOTTOM_VERTICAL points are present, they will
	 * ususally be used. Otherwise, the ORIENTATION_POINT will be rotated
	 * to lie directly below the centre of mass
	 */
	public void alignVertically();
	
		
	/**
	 * Given two points, rotate the object so that they are vertical. It is not necessary
	 * for the points to be within the object, only that they have the same coordinate system
	 * @param topPoint the point to have the higher Y value
	 * @param bottomPoint the point to have the lower Y value
	 */
	public default void alignPointsOnVertical(XYPoint topPoint, XYPoint bottomPoint){
		
		/*
		 * If the points are already aligned vertically, the rotation should not have any effect
		 */
		double angleToRotate 	= 0;

		XYPoint upperPoint = topPoint.getY()>bottomPoint.getY()? topPoint : bottomPoint;
		XYPoint lowerPoint = upperPoint==topPoint ? bottomPoint : topPoint;

		XYPoint comp = new XYPoint(lowerPoint.getX(),upperPoint.getY());

		/*
		 *      LA             RA        RB         LB         
		 * 
		 *      T  C          C  T      B  C       C  B
		 *       \ |          | /        \ |       | /
		 *         B          B            T       T
		 * 
		 * When Ux<Lx, angle describes the clockwise rotation around L needed to move U above it.
		 * When Ux>Lx, angle describes the anticlockwise rotation needed to move U above it.
		 * 
		 * If L is supposed to be on top, the clockwise rotation must be 180+a
		 * 
		 */
		
		double currentAngle = lowerPoint.findAngle( upperPoint, comp);

		
		
		if(topPoint.isLeftOf(bottomPoint) && topPoint.isAbove(bottomPoint)){		
			angleToRotate = 360-currentAngle;
//			log("LA: "+currentAngle+" to "+angleToRotate); // Tested working
		}

		if(topPoint.isRightOf(bottomPoint) && topPoint.isAbove(bottomPoint)){
			angleToRotate = currentAngle;
//			log("RA: "+currentAngle+" to "+angleToRotate); // Tested working
		}

		if(topPoint.isLeftOf(bottomPoint) && topPoint.isBelow(bottomPoint)){
			angleToRotate = currentAngle+180;
			//					angle = 180-angleFromVertical;
//			log("LB: "+currentAngle+" to "+angleToRotate); // Tested working
		}

		if(topPoint.isRightOf(bottomPoint) && topPoint.isBelow(bottomPoint)){
			//					angle = angleFromVertical+180;
			angleToRotate = 180-currentAngle;
//			log("RB: "+currentAngle+" to "+angleToRotate); // Tested working
		}
			
		angleToRotate += 270; // dunno why this works. The basic code is copied from 
		// the image import worker, and does not use an extra rotation

		this.rotate(angleToRotate);
	}
	
	
	/**
	 * Rotate the object so that the given point is directly 
	 * below the centre of mass
	 * @param bottomPoint
	 */
	public void rotatePointToBottom(XYPoint bottomPoint);
	
	/**
	 * Rotate the object by the given amount around the centre of mass
	 * @param angle
	 */
	public void rotate(double angle);

}
