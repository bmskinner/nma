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

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;

/**
 * The abstract implementation of IMutableDetectionOptions, which is extended
 * for all component types
 * @author bms41
 * @since 1.13.3
 *
 */
public abstract class AbstractDetectionOptions implements IMutableDetectionOptions {

	private static final long serialVersionUID = 1L;

	private File folder;
	
	private int threshold, channel;
	
	private double minCirc, maxCirc, minSize, maxSize, scale;
	
	private boolean isNormaliseContrast;
	
	private IMutableCannyOptions cannyOptions = null;
	
	
	/**
	 * Construct specifying a folder of images to be analysed
	 * @param folder
	 */
	public AbstractDetectionOptions(File folder){
		
		this.folder = folder;
	}
	
	/**
	 * Construct from a template options
	 * @param template
	 */
	protected AbstractDetectionOptions(IDetectionOptions template){
		if(template==null){
			throw new IllegalArgumentException("Template options is null");
		}
		
		folder    = template.getFolder();
		threshold = template.getThreshold();
		channel   = template.getChannel();
		
		minCirc = template.getMinCirc();
		maxCirc = template.getMaxCirc();
		minSize = template.getMinSize();
		maxSize = template.getMaxSize();
		scale   = template.getScale();
		
		isNormaliseContrast = template.isNormaliseContrast();
		
		if(template.hasCannyOptions()){
			cannyOptions = template.getCannyOptions().duplicate();
		} else {
			cannyOptions = new DefaultCannyOptions();
			cannyOptions.setUseCanny(false);
		}
		

	}
	
	public AbstractDetectionOptions setSize(double min, double max){
		this.minSize = min;
		this.maxSize = max;
		return this;
	}
	
	public AbstractDetectionOptions setCircularity(double min, double max){
		this.minCirc = min;
		this.maxCirc = max;
		return this;
	}
		
	
	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getThreshold()
	 */
	@Override
	public int getThreshold() {
		return threshold;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setThreshold(int)
	 */
	@Override
	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMinCirc()
	 */
	@Override
	public double getMinCirc() {
		return minCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMinCirc(double)
	 */
	@Override
	public void setMinCirc(double minCirc) {
		this.minCirc = minCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMaxCirc()
	 */
	@Override
	public double getMaxCirc() {
		return maxCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMaxCirc(double)
	 */
	@Override
	public void setMaxCirc(double maxCirc) {
		this.maxCirc = maxCirc;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#getMinSize()
	 */
	@Override
	public double getMinSize() {
		return minSize;
	}

	/* (non-Javadoc)
	 * @see analysis.signals.INuclearSignalOptions#setMinSize(double)
	 */
	@Override
	public void setMinSize(double minSize) {
		this.minSize = minSize;
	}

	@Override
	public File getFolder() {
		return folder;
	}

	@Override
	public double getMaxSize() {
		return maxSize;
	}

	@Override
	public double getScale() {
		return scale;
	}

	@Override
	public int getChannel() {
		return channel;
	}

	@Override
	public boolean isNormaliseContrast() {
		return isNormaliseContrast;
	}

	@Override
	public boolean hasCannyOptions() {
		return cannyOptions!=null;
	}

	@Override
	public IMutableCannyOptions getCannyOptions() {
		return cannyOptions;
	}

	@Override
	public boolean isValid(CellularComponent c){
		
		if(c==null){
			return false;
		}
		if(c.getStatistic(NucleusStatistic.AREA) < this.minSize){
			return false;
		}
		if(c.getStatistic(NucleusStatistic.AREA) > this.maxSize){
			return false;
		}
		if(c.getStatistic(NucleusStatistic.CIRCULARITY) < this.minCirc){
			return false;
		}
		
		if(c.getStatistic(NucleusStatistic.CIRCULARITY) > this.maxCirc){
			return false;
		}
		return true;
		
	}

	@Override
	public void setChannel(int channel) {
		this.channel = channel;
	}

	@Override
	public void setScale(double scale) {
		this.scale = scale;
	}

	@Override
	public void setMaxSize(double maxSize) {
		this.maxSize = maxSize;
		
	}

	@Override
	public void setFolder(File folder) {
		this.folder = folder;
	}

	@Override
	public void setCannyOptions(IMutableCannyOptions canny) {
		this.cannyOptions = canny;		
	}
	
	@Override
	public void setNormaliseContrast(boolean b) {
		this.isNormaliseContrast = b;
	}
	
	@Override
	public int hashCode(){
		
		final int prime = 31;
		int result = super.hashCode();
		
		result = prime * result + folder.hashCode();
		result = prime * result + threshold;
		result = prime * result + channel;
		
		long temp = Double.doubleToLongBits(minCirc);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		temp = Double.doubleToLongBits(maxCirc);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		temp = Double.doubleToLongBits(minSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		temp = Double.doubleToLongBits(maxSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		temp = Double.doubleToLongBits(scale);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		result = prime * result + (isNormaliseContrast ? 1231 : 1237);
		
		if(cannyOptions!=null)
			result = prime * result + cannyOptions.hashCode();
		
		return result;
		
	}
	
	@Override
	public boolean equals(Object o){
		if (this == o)
			return true;
		
		if(o==null)
			return false;
	
		if( ! ( o instanceof IDetectionOptions))
			return false;
		
		
		IDetectionOptions other = (IDetectionOptions) o;
		
//		if(! folder.equals(other.getFolder()))
//			return false;
		
		if(threshold!=other.getThreshold())
			return false;
		
		if(channel!=other.getChannel())
			return false;
		
		if( Double.doubleToLongBits(minCirc)!= 
				Double.doubleToLongBits(other.getMinCirc()))
			return false;
		
		if( Double.doubleToLongBits(maxCirc)
				!=Double.doubleToLongBits(other.getMaxCirc()))
			return false;

		if(Double.doubleToLongBits(minSize)!=
				Double.doubleToLongBits(other.getMinSize()))
			return false;
		
		if(Double.doubleToLongBits(maxSize)	!=
				Double.doubleToLongBits(other.getMaxSize()))
			return false;
		
		if(Double.doubleToLongBits(scale)!=
				Double.doubleToLongBits(other.getScale()))
			return false;
		
		if(isNormaliseContrast!=other.isNormaliseContrast())
			return false;
		
		if(! cannyOptions.equals(other.getCannyOptions())){
			return false;
		}
				
		return true;
		
	}

}
