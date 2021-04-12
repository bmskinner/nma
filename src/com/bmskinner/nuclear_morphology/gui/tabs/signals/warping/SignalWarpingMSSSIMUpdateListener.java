package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

public interface SignalWarpingMSSSIMUpdateListener {
	
	/**
	 * Inform listeners that MS-SSIM* values to be displayed
	 * have changed
	 * @param value the value to display
	 * @param message the message to display
	 */
	void MSSSIMUpdated(String message);

}
