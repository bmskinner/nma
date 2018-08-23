package com.bmskinner.nuclear_morphology.gui.events;

import java.util.EventObject;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;

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
