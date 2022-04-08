package com.bmskinner.nuclear_morphology.gui.events.revamp;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * Listener for updates to the profiles of a dataset.
 * 
 * @author ben
 *
 */
public interface ProfilesUpdatedListener {

	/**
	 * Inform the listener that the profiles in the given datasets has changed
	 * 
	 * @param datasets
	 */
	void profilesUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener that the profiles in the given dataset has changed
	 * 
	 * @param datasets
	 */
	void profilesUpdated(IAnalysisDataset dataset);

}
