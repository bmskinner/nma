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

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Provides methods relating to the abillity to refold consensus shapes for a
 * component
 * 
 * @author bms41
 * @since 1.13.4
 *
 * @param <E> the component that can be refolded
 */
public interface Refoldable<E extends CellularComponent> {

    /**
     * Check if the collection has a consensus component of type <b>E</b>
     * 
     * @return
     */
    boolean hasConsensus();

    /**
     * Set the consensus nucleus for the collection
     * 
     * @param n
     */
    void setConsensus(@Nullable Consensus<E> component);

    /**
     * Get the consensus nucleus if set
     * 
     * @return the consensus, or null if not present
     */
    E getConsensus();
    
    Consensus<E> getRawConsensus();
    
    /**
     * Apply an offset to the consensus shape
     * @param xOffset
     * @param yOffset
     */
    void offsetConsensus(double xOffset, double yOffset); 
    
    /**
     * Apply a rotation to the consensus shape;
     * @param degrees
     */
    void rotateConsensus(double degrees);
    
    /**
     * Get the current x and y offset as a point
     * @return
     */
    IPoint currentConsensusOffset();
    
    /**
     * Get the current rotation offset
     * @return
     */
    double currentConsensusRotation();
}
