package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that nuclear signals have changed in datasets.
 * 
 * @author Ben Skinner
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

//	/**
//	 * Inform the listener that signals in the given dataset have has a colour
//	 * change
//	 * 
//	 * @param dataset
//	 */
//	void nuclearSignalColourUpdated(IAnalysisDataset dataset);
//
//	/**
//	 * Inform the listener that signals in the given datasets have has a colour
//	 * change
//	 * 
//	 * @param dataset
//	 */
//	void nuclearSignalColourUpdated(List<IAnalysisDataset> datasets);

}
