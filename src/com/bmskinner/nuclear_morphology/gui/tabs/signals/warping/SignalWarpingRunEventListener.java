package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

/**
 * Interface for classes that respond to requests
 * for a new signal warping
 * @author ben
 * @since 1.19.4
 *
 */
public interface SignalWarpingRunEventListener {
	
	/**
	 * Inform the listener that a new signal warping has
	 * been requested
	 * @param settings
	 */
	void runEventReceived(SignalWarpingRunSettings settings);

}
