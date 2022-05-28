package com.bmskinner.nma.gui.events;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

public interface GLCMUpdateListener {

	/**
	 * Alert the listener that GLCM data has been added to the given dataset
	 * 
	 * @param datasets
	 */
	void GLCMDataAdded(List<IAnalysisDataset> datasets);

}
