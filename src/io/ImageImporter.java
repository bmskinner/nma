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

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.logging.Level;

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
	
	private final File f;
	private static final int[] IMAGE_TYPES_PROCESSED = { ImagePlus.GRAY8, ImagePlus.COLOR_RGB, ImagePlus.GRAY16 };
	
	/**
	 * Construct from a file. Checks that the given File object is valid, and 
	 * throws an IllegalArgumentException if not.
	 * @param f the file to import
	 */
	public ImageImporter(final File f){
		if(f==null){
			throw new IllegalArgumentException("File cannot be null");
		}
		
		if( ! f.exists()){
			throw new IllegalArgumentException("File does not exist");
		}
		
		if( f.isDirectory()){
			throw new IllegalArgumentException("File is a directory");
		}
		
		if( ! f.isFile()){
			throw new IllegalArgumentException("File has non-normal attributes or is not a file");
		}
		
		this.f = f;
	}


	/**
	 * Import and convert the image in the given file to an ImageStack
	 * @return the ImageStack
	 */
	public ImageStack importImage() throws ImageImportException{

		ImageStack stack = null;

		fine("Importing image: "+f.getAbsolutePath());

		ImagePlus image = new ImagePlus(f.getAbsolutePath());

		stack = convert(image);
		image.close();
		return stack;
	}
	
	/**
	 * Import the image in the given file, and return an image processor
	 * for the channel requested. Inverts the greyscale image so white==no signal
	 * and black==full signal
	 * @param channel
	 * @return
	 */
	public ImageProcessor importImage(int channel) throws ImageImportException {
		ImageStack s = importImage();
		int stack = Constants.rgbToStack(channel);
		ImageProcessor ip = s.getProcessor(stack);
		ip.invert();
		return ip;
	}
	
	/**
	 * Test if the given image can be read by this program
	 * @param image
	 * @return
	 */
	private boolean canImport(ImagePlus image){
		// check that we are able to handle this image type
		boolean ok = false;
		for(int i : IMAGE_TYPES_PROCESSED){
			if(i==image.getType()){
				ok = true;
			}
		}
		return  ok;
	}
	
	/**
	 * Create an ImageStack from the input image
	 * @param image the image to be converted to a stack
	 * @return the stack with countertain in index 0
	 */
	private ImageStack convert(ImagePlus image) throws ImageImportException {
		if(image==null){
			throw new ImageImportException("Input image is null");
		}
		
		if( ! canImport(image) ){
			throw new ImageImportException("Cannot handle image type: "+image.getType());
		}

		// do the conversions
		ImageStack result = null;
		
		switch(image.getType()){
		
			case ImagePlus.GRAY8:{
				fine("Converting 8 bit greyscale to stack");
				result = convertGreyscale(image);
				break;
			}
			
			case ImagePlus.COLOR_RGB:{
				fine("Converting RGB to stack");
				result = convertRGB(image);
				break;
			}
			
			case ImagePlus.GRAY16:{
				fine("Converting 16 bit greyscale to 8 bit stack");
				result = convert16bitGrey(image);
				break;
			}
			
			default:{
				// Should never occur given the test in canImport(), but shows the intent 
				throw new ImageImportException("Unsupported image type: "+image.getType());
			}
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
	private ImageStack convert16bitGrey(ImagePlus image) throws ImageImportException {
		log(Level.FINE, "Converting image from 16 bit");
		ImageConverter converter = new ImageConverter(image);
		converter.convertToGray8();
		return convertGreyscale(image);
	}
	
	public class ImageImportException extends Exception {
		private static final long serialVersionUID = 1L;
		public ImageImportException() { super(); }
		public ImageImportException(String message) { super(message); }
		public ImageImportException(String message, Throwable cause) { super(message, cause); }
		public ImageImportException(Throwable cause) { super(cause); }
	}
}
