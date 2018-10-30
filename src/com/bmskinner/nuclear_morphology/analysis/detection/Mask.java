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
package com.bmskinner.nuclear_morphology.analysis.detection;

/**
 * A mask that describes whether a value is present or not within each pixel
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface Mask {

    int getWidth();

    int getHeight();

    /**
     * Offset the mask by the given amount. Empty values are filled in with
     * false
     * 
     * @param x
     * @param y
     * @return
     */
    Mask offset(int x, int y);

    /**
     * Calculate the logical AND of the two masks
     * 
     * @param m
     * @return
     */
    Mask and(Mask m);

    /**
     * Get the value at the given position
     * 
     * @param x
     * @param y
     * @return
     */
    boolean get(int x, int y);

    /**
     * Set all the values in the mask to false
     * 
     * @return
     */
    Mask setFalse();

    /**
     * Set all the values in the mask to true
     * 
     * @return
     */
    Mask setTrue();

    /**
     * Set the value at the given point
     * 
     * @param x
     * @param y
     * @param b
     */
    void set(int x, int y, boolean b);

    boolean[][] toArray();

}
