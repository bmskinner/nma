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

import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.BorderTag.BorderTagType;

/**
 * This is a wrapper for the {@link BorderTag}, to allow custom points of
 * interest to be added to a nucleus. For these, use the BorderTag.CUSTOM tag,
 * and a unique name
 * 
 * @author bms41
 *
 */
public class BorderTagObject implements Tag {

    private static final long serialVersionUID = 1L;

    private final String    name;
    private final BorderTag tag;

    public BorderTagObject(final BorderTag tag) {
        this.tag = tag;
        this.name = tag.toString();
    }

    public BorderTagObject(final String name, final BorderTag tag) {
        this.tag = tag;
        this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.Tag#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.Tag#getTag()
     */
    @Override
    public BorderTag getTag() {
        return tag;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.Tag#type()
     */
    @Override
    public BorderTagType type() {
        return tag.type();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.Tag#toString()
     */
    @Override
    public String toString() {
        return name;
    }

    public static BorderTagObject[] values() {
        BorderTagObject[] result = { REFERENCE_POINT, 
        		ORIENTATION_POINT, 
        		TOP_VERTICAL, 
        		BOTTOM_VERTICAL,
                INTERSECTION_POINT };
        return result;
    }

    public static BorderTagObject[] values(BorderTagType type) {

        List<BorderTagObject> list = new ArrayList<BorderTagObject>();
        for (BorderTagObject o : values()) {
            if (o.tag.type().equals(type)) {
                list.add(o);
            }
        }

        return list.toArray(new BorderTagObject[0]);

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.Tag#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((tag == null) ? 0 : tag.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.Tag#equals(java.lang.Object)
     */
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
        if (tag != other.tag)
            return false;
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.generic.Tag#compareTo(components.generic.BorderTagObject)
     */
    @Override
    public int compareTo(Tag tag) {
        return name.compareTo(tag.getName());
    }

}
