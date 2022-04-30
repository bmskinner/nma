package com.bmskinner.nma.gui.events.revamp;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;

public interface ClusterGroupsUpdatedListener {

	/**
	 * Inform the listener the given datasets have updated cluster groups
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

	/**
	 * Inform listeners that the dataset has added the given group
	 * 
	 * @param dataset
	 * @param group
	 */
	void clusterGroupAdded(IAnalysisDataset dataset, IClusterGroup group);

}
