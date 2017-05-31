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

package com.bmskinner.nuclear_morphology.analysis.image;

import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.ImageCalculator;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.TypeConverter;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.util.List;

import javax.swing.ImageIcon;

import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Contains methods for manipulating ImageProcessors, and
 * provides conversion between ImageStacks and ImageIcons
 * for use in the UI. 
 * @author ben
 *
 */
public abstract class AbstractImageFilterer implements Loggable {
	
	private static final int RGB_WHITE = 16777215;
	protected ImageProcessor ip = null;
	protected ImageStack     st = null;
	
	/**
	 * Construct with an image processor
	 * @param ip the image processor
	 */
	public AbstractImageFilterer(final ImageProcessor ip){
		this.ip = ip;
	}
	
	/**
	 * Construct with an image processor from an image
	 * @param img the image
	 */
	public AbstractImageFilterer(final ImagePlus img){
		this(img.getProcessor());
	}
	
	/**
	 * Construct from a stack
	 * @param st the image stack
	 */
	public AbstractImageFilterer(final ImageStack st){
		this.st = st;
	}
	
	/**
	 * Duplicate the filterer - use the template processor and stack
	 * @param f the template filterer
	 */
	public AbstractImageFilterer(final AbstractImageFilterer f){
		this.ip = f.ip;
		this.st = f.st;
	}
	
	/**
	 * Create an annotator containing this image
	 * @return
	 */
	public ImageAnnotator toAnnotator(){
		return new ImageAnnotator(ip);
	}
	
	/**
	 * Create a converter containing this image
	 * @return
	 */
	public ImageConverter toConverter(){
		return new ImageConverter(ip);
	}
	
	/**
	 * Create a converter containing this image
	 * @return
	 */
	public ImageFilterer toFilterer(){
		return new ImageFilterer(ip);
	}
	
	/**
	 * Get the current image processor
	 * @return
	 */
	public ImageProcessor toProcessor(){
		if(ip==null){
			throw new NullPointerException("Filterer does not contain an image processor");
		}
		return ip;
	}
	
	/**
	 * Test if the image is a 32-bit RGB processor
	 * @return
	 */
	public boolean isColorProcessor(){
		return ip instanceof ColorProcessor;
	}
	
	/**
	 * Test if the image is an 8-bit processor
	 * @return
	 */
	public boolean isByteProcessor(){
		return ip instanceof ByteProcessor;
	}
	
	/**
	 * Test if the image is a 16-bit unsigned processor
	 * @return
	 */
	public boolean isShortProcessor(){
		return ip instanceof ShortProcessor;
	}
	
	/**
	 * Invert the processor
	 * @return
	 */
	public AbstractImageFilterer invert(){
		ip.invert();
		return this;
	}
	
	/**
	 * Convert the processor into a ByteProcessor. Has no effect
	 * if the processor is already a ByteProcessor
	 * @return
	 */
	public AbstractImageFilterer convertToByteProcessor(){
		if( ! isByteProcessor()){
			TypeConverter tc = new TypeConverter(ip, false);
			ip = tc.convertToByte();
		}
		return this;
	}
	
	/**
	 * Convert the processor into a ShortProcessor (16-bit unsigned). Has no effect
	 * if the processor is already a ShortProcessor
	 * @return
	 */
	public AbstractImageFilterer convertToShortProcessor(){
		if(! isShortProcessor()){
			TypeConverter tc = new TypeConverter(ip, false);
			ip = tc.convertToShort();
		}
		return this;
	}
	
	/**
	 * Convert the processor into a ColorProcessor. Has no effect
	 * if the processor is already a ColorProcessor
	 * @return
	 */
	public AbstractImageFilterer convertToColorProcessor(){
		if(!isColorProcessor()){


			TypeConverter tc = new TypeConverter(ip, false);
			ip = tc.convertToRGB();
		}
		return this;
	}
	
	/**
	 * Get the current image stack
	 * @return
	 */
	public ImageStack toStack(){
		if(st==null){
			throw new NullPointerException("Filterer does not contain an image stack");
		}
		return st;
	}
	
	/**
	 * Create an image icon from the current processor
	 * @return
	 */
	public ImageIcon toImageIcon(){
		if(ip==null){
			throw new NullPointerException("Filterer does not contain an image processor");
		}
		return new ImageIcon( ip.getBufferedImage() );
	}
	
	/**
	 * Create an empty white byte processor
	 * @param w the width
	 * @param h the height
	 * @return
	 */
	public static ImageProcessor createBlankByteProcessor(int w, int h){
		
		// Create an empty white processor
		ImageProcessor ip = new ByteProcessor(w, h);
		for(int i=0; i<ip.getPixelCount(); i++){
			ip.set(i, 255); // set all to white initially
		}
		
		return ip;
	}
	
