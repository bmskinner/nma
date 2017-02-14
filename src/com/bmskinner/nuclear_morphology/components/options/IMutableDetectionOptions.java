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

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

/**
 * Adds setters to the IDetectionOptions interface
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IMutableDetectionOptions extends IDetectionOptions {
	
	IMutableDetectionOptions duplicate();
	
	/**
	 * Lock the options from editing
	 * @return
	 */
	IDetectionOptions lock();
	
	/**
	 * Set the RGB channel to detect the object in
	 * @param channel
	 */
	void setChannel(int channel);

	/**
	 * Set the scale of the object in pixels per micron
	 * @param scale
	 */
	void setScale(double scale);

	/**
	 * Set the detection threshold
	 * @param nucleusThreshold
	 */
	void setThreshold(int nucleusThreshold);

	/*
	 * Set the minimum size of the object
	 * @param minNucleusSize
	 */
	void setMinSize(double minNucleusSize);

	/*
	 * Set the maximum size of the object
	 * @param minNucleusSize
	 */
	void setMaxSize(double maxNucleusSize);

	/*
	 * Set the minimum circularity of the object
	 * @param minNucleusSize
	 */
	void setMinCirc(double minNucleusCirc);

	/*
	 * Set the maximum circularity of the object
	 * @param minNucleusSize
	 */
	void setMaxCirc(double maxNucleusCirc);
	
	
	/**
	 * Set the folder of images where the objects are to be detected
	 * @param folder
	 */
	void setFolder(File folder);
	
	/**
	 * Set Canny edge detection options to override thresholds
	 * @param canny
	 */
	void setCannyOptions(IMutableCannyOptions canny);
	
	void setNormaliseContrast(boolean b);
	
	/**
	 * Set this options to match the parameters in the given option
	 * @param options
	 */
	void set(IDetectionOptions options);

	/**
	 * Set the hough options
	 * @param hough
	 */
	void setHoughOptions(IHoughDetectionOptions hough);

	
	/**
	 * Set arbitrary sub options
	 * @param s the options key
	 * @param sub
	 */
	void setSubOptions(String s, IDetectionSubOptions sub);

	/**
	 * Set if the image is RGB or greyscale
	 * @param b
	 */
	void setRGB(boolean b);

}
