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

package com.bmskinner.nuclear_morphology.components.generic;

/**
 * Thrown when a profile collection or segmented profile has no assigned
 * segments
 * @author bms41
 * @since 1.13.3
 *
 */
public class UnsegmentedProfileException extends Exception {
		private static final long serialVersionUID = 1L;
		public UnsegmentedProfileException() { super(); }
		public UnsegmentedProfileException(String message) { super(message); }
		public UnsegmentedProfileException(String message, Throwable cause) { super(message, cause); }
		public UnsegmentedProfileException(Throwable cause) { super(cause); }
	
}
