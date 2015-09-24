/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package no.imports;

import java.io.File;

import utility.Constants;
import utility.Logger;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;
import ij.process.ImageConverter;

/**
 * This class takes any given input image, and will convert it
 * to the ImageStack needed for the analyses. The DNA/DAPI will
 * always be set at index 0, with other signals appended 
 *
 */
public class ImageImporter {

	private static Logger logger;
	
	private static int[] imageTypesProcessed = { ImagePlus.GRAY8, ImagePlus.COLOR_RGB, ImagePlus.GRAY16 };
	
	
	/**
	 * Import and convert the image in the given file to an ImageStack
	 * @param f the file to import
	 * @return the ImageStack
	 */
	public static ImageStack importImage(File f, File log){
		logger = new Logger(log, "ImageImporter");
		ImageStack stack = null;
		try{
			if(f.isFile()){
				logger.log("Importing image: "+f.getAbsolutePath(), Logger.DEBUG);
				ImagePlus image = new ImagePlus(f.getAbsolutePath());
				stack = convert(image);
			} else {
				logger.log("Not a file: "+f.getAbsolutePath(), Logger.DEBUG);
			}
		} catch (Exception e){
			logger.log("Error importing image: "+e.getMessage(), Logger.ERROR);
		}
		return stack;
	}
	/**
	 * Create an ImageStack from the input image
	 * @param image the image to be converted to a stack
	 * @return the stack with countertain in index 0
	 */
	public static ImageStack convert(ImagePlus image){
		if(image==null){
			logger.log("Input image is null", Logger.ERROR);
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
			logger.log("Cannot handle image type: "+image.getType(), Logger.ERROR);
			throw new IllegalArgumentException("Cannot handle image type: "+image.getType());
		}
						
		logger.log("Image is type: "+image.getType());
		// do the conversions
		ImageStack result = null;
		if(image.getType()==ImagePlus.GRAY8){
			logger.log("Converting 8 bit greyscale to stack",Logger.DEBUG);
			result = convertGreyscale(image);
		}
		
		if(image.getType()==ImagePlus.COLOR_RGB){
			logger.log("Converting RGB to stack",Logger.DEBUG);
			result = convertRGB(image);
		}
		
		if(image.getType()==ImagePlus.GRAY16){
			logger.log("Converting 16 bit greyscale to 8 bit stack",Logger.DEBUG);
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
	    logger.log("New stack has "+result.getSize()+" slices",Logger.DEBUG);
//	    ImagePlus demo = new ImagePlus(null, result);
//	    
//	    demo.show();
	    return result;
	}
	
	/**
	 * Convert a 16 bit greyscale image. For now, this just down
	 * converts to an 8 bit image.
	 * @param image the 16 bit image to convert
	 * @return the stack
	 */
	private static ImageStack convert16bitGrey(ImagePlus image){
		ImageConverter converter = new ImageConverter(image);
		converter.convertToGray8();
		return convertGreyscale(image);
	}
}
