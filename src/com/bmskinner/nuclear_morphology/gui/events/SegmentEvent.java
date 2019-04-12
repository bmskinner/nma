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
package com.bmskinner.nuclear_morphology.gui.events;

import java.util.EventObject;
import java.util.UUID;

/**
 * Sends update requests for segments
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SegmentEvent extends EventObject {

	public final UUID id;
    public final int  index;
    public final SegmentUpdateType  type;

    
    /**
     * The types of update that can be applied to a segment
     * @author bms41
     * @since 1.15.2
     *
     */
    public enum SegmentUpdateType {
    	MOVE_START_INDEX;
    }

    /**
     * Create an event from a source, with the given message
     * 
     * @param source the source of the datasets
     * @param id the the segment to be updated
     * @param index the new index to apply via the update type
     * @param type the type of segment update to perform
     */
    public SegmentEvent(final Object source, final UUID id, final int index, final SegmentUpdateType type) {
        super(source);
        this.id = id;
        this.index = index;
        this.type = type;
    }
}
