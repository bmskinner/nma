package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that landmarks have changed in datasets.
 * 
 * @author bs19022
 *
 */
public interface LandmarkUpdatedListener {

	/**
	 * Inform the listener that landmarks in the given datasets have changed
	 * 
	 * @param datasets
	 */
	void landmarkUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener that landmarks in the given dataset have changed
	 * 
	 * @param datasets
	 */
	void landmarkUpdated(IAnalysisDataset dataset);

}
