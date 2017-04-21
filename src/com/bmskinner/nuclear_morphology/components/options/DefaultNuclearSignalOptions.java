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
import java.util.List;

/**
 * The default implementation of the nuclear signal options
 * interface
 * @author ben
 * @deprecated since 1.13.4
 *
 */
@Deprecated
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

	@Override
	public IMutableDetectionOptions unlock() {
		return this;
	}

	@Override
	public boolean isUseHoughTransform() {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
		return false;
	}

	@Override
	public IDetectionOptions lock() {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
		return null;
	}

	@Override
	public void setHoughOptions(IHoughDetectionOptions hough) {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
	}

	@Override
	public IDetectionSubOptions getSubOptions(String s) {
		warn("Unimplemented method");
		return null;
	}

	@Override
	public boolean isRGB() {
		warn("Unimplemented method");
		return false;
	}

	@Override
	public void setSubOptions(String s, IDetectionSubOptions sub) {
		warn("Unimplemented method");
	}

	@Override
	public void setRGB(boolean b) {	
		warn("Unimplemented method");
		}

	@Override
	public List<String> getKeys() {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
		return null;
	}

	@Override
	public boolean hasSubOptions(String key) {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
		return false;
	}

	@Override
	public double getDouble(String s) {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
		return 0;
	}

	@Override
	public int getInt(String s) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean getBoolean(String s) {
		warn("Unimplemented method");
		return false;
	}

	@Override
	public void setDouble(String s, double d) {
		warn("Unimplemented method");
		
	}

	@Override
	public void setInt(String s, int i) {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
	}

	@Override
	public void setBoolean(String s, boolean b) {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
	}

	@Override
	public float getFloat(String s) {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
		return 0;
	}

	@Override
	public void setFloat(String s, float f) {
		// TODO Auto-generated method stub
		warn("Unimplemented method");
		
	}

}
