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
package com.bmskinner.nma.visualisation.datasets;

import org.jfree.data.xy.XYDataset;

import com.bmskinner.nma.components.cells.CellularComponent;

/**
 * Adds storage of cellular components to an XYDataset
 * 
 * @author Ben Skinner
 *
 * @param <E>
 *            A cellular component to be drawn
 */
public interface OutlineDataset<E extends CellularComponent> extends XYDataset {

    /**
     * Set the component for the given series
     * 
     * @param seriesKey
     * @param n
     */
    void setComponent(Comparable seriesKey, E n);

    /**
     * Get the component for the given series
     * 
     * @param seriesKey
     *            the series
     * @return
     */
    E getComponent(Comparable seriesKey);

    /**
     * Check if the given series has a component
     * 
     * @param seriesKey
     * @return
     */
    boolean hasComponent(Comparable seriesKey);
}
