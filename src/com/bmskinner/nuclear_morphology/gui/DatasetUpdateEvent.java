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

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * Send a list of datasets to registered listeners to draw charts and tables
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class DatasetUpdateEvent extends EventObject {

    private final List<IAnalysisDataset> list;

    /**
     * Get the datasets in the event
     * 
     * @return
     */
    public List<IAnalysisDataset> getDatasets() {
        return list;
    }

    /**
     * Construct from an existing event. Use to pass messages on.
     * 
     * @param event
     */
    public DatasetUpdateEvent(Object source, final List<IAnalysisDataset> list) {
        super(source);
        this.list = new ArrayList<IAnalysisDataset>(list);
    }

    /**
     * Construct from a single dataset. Use to pass messages on.
     * 
     * @param event
     */
    public DatasetUpdateEvent(Object source, final IAnalysisDataset dataset) {
        super(source);

        this.list = new ArrayList<IAnalysisDataset>();
        this.list.add(dataset);
    }
}
