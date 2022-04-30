package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that datasets have been selected for display
 * 
 * @author bs19022
 *
 */
public interface DatasetSelectionUpdatedListener {

	/**
	 * Inform the listener the given datasets have been selected for display
	 * 
	 * @param datasets
	 */
	void datasetSelectionUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener the given dataset has been selected for display
	 * 
	 * @param datasets
	 */
	void datasetSelectionUpdated(IAnalysisDataset dataset);

}
