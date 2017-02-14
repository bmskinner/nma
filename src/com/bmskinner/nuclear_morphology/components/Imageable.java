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

package com.bmskinner.nuclear_morphology.components;

import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

/**
 * This covers the things than can be found within an image.
 * @author bms41
 * @since 1.13.3
 *
 */
public interface Imageable {

	/**
	 * The array index of the left-most x coordinate of the object
	 * in {@link #setPosition(double[])} and {@link #getPosition(double[])}
	 */
	static final int X_BASE 	= 0;
	
	/**
	 * The array index of the top-most (lowest) y coordinate of the object
	 * in {@link #setPosition(double[])} and {@link #getPosition(double[])}
	 */
	static final int Y_BASE 	= 1;
	
	/**
	 * The array index of the width of the object
	 * in {@link #setPosition(double[])} and {@link #getPosition(double[])}
	 */
	static final int WIDTH 	= 2;
	
	/**
	 * The array index of the height of the object
	 * in {@link #setPosition(double[])} and {@link #getPosition(double[])}
	 */
	static final int HEIGHT 	= 3;
	
	
	/**
	 * Get the position of the object in the 
	 * original image. The indexes in the array are
	 * {@link #X_BASE} of the bounding box,
	 * {@link #Y_BASE} of the bounding box,
	 * {@link #WIDTH} of the bounding box and
	 * {@link #HEIGHT} of the bounding box
	 * @return the array with the position 
	 */
	int[] getPosition();
			
	/**
	 * Get the RGB channel the object was detected in
	 * @return the channel
	 */
	int getChannel();
	
	/**
	 * Get the image from which the component was detected. Opens
	 * the image via the {@link com.bmskinner.nuclear_morphology.io.ImageImporter}, fetches the appropriate
	 * channel and inverts it. The complete image is returned; no cropping is performed
	 * @return an ImageJ image processor
	 * @throws UnloadableImageException if the image can't be loaded
	 */
	ImageProcessor getImage() throws UnloadableImageException;
	
	
	/**
	 * Get the image from which the component was detected. Opens
	 * the image via the {@link com.bmskinner.nuclear_morphology.io.ImageImporter}.
	 * The complete image is returned; no cropping is performed.
	 * Use when an RGB image needs to be displayed, such as H&E
	 * @return
	 * @throws UnloadableImageException
	 */
	ImageProcessor getRGBImage() throws UnloadableImageException;
	
	/**
	 * Get the image from which the component was detected, and crops
	 * it to only the region containing the component
	 * @return an ImageJ image processor cropped to size
	 * @throws UnloadableImageException if the image can't be loaded
	 */
	ImageProcessor getComponentImage() throws UnloadableImageException;

	
	
	/**
	 * Get the pixels within this object as a list of points
	 * @return a list of points
	 */
	List<IPoint> getPixelsAsPoints();
	
	/**
	 * Get the bounding rectangle for the object.
	 * @return the bounding rectangle
	 */
	Rectangle getBounds();
	
	/**
	 * Get the folder of the image the component was found in.
	 *  e.g. C:\Folder\ImageFolder\
	 * will return ImageFolder
	 * @return the image folder name
	 */
	File getSourceFolder();
	
	/**
	 * Get the folder of the image the component was found in.
	 *  e.g. C:\Folder\ImageFolder\1.tiff
	 * will return 1.tiff
	 * @return the file name
	 */
	File getSourceFile();

	/**
	 * Get the name of the image the component was found in
	 * @return the file name
	 */
	String getSourceFileName();
	
	
	/**
	 * Get the name of the source file with the '.ext' removed
	 * @return a file name without extension
	 */
	String getSourceFileNameWithoutExtension();
	
	
	/**
	 * Set the image file the component was found in
	 * @param sourceFile the file
	 */
	void setSourceFile(File sourceFile);
	
	/**
	 * Set the RGB channel the component was detected in
	 * @param channel the channel
	 */
	void setChannel(int channel);
		
	/**
	 * Set the folder the source image file belongs to
	 * @param sourceFolder the folder
	 */
	void setSourceFolder(File sourceFolder);
	

}
