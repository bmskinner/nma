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

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

/**
 * Store options for background removal in images
 * @author bms41
 * @since 1.13.4
 *
 */
public class PreprocessingOptions 
	extends AbstractHashOptions 
	implements IDetectionSubOptions {

	private static final long serialVersionUID = 1L;
	
	public static final String USE_GAUSSIAN     = "Use Gaussian blur";
	public static final String USE_KUWAHARA     = "Use Kuwahara filter";
	public static final String USE_ROLLING_BALL = "Use Rolling ball";
	public static final String USE_FLATTENING   = "Use flattening";
	public static final String USE_RAISING      = "Use raising";
	
	public static final String GAUSSIAN_RADIUS = "Gaussian radius";
	public static final String KUWAHARA_RADIUS = "Kuwahara radius";
	public static final String ROLLING_BALL_RADIUS = "Rolling ball radius";
	public static final String FLATTENING_THRESHOLD = "Flattening threshold";
	public static final String RAISING_THRESHOLD = "Raising threshold";
	
	public static final int     DEFAULT_KUWAHARA_KERNEL_RADIUS    = 3;
	public static final boolean DEFAULT_USE_KUWAHARA              = true;
	public static final boolean DEFAULT_FLATTEN_CHROMOCENTRES     = true;
	public static final int     DEFAULT_FLATTEN_THRESHOLD         = 100;
	public static final boolean DEFAULT_RAISE_CHROMOCENTRES     = false;
	public static final int     DEFAULT_RAISE_THRESHOLD         = 100;
	
	public PreprocessingOptions(){
		
		setBoolean(USE_GAUSSIAN, false);
		setBoolean(USE_KUWAHARA, DEFAULT_USE_KUWAHARA);
		setBoolean(USE_ROLLING_BALL, false);
		setBoolean(USE_ROLLING_BALL, false);
		setBoolean(USE_FLATTENING, DEFAULT_FLATTEN_CHROMOCENTRES);
		setBoolean(USE_RAISING, DEFAULT_RAISE_CHROMOCENTRES);
	}
	 

}
