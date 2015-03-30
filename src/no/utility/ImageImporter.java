package no.utility;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ChannelSplitter;

/**
 * This class takes any given input image, and will convert it
 * to the ImageStack needed for the analyses. The DNA/DAPI will
 * always be set at index 0, with other signals appended 
 *
 */
public class ImageImporter {
		
	public ImageImporter(){
	}
	
	public static ImageStack convert(ImagePlus image){
		if(image==null){
			throw new IllegalArgumentException("Input image is null");
		}
		ImageStack result = new ImageStack();
		if(image.getType()==ImagePlus.GRAY8){
			result = convertGreyscale(image);
		}
		
		if(image.getType()==ImagePlus.COLOR_RGB){
			result = convertRGB(image);
		}
		
		return result;
	}
	
	/**
	 * @param image the image to convert
	 * @return a stack with the input image as position 0
	 */
	private static ImageStack convertGreyscale(ImagePlus image){
		ImageStack result = new ImageStack();
	    result.addSlice("counterstain", image.getProcessor());
	    return result;
	}
	
	/**
	 * Given an RGB image, convert it to a stack, with the blue 
	 * channel first
	 * @param image the image to convert to a stack
	 * @return the stack
	 */
	private static ImageStack convertRGB(ImagePlus image){
		ImageStack result = new ImageStack();
		
		// split out colour channel
	    ImagePlus[] channels = ChannelSplitter.split(image);
	    
	    result.addSlice("counterstain", channels[2].getProcessor());
	    result.addSlice(channels[0].getProcessor());
	    result.addSlice(channels[1].getProcessor());
	    return result;
	}
}
