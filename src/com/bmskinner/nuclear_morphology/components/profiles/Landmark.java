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
package com.bmskinner.nuclear_morphology.components.profiles;

import java.io.Serializable;

/**
 * This interface accesses the tagged points around the periphery of an object.
 * Default tags are provided.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface Landmark extends Comparable<Landmark>, Serializable {

//    @NonNull Landmark REFERENCE_POINT    = new DefaultLandmark("Reference point", LandmarkType.CORE);
//    @NonNull Landmark ORIENTATION_POINT  = new DefaultLandmark("Orientation point", LandmarkType.EXTENDED);
//    @NonNull Landmark TOP_VERTICAL       = new DefaultLandmark("Top vertical", LandmarkType.EXTENDED);
//    @NonNull Landmark BOTTOM_VERTICAL    = new DefaultLandmark("Bottom vertical", LandmarkType.EXTENDED);
//    
//    @NonNull Landmark LEFT_HORIZONTAL    = new DefaultLandmark("Left horizontal", LandmarkType.EXTENDED);
//    @NonNull Landmark RIGHT_HORIZONTAL   = new DefaultLandmark("Right horizontal", LandmarkType.EXTENDED);

	/**
	 * Create a tag with the given name
	 * 
	 * @param name the name of the tag.
	 * @return a tag with the given name
	 */
//	static Landmark of(@NonNull String name, @NonNull LandmarkType type) {
//		return new DefaultLandmark(name, type);
//	}

//	static Landmark of(@NonNull String name) {
//		return new DefaultLandmark(name);
//	}

	/**
	 * Get the default built-in landmark types
	 * 
	 * @return
	 */
//	static Landmark[] defaultValues() {
//		return new Landmark[] { REFERENCE_POINT, ORIENTATION_POINT, TOP_VERTICAL, BOTTOM_VERTICAL, LEFT_HORIZONTAL,
//				RIGHT_HORIZONTAL };
//	}

	/**
	 * Get the name of the tag
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Get the tag type
	 * 
	 * @return
	 */
//	LandmarkType type();

}
