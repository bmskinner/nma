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

import logging.Loggable;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import utility.Constants;

/**
 * This class handles the flattening of the image stacks used
 * internally by a Nucleus into an RGB image.
 * @author Ben Skinner
 *
 */
public class ImageExporter implements Loggable {
	
	private static ImageExporter instance = null;
	
	private ImageExporter(){}
	
	public static ImageExporter getInstance(){
		if(instance==null){
			instance = new ImageExporter();
		}
		return instance;
	}
	
			
	/**
	 * Convert an ImageStack to RGB images suitable for export
	 * @param stack the stack to convert
	 * @return an image
	 */
	public ImagePlus convertToRGB(ImageStack stack){
		
		if(stack==null){
			throw new IllegalArgumentException("Stack is null");
		}
		ImagePlus result = null;
		if(stack.getSize()==1){
			result = makeGreyScaleIamge(stack);
		}
		if(stack.getSize()>1){
			result = mergeStack(stack);
		}
		
		return result;
	}
	
	/**
	 * Given a stack, invert slice 1 to white on black.
	 * @param stack
	 * @return
	 */
	public ImageStack invertCounterstain(ImageStack stack){
		
		if(stack==null){
			throw new IllegalArgumentException("Stack is null");
		}
		stack.getProcessor(Constants.COUNTERSTAIN).invert();
		return stack;		
	}
	
	/**
	 * Given a stack, make an RGB greyscale image from the counterstain
	 * @param stack
	 * @return a new ImagePlus, or null if the stack was null
	 */
	public ImagePlus makeGreyRGBImage(ImageStack stack){
		
		if(stack!=null){
			ImagePlus[] images = new ImagePlus[3];
			images[Constants.RGB_RED]   = new ImagePlus("red", stack.getProcessor(Constants.COUNTERSTAIN));  
			images[Constants.RGB_GREEN] = new ImagePlus("green", stack.getProcessor(Constants.COUNTERSTAIN));  
			images[Constants.RGB_BLUE]  = new ImagePlus("blue", stack.getProcessor(Constants.COUNTERSTAIN));      

			ImagePlus result = RGBStackMerge.mergeChannels(images, false); 
			result = result.flatten();
			return result;
		} else {
			return null;
		}
	}
	
	/**
	 * Convert a single-plane stack to RGB.
	 * @param stack
	 * @return the image
	 */
	private ImagePlus makeGreyScaleIamge(ImageStack stack){

		byte[] blank = new byte[stack.getWidth() * stack.getHeight()];
		//	      for( byte b : blank){
		//	        b = -128;
		//	      }

		ImagePlus[] images = new ImagePlus[3];
		images[Constants.RGB_RED]   = new ImagePlus("red",   new ByteProcessor(stack.getWidth(), stack.getHeight(), blank));  
		images[Constants.RGB_GREEN] = new ImagePlus("green", new ByteProcessor(stack.getWidth(), stack.getHeight(), blank));
		images[Constants.RGB_BLUE]  = new ImagePlus("blue", stack.getProcessor(Constants.COUNTERSTAIN));      

		ImagePlus result = RGBStackMerge.mergeChannels(images, false); 

		result = result.flatten();
		return result;
	}

	/**
	 * Convert a multi-plane stack to RGB. Only the first two signal channels are used. 
	 * @param stack the stack
	 * @return an RGB image
	 */
	private ImagePlus mergeStack(ImageStack stack){

		ImagePlus[] images = new ImagePlus[3];
		images[Constants.RGB_RED]   = new ImagePlus("red", stack.getProcessor(Constants.FIRST_SIGNAL_CHANNEL));  
		images[Constants.RGB_GREEN] = new ImagePlus("green", stack.getProcessor(3));  
		images[Constants.RGB_BLUE]  = new ImagePlus("blue", stack.getProcessor(Constants.COUNTERSTAIN));      

		ImagePlus result = RGBStackMerge.mergeChannels(images, false); 
		
		result = result.flatten();
		return result;
	}
	
	
	/**
	 * Given a greyscale image processor, make a grey RGB processor
	 * @param ip
	 * @return
	 */
	public ImageProcessor convertTORGBGreyscale(ImageProcessor ip){
		ImagePlus[] images = new ImagePlus[3];
		images[Constants.RGB_RED]   = new ImagePlus("red", ip);  
		images[Constants.RGB_GREEN] = new ImagePlus("green", ip);  
		images[Constants.RGB_BLUE]  = new ImagePlus("blue", ip);      

		ImagePlus result = RGBStackMerge.mergeChannels(images, false); 
		
		result = result.flatten();
		
		return result.getProcessor();
	}
}     
