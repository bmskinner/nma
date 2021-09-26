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

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.signals.IShellResult.ShrinkType;

/**
 * Options for shell analysis
 * @author ben
 * @since 1.14.0
 *
 */
public interface IShellOptions extends IDetectionSubOptions {
	
	static final String SHELL_COUNT_KEY = "SHELL_COUNT";
	static final String EROSION_METHOD_KEY = "EROSION_METHOD";
	
	static final int DEFAULT_SHELL_COUNT = 5;
	static final ShrinkType DEFAULT_EROSION_METHOD = ShrinkType.AREA;
	
	/**
	 * Get the number of shells to divide the nucleus into
	 * @return
	 */
	int getShellNumber();
	
	/**
	 * Set the number of shells to divide the nucleus into
	 * @return
	 */
	void setShellNumber(int i);
	
	/**
	 * Get the erosion method to use for dividing
	 * @return
	 */
	ShrinkType getErosionMethod();
	
	/**
	 * Set the erosion method to use for dividing
	 * @return
	 */
	void setErosionMethod(@NonNull ShrinkType s);
	
	

}
