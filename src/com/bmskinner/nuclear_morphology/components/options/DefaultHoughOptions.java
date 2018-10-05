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

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

/**
 * The default implementation of the IHoughDetectionOptions interface.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultHoughOptions extends AbstractHashOptions implements IHoughDetectionOptions {

    private static final long serialVersionUID        = 1L;
    public static final int   DEFAULT_MIN_RADIUS      = 5;
    public static final int   DEFAULT_MAX_RADIUS      = 30;
    public static final int   DEFAULT_NUM_CIRCLES     = 0;
    public static final int   DEFAULT_HOUGH_THRESHOLD = 20;

    /**
     * Construct with default options
     */
    public DefaultHoughOptions() {
        this.setMinRadius(DEFAULT_MIN_RADIUS);
        this.setMaxRadius(DEFAULT_MAX_RADIUS);
        this.setNumberOfCircles(DEFAULT_NUM_CIRCLES);
        this.setHoughThreshold(DEFAULT_HOUGH_THRESHOLD);
    }

    /**
     * Construct using a template
     * 
     * @param template
     */
    public DefaultHoughOptions(IHoughDetectionOptions template) {
    	set(template);
    }
    
	@Override
	public IDetectionSubOptions duplicate() {
		return new DefaultHoughOptions(this);
	}

    @Override
    public int getMinRadius() {
        return getInt(MIN_RADIUS);
    }

    @Override
	public void setMinRadius(int i) {
        setInt(MIN_RADIUS, i);
    }

    @Override
    public int getMaxRadius() {
        return getInt(MAX_RADIUS);
    }

    @Override
	public void setMaxRadius(int i) {
        setInt(MAX_RADIUS, i);
    }

    @Override
    public int getNumberOfCircles() {
        return getInt(NUM_CIRCLES);
    }

    @Override
	public void setNumberOfCircles(int i) {
        setInt(NUM_CIRCLES, i);
    }

    @Override
    public int getHoughThreshold() {
        return getInt(HOUGH_THRESHOLD);

    }

    @Override
    public void setHoughThreshold(int i) {
        setInt(HOUGH_THRESHOLD, i);

    }

    @Override
	public String toString() {
        return "Hough options:" + IDetectionOptions.NEWLINE + super.toString();
    }



}
