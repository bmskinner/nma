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

package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.AbstractDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultCannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

/**
 * The default detection options for a nucleus
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultNucleusDetectionOptions extends AbstractDetectionOptions {

	public static final int    DEFAULT_MIN_NUCLEUS_SIZE = 2000;
	public static final int    DEFAULT_MAX_NUCLEUS_SIZE = 10000;
	public static final double DEFAULT_MIN_NUCLEUS_CIRC = 0.2;
	public static final double DEFAULT_MAX_NUCLEUS_CIRC = 0.8;
	public static final int    DEFAULT_NUCLEUS_THRESHOLD = 36;
	private static final double    DEFAULT_SCALE = 1.0;
	private static final int    DEFAULT_CHANNEL      = 2;
	private static final long serialVersionUID = 1L;
	
	/**
	 * Construct for a folder with default values
	 * @param folder the image folder
	 */
	public DefaultNucleusDetectionOptions(File folder){
		super(folder);
		
		this.setSize(DEFAULT_MIN_NUCLEUS_SIZE, DEFAULT_MAX_NUCLEUS_SIZE);
		this.setCircularity(DEFAULT_MIN_NUCLEUS_CIRC, DEFAULT_MAX_NUCLEUS_CIRC);
		this.setThreshold(DEFAULT_NUCLEUS_THRESHOLD);
		this.setScale(DEFAULT_SCALE);
		this.setChannel(DEFAULT_CHANNEL);
		
		this.setCannyOptions( new DefaultCannyOptions());
	}
	
	public DefaultNucleusDetectionOptions(IDetectionOptions template){
		super(template);
		
	}
	
	@Override
	public DefaultNucleusDetectionOptions setSize(double min, double max){
		super.setSize(min, max);
		return this;
	}
	
	@Override
	public DefaultNucleusDetectionOptions setCircularity(double min, double max){
		super.setCircularity(min, max);
		return this;
	}
	

	@Override
	public boolean isValid(CellularComponent c) {
		if(c instanceof Nucleus){
			return super.isValid(c);
		} else {
			return false;
		}
	}

	@Override
	public IMutableDetectionOptions duplicate() {
		return new DefaultNucleusDetectionOptions(this);
	}
	
	

}
