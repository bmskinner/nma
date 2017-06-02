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


package com.bmskinner.nuclear_morphology.components;

import ij.process.ImageProcessor;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

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
     * cropping its source image. This prevents the component nestling right up
     * to the edges of the resulting cropped image
     */
    public static final int COMPONENT_BUFFER = 10;

    /**
     * The array index of the left-most x coordinate of the object in
     * {@link #setPosition(double[])} and {@link #getPosition(double[])}
     */
    static final int X_BASE = 0;

    /**
     * The array index of the top-most (lowest) y coordinate of the object in
     * {@link #setPosition(double[])} and {@link #getPosition(double[])}
     */
    static final int Y_BASE = 1;

    /**
     * The array index of the width of the object in
     * {@link #setPosition(double[])} and {@link #getPosition(double[])}
     */
    static final int WIDTH = 2;

    /**
     * The array index of the height of the object in
     * {@link #setPosition(double[])} and {@link #getPosition(double[])}
     */
    static final int HEIGHT = 3;

    /**
     * Get the position of the object in the original image. The indexes in the
     * array are {@link #X_BASE} of the bounding box, {@link #Y_BASE} of the
     * bounding box, {@link #WIDTH} of the bounding box and {@link #HEIGHT} of
     * the bounding box
     * 
     * @return the array with the position
     */
    int[] getPosition();

    /**
     * Get the base X and Y position of the component in the original image.
     * Corresponds to the points in {@link #getPosition()} {@link #X_BASE} and
     * {@link #Y_BASE}
     * 
     * @return the base coordinate in the original image
     */
    IPoint getOriginalBase();

    /**
     * Get the RGB channel the object was detected in
     * 
     * @return the channel
     */
    int getChannel();

    /**
     * Get the image from which the component was detected. Opens the image via
     * the {@link com.bmskinner.nuclear_morphology.io.ImageImporter}, fetches
     * the appropriate channel and inverts it. The complete image is returned;
     * no cropping is performed
     * 
     * @return an ImageJ image processor
     * @throws UnloadableImageException
     *             if the image can't be loaded
     */
    ImageProcessor getImage() throws UnloadableImageException;

    /**
     * Get the image from which the component was detected. Opens the image via
     * the {@link com.bmskinner.nuclear_morphology.io.ImageImporter}. The
     * complete image is returned; no cropping is performed. Use when an RGB
     * image needs to be displayed, such as H&E
     * 
     * @return
     * @throws UnloadableImageException
     */
    ImageProcessor getRGBImage() throws UnloadableImageException;

    /**
     * Get the image from which the component was detected, and crops it to only
     * the region containing the component
     * 
     * @return an ImageJ image processor cropped to size, with a padding of n
     *         pixels specified in {@link CellularComponent.COMPONENT_BUFFER}
     * @throws UnloadableImageException
     *             if the image can't be loaded
     */
    ImageProcessor getComponentImage() throws UnloadableImageException;

    /**
     * Get the image from which the component was detected, and crops it to only
     * the region containing the component. Use when an RGB image needs to be
     * displayed, such as H&E
     * 
     * @return an ImageJ image processor cropped to size, with a padding of n
     *         pixels specified in {@link CellularComponent.COMPONENT_BUFFER}
     * @throws UnloadableImageException
     *             if the image can't be loaded
     */
    ImageProcessor getComponentRGBImage() throws UnloadableImageException;

    /**
     * Get the pixels within this object as a list of points
     * 
     * @return a list of points
     */
    // List<IPoint> getPixelsAsPoints();

    /**
     * Get the bounding rectangle for the object.
     * 
     * @return the bounding rectangle
     */
    Rectangle2D getBounds();

    /**
     * Get the folder of the image the component was found in. e.g.
     * C:\Folder\ImageFolder\ will return ImageFolder
     * 
     * @return the image folder name
     */
    File getSourceFolder();

    /**
     * Get the folder of the image the component was found in. e.g.
     * C:\Folder\ImageFolder\1.tiff will return 1.tiff
     * 
     * @return the file name
     */
    File getSourceFile();

    /**
     * Get the name of the image the component was found in
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
     * Update the image source folder to the given new folder
     * 
     * @param newFolder
     */
    void updateSourceFolder(File newFolder);

    /**
     * Set the image file the component was found in
     * 
     * @param sourceFile
     *            the file
     */
    void setSourceFile(File sourceFile);

    /**
     * Set the RGB channel the component was detected in
     * 
     * @param channel
     *            the channel
     */
    void setChannel(int channel);

    /**
     * Set the folder the source image file belongs to
     * 
     * @param sourceFolder
     *            the folder
     */
    void setSourceFolder(File sourceFolder);

    /**
     * Translate the given coordinate from the template component image into the
     * equivalent coordinate in the target image.
     * 
     * @param point
     *            the point to convert
     * @param template
     *            the component image the point has come from
     * @param target
     *            the component image the point should be translated to
     * @return
     */
    static IPoint translateCoordinateToComponentImage(IPoint point, Imageable template, Imageable target) {

        /*
         * When using the component images, there is both a cropping of the
         * source image, and the addition of a buffer to each side
         * (#COMPONENT_BUFFER) so that the edges of the object are not obscured.
         * This means these must be corrected for when translating coordinates
         * between component images.
         * 
         * 30 _Template____________ | | | Target | | _____ | | | | | 20 | | X |
         * 5 | | |____| | | 5 | |___________________|
         * 
         * 
         * In the template image, point X is at 30, 20. In the target image, X
         * is at 5, 5. The template x-coordinate must be adjusted by -25 (the
         * target x - template x)
         * 
         */

        IPoint temBase = template.getOriginalBase();
        IPoint tarBase = target.getOriginalBase();

        double xDiff = tarBase.getX() - temBase.getX();
        double yDiff = tarBase.getY() - temBase.getY();

        double newX = point.getX() + xDiff;
        double newY = point.getY() + yDiff;

        return IPoint.makeNew(newX, newY);
        //
        // int xOffset = (int) cyCom.getX() +
        // CellularComponent.COMPONENT_BUFFER;
        // int yOffset = (int) cyCom.getY() +
        // CellularComponent.COMPONENT_BUFFER;

    }

    /**
     * Translate the given coordinate from the template component image into the
     * equivalent coordinate in the template source image.
     * 
     * @param point
     * @param template
     * @return
     */
    static IPoint translateCoordinateToSourceImage(IPoint point, Imageable template) {

        /*
         * When using the component images, there is both a cropping of the
         * source image, and the addition of a buffer to each side
         * (#COMPONENT_BUFFER) so that the edges of the object are not obscured.
         * These must be corrected for when translating coordinates between
         * component images.
         * 
         * 300 ___________________ | | Source Image | __________ | __ | | _____
         * | | Component buffer | 195 | | | | | | | 200 | | | X | | 15 |
         * Template | Component image | | |____| | | | | |__________| | __|
         * |_______15__________|
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
         * The template base is at (295, 195). Applying the offset to point X
         * gives a final position of (300, 200)
         * 
         */

        double xTem = point.getX() - COMPONENT_BUFFER;
        double yTem = point.getY() - COMPONENT_BUFFER;

        IPoint temBase = template.getOriginalBase();
        double x = xTem + temBase.getX();
        double y = yTem + temBase.getY();

        return IPoint.makeNew(x, y);

    }

    /**
     * Translate the given point from the source image of the template object to
     * the equivalent coordinate in the component image
     * 
     * @param point
     * @param template
     * @return
     */
    static IPoint translateCoordinateToComponentImage(IPoint point, Imageable template) {

        /*
         * When using the component images, there is both a cropping of the
         * source image, and the addition of a buffer to each side
         * (#COMPONENT_BUFFER) so that the edges of the object are not obscured.
         * These must be corrected for when translating coordinates between
         * component images.
         * 
         * 300 ___________________ | | Source Image | __________ | __ | | _____
         * | | Component buffer | 195 | | | | | | | 200 | | | X | | 15 |
         * Template | Component image | | |____| | | | | |__________| | __|
         * |_______15__________|
         * 
         * 295
         * 
         * In the template source image, point X is at (300,200). To get the
         * point in the component image, we need to subtract the template base
         * coordinates (295, 195), to give the position in the component (5,5).
         * 
         * The component image includes the COMPONENT_BUFFER, so offset the
         * component coordinate again, giving a final position of (15, 15).
         * 
         */

        IPoint temBase = template.getOriginalBase();

        double xCom = point.getX() - temBase.getX();
        double yCom = point.getY() - temBase.getY();

        double x = xCom + COMPONENT_BUFFER;
        double y = yCom + COMPONENT_BUFFER;

        return IPoint.makeNew(x, y);

    }

}
