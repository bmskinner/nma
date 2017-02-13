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
import java.io.Serializable;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This interface defines the detection options available for detecting
 * an object in an image.
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IDetectionOptions extends Serializable, Loggable {
	
	/**
	 * All sub option classes implement this interface.
	 * They must be cast appropriately later.
	 * @author ben
	 * @since 1.13.4
	 *
	 */
	public interface IDetectionSubOptions extends Serializable, Loggable {
		public static final String CANNY_OPTIONS      = "Canny";
		public static final String HOUGH_OPTIONS      = "Hough";
		public static final String BACKGROUND_OPTIONS = "Background";
		
	}
	
	static final double DEFAULT_SCALE = 1;
	static final double DEFAULT_MIN_CIRC = 0;
	static final double DEFAULT_MAX_CIRC = 1;
	
	/**
	 * Unlock the options to allow modification
	 * @return
	 */
	IMutableDetectionOptions unlock();
	
	/**
	 * Create a copy of these options
	 * @return
	 */
	IDetectionOptions duplicate();
	
	/**
	 * Get the folder to be analysed
	 * @return
	 */
	File getFolder();

	/**
	 * Get the threshold for detecting object when not using edge detection
	 * @return the pixel intensity threshold above which a pixel may be part of a nucleus
	 */
	int getThreshold();

	/**
	 * Get the minimum size of the object in pixels
	 * @return the minimum size
	 */
	double getMinSize();

	/**
	 * Get the maximum of the object in pixels
	 * @return the maximum size
	 */
	double getMaxSize();

	/**
	 * Get the minimum circularity of the object
	 * @return the minimum circularity
	 */
	double getMinCirc();

	/**
	 * Get the maximum circularity of the object
	 * @return the maximum circularity
	 */
	double getMaxCirc();
	
	/**
	 * Get the scale of the objects detected - the number of pixels per micon
	 * @return the scale
	 */
	double getScale();

	/**
	 * Get the channel the objects are being detected in
	 * @return
	 */
	int getChannel();
	
	/**
	 * Test if constrasts should be normalised when detecting this object
	 * @return
	 */
	boolean isNormaliseContrast();
	
	/**
	 * Test if canny edge detection options have been set for this component
	 * @return true if Canny options have been set, false otherwise
	 */
	boolean hasCannyOptions();
	
	/**
	 * Should a Hough circle detection be run?
	 * @return
	 */
	boolean isUseHoughTransform();
	
	
	/**
	 * Get the sub options specified by the given key
	 * @param s
	 * @return
	 */
	IDetectionSubOptions getSubOptions(String s) throws MissingOptionException;
	
	/**
	 * Get the Canny edge detection options for this object. 
	 * @return the Canny options, or null if none have been set
	 */
	IMutableCannyOptions getCannyOptions() throws MissingOptionException;
	
	/**
	 * Test if the given component meets the criteria within
	 * these options. Note that if Canny options are present, they take
	 * precedence over thresholding options.
	 * @param c the component to test
	 * @return true if the component would be detected with these options, false otherwise
	 */
	boolean isValid(CellularComponent c);

}
