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

/**
 * This is used to signal tab panels should update charts and tables to display
 * the datasets provided in the DatasetUpdateEvent.
 * 
 * @author ben
 *
 */
public interface DatasetUpdateEventListener {

    /**
     * Signal that the implementing class should respond to the given dataset
     * update request, and redraw all charts and tables appropriately
     * 
     * @param event
     *            the event to respond to
     */
    void datasetUpdateEventReceived(DatasetUpdateEvent event);
}
