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
package com.bmskinner.nuclear_morphology.components.nuclear;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

/**
 * The methods available to a nuclear signal, which is a type
 * of cellular component
 * @author ben
 * @since 1.13.3
 *
 */
public interface INuclearSignal 
	extends CellularComponent {

	/**
	 * Get the index of the closest point in the nuclear 
	 * periphery to this signal
	 * @return
	 */
	int getClosestBorderPoint();

	/**
	 * Set the index of the closest point in the nuclear 
	 * periphery to this signal
	 * @return
	 */
	void setClosestBorderPoint(int p);

	INuclearSignal duplicate();
	
	/**
	 * Translate the border and centre of mass of the signal
	 * to lie within the specified component. The signal must originate
	 * from an image with the same dimensions as the component.
	 * @param c the component to place the signal within
	 */
//	void setPositionWithin(CellularComponent c);

}