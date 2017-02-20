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
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions.IPreprocessingOptions;

/**
 * Store options for background removal in images
 * @author bms41
 * @since 1.13.4
 *
 */
public class PreprocessingOptions 
	extends AbstractHashOptions 
	implements IPreprocessingOptions {

	private static final long serialVersionUID = 1L;
	
	public static final String USE_GAUSSIAN     = "Use Gaussian blur";
	public static final String USE_KUWAHARA     = "Use Kuwahara filter";
	public static final String USE_ROLLING_BALL = "Use Rolling ball";
	public static final String USE_FLATTENING   = "Use flattening";
	public static final String USE_RAISING      = "Use raising";
	public static final String USE_COLOUR_THRESHOLD = "Use colour threshold";
		
	public static final String GAUSSIAN_RADIUS = "Gaussian radius";
	public static final String KUWAHARA_RADIUS = "Kuwahara radius";
	public static final String ROLLING_BALL_RADIUS = "Rolling ball radius";
	public static final String FLATTENING_THRESHOLD = "Flattening threshold";
	public static final String RAISING_THRESHOLD = "Raising threshold";
	
	public static final String MIN_HUE = "Min hue";
	public static final String MAX_HUE = "Max hue";
	public static final String MIN_SAT = "Min saturation";
	public static final String MAX_SAT = "Max saturation";
	public static final String MIN_BRI = "Min brightness";
	public static final String MAX_BRI = "Max brightness";
	
	public static final int     DEFAULT_KUWAHARA_KERNEL_RADIUS    = 3;
	public static final boolean DEFAULT_USE_GAUSSIAN              = false;
	public static final boolean DEFAULT_USE_KUWAHARA              = true;
	public static final boolean DEFAULT_USE_ROLLING_BALL          = false;
	public static final boolean DEFAULT_FLATTEN_CHROMOCENTRES     = true;
	public static final int     DEFAULT_FLATTEN_THRESHOLD         = 100;
	public static final boolean DEFAULT_RAISE_CHROMOCENTRES     = false;
	public static final int     DEFAULT_RAISE_THRESHOLD         = 100;
	public static final boolean DEFAULT_USE_COLOUR_THRESHOLD    = false;
	
	public static final int     DEFAULT_MIN_HUE         = 0;
	public static final int     DEFAULT_MAX_HUE         = 255;
	public static final int     DEFAULT_MIN_SAT         = 0;
	public static final int     DEFAULT_MAX_SAT         = 255;
	public static final int     DEFAULT_MIN_BRI         = 0;
	public static final int     DEFAULT_MAX_BRI         = 255;
	
	/**
	 * Create options with default values
	 * 
	 */
	public PreprocessingOptions(){
		
		setBoolean(USE_GAUSSIAN, DEFAULT_USE_GAUSSIAN);
		setBoolean(USE_KUWAHARA, DEFAULT_USE_KUWAHARA);
		setBoolean(USE_ROLLING_BALL, DEFAULT_USE_ROLLING_BALL);
		setBoolean(USE_FLATTENING, DEFAULT_FLATTEN_CHROMOCENTRES);
		setBoolean(USE_RAISING, DEFAULT_RAISE_CHROMOCENTRES);
		setBoolean(USE_COLOUR_THRESHOLD, DEFAULT_USE_COLOUR_THRESHOLD);
		
		setInt(MIN_HUE, DEFAULT_MIN_HUE);
		setInt(MAX_HUE, DEFAULT_MAX_HUE);
		setInt(MIN_SAT, DEFAULT_MIN_SAT);
		setInt(MAX_SAT, DEFAULT_MAX_SAT);
		setInt(MIN_BRI, DEFAULT_MIN_BRI);
		setInt(MAX_BRI, DEFAULT_MAX_BRI);
	}
	
	
	
	public void setUseColourThreshold(boolean b){
		setBoolean(USE_COLOUR_THRESHOLD, b);
	}
	
	public void setHueThreshold(int min, int max){
		setInt(MIN_HUE, min);
		setInt(MAX_HUE, max);
	}
	
	public void setSaturationThreshold(int min, int max){
		setInt(MIN_SAT, min);
		setInt(MAX_SAT, max);
	}
	
	public void setBrightnessThreshold(int min, int max){
		setInt(MIN_BRI, min);
		setInt(MAX_BRI, max);
	}



	@Override
	public boolean isUseColourThreshold() {
		return getBoolean(USE_COLOUR_THRESHOLD);
	}



	@Override
	public int getMinHue() {
		return getInt(MIN_HUE);
	}



	@Override
	public int getMaxHue() {
		return getInt(MAX_HUE);
	}



	@Override
	public int getMinSaturation() {
		return getInt(MIN_SAT);
	}



	@Override
	public int getMaxSaturation() {
		return getInt(MAX_SAT);
	}



	@Override
	public int getMinBrightness() {
		return getInt(MIN_BRI);
	}



	@Override
	public int getMaxBrightness() {
		return getInt(MAX_BRI);
	}
	 

}
