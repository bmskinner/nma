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

/**
 * The standard implementation of the {@link IBorderPoint} interface.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultBorderPoint extends FloatPoint implements IBorderPoint {
    private static final long serialVersionUID = 1L;

    private transient IBorderPoint prevPoint = null;
    private transient IBorderPoint nextPoint = null;

    /**
     * Construct from x and y positions
     * 
     * @param x
     * @param y
     */
    public DefaultBorderPoint(float x, float y) {
        super(x, y);
    }

    /**
     * Construct from x and y positions
     * 
     * @param x
     * @param y
     */
    public DefaultBorderPoint(double x, double y) {
        super((float) x, (float) y);
    }

    /**
     * Construct from an existing XY point
     * 
     * @param p
     */
    public DefaultBorderPoint(IPoint p) {
        super(p);
    }

    /**
     * Set the next point in the border
     * 
     * @param next
     */
    @Override
	public void setNextPoint(IBorderPoint next) {
        this.nextPoint = next;
    }

    /**
     * Set the previous point in the border
     * 
     * @param prev
     */
    @Override
	public void setPrevPoint(IBorderPoint prev) {
        this.prevPoint = prev;
    }

    @Override
	public IBorderPoint nextPoint() {
        return this.nextPoint;
    }

    /**
     * Get the point n points ahead
     * 
     * @param points
     * @return
     */
    @Override
	public IBorderPoint nextPoint(int points) {
        if (points == 1)
            return this.nextPoint;
		return nextPoint.nextPoint(--points);
    }

    @Override
	public IBorderPoint prevPoint() {
        return this.prevPoint;
    }

    /**
     * Get the point n points behind
     * 
     * @param points
     * @return
     */
    @Override
	public IBorderPoint prevPoint(int points) {
        if (points == 1)
            return this.prevPoint;
		return prevPoint.prevPoint(--points);
    }

    @Override
	public boolean hasNextPoint() {
        return nextPoint != null;
    }

    @Override
	public boolean hasPrevPoint() {
        return prevPoint != null;
    }
    
    
    
}
