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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.events.CellUpdatedEventListener.CellUpdatedEvent;

public class CelllUpdateEventHandler {
	
	Object parent;
	List<CellUpdatedEventListener> listeners = new CopyOnWriteArrayList<>();

	public CelllUpdateEventHandler(Object parent){
		this.parent = parent;
	}

	/**
	 * Fire a cell update event with the given cell
	 * @param method the method name
	 * @param dataset the dataset to run the method on
	 */
	public synchronized void fireCelllUpdateEvent(ICell cell, IAnalysisDataset dataset) {
		CellUpdatedEvent event = new CellUpdatedEvent(parent, cell, dataset);
		for(CellUpdatedEventListener l : listeners) {
			l.cellUpdatedEventReceived(event);
		}
	}
	
	public void addCellUpdatedEventListener(CellUpdatedEventListener l) {
		listeners.add(l);
	}
	
	public void removeCellUpdatedEventListener(CellUpdatedEventListener l) {
		listeners.remove(l);
	}

}
