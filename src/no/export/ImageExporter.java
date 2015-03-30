package no.export;

import no.utility.ImageImporter;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.RGBStackMerge;

/**
 * This handles the flattening of the image stacks used
 * internally by a Nucleus into an RGB image.
 */
public class ImageExporter {
	
			
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
	
	private static ImagePlus makeGreyScaleIamge(ImageStack stack){
		return  new ImagePlus(null, stack.getProcessor(1));
	}
	
	private static ImagePlus mergeStack(ImageStack stack){

		ImagePlus[] images = new ImagePlus[3];
		images[ImageImporter.RGB_RED]   = new ImagePlus("red", stack.getProcessor(ImageImporter.FIRST_SIGNAL_CHANNEL));  
		images[ImageImporter.RGB_GREEN] = new ImagePlus("green", stack.getProcessor(3));  
		images[ImageImporter.RGB_BLUE]  = new ImagePlus("blue", stack.getProcessor(ImageImporter.COUNTERSTAIN));      

		ImagePlus result = RGBStackMerge.mergeChannels(images, false); 
		
		result = result.flatten();
		return result;
	}
}     
