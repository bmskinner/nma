package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that values in datasets have been updated
 * 
 * @author Ben Skinner
 *
 */
public interface DatasetUpdatedListener {
	/**
	 * Inform the listener the given datasets have been updated
	 * 
	 * @param datasets
	 */
	void datasetUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener the given dataset has been updated
	 * 
	 * @param datasets
	 */
	void datasetUpdated(IAnalysisDataset dataset);
}
