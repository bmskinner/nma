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

import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;

/**
 * Store listeners for interface events, and allows firing of interface events
 * @author bms41
 * @since 1.13.7
 *
 */
public class InterfaceEventHandler extends AbstractEventHandler {

    public InterfaceEventHandler(final Object parent){
        super(parent);
    }
    
    public void fireInterfaceEvent(InterfaceMethod method) {
    	fire(InterfaceEvent.of(parent, method));
    }
}
