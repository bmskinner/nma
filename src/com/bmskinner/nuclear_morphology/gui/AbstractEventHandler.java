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


package com.bmskinner.nuclear_morphology.gui;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract holder for all event handlers
 * @author bms41
 * @since 1.13.7
 *
 */
public abstract class AbstractEventHandler {
    
    protected final Object parent;
    protected final List<Object> listeners   = new CopyOnWriteArrayList<Object>();
    
    /**
     * Construct with a parent object
     * @param parent the parent
     */
    public AbstractEventHandler(final Object parent){
        this.parent = parent;
    }

}
