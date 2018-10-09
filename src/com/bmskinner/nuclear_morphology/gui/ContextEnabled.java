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
package com.bmskinner.nuclear_morphology.gui;

import java.util.Collection;

/**
 * Interface for components that have their enabled state driven 
 * by the number and type of selected objects. Components should specify a
 * bitmask describing the combinations of objects they will respond to. 
 * @author bms41
 * @since 1.14.0
 *
 */
public interface ContextEnabled {
	
	int ACTIVE_ON_ROOT_DATASET  = 1;
	int ACTIVE_ON_CHILD_DATASET = 2;
	int ACTIVE_ON_CLUSTER_GROUP = 4;
	int ACTIVE_ON_WORKSPACE     = 8;
	int ACTIVE_ON_SINGLE_OBJECT = 16;
	int ACTIVE_ON_MULTI_OBJECTS = 32;
	
	 /**
     * Tell the menu items to update their state based on the selected items
     * @param nItems the number of selected items
     */
	void updateSelectionContext(Collection<Object> objects);
}
