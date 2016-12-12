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
	
	public ImageProcessor toProcessor(){
		return ip;
	}
	
	public ImageStack toStack(){
		return st;
	}
	
	public ImageIcon toImageIcon(){
		return new ImageIcon( ip.getBufferedImage() );
	}
	
	
	
}
