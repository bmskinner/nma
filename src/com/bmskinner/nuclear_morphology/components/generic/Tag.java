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
package com.bmskinner.nuclear_morphology.components.generic;

import java.io.Serializable;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.BorderTag.BorderTagType;

/**
 * This interface accesses the tagged points around the periphery of an object.
 * Default tags are provided.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface Tag extends Comparable<Tag>, Serializable {

    @NonNull public static final Tag REFERENCE_POINT    = new BorderTagObject(BorderTag.REFERENCE_POINT);
    @NonNull public static final Tag ORIENTATION_POINT  = new BorderTagObject(BorderTag.ORIENTATION_POINT);
    @NonNull public static final Tag TOP_VERTICAL       = new BorderTagObject(BorderTag.TOP_VERTICAL);
    @NonNull public static final Tag BOTTOM_VERTICAL    = new BorderTagObject(BorderTag.BOTTOM_VERTICAL);
    @NonNull public static final Tag INTERSECTION_POINT = new BorderTagObject(BorderTag.INTERSECTION_POINT);
    @NonNull public static final Tag CUSTOM_POINT       = new BorderTagObject(BorderTag.CUSTOM);

    
    /**
     * Create a tag with the given name
     * @param name the name of the tag. Must be one of {@link BorderTag#values()}
     * @return
     */
    static Tag of(String name) {
    	for(BorderTag t : BorderTag.values()) {
    		if(t.toString().equals(name))
    			return new BorderTagObject(t);
    	}
    	return null;
    }
    
    /**
     * Get the name of the tag
     * 
     * @return
     */
    String getName();

    /**
     * Get the underlying tag
     * 
     * @return
     */
    BorderTag getTag();

    /**
     * Get the tag type
     * 
     * @return
     */
    BorderTagType type();

}
