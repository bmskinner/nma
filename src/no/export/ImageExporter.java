package no.export;

import utility.Constants;
import no.imports.ImageImporter;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;
import ij.process.ByteProcessor;

/**
 * This class handles the flattening of the image stacks used
 * internally by a Nucleus into an RGB image.
 * @author Ben Skinner
 *
 */
public class ImageExporter {
	
			
	/**
	 * Convert an ImageStack to RGB images suitable for export
	 * @param stack the stack to convert
	 * @return an image
	 */
	public static ImagePlus convert(ImageStack stack){
		
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
	 * Convert a single-plane stack to RGB.
	 * @param stack
	 * @return the image
	 */
	private static ImagePlus makeGreyScaleIamge(ImageStack stack){

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
	private static ImagePlus mergeStack(ImageStack stack){

		ImagePlus[] images = new ImagePlus[3];
		images[Constants.RGB_RED]   = new ImagePlus("red", stack.getProcessor(Constants.FIRST_SIGNAL_CHANNEL));  
		images[Constants.RGB_GREEN] = new ImagePlus("green", stack.getProcessor(3));  
		images[Constants.RGB_BLUE]  = new ImagePlus("blue", stack.getProcessor(Constants.COUNTERSTAIN));      

		ImagePlus result = RGBStackMerge.mergeChannels(images, false); 
		
		result = result.flatten();
		return result;
	}
}     
