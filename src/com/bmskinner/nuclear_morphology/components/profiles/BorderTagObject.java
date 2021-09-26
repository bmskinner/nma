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

import java.util.ArrayList;
import java.util.List;

/**
 * Allow custom points of interest to be added to a nucleus.
 * 
 * @author bms41
 *
 */
public class BorderTagObject implements Tag {

    private static final long serialVersionUID = 1L;
        
    /**
     * The types of border tag that can be present
     * @author ben
     *
     */
    public enum BorderTagType {
    	
    	/** Core border tags are essential for the software to display or calculate profiles */
        CORE, 
        /** Extended border tags are optional, and can be added as needed */
        EXTENDED
    };

    private final String    name;
    private final BorderTagType type;

    public BorderTagObject(final String name, final BorderTagType type) {
        this.type = type;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BorderTagType type() {
        return type;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Tag[] values() {
        return new Tag[] { REFERENCE_POINT, 
        		ORIENTATION_POINT, 
        		TOP_VERTICAL, 
        		BOTTOM_VERTICAL,
                INTERSECTION_POINT };
    }

    public static BorderTagObject[] values(BorderTagType type) {

        List<Tag> list = new ArrayList<>();
        for (Tag o : values()) {
            if (o.type().equals(type)) {
                list.add(o);
            }
        }

        return list.toArray(new BorderTagObject[0]);

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BorderTagObject other = (BorderTagObject) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public int compareTo(Tag tag) {
        return name.compareTo(tag.getName());
    }

}
