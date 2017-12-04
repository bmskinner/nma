package com.bmskinner.nuclear_morphology.components;

import java.util.Comparator;
import java.util.UUID;

/**
 * Defines the sorting operations for child datasets
 * @author ben
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
	 * Sort child datasets by the given comparator
	 * @param comparator
	 */
	void sortChildren(Comparator<IAnalysisDataset> comparator);
	
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
