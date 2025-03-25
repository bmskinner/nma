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
package com.bmskinner.nma.components.signals;

import com.bmskinner.nma.components.cells.CellularComponent;

/**
 * The methods available to a nuclear signal, which is a type of cellular
 * component
 * 
 * @author Ben Skinner
 * @since 1.13.3
 *
 */
public interface INuclearSignal extends CellularComponent {

    /**
     * Get the index of the closest point in the nuclear periphery to this
     * signal
     * 
     * @return
     */
    int getClosestBorderPoint();

    /**
     * Set the index of the closest point in the nuclear periphery to this
     * signal
     * 
     * @return
     */
    void setClosestBorderPoint(int p);


    @Override
	INuclearSignal duplicate();

}
