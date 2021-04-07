package com.bmskinner.nuclear_morphology.gui.tabs.signals.warping;

import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.options.AbstractHashOptions;

public class SignalWarpingDisplaySettings extends AbstractHashOptions {
	
	public static final String PSEUDOCOLOUR_KEY = "IS_PSEUDOCOLOUR";
	public static final String THRESHOLD_KEY = "DISPLAY_THRESHOLD";
	public static final String SIGNAL_UUID_KEY = "SIGNAL_UUID";

	private static final long serialVersionUID = 1L;
	
	private UUID signalId;
	
	public SignalWarpingDisplaySettings() {
		
	}
	
	public UUID getSignalUUID() {
		return signalId;
	}
	
	public void setSignalUUID(UUID id) {
		signalId = id;
	}

}
