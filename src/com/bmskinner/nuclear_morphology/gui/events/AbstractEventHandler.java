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
    protected final List<EventListener> listeners   = new CopyOnWriteArrayList<EventListener>();
    
    /**
     * Construct with a parent object
     * @param parent the parent
     */
    public AbstractEventHandler(final Object parent){
        this.parent = parent;
    }
    
    /**
     * Add a listener for the given event type
     * 
     * @param l the listener to add
     */
    public synchronized void addListener(EventListener l) {
        listeners.add(l);
    }
    
    /**
     * Remove the given listener if present.
     * 
     * @param l the listener to remove
     */
    public synchronized void removeListener(EventListener l) {
        listeners.remove(l);
    }
    
    /**
     * Fire the given event to all listeners
     * @param event
     */
    public void fire(EventObject event) {
    	
    	for(EventListener l : listeners) {
    		
    		if(event instanceof DatasetEvent)
    			l.eventReceived((DatasetEvent) event);
    		if(event instanceof InterfaceEvent)
    			l.eventReceived((InterfaceEvent) event);
    		if(event instanceof SignalChangeEvent)
    			l.eventReceived((SignalChangeEvent) event);
    		if(event instanceof DatasetUpdateEvent)
    			l.eventReceived((DatasetUpdateEvent) event);
    		if(event instanceof ChartOptionsRenderedEvent)
    			l.eventReceived((ChartOptionsRenderedEvent) event);
    		
    	}
    }
    
    

}
