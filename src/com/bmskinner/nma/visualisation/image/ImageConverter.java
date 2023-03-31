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
package com.bmskinner.nma.visualisation.image;

import java.awt.Color;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.io.ImageImporter;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Toolbar;
import ij.plugin.CanvasResizer;
import ij.plugin.RGBStackMerge;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

/**
 * This class handles the flattening of the image stacks used internally by a
 * Nucleus into an RGB image.
 * 
 * @author Ben Skinner
 *
 */
public class ImageConverter extends ImageFilterer {

    public ImageConverter(@NonNull ImageProcessor ip) {
        super(ip);
    }

    /**
     * Create a blank byte processor image of the specified dimensions
     * 
     * @param w the width
     * @param h the height
     * @return an image processor
     */
    public static ImageProcessor createBlankImage(int w, int h) {
        ImageProcessor ip = new ByteProcessor(w, h);
        ip.invert();
        return new ImageConverter(ip).convertToRGBGreyscale(ip).toProcessor();
    }

    /**
     * Create a blank byte processor image of the specified dimensions
     * 
     * @param w the width
     * @param h the height
     * @return an image processor
     */
    public static ImageConverter createBlankImageConverter(int w, int h) {
        ImageProcessor ip = new ByteProcessor(w, h);
        ip.invert();
        return new ImageConverter(ip).convertToRGBGreyscale(ip);
    }

    /**
     * Convert to RGB images suitable for export or display
     * 
     * @return an ImageConverter containing the converted image
     */
    public ImageConverter convertToRGB() {
        if (ip != null)
            return convertToRGBGreyscale(ip);
        return this;
    }

    /**
     * Convert the stack or processor into an RBG greyscale image.
     * 
     * @return
     */
    public ImageConverter convertToRGBGreyscale() {
        if (ip != null) {
        	
        	if(ip instanceof ColorProcessor)
        		return new ImageConverter(ip.convertToByteProcessor());
        	
            return convertToRGBGreyscale(ip);
        }
        return this;
    }


    /**
     * Invert the stored image. If a stack is present, invert slice 1
     * 
     * @param stack
     * @return
     */
    @Override
	public ImageConverter invert() {
        if (ip != null) {
            ip.invert();
        }
        return this;
    }

    /**
     * Add a border of pixels to the image. The image will be centred in the new
     * image. The border will be filled with the background fill of the original
     * image
     * 
     * @param borderWidth the number of pixels to add to each side of the image
     * @return the converter
     */
    public ImageConverter addBorder(int borderWidth) {
    	ip = addBorder(ip, borderWidth);
    	return this;
    }

    /**
     * Add a border of pixels to the image. The image will be centred in the new
     * image. The border will be filled with the background fill of the original
     * image
     * 
     * @param ip the image processor to add the border to
     * @param borderWidth the number of pixels to add to each side of the image
     * @return the converter
     */
    private ImageProcessor addBorder(ImageProcessor ip, int borderWidth) {

        int w = ip.getWidth();
        int h = ip.getHeight();

        int newW = w + (2 * borderWidth);
        int newH = h + (2 * borderWidth);

        ImageProcessor newIp = ip.createProcessor(newW, newH);
        newIp.setColor(Color.BLACK);
        newIp.fill();

        newIp.copyBits(ip, borderWidth, borderWidth, Blitter.COPY);

        return newIp;
    }
    
    /**
	 * Add a buffer of the given size to the image canvas. The given image
	 * background colour is used to fill in the new space
	 * @param buffer the amount to add
	 * @param color the background fill colour
	 * @return
	 */
    public ImageConverter expandCanvas(int buffer, Color color) {
    	ip = expandCanvas(ip, buffer, color);
    	return this;
    }
    
    
    /**
	 * Add a buffer of the given size to the image canvas. The given image
	 * background colour is used to fill in the new space
	 * @param ip the image
	 * @param buffer the amount to add
	 * @param color the background fill colour
	 * @return
	 */
	public static ImageProcessor expandCanvas(ImageProcessor ip, int buffer, Color color) {
		return expandCanvas(ip, buffer, buffer, buffer, buffer, color);
	}
	

	/**
	 * @param ip the image
	 * @param lbuffer the amount to add to the left
	 * @param rbuffer the amount to add to the right
	 * @param tbuffer the amount to add to the top
	 * @param bbuffer the amount to add to the bottom
	 * @param color the background fill colour
	 * @return the new image
	 */
	public static ImageProcessor expandCanvas(ImageProcessor ip, int lbuffer, 
			int rbuffer, 
			int tbuffer,
			int bbuffer,
			Color color) {
		Color oldBackground = Toolbar.getBackgroundColor();
		IJ.setBackgroundColor(color.getRed(), color.getGreen(), color.getBlue());
		CanvasResizer cr = new CanvasResizer();
		ImageProcessor result = cr.expandImage(ip, 
				ip.getWidth()+lbuffer+rbuffer, 
				ip.getHeight()+tbuffer+bbuffer, 
				lbuffer, tbuffer);
		IJ.setBackgroundColor(oldBackground.getRed(), oldBackground.getGreen(), oldBackground.getBlue());
		return result;
	}

    /**
     * Given a greyscale image processor, make a grey RGB processor
     * 
     * @param ip
     * @return
     */
    private ImageConverter convertToRGBGreyscale(ImageProcessor greyImage) {
        ImagePlus[] images = new ImagePlus[3];
        images[ImageImporter.RGB_RED]   = new ImagePlus("red",   greyImage);
        images[ImageImporter.RGB_GREEN] = new ImagePlus("green", greyImage);
        images[ImageImporter.RGB_BLUE]  = new ImagePlus("blue",  greyImage);

        ImagePlus result = RGBStackMerge.mergeChannels(images, false);

        result = result.flatten();

        return new ImageConverter(result.getProcessor());
    }
}
