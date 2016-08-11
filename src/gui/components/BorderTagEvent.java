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

package gui.components;

import java.util.EventObject;

import components.generic.BorderTagObject;

@SuppressWarnings("serial")
public class BorderTagEvent extends EventObject {

	private BorderTagObject tag;

	/**
	 * Create an event from a source, with the given message
	 * @param source the source of the datasets
	 * @param message the instruction on what to do with the datasets
	 * @param sourceName the name of the object or component generating the datasets
	 * @param list the datasets to carry
	 */
	public BorderTagEvent( Object source, BorderTagObject tag ) {
		super( source );
		this.tag = tag;
	}

	public BorderTagObject getTag() {
		return tag;
	}
	
	

}
