/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components.options;


/**
 * The options that must be available for detecting nuclear signals
 * @author bms41
 * @since 1.13.3
 *
 */
public interface INuclearSignalOptions extends IDetectionOptions {

	static final int    DEFAULT_SIGNAL_THRESHOLD     = 70;
	static final int    DEFAULT_MIN_SIGNAL_SIZE      = 5;
	static final double DEFAULT_MAX_SIGNAL_FRACTION  = 0.1;
	static final double DEFAULT_MIN_CIRC             = 0.0;
	static final double DEFAULT_MAX_CIRC             = 1.0;
		
	/**
	 * The analysis types available for detecting signals
	 * @author bms41
	 * @since 1.13.3
	 *
	 */
	public enum SignalDetectionMode {
		FORWARD, REVERSE, ADAPTIVE;
	}

	/**
	 * Get the maximum fraction of the parent component that the signal can occupy
	 * @return the maximum faction
	 */
	double getMaxFraction();

	/**
	 * Get the detection mode for the signal
	 * @return
	 */
	SignalDetectionMode getDetectionMode();

}