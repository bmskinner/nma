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

package components;

import ij.process.ImageProcessor;
import io.UnloadableImageException;

import java.awt.Rectangle;
import java.io.File;
import java.util.List;

import components.generic.IPoint;

/**
 * This covers the things than can be found within an image.
 * @author bms41
 *
 */
public interface Imageable {
	
	/*
	 * GETTERS
	 */
	
	/**
	 * The array index of the left-most x coordinate of the object
	 * in {@link #setPosition(double[])} and {@link #getPosition(double[])}
	 */
	public static final int X_BASE 	= 0;
	
	/**
	 * The array index of the top-most (lowest) y coordinate of the object
	 * in {@link #setPosition(double[])} and {@link #getPosition(double[])}
	 */
	public static final int Y_BASE 	= 1;
	
	/**
	 * The array index of the width of the object
	 * in {@link #setPosition(double[])} and {@link #getPosition(double[])}
	 */
	public static final int WIDTH 	= 2;
	
	/**
	 * The array index of the height of the object
	 * in {@link #setPosition(double[])} and {@link #getPosition(double[])}
	 */
	public static final int HEIGHT 	= 3;
	
	
	/**
	 * Get the position of the object in the 
	 * original image. The indexes in the array are
	 * {@link #X_BASE} of the bounding box,
	 * {@link #Y_BASE} of the bounding box,
	 * {@link #WIDTH} of the bounding box and
	 * {@link #HEIGHT} of the bounding box
	 * @return the array with the position 
	 */
	public int[] getPosition();
			
	/**
	 * Get the RGB channel the object was detected in
	 * @return
	 */
	public int getChannel();
	
	/**
	 * Get the image from which the component was detected. Opens
	 * the image via the ImageImporter, fetches the appropriate
	 * channel and inverts it
	 * @return
	 * @throws UnloadableImageException if the image can't be loaded
	 */
	public ImageProcessor getImage() throws UnloadableImageException;
	
	/**
	 * Get the image from which the component was detected, and crops
	 * it to only the region containing the component
	 * @return
	 * @throws UnloadableImageException if the image can't be loaded
	 */
	public ImageProcessor getComponentImage() throws UnloadableImageException;

	
	
	/**
	 * Get the pixels within this object as a list of points
	 * @return
	 */
	public List<IPoint> getPixelsAsPoints();
	
	/**
	 * Get the bounding rectangle for the object.
	 * @return
	 */
	public Rectangle getBounds();
	
	/**
	 * Get the folder of the image the component was found in.
	 *  e.g. C:\Folder\ImageFolder\
	 * will return ImageFolder
	 * @return
	 */
	public File getSourceFolder();
	
	/**
	 * Get the folder of the image the component was found in.
	 *  e.g. C:\Folder\ImageFolder\1.tiff
	 * will return ImageFolder
	 * @return
	 */
	public File getSourceFile();

	/**
	 * Get the name of the image the component was found in
	 * @return
	 */
	public String getSourceFileName();
	
	
	/**
	 * Get the name of the source file with the '.ext' removed
	 * @return
	 */
	public String getSourceFileNameWithoutExtension();
	
	
	/*
	 * SETTERS
	 */
	
	/**
	 * Set the image file the component was found in
	 * @param sourceFile
	 */
	public void setSourceFile(File sourceFile);
	
	/**
	 * Set the RGB channel the component was detected in
	 * @param channel
	 */
	public void setChannel(int channel);
		
	/**
	 * Set the folder the source image file belongs to
	 * @param sourceFolder
	 */
	public void setSourceFolder(File sourceFolder);
	

}
