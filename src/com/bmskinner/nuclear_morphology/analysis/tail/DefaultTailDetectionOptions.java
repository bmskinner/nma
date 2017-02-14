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

package com.bmskinner.nuclear_morphology.analysis.tail;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.options.AbstractDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

/**
 * Early implementation of tail detection options. Use a hash version instead.
 * @author bms41
 * @deprecated since 1.13.4
 *
 */
@Deprecated
public class DefaultTailDetectionOptions
	extends AbstractDetectionOptions {

	private static final long serialVersionUID = 1L;

	public DefaultTailDetectionOptions(File folder){
		super(folder);
	}
	
	protected DefaultTailDetectionOptions(DefaultTailDetectionOptions template) {
		super(template);
	}

	@Override
	public IMutableDetectionOptions duplicate() {
		// TODO Auto-generated method stub
		return new DefaultTailDetectionOptions(this);
	}

	@Override
	public IDetectionOptions lock() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public void setHoughOptions(IHoughDetectionOptions hough) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IMutableDetectionOptions unlock() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public boolean isUseHoughTransform() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public IDetectionSubOptions getSubOptions(String s) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSubOptions(String s, IDetectionSubOptions sub) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRGB(boolean b) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isRGB() {
		// TODO Auto-generated method stub
		return false;
	}
	
	

}
