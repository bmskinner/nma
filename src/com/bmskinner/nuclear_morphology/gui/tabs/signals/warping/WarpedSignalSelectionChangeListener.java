package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.signals.IWarpedSignal;

public interface WarpedSignalSelectionChangeListener {

	/**
	 * Notify listeners that the selected warped images have changed
	 * 
	 * @param images
	 */
	void warpedSignalSelectionChanged(List<IWarpedSignal> images);

	/**
	 * Notify listeners when the colour, thresholding or other visualisation of a
	 * warped signal changes
	 * 
	 * @param images
	 */
	void warpedSignalVisualisationChanged(List<IWarpedSignal> images);

}
