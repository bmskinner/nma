package no.export;

import ij.ImagePlus;
import ij.ImageStack;

/**
 * This handles the flattening of the image stacks used
 * internally by a Nucleus into an RGB image.
 */
public class ImageExporter {
	
			
	public static ImagePlus convert(ImageStack stack){
		
		if(stack==null){
			throw new IllegalArgumentException("Stack is null");
		}
		
		ImagePlus image = new ImagePlus(null, stack);
		return image.flatten();
	}
}
