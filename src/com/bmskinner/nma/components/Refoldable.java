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
package com.bmskinner.nma.components;

import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Consensus;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;

/**
 * Provides methods relating to the abillity to refold consensus shapes for a
 * component
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 * @param <E> the component that can be refolded
 */
public interface Refoldable {

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
    void setConsensus(@Nullable Consensus component);

    /**
     * Get a vertically oriented copy of the consensus
     * nucleus if present, with any other offsets applied
     * 
     * @return the consensus, or null if not present
     * @throws MissingLandmarkException 
     * @throws ComponentCreationException 
     */
    Nucleus getConsensus() throws MissingLandmarkException, ComponentCreationException;
    
    /**
     * Get the consensus nucleus. Unlike {@link getConsensus},
     * the actual object is returned, not a vertically oriented
     * copy
     * @return
     */
    Consensus getRawConsensus();
    
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
