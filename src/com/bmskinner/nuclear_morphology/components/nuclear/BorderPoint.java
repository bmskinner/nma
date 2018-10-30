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
package com.bmskinner.nuclear_morphology.components.nuclear;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * This class contains border points around the periphery of a nucleus. Mostly
 * the same as an XYPoint now, after creation of Profiles. It does allow linkage
 * of points.
 * 
 * @deprecated since 1.13.3
 */
@Deprecated
public class BorderPoint extends com.bmskinner.nuclear_morphology.components.generic.XYPoint implements IBorderPoint {

    private static final long serialVersionUID = 1L;

    private BorderPoint prevPoint = null;
    private BorderPoint nextPoint = null;

    /**
     * Construct from x and y positions
     * 
     * @param x
     * @param y
     */
    public BorderPoint(double x, double y) {
        super(x, y);
    }

    /**
     * Construct from an existing XY point
     * 
     * @param p
     */
    public BorderPoint(IPoint p) {
        super(p);
    }

    /**
     * Construct from an existing border point
     * 
     * @param p
     */
    public BorderPoint(BorderPoint p) {
        super(p);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderPoint#setNextPoint(components.nuclear.
     * BorderPoint)
     */
    @Override
    public void setNextPoint(IBorderPoint next) {
        this.nextPoint = (BorderPoint) next;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderPoint#setPrevPoint(components.nuclear.
     * BorderPoint)
     */
    @Override
    public void setPrevPoint(IBorderPoint prev) {
        this.prevPoint = (BorderPoint) prev;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderPoint#nextPoint()
     */
    @Override
    public IBorderPoint nextPoint() {
        return this.nextPoint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderPoint#nextPoint(int)
     */
    @Override
    public IBorderPoint nextPoint(int points) {
        if (points == 1)
            return this.nextPoint;
        else {
            return nextPoint.nextPoint(--points);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderPoint#prevPoint()
     */
    @Override
    public IBorderPoint prevPoint() {
        return this.prevPoint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderPoint#prevPoint(int)
     */
    @Override
    public IBorderPoint prevPoint(int points) {
        if (points == 1)
            return this.prevPoint;
        else {
            return prevPoint.prevPoint(--points);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderPoint#hasNextPoint()
     */
    @Override
    public boolean hasNextPoint() {
        return nextPoint != null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.IBorderPoint#hasPrevPoint()
     */
    @Override
    public boolean hasPrevPoint() {
        return prevPoint != null;
    }

}
