package com.bmskinner.nuclear_morphology.gui.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
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
