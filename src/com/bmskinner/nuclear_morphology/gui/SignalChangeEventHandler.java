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

import java.util.Iterator;

/**
 * Handle selected signal change events
 * 
 * @author ben
 * @since 1.13.7
 *
 */
public class SignalChangeEventHandler extends AbstractEventHandler {

    public SignalChangeEventHandler(Object parent) {
        super(parent);
    }
    
    public synchronized void addSignalChangeListener(SignalChangeListener l) {
        listeners.add(l);
    }

    public synchronized void removeSignalChangeListener(SignalChangeListener l) {
        listeners.remove(l);
    }
    
    public synchronized void fireSignalChangeEvent(String message) {

        SignalChangeEvent event = new SignalChangeEvent(parent, message, parent.getClass().getSimpleName());
        Iterator<Object> iterator =listeners.iterator();
        while (iterator.hasNext()) {
            ((SignalChangeListener) iterator.next()).signalChangeReceived(event);
        }
    }

    public synchronized void fireSignalChangeEvent(SignalChangeEvent event) {
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((SignalChangeListener) iterator.next()).signalChangeReceived(event);
        }
    }

}
