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

package com.bmskinner.nuclear_morphology.components;

import ij.gui.Roi;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * A factory to create cytoplasms
 * @author bms41
 * @since 1.13.4
 *
 */
public class CytoplasmFactory implements ComponentFactory<ICytoplasm> {

//	private final File file;
	
//	/**
//	 * Create a factory for nuclei of the given type
//	 * @param imageFile
//	 * @param nucleusType
//	 */
//	public CytoplasmFactory(File imageFile){
//		
//		
//		file = imageFile;
//
//	}
	
	
	@Override
	public ICytoplasm buildInstance(Roi roi, File imageFile, int channel,
			int[] originalPosition, IPoint centreOfMass)
			throws ComponentCreationException {
		
		if(imageFile==null){
			throw new IllegalArgumentException("File cannot be null in factory");
		}
		
		ICytoplasm result = new DefaultCytoplasm(roi, centreOfMass, imageFile, channel, originalPosition);
		return result;
	}
	
	

}
