package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

/**
 * Used by classes that want to know the progress of
 * a signal warping analysis
 * @author ben
 *
 */
public interface SignalWarpingProgressEventListener {
	
	/**
	 * Inform listeners that a signal warping has progressed
	 * @param progress the value (0-100)
	 */
	void warpingProgressed(int progress);

}
