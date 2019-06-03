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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RecursiveAction;

/**
 * The abstract class for actions that can be split into fork-join tasks.
 * Progress through a task can be signalled to listeners via ProgressEvents.
 * 
 * @author bms41
 * @since 1.12.0
 * @deprecated since 1.15.1 because we're moving away from FJPs
 *
 */
@SuppressWarnings("serial")
@Deprecated
public abstract class AbstractProgressAction extends RecursiveAction implements ProgressListener {

    private final List<Object> listeners = new ArrayList<Object>();

    public synchronized void addProgressListener(ProgressListener l) {
        listeners.add(l);
    }

    public synchronized void removeProgressListener(ProgressListener l) {
        listeners.remove(l);
    }

    final protected synchronized void fireProgressEvent() {

        ProgressEvent event = new ProgressEvent(this);
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((ProgressListener) iterator.next()).progressEventReceived(event);
        }
    }

    @Override
    public void progressEventReceived(ProgressEvent event) {
        fireProgressEvent();
    }

}
