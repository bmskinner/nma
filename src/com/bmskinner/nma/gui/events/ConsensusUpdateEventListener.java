package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Interface for handlers of consensus rotation and translation
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public interface ConsensusUpdateEventListener {

	/**
	 * Alert the listener that consensus rotation has been updated
	 * 
	 * @param datasets
	 * @param rotation
	 */
	void consensusRotationUpdateReceived(List<IAnalysisDataset> datasets, double rotation);

	/**
	 * Alert the listener that consensus rotation has been updated
	 * 
	 * @param datasets
	 * @param rotation
	 */
	void consensusRotationUpdateReceived(IAnalysisDataset dataset, double rotation);

	/**
	 * Alert the listener that consensus rotation has been reset to default
	 * 
	 * @param datasets
	 */
	void consensusRotationResetReceived(List<IAnalysisDataset> datasets);

	/**
	 * Alert the listener that consensus rotation has been reset to default
	 * 
	 * @param datasets
	 */
	void consensusRotationResetReceived(IAnalysisDataset dataset);

	/**
	 * Alert the listener that consensus translation has been updated
	 * 
	 * @param datasets
	 * @param x
	 * @param y
	 */
	void consensusTranslationUpdateReceived(List<IAnalysisDataset> datasets, double x, double y);

	/**
	 * Alert the listener that consensus translation has been updated
	 * 
	 * @param dataset
	 * @param x
	 * @param y
	 */
	void consensusTranslationUpdateReceived(IAnalysisDataset dataset, double x, double y);

	/**
	 * Alert the listener that consensus translation has been reset to default
	 * 
	 * @param datasets
	 */
	void consensusTranslationResetReceived(List<IAnalysisDataset> datasets);

	/**
	 * Alert the listener that consensus translation has been reset to default
	 * 
	 * @param dataset
	 */
	void consensusTranslationResetReceived(IAnalysisDataset dataset);
}
