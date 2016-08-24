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

package analysis;

import components.nuclear.BorderPoint;

/**
 * Objects implementing this interface can be rotated
 * by arbirtary amounts, and can have specific border points
 * positioned to the bottom.
 * @author bms41
 *
 */
public interface Rotatable {
	
	/**
	 * Detect the points that can be used for vertical alignment.These are based on the
	 * BorderTags TOP_VERTICAL and BOTTOM_VETICAL. The actual points returned are not
	 * necessarily on the border of the nucleus; a bibble correction is performed on the
	 * line drawn between the two border points, minimising the sum-of-squares to each border
	 * point within the region covered by the line. 
	 * @return
	 */
	public BorderPoint[] getBorderPointsForVerticalAlignment();
	
	
	/**
	 * Calculate the angle that the nucleus must be rotated by. 
	 * If the BorderTags TOP_VERTICAL and BOTTOM_VERTICAL have been set, 
	 * the angle will align these points on the y-axis. Otherwise, the angle will
	 * place the orientation point directly below the centre of mass.
	 * @return
	 */
	public double findRotationAngle();
	
	/**
	 * Given two points in the nucleus, rotate the nucleus so that they are vertical.
	 * @param topPoint the point to have the higher Y value
	 * @param bottomPoint the point to have the lower Y value
	 */
	public void alignPointsOnVertical(BorderPoint topPoint, BorderPoint bottomPoint);
	
	/**
	 * Rotate the nucleus so that the given point is directly 
	 * below the centre of mass
	 * @param bottomPoint
	 */
	public void rotatePointToBottom(BorderPoint bottomPoint);
	
	/**
	 * Rotate the nucleus by the given amount around the centre of mass
	 * @param angle
	 */
	public void rotate(double angle);

}
