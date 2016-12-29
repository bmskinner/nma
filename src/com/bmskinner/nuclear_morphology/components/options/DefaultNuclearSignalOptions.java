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

import java.io.File;

/**
 * The default implementation of the nuclear signal options
 * interface
 * @author ben
 *
 */
public class DefaultNuclearSignalOptions 
	extends AbstractDetectionOptions 
	implements IMutableNuclearSignalOptions {
	 
	
	private static final long serialVersionUID = 1L;
	
	private double maxFraction;
	private SignalDetectionMode mode;
	
	public DefaultNuclearSignalOptions(File folder){
		super(folder);
		maxFraction = DEFAULT_MAX_SIGNAL_FRACTION;
		mode        = DEFAULT_METHOD;
		
		setMinSize(DEFAULT_MIN_SIGNAL_SIZE);
		setMaxSize(DEFAULT_MAX_SIGNAL_SIZE);
		setMinCirc(INuclearSignalOptions.DEFAULT_MIN_CIRC);
		setMaxCirc(INuclearSignalOptions.DEFAULT_MAX_CIRC);
		setChannel(DEFAULT_CHANNEL);
		setThreshold(DEFAULT_SIGNAL_THRESHOLD);
		setScale(DEFAULT_SCALE);
		
	}
	
	/**
	 * Construct from an existing nuclear options
	 * @param template
	 */
	public DefaultNuclearSignalOptions(INuclearSignalOptions template){
		super(template);
		this.maxFraction = template.getMaxFraction();
		this.mode        = template.getDetectionMode();
		
	}
	
	public DefaultNuclearSignalOptions setSize(double min, double max){
		super.setSize(min, max);
		return this;
	}
	
	public DefaultNuclearSignalOptions setCircularity(double min, double max){
		super.setCircularity(min, max);
		return this;
	}
	

	@Override
	public double getMaxFraction() {
		return maxFraction;
	}

	@Override
	public SignalDetectionMode getDetectionMode() {
		return mode;
	}

	@Override
	public void setMaxFraction(double maxFraction) {
		this.maxFraction = maxFraction;
		
	}

	@Override
	public void setDetectionMode(SignalDetectionMode detectionMode) {
		mode = detectionMode;
		
	}

	@Override
	public IMutableDetectionOptions duplicate() {
		return new DefaultNuclearSignalOptions(this) ;
	}
	
	@Override
	public int hashCode(){
		final int prime = 31;
		int result = super.hashCode();
		
		long temp = Double.doubleToLongBits(maxFraction);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + mode.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object o){
		
		if(! super.equals(o))
			return false;
		
		if( ! (o instanceof INuclearSignalOptions))
			return false;
		
		INuclearSignalOptions other = (INuclearSignalOptions) o;
		
		if(Double.doubleToLongBits(maxFraction) != 
				Double.doubleToLongBits(other.getMaxFraction()))
			return false;
		
		if(mode != other.getDetectionMode())
			return false;
		
		return true;
		
	}

}
