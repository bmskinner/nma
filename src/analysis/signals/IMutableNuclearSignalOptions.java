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

package analysis.signals;

import analysis.IMutableDetectionOptions;

/**
 * The interface for signal detection parameters
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IMutableNuclearSignalOptions extends INuclearSignalOptions, IMutableDetectionOptions {
	
	/**
	 * Set the maximum fraction of the parent component (e.g. nucleus) that the signal can occupy
	 * @param maxFraction
	 */
	void setMaxFraction(double maxFraction);
	
	/**
	 * Set the detection mode for signals
	 * @param detectionMode
	 */
	void setDetectionMode(SignalDetectionMode detectionMode);

}
