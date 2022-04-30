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
package com.bmskinner.nma.analysis;

import java.util.EventObject;

/**
 * ProgressEvents are used to signal a JProgressBar should be incremented. They
 * are used in RecursiveActions and ForkJoinTasks, where a direct signal to an
 * encompassing ProgressableAction is not possible.
 * 
 * @author bms41
 * @since 1.12.0
 *
 */
@SuppressWarnings("serial")
public class ProgressEvent extends EventObject {

    public static final int NO_MESSAGE         = 0;
    public static final int SET_TOTAL_PROGRESS = 1;
    public static final int SET_INDETERMINATE  = 2;
    public static final int INCREASE_BY_VALUE  = 3;
    
    private int message = NO_MESSAGE;
    private long value = 0;

    /**
     * Create an event from a source
     */
    public ProgressEvent(Object source) {
        super(source);
    }

    /**
     * Create from a source, including a message type and value. This allows -
     * for example - the total progress bar length to be altered based on a
     * computation in an IAnalysisMethod
     * 
     * @param source the source firing the event
     * @param m the messsage type
     * @param v the value in the message
     */
    public ProgressEvent(Object source, int m, long v) {
        super(source);
        message = m;
        value = v;
    }

    /**
     * Get the message in this event
     * @return
     */
    public int getMessage() {
        return message;
    }

    /**
     * Get the value in this event
     * @return
     */
    public long getValue() {
        return value;
    }

    public boolean hasMessage() {
        return message != NO_MESSAGE;
    }
}
