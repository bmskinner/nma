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

import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/**
 * Store listeners for dataset events, and allows firing of dataset events
 * The dataset event listeners are used to signal an activity should be
 * performed on a dataset or datasets. They are sent by a tab panel.
 * 
 * @author bms41
 * @since 1.13.7
 *
 */
public class DatasetEventHandler extends AbstractEventHandler {

    public DatasetEventHandler(Object parent){
        super(parent);
    }

    /**
     * Fire a dataset event with the given name
     * @param method the method name
     * @param dataset the dataset to run the method on
     */
    public synchronized void fireDatasetEvent(String method, IAnalysisDataset dataset) {

        List<IAnalysisDataset> list = new ArrayList<>();
        list.add(dataset);
        fireDatasetEvent(method, list);
    }

    /**
     * Fire a dataset event with the given name
     * @param method the method name
     * @param list the datasets to run the method on
     */
    public synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list) {

        DatasetEvent event = new DatasetEvent(parent, method, parent.getClass().getSimpleName(), list);
        fire(event);
    }

    /**
     * Fire a dataset event with the given name and secondary dataset
     * @param method method the method name
     * @param list list the datasets to run the method on
     * @param secondary the secondary or template dataset
     */
    public synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list,
            IAnalysisDataset secondary) {

        DatasetEvent event = new DatasetEvent(parent, method, parent.getClass().getSimpleName(), list, secondary);
        fire(event);
    }
    
    /**
     * Fire the given dataset event
     * @param event the event
     */
    public synchronized void fireDatasetEvent(DatasetEvent event) {
    	fire(event);
    }
}
