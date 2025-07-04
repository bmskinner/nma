package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Listener for updates to the file paths with a dataset. Used when nucleus
 * image paths need to be updated.
 * 
 * @author ben
 *
 */
public interface FilePathUpdatedListener {

	/**
	 * Inform the listener that file paths in the given datasets have changed
	 * 
	 * @param datasets
	 */
	void filePathUpdated(List<IAnalysisDataset> datasets);

	/**
	 * Inform the listener that file paths in the given dataset have changed
	 * 
	 * @param datasets
	 */
	void filePathUpdated(IAnalysisDataset dataset);

}
