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

import javax.swing.ImageIcon;

import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public abstract class AbstractImageFilterer implements Loggable {
	
	protected ImageProcessor ip = null;
	protected ImageStack     st = null;
	
	public AbstractImageFilterer(final ImageProcessor ip){
		this.ip = ip;
	}
	
	public AbstractImageFilterer(final ImagePlus img){
		this.ip = img.getProcessor();
	}
	
	public AbstractImageFilterer(final ImageStack st){
		this.st = st;
	}
	
	public AbstractImageFilterer(AbstractImageFilterer f){
		this.ip = f.ip;
		this.st = f.st;
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
	
	
	
}
