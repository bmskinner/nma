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

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import logging.DebugFileHandler;
import utility.Constants;

/**
 * This class takes any given input image, and will convert it
 * to the ImageStack needed for the analyses. The DNA/DAPI will
 * always be set at index 0, with other signals appended 
 *
 */
public class ImageImporter {

	private static java.util.logging.Logger programLogger = null;
	
	private static int[] imageTypesProcessed = { ImagePlus.GRAY8, ImagePlus.COLOR_RGB, ImagePlus.GRAY16 };
	
	
	/**
	 * Import and convert the image in the given file to an ImageStack
	 * @param f the file to import
	 * @param handler the debug file handler to write to
	 * @return the ImageStack
	 */
	public static ImageStack importImage(File f, DebugFileHandler handler){
		programLogger = Logger.getLogger(ImageImporter.class.getName());
		programLogger.addHandler(handler);

		ImageStack stack = null;
		try{
			if(f.isFile()){
				programLogger.log(Level.FINE, "Importing image: "+f.getAbsolutePath());
				ImagePlus image = new ImagePlus(f.getAbsolutePath());
				stack = convert(image);
			} else {
				programLogger.log(Level.FINE, "Not a file: "+f.getAbsolutePath());
			}
		} catch (Exception e){
			programLogger.log(Level.SEVERE, "Error importing image", e);
		} finally {
			for(Handler h : programLogger.getHandlers()){
				h.close();
			}
		}
		return stack;
	}
	
	public static ImageStack importImage(File f){
		return importImage(f, (Logger) null);
	}
	
	public static ImageStack importImage(File f, Logger logger){
		programLogger = logger;
		ImageStack stack = null;
		try{
			if(f.isFile()){
				if(programLogger!=null){
					programLogger.log(Level.FINE, "Importing image: "+f.getAbsolutePath());
				}
				ImagePlus image = new ImagePlus(f.getAbsolutePath());
				stack = convert(image);
			} else {
				if(programLogger!=null){
					programLogger.log(Level.WARNING, "Not a file: "+f.getAbsolutePath());
				}
			}
				
		} catch (Exception e){
			if(programLogger!=null){
				programLogger.log(Level.SEVERE, "Error importing image", e);
			}
		}
		return stack;
	}
	
	/**
	 * Create an ImageStack from the input image
	 * @param image the image to be converted to a stack
	 * @return the stack with countertain in index 0
	 */
	public static ImageStack convert(ImagePlus image) throws Exception {
		if(image==null){
			if(programLogger!=null){
				programLogger.log(Level.WARNING, "Input image is null");
			} 
			throw new IllegalArgumentException("Input image is null");
		}
		
		// check that we are able to handle this image type
		boolean ok = false;
		for(int i : imageTypesProcessed){
			if(i==image.getType()){
				ok = true;
			}
		}
		if(!ok ){
			if(programLogger==null){
				programLogger.log(Level.WARNING, "Cannot handle image type: "+image.getType());
			}
			throw new IllegalArgumentException("Cannot handle image type: "+image.getType());
		}
		if(programLogger!=null){
			programLogger.log(Level.FINE, "Image is type: "+image.getType());
		}				
		
		// do the conversions
		ImageStack result = null;
		if(image.getType()==ImagePlus.GRAY8){
			if(programLogger!=null){
				programLogger.log(Level.FINE, "Converting 8 bit greyscale to stack");
			}
			
			result = convertGreyscale(image);
		}
		
		if(image.getType()==ImagePlus.COLOR_RGB){
			if(programLogger!=null){
				programLogger.log(Level.FINE, "Converting RGB to stack");
			}
			
			result = convertRGB(image);
		}
		
		if(image.getType()==ImagePlus.GRAY16){
			if(programLogger!=null){
				programLogger.log(Level.FINE, "Converting 16 bit greyscale to 8 bit stack");
			}
			
			result = convert16bitGrey(image);
		}
		
		return result;
	}
	
	
	/**
	 * @param image the image to convert
	 * @return a stack with the input image as position 0
	 */
	private static ImageStack convertGreyscale(ImagePlus image){
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
	private static ImageStack convertRGB(ImagePlus image){
		ImageStack result = ImageStack.create(image.getWidth(), image.getHeight(), 0, 8);
//		ImageStack result = new ImageStack(image.getWidth(), image.getHeight());
		
		// split out colour channel
	    ImagePlus[] channels = ChannelSplitter.split(image);
	    
	    result.addSlice("counterstain", channels[Constants.RGB_BLUE].getProcessor());
	    result.addSlice(channels[Constants.RGB_RED].getProcessor());
	    result.addSlice(channels[Constants.RGB_GREEN].getProcessor());
	    result.deleteSlice(1); // remove the blank first slice
	    if(programLogger!=null){
	    	programLogger.log(Level.FINE, "New stack has "+result.getSize()+" slices");
	    } 
	    return result;
	}
	
	/**
	 * Convert a 16 bit greyscale image. For now, this just down
	 * converts to an 8 bit image.
	 * @param image the 16 bit image to convert
	 * @return the stack
	 */
	private static ImageStack convert16bitGrey(ImagePlus image) throws Exception {
		if(programLogger!=null){
			programLogger.log(Level.FINE, "Converting image from 16 bit");
	    }
		ImageConverter converter = new ImageConverter(image);
		converter.convertToGray8();
		return convertGreyscale(image);
	}
}
