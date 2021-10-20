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

import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Calculate basic stats for cellular components. Used to fill in missing values
 * when objects were not created using a detector
 *
 * @author ben
 * @since 1.13.5
 *
 */
public final class ComponentMeasurer {
	
	private static final Logger LOGGER = Logger.getLogger(ComponentMeasurer.class.getName());
	
	/**
	 * We only use static methods here. Don't allow a public constructor.
	 */
	private ComponentMeasurer() {}

    /**
     * Calculate the perimeter of the shape based on the border list.
     * 
     * @param c the component to measure
     * @return the perimeter of the component
     */
	public static double calculatePerimeter(@NonNull final CellularComponent c) {
		double perimeter = 0;
		try {
			for(int i=0; i<c.getBorderLength(); i++) {
				perimeter += c.getBorderPoint(i)
						.getLengthTo(c.getBorderPoint(CellularComponent.wrapIndex(i+1, c.getBorderLength())));
			}
		} catch(UnavailableBorderPointException e) {
			LOGGER.log(Loggable.STACK, "Unable to calculate perimeter of object", e);
			return Statistical.ERROR_CALCULATING_STAT;
		}
		return perimeter;
	}

    /**
     * Calculate the area of an object.
     * 
     * @param c the component to measure
     * @return the area of the component
     */
    public static double calculateArea(@NonNull final CellularComponent c) {
        return Stats.area(c.toShape());
    }

}
