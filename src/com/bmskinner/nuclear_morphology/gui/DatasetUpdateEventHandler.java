/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui;

import java.util.Iterator;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * Handle selected dataset list update events
 * 
 * @author ben
 * @since 1.13.7
 *
 */
public class DatasetUpdateEventHandler extends AbstractEventHandler {

    public DatasetUpdateEventHandler(Object parent) {
        super(parent);
    }
    
    /**
     * Signal listeners that the given datasets should be displayed
     * 
     * @param list
     */
    public void fireDatasetUpdateEvent(List<IAnalysisDataset> list) {
        DatasetUpdateEvent e = new DatasetUpdateEvent(parent, list);
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((DatasetUpdateEventListener) iterator.next()).datasetUpdateEventReceived(e);
        }
    }

    /**
     * Add a listener for dataset update events.
     * 
     * @param l
     */
    public synchronized void addDatasetUpdateEventListener(DatasetUpdateEventListener l) {
        listeners.add(l);
    }

    public synchronized void removeDatasetUpdateEventListener(DatasetUpdateEventListener l) {
        listeners.remove(l);
    }

}
