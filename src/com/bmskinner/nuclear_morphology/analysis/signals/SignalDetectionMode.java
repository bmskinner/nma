package com.bmskinner.nuclear_morphology.analysis.signals;

import com.bmskinner.nuclear_morphology.gui.Labels;

/**
 * The analysis types available for detecting signals
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public enum SignalDetectionMode {
    FORWARD("Forward", Labels.Signals.FORWARD_THRESHOLDING_RADIO_LABEL), 
    REVERSE("Reverse", Labels.Signals.REVERSE_THRESHOLDING_RADIO_LABEL), 
    ADAPTIVE("Adaptive", Labels.Signals.ADAPTIVE_THRESHOLDING_RADIO_LABEL);

    private String name;
    private String desc;

    private SignalDetectionMode(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    @Override
	public String toString() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

}