	/**
	 * Recolour the given 8-bit image to use the given colour, weighting the
	 * greyscale values by the HSB saturation level
	 * @param ip
	 * @param colour
	 * @return a colour processor with the recoloured values
	 */
	public static ImageProcessor recolorImage(ImageProcessor ip, Color colour){
		
		float[] hsb = Color.RGBtoHSB(colour.getRed(), colour.getGreen(), colour.getBlue(), null);
		
		// Scale the brightness from 0-bri across the image
		ColorProcessor cp = new ColorProcessor(ip.getWidth(), ip.getHeight());
		
		for(int i=0; i<ip.getPixelCount(); i++){
			int pixel = ip.get(i);
			
			if(pixel==255){ // skip fully white pixels
				int full = RGB_WHITE;
				cp.set(i, full);
			} else {
				float pct = getSaturationFromIntensity(pixel);
				
				// The saturation is scaled with percent - lower values give whiter image 
				// The value is scaled with percent - lower values give whiter image
				//TODO: check calculations
				float vScale = 1f - hsb[2]; // range for value scaling between 0 and 1
				float v = (vScale*pct) + hsb[2]; // hsb[2] can only be increased with intensity
				
				pct = 1f-pct;

				int full = Color.HSBtoRGB(hsb[0], pct, 1); // if issues, replace 1 with the hsb[2] - for now it keeps the white border
				cp.set(i, full);
			}
			
			
		}

		return cp;
	}
	
	/**
	 * Adjust the intensity of the given image so that the brightest pixel is at 255
	 * and the dimmest pixel is at 0
	 * @param ip the image to adjust
	 * @return a new ByteProcessor with rescaled values
	 */
	public static ImageProcessor rescaleImageIntensity(final ImageProcessor ip){
		
		if(ip==null){
			throw new IllegalArgumentException("Image cannot be null");
		}
		
		double maxIntensity = 0;
		double minIntensity = 255;
		ImageProcessor result = null;
		
		if(ip instanceof ByteProcessor){
			maxIntensity = 0;
			minIntensity = 255;
			result = new ByteProcessor(ip.getWidth(), ip.getHeight());
		}
		
		if(ip instanceof FloatProcessor){
			maxIntensity = 0;
			minIntensity = Float.MAX_VALUE;
			result = new FloatProcessor(ip.getWidth(), ip.getHeight());
		}
		
		
		if(ip instanceof ColorProcessor){
			maxIntensity = 0;
			minIntensity = 255;
			result = new ColorProcessor(ip.getWidth(), ip.getHeight());
		}
		
		if(result==null){
			throw new IllegalArgumentException("Unsupported image type: "+ip.getClass().getSimpleName());
		}
		
		// Find the range in the image	
		
		
		for(int i=0; i<ip.getPixelCount(); i++){
			int pixel = ip.get(i);
			maxIntensity = pixel > maxIntensity ? pixel : maxIntensity;
			minIntensity = pixel < minIntensity ? pixel : minIntensity;
		}
		
		if(maxIntensity==0){
			return ip;
		}
		
		double range        = maxIntensity - minIntensity;
		
		// Adjust each pixel to the proportion in range 0-255
		for(int i=0; i<ip.getPixelCount(); i++){
			int pixel = ip.get(i);

			double proportion = ( (double) pixel - minIntensity) / range;
			
			int newPixel  = (int) (255 * proportion);
			result.set(i, newPixel);
		}
		return result;
	}
	
	/**
	 * Create a new 8-bit image processor with the average of all the non-null 8-bit images
	 * in the given list
	 * @return
	 */
	public static ImageProcessor averageByteImages(List<ImageProcessor> list){
		
		if(list==null || list.isEmpty()){
			throw new IllegalArgumentException("List null or empty");
		}
		
		// Check images are same dimensions
		int w = list.get(0).getWidth();
		int h = list.get(0).getHeight();
		
		for(ImageProcessor ip : list){
			if(ip==null){
				continue;
			}
			if(w!=ip.getWidth() || h!=ip.getHeight()){
				throw new IllegalArgumentException("Dimensions do not match");
			}
		}
		// Create an empty white processor of the correct dimensions
		ImageProcessor mergeProcessor = ImageFilterer.createBlankByteProcessor(w, h);
		
		int nonNull = 0;
		
		// check sizes match
		for(ImageProcessor ip : list){
			if(ip==null){
				continue;
			}
			nonNull++;
		}
		

		
		if(nonNull==0){
			return mergeProcessor;
		}
		
		// Average the pixels
		for(int x=0; x<w; x++){
			for(int y=0; y<h; y++){

				int pixelTotal = 0;
				for(ImageProcessor ip : list){
					if(ip==null){
						continue;
					}
					pixelTotal += ip.get(x, y);
				}
				
				pixelTotal /= nonNull; // scale back down to 0-255;
				
				if(pixelTotal<255){// Ignore anything that is not signal - the background is already white
					mergeProcessor.set(x, y, pixelTotal);
				} 
			}
		}
		return mergeProcessor;
	}
	
