package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that datasets have been added to the global set
 * 
 * @author bs19022
 *
 */
public interface DatasetAddedListener {

	/**
	 * Inform the listener the given datasets have been added to the global set
	 * 
	 * @param datasets
	 */
	void datasetAdded(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener the given dataset has been added to the global set
	 * 
	 * @param datasets
	 */
	void datasetAdded(IAnalysisDataset dataset);

	/**
	 * Inform the listener the given datasets have been deleted
	 * 
	 * @param datasets
	 */
	void datasetDeleted(List<IAnalysisDataset> datasets);

}
