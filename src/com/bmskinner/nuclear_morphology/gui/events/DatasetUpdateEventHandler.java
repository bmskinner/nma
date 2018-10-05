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
     * Signal listeners that the given datasets should be displayed.
     * 
     * @param list the list of datasets include in the event
     */
    public void fireDatasetUpdateEvent(List<IAnalysisDataset> list) {
        DatasetUpdateEvent e = new DatasetUpdateEvent(parent, list);
        fire(e);
    }

}
