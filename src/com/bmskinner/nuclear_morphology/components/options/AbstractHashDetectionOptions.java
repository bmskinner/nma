/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * A replacement for the AbstractDetectionOptions providing more extensibility
 * for the future by using a map rather than fixed fields for the stored options
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public abstract class AbstractHashDetectionOptions extends AbstractHashOptions implements IDetectionOptions {

    private static final long serialVersionUID = 1L;

    private File folder;

    private Map<String, IDetectionSubOptions> subMap = new HashMap<String, IDetectionSubOptions>();

    /**
     * Construct specifying a folder of images to be analysed
     * 
     * @param folder
     */
    public AbstractHashDetectionOptions(File folder) {

        this.folder = folder;
    }

    /**
     * Construct from a template options
     * 
     * @param template
     */
    protected AbstractHashDetectionOptions(IDetectionOptions template) {
        if (template == null) {
            throw new IllegalArgumentException("Template options is null");
        }

        folder = template.getFolder();
        intMap.put(THRESHOLD, template.getThreshold());
        intMap.put(CHANNEL, template.getChannel());

        dblMap.put(MIN_CIRC, template.getMinCirc());
        dblMap.put(MAX_CIRC, template.getMaxCirc());
        dblMap.put(MIN_SIZE, template.getMinSize());
        dblMap.put(MAX_SIZE, template.getMaxSize());
        dblMap.put(SCALE, template.getScale());

        boolMap.put(IS_NORMALISE_CONTRAST, template.isNormaliseContrast());
        boolMap.put(IS_RGB, template.isRGB());

        if (template.hasCannyOptions()) {
            try {
                subMap.put(IDetectionSubOptions.CANNY_OPTIONS, template.getCannyOptions().duplicate());
            } catch (MissingOptionException e) {
                error("Missing Canny options", e);
            }
        } else {

        	ICannyOptions cannyOptions = OptionsFactory.makeCannyOptions();
            cannyOptions.setUseCanny(false);
            subMap.put(IDetectionSubOptions.CANNY_OPTIONS, cannyOptions);
        }

    }

    public AbstractHashDetectionOptions setSize(double min, double max) {
        dblMap.put(MIN_SIZE, min);
        dblMap.put(MAX_SIZE, max);
        return this;
    }

    public AbstractHashDetectionOptions setCircularity(double min, double max) {
        dblMap.put(MIN_CIRC, min);
        dblMap.put(MAX_CIRC, max);
        return this;
    }

    @Override
    public boolean hasSubOptions(String s) {
        return subMap.containsKey(s);
    }

    @Override
    public IDetectionSubOptions getSubOptions(String s) throws MissingOptionException {
        if (subMap.containsKey(s)) {
            return subMap.get(s);
        } else {
            throw new MissingOptionException("Options not present: " + s);
        }
    }

    @Override
    public void setSubOptions(String s, IDetectionSubOptions op) {
        subMap.put(s, op);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getThreshold()
     */
    @Override
    public int getThreshold() {
        return intMap.get(THRESHOLD);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setThreshold(int)
     */
    @Override
    public void setThreshold(int threshold) {
        intMap.put(THRESHOLD, threshold);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMinCirc()
     */
    @Override
    public double getMinCirc() {
        return dblMap.get(MIN_CIRC);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMinCirc(double)
     */
    @Override
    public void setMinCirc(double minCirc) {
        dblMap.put(MIN_CIRC, minCirc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMaxCirc()
     */
    @Override
    public double getMaxCirc() {
        return dblMap.get(MAX_CIRC);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMaxCirc(double)
     */
    @Override
    public void setMaxCirc(double maxCirc) {
        dblMap.put(MAX_CIRC, maxCirc);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#getMinSize()
     */
    @Override
    public double getMinSize() {
        return dblMap.get(MIN_SIZE);
    }

    /*
     * (non-Javadoc)
     * 
     * @see analysis.signals.INuclearSignalOptions#setMinSize(double)
     */
    @Override
    public void setMinSize(double minSize) {
        dblMap.put(MIN_SIZE, minSize);
    }

    @Override
    public void setRGB(boolean b) {
        boolMap.put(IS_RGB, b);
    }

    @Override
    public File getFolder() {
        return folder;
    }

    @Override
    public double getMaxSize() {
        return dblMap.get(MAX_SIZE);
    }

    @Override
    public double getScale() {
        return dblMap.get(SCALE);
    }

    @Override
    public int getChannel() {
        return intMap.get(CHANNEL);
    }

    @Override
    public boolean isRGB() {
        return boolMap.get(IS_RGB);
    }

    @Override
    public boolean isNormaliseContrast() {
        return boolMap.get(IS_NORMALISE_CONTRAST);
    }

    @Override
    public boolean hasCannyOptions() {
        return subMap.containsKey(IDetectionSubOptions.CANNY_OPTIONS);
    }

    @Override
    public ICannyOptions getCannyOptions() throws MissingOptionException {

        if (this.hasCannyOptions()) {
            IDetectionSubOptions c = subMap.get(IDetectionSubOptions.CANNY_OPTIONS);
            if (c instanceof ICannyOptions) {
                return (ICannyOptions) c;
            } else {
                throw new MissingOptionException("Sub options cannot be cast to canny");
            }
        } else {
            throw new MissingOptionException("Canny options not present");
        }

    }

    @Override
    public boolean isValid(CellularComponent c) {

        if (c == null) {
            return false;
        }
        if (c.getStatistic(PlottableStatistic.AREA) < this.getMinSize()) {
            return false;
        }
        if (c.getStatistic(PlottableStatistic.AREA) > this.getMaxSize()) {
            return false;
        }
        if (c.getStatistic(PlottableStatistic.CIRCULARITY) < this.getMinCirc()) {
            return false;
        }

        if (c.getStatistic(PlottableStatistic.CIRCULARITY) > this.getMaxCirc()) {
            return false;
        }
        return true;

    }

    @Override
    public void setChannel(int channel) {
        intMap.put(CHANNEL, channel);
    }

    @Override
    public void setScale(double scale) {
        dblMap.put(SCALE, scale);
    }

    @Override
    public void setMaxSize(double maxSize) {
        dblMap.put(MAX_SIZE, maxSize);

    }

    @Override
    public void setFolder(File folder) {
        this.folder = folder;
    }

    @Override
    public void setCannyOptions(ICannyOptions canny) {
        subMap.put(IDetectionSubOptions.CANNY_OPTIONS, canny);
    }

    @Override
    public void setHoughOptions(IHoughDetectionOptions hough) {
        subMap.put(IDetectionSubOptions.HOUGH_OPTIONS, hough);
    }

    @Override
    public void setNormaliseContrast(boolean b) {
        boolMap.put(IS_NORMALISE_CONTRAST, b);
    }

    @Override
    public void set(IDetectionOptions options) {

        try {
            this.setCannyOptions(options.getCannyOptions());
        } catch (MissingOptionException e) {
            fine("No canny options to copy");
        }
        this.setChannel(options.getChannel());
        this.setMaxCirc(options.getMaxCirc());
        this.setMinCirc(options.getMinCirc());
        this.setMaxSize(options.getMaxSize());
        this.setMinSize(options.getMinSize());
        this.setThreshold(options.getThreshold());
        this.setScale(options.getScale());
        this.setNormaliseContrast(options.isNormaliseContrast());

        folder = new File(options.getFolder().getAbsolutePath());

    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = super.hashCode();

        result = prime * result + folder.hashCode();
        result = prime * result + intMap.hashCode();
        result = prime * result + dblMap.hashCode();
        result = prime * result + boolMap.hashCode();
        result = prime * result + subMap.hashCode();
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

        if (getThreshold() != other.getThreshold())
            return false;

        if (getChannel() != other.getChannel())
            return false;

        if (Double.doubleToLongBits(getMinCirc()) != Double.doubleToLongBits(other.getMinCirc()))
            return false;

        if (Double.doubleToLongBits(getMaxCirc()) != Double.doubleToLongBits(other.getMaxCirc()))
            return false;

        if (Double.doubleToLongBits(getMinSize()) != Double.doubleToLongBits(other.getMinSize()))
            return false;

        if (Double.doubleToLongBits(getMaxSize()) != Double.doubleToLongBits(other.getMaxSize()))
            return false;

        if (Double.doubleToLongBits(getScale()) != Double.doubleToLongBits(other.getScale()))
            return false;

        if (isNormaliseContrast() != other.isNormaliseContrast())
            return false;

        try {
            if (!getCannyOptions().equals(other.getCannyOptions())) {
                finer("Inequality in canny options");
                finer("This Canny options class is: "+getCannyOptions().getClass().getName());
                finer("The compared Canny options class is: "+other.getCannyOptions().getClass().getName());
                return false;
            }
        } catch (MissingOptionException e) {
            error("Canny options missing in comparison", e);
            return false;
        }

        return true;

    }

//    @Override
//    public IMutableDetectionOptions unlock() {
//        return this;
//    }

    @Override
    public boolean isUseHoughTransform() {
        return boolMap.get(IS_USE_HOUGH);
    }

//    @Override
//    public IDetectionOptions lock() {
//        return this;
//    }

}
