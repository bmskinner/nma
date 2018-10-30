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

import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * There can be many types of flagellum; the type of interest mainly is the
 * sperm tail.
 * 
 * @author bms41
 *
 */
public interface Flagellum extends CellularComponent {

    public List<IPoint> getSkeleton();

    /**
     * Fetch the skeleton offset to zero
     * 
     * @return
     */
    List<IPoint> getOffsetSkeleton();

    List<IPoint> getBorder();

    // positions are offset by the bounding rectangle for easier plotting
    List<IPoint> getOffsetBorder();

    double getLength();

    Flagellum duplicate();

}
