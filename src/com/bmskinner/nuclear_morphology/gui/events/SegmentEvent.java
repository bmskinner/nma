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


package com.bmskinner.nuclear_morphology.gui.events;

import java.util.EventObject;
import java.util.UUID;

@SuppressWarnings("serial")
public class SegmentEvent extends EventObject {

    private UUID id;
    private int  index;
    private int  type;

    public static final int MOVE_START_INDEX = 0;

    /**
     * Create an event from a source, with the given message
     * 
     * @param source
     *            the source of the datasets
     * @param message
     *            the instruction on what to do with the datasets
     * @param sourceName
     *            the name of the object or component generating the datasets
     * @param list
     *            the datasets to carry
     */
    public SegmentEvent(Object source, UUID id, int index, int type) {
        super(source);
        this.id = id;
        this.index = index;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public int getIndex() {
        return index;
    }

    /**
     * The type of action to take - see the static ints of SegmentEvent
     * 
     * @return
     */
    public int getType() {
        return type;
    }

}
