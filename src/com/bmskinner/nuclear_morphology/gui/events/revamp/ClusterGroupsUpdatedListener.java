package com.bmskinner.nuclear_morphology.gui.events.revamp;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

public interface ClusterGroupsUpdatedListener {

	/**
	 * Inform the listener the given datasets have been selected for display
	 * 
	 * @param datasets
	 */
	void clusterGroupsUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener the given datasets has updated cluster groups
	 * 
	 * @param datasets
	 */
	void clusterGroupsUpdated(IAnalysisDataset dataset);

}
