package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Implemented by classes that listen for changes in the display options
 * of warped images 
 * @author ben
 * @since 1.19.4
 */
public interface SignalWarpingDisplayListener {
	
	/**
	 * Inform that a signal display option has changed
	 * @param settings
	 */
	void signalWarpingDisplayChanged(@NonNull final SignalWarpingDisplaySettings settings);

}
