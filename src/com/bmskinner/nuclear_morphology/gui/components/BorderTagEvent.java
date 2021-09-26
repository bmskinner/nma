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
package com.bmskinner.nuclear_morphology.gui.components;

import java.util.EventObject;

import com.bmskinner.nuclear_morphology.components.profiles.Tag;

/**
 * Border tag events indicate a change to the position of a border tag in a
 * profile. They are sent from detail panels.
 * 
 * @author ben
 * @since 1.13.2
 *
 */
@SuppressWarnings("serial")
public class BorderTagEvent extends EventObject {

    private Tag tag;
    private int index;

    /**
     * Create an event from a source, with the given message
     * 
     * @param source the source of the datasets
     * @param tag the affected border tag
     * @param index the affected index
     */
    public BorderTagEvent(Object source, Tag tag, int index) {
        super(source);
        this.tag = tag;
        this.index = index;
    }

    public Tag getTag() {
        return tag;
    }

    public int getIndex() {
        return index;
    }

}
