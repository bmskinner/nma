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

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

public interface IMutableAnalysisOptions extends IAnalysisOptions {
	
	
	/**
	 * Set the detection options for the given component
	 * @param key
	 * @param options
	 */
	void setDetectionOptions(String key, IMutableDetectionOptions options);

	/**
	 * Set the proportion of the perimeter to use when profiling nuclei
	 * @param proportion
	 */
	void setAngleWindowProportion(double proportion);

	/**
	 * Set the type of nucleus / cell being analysed
	 * @param nucleusType
	 */
	void setNucleusType(NucleusType nucleusType);


	/**
	 * Set whether the consensus nucleus should be refolded during the analysis
	 * @param refoldNucleus
	 */
	void setRefoldNucleus(boolean refoldNucleus);
	
	
	/**
	 * Set whether nuclei that cannot be detected should be retained as a separate collection
	 * @param keepFailedCollections
	 */
	void setKeepFailedCollections(boolean keepFailedCollections);

}
