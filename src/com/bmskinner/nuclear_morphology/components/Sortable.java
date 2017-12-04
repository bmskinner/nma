package com.bmskinner.nuclear_morphology.components;

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
