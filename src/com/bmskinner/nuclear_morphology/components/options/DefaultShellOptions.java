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
 * The default implementation of the shell options
 * @author ben
 * @since 1.14.0
 *
 */
public class DefaultShellOptions extends AbstractHashOptions implements IShellOptions {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Create with default options
	 */
	public DefaultShellOptions() {
		intMap.put(SHELL_COUNT_KEY, DEFAULT_SHELL_COUNT);
		stringMap.put(EROSION_METHOD_KEY, DEFAULT_EROSION_METHOD.name());
	}
	
	/**
	 * Create from an existing set of options
	 * @param s the options to copy
	 */
	public DefaultShellOptions(@NonNull IShellOptions s) {
		setShellNumber(s.getShellNumber());
		setErosionMethod(s.getErosionMethod());
	}

	@Override
	public int getShellNumber() {
		if(intMap.containsKey(SHELL_COUNT_KEY))
			return intMap.get(SHELL_COUNT_KEY);
		return -1;
	}

	@Override
	public void setShellNumber(int i) {
		intMap.put(SHELL_COUNT_KEY, i);
	}

	@Override
	public ShrinkType getErosionMethod() {
		if(stringMap.containsKey(EROSION_METHOD_KEY))
			return(ShrinkType.valueOf(stringMap.get(EROSION_METHOD_KEY)));
		return null;
	}

	@Override
	public void setErosionMethod(@NonNull ShrinkType s) {
		stringMap.put(EROSION_METHOD_KEY, s.name());
	}

	@Override
	public IDetectionSubOptions duplicate() {
		return new DefaultShellOptions(this);
	}

}
