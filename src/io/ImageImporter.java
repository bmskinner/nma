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
package io;

import gui.ThreadManager;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.logging.Level;

import logging.DebugFileHandler;
import logging.Loggable;
import utility.Constants;

/**
 * This class takes any given input image, and will convert it
 * to the ImageStack needed for the analyses. The DNA/DAPI will
 * always be set at index 0, with other signals appended .
 * 
 * Uses a singleton pattern
 *
 */
public class ImageImporter implements Loggable {
	
	private static ImageImporter imp = null;
	
	private ImageImporter(){}
	
	public static ImageImporter getInstance(){
		if(imp==null){
			imp = new ImageImporter();
		}
		return imp;
	}
	
	private static final int[] IMAGE_TYPES_PROCESSED = { ImagePlus.GRAY8, ImagePlus.COLOR_RGB, ImagePlus.GRAY16 };
	
	
	/**
	 * Import and convert the image in the given file to an ImageStack
	 * @param f the file to import
	 * @param handler the debug file handler to write to
	 * @return the ImageStack
	 */
	public ImageStack importImage(File f, DebugFileHandler handler){
		
		ImageStack stack = null;
		try{
			if(f.isFile()){
				log(Level.FINE, "Importing image: "+f.getAbsolutePath());
				ImagePlus image = new ImagePlus(f.getAbsolutePath());
				stack = convert(image);
			} else {
				log(Level.FINE, "Not a file: "+f.getAbsolutePath());
			}
		} catch (Exception e){
			log(Level.SEVERE, "Error importing image", e);
		} finally {

		}
		return stack;
	}

	public ImageStack importImage(File f){

		ImageStack stack = null;

		try{
			if(f.isFile()){

				fine("Importing image: "+f.getAbsolutePath());

				ImagePlus image = new ImagePlus(f.getAbsolutePath());
				stack = convert(image);

			} else {
				warn("Not a file: "+f.getAbsolutePath());
			}

		} catch (Exception e){

			error("Error importing image", e);

		}

		return stack;
	}
	
	/**
	 * Import the image in the given file, and return an image processor
	 * for the channel requested. Inverts the greyscale image so white==no signal
	 * and black==full signal
	 * @param f
	 * @param channel
	 * @return
	 */
	public ImageProcessor importImage(File f, int channel){
		ImageStack s = importImage(f);
		int stack = Constants.rgbToStack(channel);
		ImageProcessor ip = s.getProcessor(stack);
		ip.invert();
		return ip;
	}
	
	/**
	 * Create an ImageStack from the input image
	 * @param image the image to be converted to a stack
	 * @return the stack with countertain in index 0
	 */
	public ImageStack convert(ImagePlus image) throws Exception {
		if(image==null){
			log(Level.WARNING, "Input image is null");

			throw new IllegalArgumentException("Input image is null");
		}

		// check that we are able to handle this image type
		boolean ok = false;
		for(int i : IMAGE_TYPES_PROCESSED){
			if(i==image.getType()){
				ok = true;
			}
		}
		if(!ok ){

			log(Level.WARNING, "Cannot handle image type: "+image.getType());

			throw new IllegalArgumentException("Cannot handle image type: "+image.getType());
		}

		log(Level.FINE, "Image is type: "+image.getType());


		// do the conversions
		ImageStack result = null;
		if(image.getType()==ImagePlus.GRAY8){

			log(Level.FINE, "Converting 8 bit greyscale to stack");


			result = convertGreyscale(image);
		}

		if(image.getType()==ImagePlus.COLOR_RGB){
			log(Level.FINE, "Converting RGB to stack");


			result = convertRGB(image);
		}

		if(image.getType()==ImagePlus.GRAY16){

			log(Level.FINE, "Converting 16 bit greyscale to 8 bit stack");


			result = convert16bitGrey(image);
		}

		return result;
	}

	
	/**
	 * @param image the image to convert
	 * @return a stack with the input image as position 0
	 */
	private ImageStack convertGreyscale(ImagePlus image){
		ImageStack result = ImageStack.create(image.getWidth(), image.getHeight(), 0, 8);
	    result.addSlice("counterstain", image.getProcessor());
	    result.deleteSlice(1); // remove the blank first slice
	    return result;
	}
	
	/**
	 * Given an RGB image, convert it to a stack, with the blue 
	 * channel first
	 * @param image the image to convert to a stack
	 * @return the stack
	 */
	private ImageStack convertRGB(ImagePlus image){
		ImageStack result = ImageStack.create(image.getWidth(), image.getHeight(), 0, 8);
//		ImageStack result = new ImageStack(image.getWidth(), image.getHeight());
		
		// split out colour channel
	    ImagePlus[] channels = ChannelSplitter.split(image);
	    
	    result.addSlice("counterstain", channels[Constants.RGB_BLUE].getProcessor());
	    result.addSlice(channels[Constants.RGB_RED].getProcessor());
	    result.addSlice(channels[Constants.RGB_GREEN].getProcessor());
	    result.deleteSlice(1); // remove the blank first slice
	    log(Level.FINE, "New stack has "+result.getSize()+" slices");
	    return result;
	}
	
	/**
	 * Convert a 16 bit greyscale image. For now, this just down
	 * converts to an 8 bit image.
	 * @param image the 16 bit image to convert
	 * @return the stack
	 */
	private ImageStack convert16bitGrey(ImagePlus image) throws Exception {
		log(Level.FINE, "Converting image from 16 bit");
		ImageConverter converter = new ImageConverter(image);
		converter.convertToGray8();
		return convertGreyscale(image);
	}
}
