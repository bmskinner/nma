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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * Store listeners for dataset events, and allows firing of dataset events
 * @author bms41
 * @since 1.13.7
 *
 */
public class DatasetEventHandler extends AbstractEventHandler {

    
    public DatasetEventHandler(Object parent){
        super(parent);
    }

    public synchronized void fireDatasetEvent(String method, IAnalysisDataset dataset) {

        List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
        list.add(dataset);
        fireDatasetEvent(method, list);
    }

    public synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list) {

        DatasetEvent event = new DatasetEvent(parent, method, parent.getClass().getSimpleName(), list);
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((DatasetEventListener) iterator.next()).datasetEventReceived(event);
        }
    }

    public synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list,
            IAnalysisDataset secondary) {

        DatasetEvent event = new DatasetEvent(parent, method, parent.getClass().getSimpleName(), list, secondary);
        Iterator<Object> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            ((DatasetEventListener) iterator.next()).datasetEventReceived(event);
        }
    }
    
    public synchronized void fireDatasetEvent(DatasetEvent event) {
      Iterator<Object> iterator = listeners.iterator();
      while (iterator.hasNext()) {
          ((DatasetEventListener) iterator.next()).datasetEventReceived(event);
      }
  }

    public synchronized void addDatasetEventListener(DatasetEventListener l) {
        listeners.add(l);
    }

    public synchronized void removeDatasetEventListener(DatasetEventListener l) {
        listeners.remove(l);
    }

}
