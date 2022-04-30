package com.bmskinner.nma.gui.events.revamp;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that nuclear signals have changed in datasets.
 * 
 * @author bs19022
 *
 */
public interface NuclearSignalUpdatedListener {

	/**
	 * Inform the listener that signals in the given datasets have changed
	 * 
	 * @param datasets
	 */
	void nuclearSignalUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener that signals in the given dataset have changed
	 * 
	 * @param datasets
	 */
	void nuclearSignalUpdated(IAnalysisDataset dataset);

}
