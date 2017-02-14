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
package com.bmskinner.nuclear_morphology.io;

import java.io.File;

import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;

/**
 * This class takes any given input image, and will convert it
 * to the ImageStack needed for the analyses. The DNA/DAPI will
 * always be set at index 0, with other signals appended .
 * @since 1.11.0
 *
 */
public class ImageImporter implements Loggable, Importer {
	
	private static final int[] IMAGE_TYPES_PROCESSED = { ImagePlus.GRAY8, ImagePlus.COLOR_RGB, ImagePlus.GRAY16 };
	
	// The file types that the program will try to open
	public static final String[] IMPORTABLE_FILE_TYPES = {".tif", ".tiff", ".jpg"};
	
	// The prefix to use when exporting images
	public static final String IMAGE_PREFIX = "export.";
	
	// Images with these prefixes are ignored by the image importer
	public static final String[] PREFIXES_TO_IGNORE = { IMAGE_PREFIX, "composite", "plot", "._"};
	
	// RGB colour channels
	public static final int RGB_RED = 0;
	public static final int RGB_GREEN = 1;
	public static final int RGB_BLUE = 2;	
	
	// imported images - stack positions
	public static final int COUNTERSTAIN = 1; // ImageStack slices are numbered from 1; first slice is blue
	public static final int FIRST_SIGNAL_CHANNEL = 2; // ImageStack slices are numbered from 1; first slice is blue
		
			
	private final File f;
	
	/**
	 * Construct from a file. Checks that the given File object is valid, and 
	 * throws an IllegalArgumentException if not.
	 * @param f the file to import
	 */
	public ImageImporter(final File f){
		if( ! Importer.isSuitableImportFile(f)){
			throw new IllegalArgumentException(INVALID_FILE_ERROR);
		}
		
		this.f = f;
	}
	
	  /**
	  *  Checks that the given file is suitable for analysis.
	  *  Is the file an image. Also check if it is in the 'banned list'.
	  *  These are prefixes that are attached to exported images
	  *  at later stages of analysis. This prevents exported images
	  *  from previous runs being analysed.
	  *
	  *  @param file the File to check
	  *  @return a true or false of whether the file passed checks
	  */
	  public static boolean checkFile(File file){
	    
	    if( ! file.isFile()){
	    	return false;
	    }
	    
	    String fileName = file.getName();
	    
	    for( String prefix : PREFIXES_TO_IGNORE){
	    	if(fileName.startsWith(prefix)){
	    		return false;
	    	}
	    }

	    for( String fileType : IMPORTABLE_FILE_TYPES){
	    	if( fileName.endsWith(fileType) ){
	    		return true;
	    	}
	    }
	    return false;
	  }

	  /**
	   * Given an RGB channel, get the ImageStack stack for internal use
	   * @param channel the channel
	   * @return the stack
	   */
	  public static int rgbToStack(int channel){

		  if(channel < 0){
			  throw new IllegalArgumentException(CHANNEL_BELOW_ZERO_ERROR);
		  }

		  int stackNumber = channel==RGB_RED 
				  ? FIRST_SIGNAL_CHANNEL
						  : channel==RGB_GREEN
						  ? FIRST_SIGNAL_CHANNEL+1
								  : COUNTERSTAIN;
		  return stackNumber;
	  }

	  /**
	   * Given a channel integer, return the name of the channel.
	   * Handles red (0), green (1) and blue(2). Other ints will 
	   * return a null string.
	   * @param channel
	   * @return
	   */
	  public static String channelIntToName(int channel){
		  
		  if(channel < 0){
			  throw new IllegalArgumentException(CHANNEL_BELOW_ZERO_ERROR);
		  }
		  
		  if(channel == RGB_RED){
			  return "Red";
		  }
		  if(channel == RGB_GREEN){
			  return "Green";
		  }
		  if(channel == RGB_BLUE){
			  return "Blue";
		  }
		  return null;
	  }


	  /**
	   * Import and convert the image in the given file to an ImageStack
	   * @return the ImageStack
	   */
	  public ImageStack importToStack() throws ImageImportException {

		ImageStack stack = null;

		ImagePlus image = new ImagePlus(f.getAbsolutePath());

		stack = convert(image);
		image.close();
		return stack;
	}
	  
	  /**
	   * Import and convert the image in the given file to a ColorProcessor
	   * @return the processor
	   */
	  public ImageProcessor importToColorProcessor() throws ImageImportException {

		ImagePlus image = new ImagePlus(f.getAbsolutePath());
		return image.getProcessor();
	}
	  
	 /**
	  * Import the image in the defined file as a stack, and return an image converter 
	 * @return
	 * @throws ImageImportException
	 */
	public com.bmskinner.nuclear_morphology.analysis.image.ImageConverter toConverter() throws ImageImportException {
		 return new com.bmskinner.nuclear_morphology.analysis.image.ImageConverter(importToStack());
	 }
	
	/**
	 * Import the image in the given file, and return an image processor
	 * for the channel requested. Inverts the greyscale image so white==no signal
	 * and black==full signal
	 * @param channel
	 * @return
	 */
	public ImageProcessor importImage(int channel) throws ImageImportException {
		ImageStack s = importToStack();
		int stack = rgbToStack(channel);
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
				result = convertGreyscale(image);
				break;
			}
			
			case ImagePlus.COLOR_RGB:{
				result = convertRGB(image);
				break;
			}
			
			case ImagePlus.GRAY16:{
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
		
		int imageDepth = 0; // number of images in the stack to begin
		int bitDepth   = 8; // default 8 bit images
		 
		// Create a new empty stack. There will be a blank image in the
		// stack at index 1. NB stacks do not use zero indexing.
		ImageStack result = ImageStack.create(image.getWidth(), 
				image.getHeight(), 
				imageDepth, 
				bitDepth);

		// split out colour channel
	    ImagePlus[] channels = ChannelSplitter.split(image);
	    
	    // Put each channel into the correct stack position
	    result.addSlice("counterstain", channels[RGB_BLUE].getProcessor());
	    result.addSlice(channels[RGB_RED].getProcessor());
	    result.addSlice(channels[RGB_GREEN].getProcessor());
	    result.deleteSlice(1); // remove the blank first slice
	    return result;
	}
	
	/**
	 * Convert a 16 bit greyscale image. For now, this just down
	 * converts to an 8 bit image.
	 * @param image the 16 bit image to convert
	 * @return the stack
	 */
	private ImageStack convert16bitGrey(ImagePlus image) throws ImageImportException {
		// this is the ij.process.ImageConverter, not my analysis.image.ImageConverter
		ImageConverter converter = new ImageConverter(image);
		converter.convertToGray8();
		return convertGreyscale(image);
	}
	
	/**
	 * Thrown when a conversion fails or a file is not convertible
	 * @author ben
	 *
	 */
	public class ImageImportException extends Exception {
		private static final long serialVersionUID = 1L;
		public ImageImportException() { super(); }
		public ImageImportException(String message) { super(message); }
		public ImageImportException(String message, Throwable cause) { super(message, cause); }
		public ImageImportException(Throwable cause) { super(cause); }
	}
}
