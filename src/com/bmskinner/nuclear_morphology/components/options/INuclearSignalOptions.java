/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.options;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.gui.Labels;

/**
 * The options that must be available for detecting nuclear signals.
 * Rather than specifying maximum areas, this enables setting a maximum proportion 
 * of the nucleus continaing the signal to be covered by the signal.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface INuclearSignalOptions extends IDetectionOptions {

    static final String MAX_FRACTION       = "Max fraction";
    static final String DETECTION_MODE_KEY = "DETECTION_MODE";

    static final int                 DEFAULT_SIGNAL_THRESHOLD    = 70;
    static final int                 DEFAULT_MIN_SIGNAL_SIZE     = 5;
    static final int                 DEFAULT_MAX_SIGNAL_SIZE     = 100000;
    static final double              DEFAULT_MAX_SIGNAL_FRACTION = 0.1;
    static final double              DEFAULT_MIN_CIRC            = 0.0;
    static final double              DEFAULT_MAX_CIRC            = 1.0;
    static final SignalDetectionMode DEFAULT_METHOD              = SignalDetectionMode.FORWARD;
    static final int                 DEFAULT_CHANNEL             = 0;

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

    /**
     * Get the maximum fraction of the parent component that the signal can
     * occupy
     * 
     * @return the maximum faction
     */
    double getMaxFraction();

    /**
     * Get the detection mode for the signal
     * 
     * @return
     */
    SignalDetectionMode getDetectionMode();
    
    /**
     * Set the maximum fraction of the parent component (e.g. nucleus) that the
     * signal can occupy
     * 
     * @param maxFraction
     */
    void setMaxFraction(double maxFraction);

    /**
     * Set the detection mode for signals
     * 
     * @param detectionMode
     */
    void setDetectionMode(SignalDetectionMode detectionMode);
    
    /**
     * Set shell analysis sub-options
     * @param shellOptions the shell options
     */
    void setShellOptions(@NonNull IShellOptions shellOptions);
    
    /**
     * Test if shell analysis options are set
     * @return true if there are shell options, false otherwise
     */
    boolean hasShellOptions();
    
    /**
     * Get shell analysis sub-options
     */
    IShellOptions getShellOptions();

}
