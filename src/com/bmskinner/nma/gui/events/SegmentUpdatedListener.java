package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that segmentation has changed in the given datasets.
 * 
 * @author bs19022
 *
 */
public interface SegmentUpdatedListener {

	/**
	 * Inform the listener that segments in the given datasets have changed
	 * 
	 * @param datasets
	 */
	void segmentUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener that segments in the given dataset have changed
	 * 
	 * @param datasets
	 */
	void segmentUpdated(IAnalysisDataset dataset);

}
