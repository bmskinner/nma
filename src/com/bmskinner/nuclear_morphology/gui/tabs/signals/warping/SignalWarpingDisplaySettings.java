package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;

/**
 * Store display settings for warped signals. These settings
 * can be applied to any selection of warped signals
 * @author ben
 * @since 1.19.4
 *
 */
public class SignalWarpingDisplaySettings extends DefaultOptions {
	
	public static final boolean DEFAULT_IS_PSEUDOCOLOUR = true;
	public static final int DEFAULT_THRESHOLD = 0;
	
	
	public static final String PSEUDOCOLOUR_KEY = "IS_PSEUDOCOLOUR";
	public static final String THRESHOLD_KEY = "DISPLAY_THRESHOLD";

	private static final long serialVersionUID = 1L;
		
	public SignalWarpingDisplaySettings() {
		setBoolean(PSEUDOCOLOUR_KEY, DEFAULT_IS_PSEUDOCOLOUR);
		setInt(THRESHOLD_KEY, DEFAULT_THRESHOLD);
	}
	

}
