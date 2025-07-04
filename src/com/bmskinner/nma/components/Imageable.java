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
package com.bmskinner.nma.components;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.generic.IPoint;

/**
 * This covers the things than can be found within an image.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface Imageable {

	/**
	 * The pixel border added to the edges of the component when fetching and
	 * cropping its source image. This prevents the component nestling right up to
	 * the edges of the resulting cropped image
	 */
	public static final int COMPONENT_BUFFER = 10;

	/**
	 * The left-most x coordinate of the object
	 */
	int getXBase();

	/**
	 * The top-most (lowest) y coordinate of the object
	 */
	int getYBase();

	/**
	 * The width of the object
	 * 
	 * @return
	 */
	double getWidth();

	/**
	 * The height of the object
	 * 
	 * @return
	 */
	double getHeight();

	/**
	 * Get the base X and Y position of the component in the original image.
	 * Corresponds to the points in {@link #getPosition()} {@link #X_BASE} and
	 * {@link #Y_BASE}
	 * 
	 * @return the base coordinate in the original image
	 */
	IPoint getOriginalBase();

	/**
	 * Get the base X and Y position of the component in the image. Corresponds to
	 * the minimum X and Y points in the border
	 * 
	 * @return the base XY coordinate in the image
	 */
	IPoint getBase();

	/**
	 * Get the RGB channel the object was detected in
	 * 
	 * @return the channel
	 */
	int getChannel();

	/**
	 * Get the folder of the image the component was found in. e.g.
	 * C:\Folder\ImageFolder\ will return ImageFolder
	 * 
	 * @return the image folder name
	 */
	File getSourceFolder();

	/**
	 * Get the file the component was found in.
	 * 
	 * @return the file name
	 */
	File getSourceFile();

	/**
	 * Get the name of the image the component was found in. e.g.
	 * C:\Folder\ImageFolder\1.tiff will return 1.tiff
	 * 
	 * @return the file name
	 */
	String getSourceFileName();

	/**
	 * Get the name of the source file with the '.ext' removed
	 * 
	 * @return a file name without extension
	 */
	String getSourceFileNameWithoutExtension();

	/**
	 * Set the image file the component was found in
	 * 
	 * @param sourceFile the file
	 */
	void setSourceFile(@NonNull File sourceFile);

	/**
	 * Set the folder the source image file belongs to
	 * 
	 * @param sourceFolder the folder
	 */
	void setSourceFolder(@NonNull File sourceFolder);

	/**
	 * Translate the given coordinate from the component image of the template
	 * object into the equivalent coordinate in the source image of the template
	 * object.
	 * 
	 * Source images are cropped and have a buffer added for display (called the
	 * component image).
	 * 
	 * @param point    the point in the template component image coordinate system
	 * @param template the template object
	 * @return the point in the original coordinate system of the template source
	 *         image
	 */
	static IPoint translateCoordinateToSourceImage(IPoint point, Imageable template) {

		/*
		 * When using the component images, there is both a cropping of the source
		 * image, and the addition of a buffer to each side (#COMPONENT_BUFFER) so that
		 * the edges of the object are not obscured. These must be corrected for when
		 * translating coordinates between component images.
		 * 
		 * 300 ___________________ | | Source Image | __________ | __ | | _____ | |
		 * Component buffer | 195 | | | | | | | 200 | | | X | | 15 | Template |
		 * Component image | | |____| | | | | |__________| | __| |_______15__________|
		 * 
		 * 295
		 * 
		 * In the template image, point X is at 15, 15. This includes the
		 * COMPONENT_BUFFER, so the position within the template object is 15 -
		 * COMPONENT_BUFFER (5, 5).
		 * 
		 * This position must be then be offset by the X_BASE and Y_BASE of the
		 * template.
		 * 
		 * The template base is at (295, 195). Applying the offset to point X gives a
		 * final position of (300, 200)
		 * 
		 */

		return point
				.minus(COMPONENT_BUFFER)
				.plus(template.getOriginalBase().minus(template.getBase()));
	}

	/**
	 * Translate the given point from the source image of the template object to the
	 * equivalent coordinate in the component image.
	 * 
	 * Source images are cropped and have a buffer added for display (called the
	 * component image).
	 * 
	 * @param point    the point in the original image coordinate system
	 * @param template the object
	 * @return the position of the point in the component image of the template
	 */
	static IPoint translateCoordinateToComponentImage(IPoint point, Imageable template) {

		/*
		 * When using the component images, there is both a cropping of the source
		 * image, and the addition of a buffer to each side (#COMPONENT_BUFFER) so that
		 * the edges of the object are not obscured. These must be corrected for when
		 * translating coordinates between component images.
		 * 
		 * Original offsets were performed according to the CoM. TODO: should that
		 * matter for these offsets?
		 * 
		 * 300 ___________________ | | Source Image | __________ | __ | | _____ | |
		 * Component buffer | 195 | | | | | | | 200 | | | X | | 15 | Template |
		 * Component image | | |____| | | | | |__________| | __| |_______15__________|
		 * 
		 * 295
		 * 
		 * In the template source image, point X is at (300,200). To get the point in
		 * the component image, we need to subtract the template base coordinates (295,
		 * 195), to give the position in the component (5,5).
		 * 
		 * The component image includes the COMPONENT_BUFFER, so offset the component
		 * coordinate again, giving a final position of (15, 15).
		 * 
		 */

		return point
				.minus(template.getOriginalBase()
						.minus(template.getBase()))
				.plus(COMPONENT_BUFFER);
	}
}
