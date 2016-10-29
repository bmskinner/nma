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

package components.nuclear;

import components.active.generic.DefaultBorderPoint;
import components.generic.IPoint;

public interface IBorderPoint extends IPoint {

	/**
	 * Create a new point of the default type
	 * @param x
	 * @param y
	 * @return
	 */
	static IBorderPoint makeNew(float x, float y){
		return new DefaultBorderPoint(x,y);
	}
	
	/**
	 * Create a new point of the default type
	 * @param x
	 * @param y
	 * @return
	 */
	static IBorderPoint makeNew(double x, double y){
		return makeNew( (float) x, (float) y);
	}
	
	/**
	 * Create a new point of the default type
	 * based on the given point
	 * @param x
	 * @param y
	 * @return
	 */
	static IBorderPoint makeNew(IBorderPoint a){
		return makeNew( a.getX(), a.getY());
	}
	
	/**
	 * Set the next point in the border
	 * @param next
	 */
	void setNextPoint(IBorderPoint next);

	/**
	 * Set the previous point in the border
	 * @param prev
	 */
	void setPrevPoint(IBorderPoint prev);

	IBorderPoint nextPoint();

	/**
	 * Get the point n points ahead
	 * @param points
	 * @return
	 */
	IBorderPoint nextPoint(int points);

	IBorderPoint prevPoint();

	/**
	 * Get the point n points behind
	 * @param points
	 * @return
	 */
	IBorderPoint prevPoint(int points);

	boolean hasNextPoint();

	boolean hasPrevPoint();

}