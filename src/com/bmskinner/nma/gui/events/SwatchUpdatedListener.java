package com.bmskinner.nma.gui.events;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Notify the listener that swatch has changed in the given datasets.
 * 
 * @author bs19022
 *
 */
public interface SwatchUpdatedListener {

	/**
	 * Inform the listener that the display swatch globally has changed.
	 */
	void globalPaletteUpdated();

	/**
	 * Inform the listener that the colour of the given dataset has changed
	 * 
	 * @param dataset
	 */
	void colourUpdated(IAnalysisDataset dataset);
}
