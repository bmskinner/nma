/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.components.profiles;

import java.io.Serializable;

/**
 * This interface accesses the tagged points around the periphery of an object.
 * Default tags are provided.
 * 
 * @author Ben Skinner
 * @since 1.13.3
 *
 */
public interface Landmark extends Comparable<Landmark>, Serializable {

	/**
	 * Get the name of the landmark
	 * 
	 * @return
	 */
	String getName();

}
