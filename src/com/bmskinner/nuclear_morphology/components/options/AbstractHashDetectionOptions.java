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
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;

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

    protected Map<String, IDetectionSubOptions> subMap = new HashMap<>();

    /**
     * Construct specifying a folder of images to be analysed
     * 
     * @param folder
     */
    public AbstractHashDetectionOptions(@NonNull File folder) {
        this.folder = folder;
    }

    /**
     * Construct from a template options
     * 
     * @param template
     */
    protected AbstractHashDetectionOptions(@NonNull IDetectionOptions template) {
        folder = template.getFolder();
        set(template);
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
    public Set<String> getSubOptionKeys(){
    	return subMap.keySet();
    }

    @Override
    public boolean hasSubOptions(String s) {
        return subMap.containsKey(s);
    }

    @Override
    public IDetectionSubOptions getSubOptions(String s) throws MissingOptionException {
        if (subMap.containsKey(s)) {
            return subMap.get(s);
        }
		throw new MissingOptionException("Options not present: " + s);
    }

    @Override
    public void setSubOptions(String s, IDetectionSubOptions op) {
        subMap.put(s, op);
    }

    @Override
    public int getThreshold() {
        return intMap.get(THRESHOLD);
    }

    @Override
    public void setThreshold(int threshold) {
        intMap.put(THRESHOLD, threshold);
    }

    @Override
    public double getMinCirc() {
        return dblMap.get(MIN_CIRC);
    }

    @Override
    public void setMinCirc(double minCirc) {
        dblMap.put(MIN_CIRC, minCirc);
    }

    @Override
    public double getMaxCirc() {
        return dblMap.get(MAX_CIRC);
    }

    @Override
    public void setMaxCirc(double maxCirc) {
        dblMap.put(MAX_CIRC, maxCirc);
    }

    @Override
    public double getMinSize() {
        return dblMap.get(MIN_SIZE);
    }

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
            if (c instanceof ICannyOptions)
                return (ICannyOptions) c;
            throw new MissingOptionException("Sub options cannot be cast to canny");
        }
        throw new MissingOptionException("Canny options not present");
    }

    @Override
    public boolean isValid(@NonNull CellularComponent c) {

        if (c == null)
            return false;
        if (c.getStatistic(PlottableStatistic.AREA) < this.getMinSize())
            return false;
        if (c.getStatistic(PlottableStatistic.AREA) > this.getMaxSize())
            return false;
        if (c.getStatistic(PlottableStatistic.CIRCULARITY) < this.getMinCirc())
            return false;
        if (c.getStatistic(PlottableStatistic.CIRCULARITY) > this.getMaxCirc())
            return false;
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
    public void setHoughOptions(@NonNull IHoughDetectionOptions hough) {
        subMap.put(IDetectionSubOptions.HOUGH_OPTIONS, hough);
    }

    @Override
    public void setNormaliseContrast(boolean b) {
        boolMap.put(IS_NORMALISE_CONTRAST, b);
    }
    
    @Override
    public boolean isUseHoughTransform() {
        return boolMap.get(IS_USE_HOUGH);
    }

    @Override
    public void set(@NonNull IDetectionOptions template) {
    	super.set(template);
    	folder = new File(template.getFolder().getAbsolutePath());
    	try {
        	for(String subKey : template.getSubOptionKeys())
        		subMap.put(subKey, template.getSubOptions(subKey).duplicate());
        } catch (MissingOptionException e) {
        	error("Missing sub options", e);
        }
    }

    @Override
    public int hashCode() {

        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + folder.hashCode();
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
        
        if(!super.equals(o))
        	return false;

        IDetectionOptions other = (IDetectionOptions) o;
        
        try {
        	for(String subKey : other.getSubOptionKeys()) {
        		if(!subMap.containsKey(subKey))
        			return false;
        		if(!other.getSubOptions(subKey).equals(getSubOptions(subKey)))
        			return false;
        	}
        	for(String subKey : getSubOptionKeys()) {
        		if(!other.hasSubOptions(subKey))
        			return false;
        	}
        	
        } catch (MissingOptionException e) {
        	return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append("\t" + folder.getAbsolutePath() + IDetectionOptions.NEWLINE);
        for (String k : subMap.keySet()) {
            sb.append("\tSub options: " + k + ":"+IDetectionOptions.NEWLINE);
            sb.append(subMap.get(k).toString());
            sb.append(IDetectionOptions.NEWLINE);
        }
        return sb.toString();

    }
}