	/**
	 * Calculate a measure of the colocalisation of values in the given images. ImageA
	 * is coloured red, imageB is coloured blue, and regions of colocalisation will be purple
	 * @param imageA the first image
	 * @param imageB the second image
	 * @return an image showing colocalisation of pixels
	 */
	public static ImageProcessor cowarpalise(ImageProcessor imageA, ImageProcessor imageB){
		
		if(imageA==null || imageB==null){
			throw new IllegalArgumentException("Image(s) null");
		}
		
		// Check images are same dimensions	
		if(imageA.getWidth()!=imageB.getWidth() || imageA.getHeight()!=imageB.getHeight()){
			throw new IllegalArgumentException("Dimensions do not match");
		}
		
		// Set the saturation scaled by intensity
		
		ImageProcessor cp = new ColorProcessor(imageA.getWidth(), imageA.getHeight());
		
		for(int i=0; i<imageA.getPixelCount(); i++){
			int r = imageA.get(i);
			int b = imageB.get(i);

			
			if(r==255 && b==255){
				cp.set(i, rgbToInt(255, 255, 255));
				continue;
			}
			
			float diff = (float)(r-b);
			float scaled = Math.abs(diff) / 255f; // fraction of 8bit space
			float ranged = 0.17f * scaled;
			
			// Scale to fit in hue range 240-360
			
			// Needs to be a fractional number that can be multiplied by 360
			// Therefore range is 0.66-1
			float h = diff<0 ? 0.83f - ranged : 0.83f + ranged;
//			float h = 0.83f + diff; // start at purple, variation of up to 128
//			float h = 300f + diff; // start at purple, variation of up to 128
			

			float s = diff < 0 ? 1-((float)b / 255f) : 1-((float)r / 255f);


			
			
			float v = 1f;
			
//			System.out.println("Chosen colour "+h+" - "+s+" - "+v);
			
			int rgb = Color.HSBtoRGB(h, s, v);
			cp.set(i, rgb);

			
		}
		
		return cp;
		
	}
	
	/**
	 * Combine individual channel data to an int
	 * @param r
	 * @param g
	 * @param b
	 * @return
	 */
	protected static int rgbToInt(int r, int g, int b){
		int rgb = r;
		rgb = (rgb << 8) + g;
		rgb = (rgb << 8) + b;
		return rgb;
	}
	
	/**
	 * Express the given pixel intensity as a fraction of 255
	 * @param i the pixel value
	 * @return a value from 0-1
	 */
	protected static float getSaturationFromIntensity(int i){
		return (float)(255f - (255f-i )) / 255f;
	}
	
	/**
	 * Merge the given list of images by averaging the RGB values
	 * @param list
	 * @return a new colour processor with the averaged values
	 */
	public static ImageProcessor averageRGBImages(List<ImageProcessor> list){
		
		if(list==null || list.isEmpty()){
			throw new IllegalArgumentException("List null or empty");
		}
				
		
		// Check images are same dimensions
		int w = list.get(0).getWidth();
		int h = list.get(0).getHeight();
		
		for(ImageProcessor ip : list){
			
			if(ip==null){
				throw new IllegalArgumentException("Null image in list");
			}
			
			if(w!=ip.getWidth() || h!=ip.getHeight()){
				throw new IllegalArgumentException("Dimensions do not match");
			}
		}
		
		ImageProcessor cp = new ColorProcessor(w, h);
				
		// Average the colours at each pixel
		// White counts as empty here
		for(int i=0; i<w*h; i++){
			
//			int rt=0, gt=0, bt=0; // count of images with pixel value
			int r=0, g=0, b=0; // total pixel values
			
			for(ImageProcessor ip : list){
				int pixel = ip.get(i);
				
				
				if(ip instanceof ColorProcessor){
					// pixel values for this image
					int rp = (pixel >> 16) & 0xFF;
					int gp = (pixel >> 8) & 0xFF;
					int bp = pixel & 0xFF; 
					
//					rt = rp<255 ? rt+1 : rt;
//					gt = gp<255 ? gt+1 : gt;
//					bt = bp<255 ? bt+1 : bt;
					
					r +=rp;
					g +=gp;
					b +=bp;
					
				} else {
					r+=pixel;
					g+=pixel;
					b+=pixel;
//					rt++;
//					gt++;
//					bt++;
				}
				
				
				
			}

//			r/=rt;
//			g/=gt;
//			b/=bt;
			
			r/=list.size();
			g/=list.size();
			b/=list.size();
				
			int rgb = rgbToInt(r, 255, b);
			cp.set(i, rgb);
			
		}
		
		return cp;
	}
	
	
	
}
