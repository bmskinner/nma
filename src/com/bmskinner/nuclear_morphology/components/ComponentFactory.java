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
package com.bmskinner.nuclear_morphology.components;

import ij.gui.Roi;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The interface for component factories
 * @author ben
 *
 * @param <E> the type of CellularComponent to be created
 */
public interface ComponentFactory<E extends CellularComponent> extends Loggable {
	
	/**
	 * Create a component of the appropriate class for the factory
	 * @param roi the roi to create a nucleus from
	 * @param channel the image channel
	 * @param originalPosition the position of the roi in the source image
	 * @param centreOfMass the centre of mass of the roi
	 * @return a component of the type for this factory
	 * @throws NucleusCreationException 
	 */
	E buildInstance(Roi roi,
			File file,
			int channel, 
			int[] originalPosition, 
			IPoint centreOfMass) throws ComponentCreationException;
	
	
	/**
	 * Thrown when a component cannot be created or initialised (includes
	 * when profile collection or segmented profile has no assigned
	 * segments)
	 * @author bms41
	 * @since 1.13.4
	 *
	 */
	static class ComponentCreationException extends Exception {
			private static final long serialVersionUID = 1L;
			public ComponentCreationException() { super(); }
			public ComponentCreationException(String message) { super(message); }
			public ComponentCreationException(String message, Throwable cause) { super(message, cause); }
			public ComponentCreationException(Throwable cause) { super(cause); }
		
	}

}
