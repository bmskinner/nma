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
package com.bmskinner.nma.gui.events;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Signal that a cell in a dataset has been updated - that is, it's data has
 * changed.
 * 
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
public interface CellUpdatedEventListener {

	/**
	 * Notify registered listeners of the event
	 */
	void cellUpdatedEventReceived(CellUpdatedEvent event);

	/**
	 * An update event for a single cell.
	 * 
	 * @author Ben Skinner
	 * @since 1.14.0
	 *
	 */
	public record CellUpdatedEvent(Object source, ICell cell, IAnalysisDataset dataset) {
	}

}
