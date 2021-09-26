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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.profiles.BorderTagObject.BorderTagType;

/**
 * This interface accesses the tagged points around the periphery of an object.
 * Default tags are provided.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface Tag extends Comparable<Tag>, Serializable {

    @NonNull public static final Tag REFERENCE_POINT    = new BorderTagObject("Reference point", BorderTagType.CORE);
    @NonNull public static final Tag ORIENTATION_POINT  = new BorderTagObject("Orientation point", BorderTagType.EXTENDED);
    @NonNull public static final Tag TOP_VERTICAL       = new BorderTagObject("Top vertical", BorderTagType.EXTENDED);
    @NonNull public static final Tag BOTTOM_VERTICAL    = new BorderTagObject("Bottom vertical", BorderTagType.EXTENDED);
    @NonNull public static final Tag INTERSECTION_POINT = new BorderTagObject("Intersection", BorderTagType.EXTENDED);
    @NonNull public static final Tag CUSTOM_POINT       = new BorderTagObject("Custom", BorderTagType.EXTENDED);

    
    /**
     * Create a tag with the given name
     * @param name the name of the tag.
     * @return a tag with the given  name
     */
    static Tag of(String name) {
    	return new DefaultTag(name);
    }
    
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
    BorderTagType type();

}
