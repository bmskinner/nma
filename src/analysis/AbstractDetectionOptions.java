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

package analysis;

import java.io.File;

import analysis.nucleus.DefaultNucleusDetectionOptions;
import stats.NucleusStatistic;
import components.CellularComponent;

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
	
	private ICannyOptions cannyOptions;
	
	
	public AbstractDetectionOptions(File folder){
		this.folder = folder;
	}
	
	protected AbstractDetectionOptions(AbstractDetectionOptions template){
		this(template.getFolder());
		
		threshold = template.getThreshold();
		channel   = template.getChannel();
		
		minCirc = template.getMinCirc();
		maxCirc = template.getMaxCirc();
		minSize = template.getMinSize();
		maxSize = template.getMaxSize();
		scale   = template.getScale();
		
		isNormaliseContrast = template.isNormaliseContrast;
		
		cannyOptions = template.getCannyOptions().duplicate();

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
	public ICannyOptions getCannyOptions() {
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
	public void setCannyOptions(ICannyOptions canny) {
		this.cannyOptions = canny;		
	}

}
