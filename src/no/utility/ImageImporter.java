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
	
	public static final int COUNTERSTAIN = 1; // ImageStack slices are numbered from 1; first slice is blue
	public static final int FIRST_SIGNAL_CHANNEL = 2; // ImageStack slices are numbered from 1; first slice is blue
	
	public static final int RGB_RED = 0;
	public static final int RGB_GREEN = 1;
	public static final int RGB_BLUE = 2;
	
	private static int[] imageTypesProcessed = { ImagePlus.GRAY8, ImagePlus.COLOR_RGB };
			
	/**
	 * Create an ImageStack from the input image
	 * @param image the image to be converted to a stack
	 * @return the stack with countertain in index 0
	 */
	public static ImageStack convert(ImagePlus image){
		if(image==null){
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
			throw new IllegalArgumentException("Cannot handle image type: "+image.getType());
		}
				
		// do the conversions
		ImageStack result = null;
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
	    
	    result.addSlice("counterstain", channels[ImageImporter.RGB_BLUE].getProcessor());
	    result.addSlice(channels[ImageImporter.RGB_RED].getProcessor());
	    result.addSlice(channels[ImageImporter.RGB_GREEN].getProcessor());
	    result.deleteSlice(1); // remove the blank first slice
//	    ImagePlus demo = new ImagePlus(null, result);
//	    
//	    demo.show();
	    return result;
	}
}
