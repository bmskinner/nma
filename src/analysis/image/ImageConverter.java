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
package analysis.image;

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
public class ImageConverter extends AbstractImageFilterer {
		
	public ImageConverter(ImageProcessor ip) {
		super(ip);
	}
	
	public ImageConverter(ImageStack st) {
		super(st);
	}
		
			
	/**
	 * Convert an ImageStack to RGB images suitable for export
	 * or display
	 * @return an ImageConverter containing the converted image
	 */
	public ImageConverter convertToRGB(){
		
		// Handle stacks first
		if(st!=null){
			if(st.getSize()==1){
				return makeGreyScaleIamge();
			}
			if(st.getSize()>1){
				return mergeStack();
			}
		}
		
		
		if(ip!=null){
			return convertTORGBGreyscale();
		}

		return this;
	}
	
	/**
	 * Convert the stack or processor into an RBG greyscale image.
	 * @return
	 */
	public ImageConverter convertToGreyscale(){
		// Handle stacks first
		if(st!=null){
			if(st.getSize()==1){
				return makeGreyScaleIamge();
			}
			if(st.getSize()>1){
				return makeGreyRGBImage(Constants.COUNTERSTAIN); // default is counterstain
			}
		}


		if(ip!=null){
			return convertTORGBGreyscale();
		}

		return this;
	}
	
	/**
	 * Create a greyscale RGB image from a single processor in a stack
	 * @param stackNumber the stack of the image to convert
	 * @return
	 */
	public ImageConverter convertToGreyscale(int stackNumber){
		// Handle stacks first
		if(st!=null){
			return makeGreyRGBImage(stackNumber); 
		}

		return this;
	}
	
	
	/**
	 * Invert the stored image. If a stack is present, invert slice 1
	 * @param stack
	 * @return
	 */
	public ImageConverter invert(){
		// Handle stacks first
		if(st!=null){
			ip = st.getProcessor(Constants.COUNTERSTAIN);
		}

		if(ip!=null){
			ip.invert();
		}
		return this;
	}
	
	/**
	 * Invert the stored stack image slice
	 * @param stack
	 * @return
	 */
	public ImageConverter invert(int stackNumber){
		// Handle stacks first
		if(st!=null){
			ip = st.getProcessor(stackNumber);
			ip.invert();
		}

		return this;
	}
	
	
//	/**
//	 * Given a stack, invert slice 1 to white on black.
//	 * @param stack
//	 * @return
//	 */
//	public ImageConverter invertCounterstain(){
//		
//		if(st==null){
//			throw new IllegalArgumentException("Stack is null");
//		}
//		st.getProcessor(Constants.COUNTERSTAIN).invert();
//		return new ImageConverter(st);		
//	}
	
	/**
	 * Given a greyscale image processor, make a grey RGB processor
	 * @param ip
	 * @return
	 */
	private ImageConverter convertTORGBGreyscale(){
		ImagePlus[] images = new ImagePlus[3];
		images[Constants.RGB_RED  ] = new ImagePlus("red",   ip);  
		images[Constants.RGB_GREEN] = new ImagePlus("green", ip);  
		images[Constants.RGB_BLUE ] = new ImagePlus("blue",  ip);      

		ImagePlus result = RGBStackMerge.mergeChannels(images, false); 
		
		result = result.flatten();
		
		return new ImageConverter(result.getProcessor());
	}
	
	
	

	
	
	/**
	 * Given a stack, make an RGB greyscale image from the given stack
	 * @param stack
	 * @return a new ImagePlus, or null if the stack was null
	 */
	private ImageConverter makeGreyRGBImage(int stackNumber){
		
		if(st!=null){
			ImagePlus[] images = new ImagePlus[3];
			images[Constants.RGB_RED  ] = new ImagePlus("red",   st.getProcessor(stackNumber));  
			images[Constants.RGB_GREEN] = new ImagePlus("green", st.getProcessor(stackNumber));  
			images[Constants.RGB_BLUE ] = new ImagePlus("blue",  st.getProcessor(stackNumber));      

			ImagePlus result = RGBStackMerge.mergeChannels(images, false); 
			result = result.flatten();
			
			return new ImageConverter(result.getProcessor());
		} else {
			return this;
		}
	}
	
	
	/**
	 * Convert a single-plane stack to RGB.
	 * @param stack
	 * @return the image
	 */
	private ImageConverter makeGreyScaleIamge(){
		int w = st.getWidth();
		int h = st.getHeight();
		
		byte[] blank = new byte[ w * h ];

		ImagePlus[] images = new ImagePlus[3];
		images[Constants.RGB_RED]   = new ImagePlus("red",   new ByteProcessor(w, h, blank));  
		images[Constants.RGB_GREEN] = new ImagePlus("green", new ByteProcessor(w, h, blank));
		images[Constants.RGB_BLUE]  = new ImagePlus("blue", st.getProcessor(Constants.COUNTERSTAIN));      

		ImagePlus result = RGBStackMerge.mergeChannels(images, false); 

		result = result.flatten();
		return new ImageConverter(result.getProcessor());
	}

	
	/**
	 * Convert a multi-plane stack to RGB. Only the first two signal channels are used. 
	 * @param stack the stack
	 * @return an RGB image
	 */
	private ImageConverter mergeStack(){

		ImagePlus[] images = new ImagePlus[3];
		images[Constants.RGB_RED]   = new ImagePlus("red",   st.getProcessor(Constants.FIRST_SIGNAL_CHANNEL));  
		images[Constants.RGB_GREEN] = new ImagePlus("green", st.getProcessor(3));  
		images[Constants.RGB_BLUE]  = new ImagePlus("blue",  st.getProcessor(Constants.COUNTERSTAIN));      

		ImagePlus result = RGBStackMerge.mergeChannels(images, false); 
		
		result = result.flatten();
		return new ImageConverter(result.getProcessor());
	}
	
}     
