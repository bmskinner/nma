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
		
		ImagePlus image = mergeStack(stack);
		return image;
	}
	
	private static ImagePlus mergeStack(ImageStack stack){
		ImagePlus result = null;
		if(stack.getSize()==1){
			IJ.log("Single channel");
			result = new ImagePlus(null, stack.getProcessor(1));
		}
		if(stack.getSize()>1){ // ignore signals above 3 for now
			IJ.log("Multi channel");
			ImagePlus[] images = new ImagePlus[3];
			images[0] = new ImagePlus(null, stack.getProcessor(2));  
			images[1] = new ImagePlus(null, stack.getProcessor(3));  
			images[2] = new ImagePlus(null, stack.getProcessor(ImageImporter.COUNTERSTAIN));      

			result = RGBStackMerge.mergeChannels(images, false); 
			result.flatten();
		}
		return result;
	}
}
