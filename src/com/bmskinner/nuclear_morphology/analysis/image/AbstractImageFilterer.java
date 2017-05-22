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
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import ij.process.TypeConverter;

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
	
}
