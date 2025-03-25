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
package com.bmskinner.nma.gui;

import java.util.Collection;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.workspaces.IWorkspace;

/**
 * Interface for components that have their enabled state driven by the number
 * and type of selected objects. Components should specify a bitmask describing
 * the combinations of objects they will respond to.
 * 
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
public interface ContextEnabled {

	int ACTIVE_ON_ROOT_DATASET = 1;
	int ACTIVE_ON_CHILD_DATASET = 2;
	int ACTIVE_ON_CLUSTER_GROUP = 4;
	int ACTIVE_ON_WORKSPACE = 8;
	int ACTIVE_ON_SINGLE_OBJECT = 16;
	int ACTIVE_ON_MULTI_OBJECTS = 32;
	int ACTIVE_WITH_CONSENSUS = 64;
	int ACTIVE_WITH_SIGNALS = 128;
//	int ACTIVE_ON_TWO_DATASETS = 64;

	int ALWAYS_ACTIVE = ACTIVE_ON_ROOT_DATASET + ACTIVE_ON_CHILD_DATASET + ACTIVE_ON_CLUSTER_GROUP + ACTIVE_ON_WORKSPACE
			+ ACTIVE_ON_SINGLE_OBJECT + ACTIVE_ON_MULTI_OBJECTS;

	int ONLY_DATASETS = ACTIVE_ON_ROOT_DATASET + ACTIVE_ON_CHILD_DATASET + ACTIVE_ON_SINGLE_OBJECT
			+ ACTIVE_ON_MULTI_OBJECTS;

	/**
	 * Tell the menu items to update their state based on the selected items
	 * 
	 */
	void updateSelectionContext(Collection<? extends Object> objects);

	/**
	 * Test if the item should be active for the given objects
	 * 
	 * @param objects
	 */
	static boolean matchesSelectionContext(Collection<? extends Object> objects, int context) {

		if ((context & ALWAYS_ACTIVE) == ALWAYS_ACTIVE)
			return true;

		if (objects.isEmpty())
			return false;

		if (objects.size() == 1 && ((context & ACTIVE_ON_SINGLE_OBJECT) != ACTIVE_ON_SINGLE_OBJECT))
			return false;

		if (objects.size() > 1 && ((context & ACTIVE_ON_MULTI_OBJECTS) != ACTIVE_ON_MULTI_OBJECTS))
			return false;

		boolean enabled = true;

		for (Object o : objects) {
			if (o instanceof IAnalysisDataset d) {
				if (d.isRoot())
					enabled &= ((context & ACTIVE_ON_ROOT_DATASET) == ACTIVE_ON_ROOT_DATASET);
				else
					enabled &= ((context & ACTIVE_ON_CHILD_DATASET) == ACTIVE_ON_CHILD_DATASET);

				if (((context & ACTIVE_WITH_CONSENSUS) == ACTIVE_WITH_CONSENSUS))
					enabled &= d.getCollection().hasConsensus();

				if (((context & ACTIVE_WITH_SIGNALS) == ACTIVE_WITH_SIGNALS))
					enabled &= d.getCollection().getSignalManager().hasSignals();
			}

			// Allow cluster groups and workspaces to be ignored in multi selections
			if (o instanceof IClusterGroup)
				enabled |= ((context & ACTIVE_ON_CLUSTER_GROUP) == ACTIVE_ON_CLUSTER_GROUP);
			if (o instanceof IWorkspace)
				enabled |= ((context & ACTIVE_ON_WORKSPACE) == ACTIVE_ON_WORKSPACE);
		}
		return enabled;
	}
}
