package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Listener for updates to the consensus nucleus of a dataset.
 * 
 * @author Ben Skinner
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

	/**
	 * Inform the listener that whether consensus nuclei should be filled or not has
	 * changed
	 */
	void consensusFillStateUpdated();

}
