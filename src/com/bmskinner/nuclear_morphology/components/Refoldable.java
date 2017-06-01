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

package com.bmskinner.nuclear_morphology.components;

/**
 * Provides methods relating to the abillity to refold consensus shapes for a
 * component
 * 
 * @author bms41
 * @since 1.13.4
 *
 * @param <E>
 *            the component that can be refolded
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
    void setConsensus(E component);

    /**
     * Get the consensus nucleus if set
     * 
     * @return the consensus, or null if not present
     */
    E getConsensus();

    /**
     * Set the refolding state
     * 
     * @param b
     */
    void setRefolding(boolean b);

    /**
     * Test if the consensus is being refolded
     * 
     * @return
     */
    boolean isRefolding();

}
