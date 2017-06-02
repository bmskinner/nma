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


package com.bmskinner.nuclear_morphology.analysis.image;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.process.Blitter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.List;

import com.bmskinner.nuclear_morphology.io.ImageImporter;

/**
 * This class handles the flattening of the image stacks used internally by a
 * Nucleus into an RGB image.
 * 
 * @author Ben Skinner
 *
 */
public class ImageConverter extends AbstractImageFilterer {

    public ImageConverter(ImageProcessor ip) {
        super(ip);
    }

    public ImageConverter(ImageStack st) {
        super(st);
    }

    /**
     * Create a blank byte processor image of the specified dimensions
     * 
     * @param w
     *            the width
     * @param h
     *            the height
     * @return an image processor
     */
    public static ImageProcessor createBlankImage(int w, int h) {
        ImageProcessor ip = new ByteProcessor(w, h);
        ip.invert();
        return new ImageConverter(ip).convertTORGBGreyscale().toProcessor();
    }

    /**
     * Create a blank byte processor image of the specified dimensions
     * 
     * @param w
     *            the width
     * @param h
     *            the height
     * @return an image processor
     */
    public static ImageConverter createBlankImageConverter(int w, int h) {
        ImageProcessor ip = new ByteProcessor(w, h);
        ip.invert();
        return new ImageConverter(ip).convertTORGBGreyscale();
    }

    /**
     * Convert an ImageStack to RGB images suitable for export or display
     * 
     * @return an ImageConverter containing the converted image
     */
    public ImageConverter convertToRGB() {

        // Handle stacks first
        if (st != null) {
            if (st.getSize() == 1) {
                return makeGreyScaleIamge();
            }
            if (st.getSize() > 1) {
                return mergeStack();
            }
        }

        if (ip != null) {
            return convertTORGBGreyscale();
        }

        return this;
    }

    /**
     * Convert the stack or processor into an RBG greyscale image.
     * 
     * @return
     */
    public ImageConverter convertToGreyscale() {
        // Handle stacks first
        if (st != null) {
            if (st.getSize() == 1) {
                return makeGreyScaleIamge();
            }
            if (st.getSize() > 1) {
                return makeGreyRGBImage(ImageImporter.COUNTERSTAIN); // default
                                                                     // is
                                                                     // counterstain
            }
        }

        if (ip != null) {
            return convertTORGBGreyscale();
        }

        return this;
    }

    /**
     * Create a greyscale RGB image from a single processor in a stack
     * 
     * @param stackNumber
     *            the stack of the image to convert
     * @return
     */
    public ImageConverter convertToGreyscale(int stackNumber) {
        // Handle stacks first
        if (st != null) {
            return makeGreyRGBImage(stackNumber);
        }

        return this;
    }

    /**
     * Invert the stored image. If a stack is present, invert slice 1
     * 
     * @param stack
     * @return
     */
    public ImageConverter invert() {
        // Handle stacks first
        if (st != null) {
            ip = st.getProcessor(ImageImporter.COUNTERSTAIN);
        }

        if (ip != null) {
            ip.invert();
        }
        return this;
    }

    /**
     * Invert the stored stack image slice
     * 
     * @param stack
     * @return
     */
    public ImageConverter invert(int stackNumber) {
        // Handle stacks first
        if (st != null) {
            ip = st.getProcessor(stackNumber);
            ip.invert();
        }

        return this;
    }

    /**
     * Add a border of pixels to the image. The image will be centred in the new
     * image. The border will be filled with the background fill of the original
     * image
     * 
     * @param borderWidth
     *            the number of pixels to add to each side of the image
     * @return the converter
     */
    public ImageConverter addBorder(int borderWidth) {

        if (st == null) {
            ip = addBorder(ip, borderWidth);
        } else {

            int newW = st.getWidth() + (2 * borderWidth);
            int newH = st.getHeight() + (2 * borderWidth);

            ImageStack newSt = new ImageStack(newW, newH);
            for (int i = 1; i <= st.getSize(); i++) {
                ImageProcessor p = st.getProcessor(i);

                p = addBorder(p, borderWidth);
                newSt.addSlice(p);
            }
            st = newSt;

        }

        return this;
    }

    /**
     * Add a border of pixels to the image. The image will be centred in the new
     * image. The border will be filled with the background fill of the original
     * image
     * 
     * @param ip
     *            the image processor to add the border to
     * @param borderWidth
     *            the number of pixels to add to each side of the image
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

    // /**
    // * Given a stack, invert slice 1 to white on black.
    // * @param stack
    // * @return
    // */
    // public ImageConverter invertCounterstain(){
    //
    // if(st==null){
    // throw new IllegalArgumentException("Stack is null");
    // }
    // st.getProcessor(Constants.COUNTERSTAIN).invert();
    // return new ImageConverter(st);
    // }

    /**
     * Given a greyscale image processor, make a grey RGB processor
     * 
     * @param ip
     * @return
     */
    private ImageConverter convertTORGBGreyscale() {
        ImagePlus[] images = new ImagePlus[3];
        images[ImageImporter.RGB_RED] = new ImagePlus("red", ip);
        images[ImageImporter.RGB_GREEN] = new ImagePlus("green", ip);
        images[ImageImporter.RGB_BLUE] = new ImagePlus("blue", ip);

        ImagePlus result = RGBStackMerge.mergeChannels(images, false);

        result = result.flatten();

        return new ImageConverter(result.getProcessor());
    }

    /**
     * Given a stack, make an RGB greyscale image from the given stack
     * 
     * @param stack
     * @return a new ImagePlus, or null if the stack was null
     */
    private ImageConverter makeGreyRGBImage(int stackNumber) {

        if (st != null) {
            ImagePlus[] images = new ImagePlus[3];
            images[ImageImporter.RGB_RED] = new ImagePlus("red", st.getProcessor(stackNumber));
            images[ImageImporter.RGB_GREEN] = new ImagePlus("green", st.getProcessor(stackNumber));
            images[ImageImporter.RGB_BLUE] = new ImagePlus("blue", st.getProcessor(stackNumber));

            ImagePlus result = RGBStackMerge.mergeChannels(images, false);
            result = result.flatten();

            return new ImageConverter(result.getProcessor());
        } else {
            return this;
        }
    }

    /**
     * Convert a single-plane stack to RGB.
     * 
     * @param stack
     * @return the image
     */
    private ImageConverter makeGreyScaleIamge() {
        return makeGreyRGBImage(ImageImporter.COUNTERSTAIN);
    }

    /**
     * Convert a multi-plane stack to RGB. Only the first two signal channels
     * are used.
     * 
     * @param stack
     *            the stack
     * @return an RGB image
     */
    private ImageConverter mergeStack() {

        ImagePlus[] images = new ImagePlus[3];
        images[ImageImporter.RGB_RED] = new ImagePlus("red", st.getProcessor(ImageImporter.FIRST_SIGNAL_CHANNEL));
        images[ImageImporter.RGB_GREEN] = new ImagePlus("green", st.getProcessor(3));
        images[ImageImporter.RGB_BLUE] = new ImagePlus("blue", st.getProcessor(ImageImporter.COUNTERSTAIN));

        ImagePlus result = RGBStackMerge.mergeChannels(images, false);

        result = result.flatten();
        return new ImageConverter(result.getProcessor());
    }

}
