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

import java.awt.Rectangle;
import java.io.File;
import java.util.List;

import components.generic.XYPoint;

/**
 * This covers the things than can be found within an image
 * @author bms41
 *
 */
public interface Imageable {
	
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
	public double[] getPosition();
	
	/**
	 * Get the image file the component was found in
	 * @return
	 */
	public File getSourceFile();
	
	/**
	 * Set the image file the component was found in
	 * @param sourceFile
	 */
	public void setSourceFile(File sourceFile);
		
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
	 */
	public ImageProcessor getImage();
	
	/**
	 * Get the image from which the component was detected, and crops
	 * it to only the region containing the component
	 * @return
	 */
	public ImageProcessor getComponentImage();

	/**
	 * Set the RGB channel the component was detected in
	 * @param channel
	 */
	public void setChannel(int channel);
	
	/**
	 * Get the pixels within this object as a list of points
	 * @return
	 */
	public List<XYPoint> getPixelsAsPoints();
	
	/**
	 * Get the bounding rectangle for the object.
	 * @return
	 */
	public Rectangle getBounds();
	
	/**
	 * Get the folder of the image the component was found in.
	 *  e.g. C:\Folder\ImageFolder\1.tiff
	 * will return ImageFolder
	 * @return
	 */
	public File getSourceFolder();

	/**
	 * Get the name of the image the component was found in
	 * @return
	 */
	public String getSourceFileName();
	
	/**
	 * Set the position of the component in the original
	 * image.
	 * @param d the array of positions
	 * @see CellularComponent#getPosition()
	 */
	public void setPosition(double[] position);
	
	/**
	 * Set the bounding rectangle for this object
	 * @param boundingRectangle
	 */
	public void setBoundingRectangle(Rectangle boundingRectangle);


	/**
	 * Set the name of the image file this object was found in
	 * @param name
	 */
	public void setSourceFileName(String name);
	
	/**
	 * Set the folder the source image file belongs to
	 * @param sourceFolder
	 */
	public void setSourceFolder(File sourceFolder);

}
