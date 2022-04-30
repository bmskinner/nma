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
package com.bmskinner.nma.gui.dialogs.prober;

import java.util.EventObject;

/**
 * Fired when options are changed in an image prober
 * @author ben
 *
 */
public class OptionsChangeEvent extends EventObject {

    public static final int KUWAHARA     = 0;
    public static final int EDGE         = 1;
    public static final int CHROMOCENTRE = 2;
    public static final int GAP_CLOSING  = 3;

    private static final long serialVersionUID = 1L;

    /**
     * Create an event from a source, with the given message
     * 
     * @param source the source of the event
     */
    public OptionsChangeEvent(Object source) {
        super(source);
    }
}
