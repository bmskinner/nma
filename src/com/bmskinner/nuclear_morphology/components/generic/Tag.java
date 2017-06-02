/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.generic;

import java.io.Serializable;

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

    public static final BorderTagObject REFERENCE_POINT    = new BorderTagObject(BorderTag.REFERENCE_POINT);
    public static final BorderTagObject ORIENTATION_POINT  = new BorderTagObject(BorderTag.ORIENTATION_POINT);
    public static final BorderTagObject TOP_VERTICAL       = new BorderTagObject(BorderTag.TOP_VERTICAL);
    public static final BorderTagObject BOTTOM_VERTICAL    = new BorderTagObject(BorderTag.BOTTOM_VERTICAL);
    public static final BorderTagObject INTERSECTION_POINT = new BorderTagObject(BorderTag.INTERSECTION_POINT);
    public static final BorderTagObject CUSTOM_POINT       = new BorderTagObject(BorderTag.CUSTOM);

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
