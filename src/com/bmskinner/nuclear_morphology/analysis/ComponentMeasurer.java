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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.Iterator;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Calculate basic stats for cellular components. Used to fill in missing values
 * when objects were not created using a detector
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class ComponentMeasurer {

    /**
     * Calculate the perimeter of the shape based on the border list
     * 
     * @param c
     *            the component to measure
     * @return
     */
    public static double calculatePerimeter(CellularComponent c) {
        double perimeter = 0;
        Iterator<IBorderPoint> it = c.getBorderList().iterator();
        while (it.hasNext()) {
            IBorderPoint point = it.next();
            perimeter += point.getLengthTo(point.prevPoint());
        }
        return perimeter;
    }

    /**
     * Calculate the area of the object
     * 
     * @param c the component to measure
     * @return
     */
    public static double calculateArea(CellularComponent c) {
        return Stats.area(c.toShape());
    }

}
