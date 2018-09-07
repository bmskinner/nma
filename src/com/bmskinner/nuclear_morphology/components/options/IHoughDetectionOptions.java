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

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

/**
 * Describes the methods available for detecting circles within images using the
 * Hough algorithm.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public interface IHoughDetectionOptions extends IDetectionSubOptions {

    public static final String MIN_RADIUS      = "Min radius";
    public static final String MAX_RADIUS      = "Max radius";
    public static final String NUM_CIRCLES     = "Number of circles";
    public static final String HOUGH_THRESHOLD = "Hough threshold";


    /**
     * Get the minimum radius circle detected
     * 
     * @return
     */
    int getMinRadius();

    /**
     * Get the maximum radius circle detected
     * 
     * @return
     */
    int getMaxRadius();

    /**
     * Get the number of circles to be detected
     * 
     * @return
     */
    int getNumberOfCircles();

    /**
     * Get the Hough space threshold for circle detection, if set, or -1 if not
     * set.
     * 
     * @return
     */
    int getHoughThreshold();
    
    void setMinRadius(int d);

    void setMaxRadius(int d);

    /**
     * Set the maximum number of circles to return. This will have no effect
     * if the hough threshold is greater than -1.
     * 
     * @param i
     */
    void setNumberOfCircles(int i);

    /**
     * Set the Hough space threshold for circle detection. Alternative to
     * setting the max number of circles.
     * 
     * @param i
     *            the threshold from 0-255. Set to -1 to disable and use max
     *            circles
     */
    void setHoughThreshold(int i);

}
