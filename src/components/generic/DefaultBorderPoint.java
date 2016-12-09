/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package components.generic;

import components.nuclear.IBorderPoint;

/**
 * The standard implementation of the {@link IBorderPoint} interface.
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultBorderPoint extends FloatPoint implements IBorderPoint {
	private static final long serialVersionUID = 1L;
	
	private IBorderPoint prevPoint = null;
	private IBorderPoint nextPoint = null;
	
	/**
	 * Construct from x and y positions 
	 * @param x
	 * @param y
	 */
	public DefaultBorderPoint( float x, float y){
		super(x, y);
	}
	
	/**
	 * Construct from x and y positions 
	 * @param x
	 * @param y
	 */
	public DefaultBorderPoint( double x, double y){
		super( (float) x, (float) y);
	}

	/**
	 * Construct from an existing XY point
	 * @param p
	 */
	public DefaultBorderPoint( IPoint p){
		super(p);
	}
		
	/**
	 * Set the next point in the border
	 * @param next
	 */
	public void setNextPoint(IBorderPoint next){
		this.nextPoint = next;
	}
	
	/**
	 * Set the previous point in the border
	 * @param prev
	 */
	public void setPrevPoint(IBorderPoint prev){
		this.prevPoint = prev;
	}
	
	public IBorderPoint nextPoint(){
		return this.nextPoint;
	}
	
	/**
	 * Get the point n points ahead
	 * @param points
	 * @return
	 */
	public IBorderPoint nextPoint(int points){
		if(points==1)
			return this.nextPoint;
		else {
			return nextPoint.nextPoint(--points);
		}
	}
	
	public IBorderPoint prevPoint(){
		return this.prevPoint;
	}
	
	/**
	 * Get the point n points behind
	 * @param points
	 * @return
	 */
	public IBorderPoint prevPoint(int points){
		if(points==1)
			return this.prevPoint;
		else {
			return prevPoint.prevPoint(--points);
		}
	}
	
	public boolean hasNextPoint(){
		return nextPoint!=null;
	}
	
	public boolean hasPrevPoint(){
		return prevPoint!=null;
	}
}
