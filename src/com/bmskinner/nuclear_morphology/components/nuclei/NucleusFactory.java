/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.components.nuclei;

import ij.gui.Roi;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

/**
 * Constructs nuclei for an image. Tracks the number of nuclei created.
 * @author ben
 *
 */
public class NucleusFactory implements ComponentFactory<Nucleus> {

	private int nucleusCount = 0; // store the number of nuclei created by this factory
	private final NucleusType type;
	
	/**
	 * Create a factory for nuclei of the given type
	 * @param imageFile
	 * @param nucleusType
	 */
	public NucleusFactory(NucleusType nucleusType){
		
		if(nucleusType==null){
			throw new IllegalArgumentException("Type cannot be null in nucleus factory");
		}
		type = nucleusType;
	}
	
	@Override
	public Nucleus buildInstance(Roi roi,
			File imageFile,
			int channel, 
			int[] originalPosition, 
			IPoint centreOfMass) throws ComponentCreationException {
		
		if(roi==null || centreOfMass==null){
			throw new IllegalArgumentException("Argument cannot be null in nucleus factory");
		}
		
		Nucleus n = null;
		
		try {

			  // The classes for the constructor
			  Class<?>[] classes = { 
					  Roi.class, 
					  IPoint.class, 
					  File.class, 
					  int.class, 
					  int[].class, 
					  int.class 
			  };
			  
			  Constructor<?> nucleusConstructor = type.getNucleusClass()
						  .getConstructor(classes);

				n = (Nucleus) nucleusConstructor.newInstance(roi,
						  centreOfMass, 
						  imageFile, 
						  channel, 
						  originalPosition,
						  nucleusCount);
				
				nucleusCount++;

		} catch (InvocationTargetException e) {
			stack("Invokation error creating nucleus", e.getCause());
			throw new ComponentCreationException("Error making nucleus:" +e.getMessage(), e);
		} catch(Error e){
			stack("Error creating nucleus", e);
			throw new ComponentCreationException("Error making nucleus:" +e.getMessage(), e);
		} catch (InstantiationException | IllegalAccessException |
				IllegalArgumentException | NoSuchMethodException | SecurityException e) {
			stack("Error creating nucleus", e);
			throw new ComponentCreationException("Error making nucleus:" +e.getMessage(), e);
		}
			  

		if(n==null){
			throw new ComponentCreationException("Error making nucleus");
		}
		  return n;
	}

}
