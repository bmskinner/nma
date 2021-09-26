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
package com.bmskinner.nuclear_morphology.gui.events;

import java.util.EventObject;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * Signal that a cell in a dataset has been updated - that is, it's data has
 * changed.
 * @author bms41
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
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public class CellUpdatedEvent extends EventObject {
		
		ICell c;
		IAnalysisDataset d;
		
		
		public CellUpdatedEvent(Object source, ICell cell, IAnalysisDataset dataset) {
	        super(source);
	        c = cell;
	        d = dataset;
	    }
		
		public IAnalysisDataset getDataset() {
			return d;
		}
		
		public ICell getCell() {
			return c;
		}
	}

}
