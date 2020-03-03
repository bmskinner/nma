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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The abstract implementation of IDetectionOptions, which is extended
 * for all component types
 * 
 * @author bms41
 * @since 1.13.3
 * @deprecated since 1.14.0
 *
 */
@Deprecated
public abstract class AbstractDetectionOptions implements IDetectionOptions {
	
	private static final Logger LOGGER = Logger.getLogger(AbstractDetectionOptions.class.getName());

    private static final long serialVersionUID = 1L;

    private File folder;

    private int threshold, channel;

    private double minCirc, maxCirc, minSize, maxSize, scale;

    private boolean isNormaliseContrast;

    private ICannyOptions cannyOptions = null;

    /**
     * Construct specifying a folder of images to be analysed
     * 
     * @param folder
     */
    public AbstractDetectionOptions(File folder) {

        this.folder = folder;
    }

    /**
     * Construct from a template options
     * 
     * @param template
     */
    protected AbstractDetectionOptions(IDetectionOptions template) {
        if (template == null) {
            throw new IllegalArgumentException("Template options is null");
        }

        folder = template.getFolder();
        threshold = template.getThreshold();
        channel = template.getChannel();

        minCirc = template.getMinCirc();
        maxCirc = template.getMaxCirc();
        minSize = template.getMinSize();
        maxSize = template.getMaxSize();
        scale = template.getScale();

        isNormaliseContrast = template.isNormaliseContrast();

        if (template.hasCannyOptions()) {
            try {
                cannyOptions = template.getCannyOptions().duplicate();
            } catch (MissingOptionException e) {
                LOGGER.log(Loggable.STACK, "Missing Canny options", e);
            }
        } else {
            cannyOptions = OptionsFactory.makeCannyOptions();
            cannyOptions.setUseCanny(false);
        }

    }

    public AbstractDetectionOptions setSize(double min, double max) {
        this.minSize = min;
        this.maxSize = max;
        return this;
    }

    public AbstractDetectionOptions setCircularity(double min, double max) {
        this.minCirc = min;
        this.maxCirc = max;
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getThreshold()
     */
    @Override
    public int getThreshold() {
        return threshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setThreshold(int)
     */
    @Override
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMinCirc()
     */
    @Override
    public double getMinCirc() {
        return minCirc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMinCirc(double)
     */
    @Override
    public void setMinCirc(double minCirc) {
        this.minCirc = minCirc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMaxCirc()
     */
    @Override
    public double getMaxCirc() {
        return maxCirc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMaxCirc(double)
     */
    @Override
    public void setMaxCirc(double maxCirc) {
        this.maxCirc = maxCirc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMinSize()
     */
    @Override
    public double getMinSize() {
        return minSize;
    }

    /*
     * (non-Javadoc)
     * 
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
        return cannyOptions != null;
    }

    @Override
    public ICannyOptions getCannyOptions() {
        return cannyOptions;
    }

    @Override
    public boolean isValid(CellularComponent c) {

        if (c == null) {
            return false;
        }
        if (c.getStatistic(PlottableStatistic.AREA) < this.minSize) {
            return false;
        }
        if (c.getStatistic(PlottableStatistic.AREA) > this.maxSize) {
            return false;
        }
        if (c.getStatistic(PlottableStatistic.CIRCULARITY) < this.minCirc) {
            return false;
        }

        if (c.getStatistic(PlottableStatistic.CIRCULARITY) > this.maxCirc) {
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

    @Override
    public void setNormaliseContrast(boolean b) {
        this.isNormaliseContrast = b;
    }

    @Override
    public void set(IDetectionOptions options) {

        try {
            this.cannyOptions.set(options.getCannyOptions());
        } catch (MissingOptionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        channel = options.getChannel();
        maxCirc = options.getMaxCirc();
        minCirc = options.getMinCirc();
        maxSize = options.getMaxSize();
        minSize = options.getMinSize();
        threshold = options.getThreshold();
        scale = options.getScale();
        isNormaliseContrast = options.isNormaliseContrast();
        


        folder = new File(options.getFolder().getAbsolutePath());

    }

    @Override
    public int hashCode() {

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

        if (cannyOptions != null)
            result = prime * result + cannyOptions.hashCode();

        return result;

    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null)
            return false;

        if (!(o instanceof IDetectionOptions))
            return false;

        IDetectionOptions other = (IDetectionOptions) o;

        if (threshold != other.getThreshold())
            return false;

        if (channel != other.getChannel())
            return false;

        if (Double.doubleToLongBits(minCirc) != Double.doubleToLongBits(other.getMinCirc()))
            return false;

        if (Double.doubleToLongBits(maxCirc) != Double.doubleToLongBits(other.getMaxCirc()))
            return false;

        if (Double.doubleToLongBits(minSize) != Double.doubleToLongBits(other.getMinSize()))
            return false;

        if (Double.doubleToLongBits(maxSize) != Double.doubleToLongBits(other.getMaxSize()))
            return false;

        if (Double.doubleToLongBits(scale) != Double.doubleToLongBits(other.getScale()))
            return false;

        if (isNormaliseContrast != other.isNormaliseContrast())
            return false;

        try {
            if (!cannyOptions.equals(other.getCannyOptions())) {
                return false;
            }
        } catch (MissingOptionException e) {
            e.printStackTrace();
            return false;
        }

        return true;

    }

    @Override
    public List<String> getKeys() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Map<String, Object> getEntries(){
    	Map<String, Object> result = new HashMap<>();
    	return result;
    }

	@Override
	public Object getValue(String key) {
		return null;
	}

}
