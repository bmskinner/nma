package com.bmskinner.nuclear_morphology.gui.events;

import java.util.EventObject;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * Signal that the populations list should be updated when datasets are added or removed
 * @author bms41
 * @since 1.14.0
 *
 */
public interface PopulationListUpdateListener {
	
	/**
	 * Inform the listener that the populations list has been updated
	 * @param event
	 */
	void populationListUpdateEventReceived(PopulationListUpdateEvent event);

	public class PopulationListUpdateEvent extends EventObject {
		public PopulationListUpdateEvent(Object source) {
			super(source);
		}
	}

}
