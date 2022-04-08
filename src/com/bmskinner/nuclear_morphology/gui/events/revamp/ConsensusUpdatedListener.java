package com.bmskinner.nuclear_morphology.gui.events.revamp;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * Listener for updates to the consensus nucleus of a dataset.
 * 
 * @author ben
 *
 */
public interface ConsensusUpdatedListener {

	/**
	 * Inform the listener that the consensus nucleus in the given datasets has
	 * changed
	 * 
	 * @param datasets
	 */
	void consensusUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener that the consensus nucleus in the given dataset has
	 * changed
	 * 
	 * @param datasets
	 */
	void consensusUpdated(IAnalysisDataset dataset);

}
