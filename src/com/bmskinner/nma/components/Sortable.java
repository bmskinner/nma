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
package com.bmskinner.nma.components;

import java.util.Comparator;
import java.util.UUID;

/**
 * Defines the sorting operations for child datasets for display in the UI
 * @author ben
 * @since 1.13.8
 *
 */
public interface Sortable {
	
	/**
	 * Get the position of the child dataset in the current sort order
	 * @param childId
	 * @return
	 */
	int getPosition(UUID childId);
	
	/**
     * Get the id of the child at the given position 
     * @param childId
     * @return
     */
	UUID getId(int position);
	
	/**
     * Get the UI display name of the child at the given position 
     * @param childId
     * @return
     */
	String getDisplayName(int position);
	
	/**
	 * Sort child datasets by the given comparator
	 * @param comparator
	 */
	void sortChildren(Comparator<Sortable> comparator);
	
	
	/**
	 * Sort the child datasets by the default comparator (name, 
	 * alphabetical ascending)
	 */
	void sortChildren();
	
	/**
	 * Move the given child dataset up in the sort list
	 * @param childId
	 */
	void moveUp(UUID childId);
	
	/**
	 * Move the given child dataset down in the sort list
	 * @param childId
	 */
	void moveDown(UUID childId);

}
