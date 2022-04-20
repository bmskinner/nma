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

public interface SegmentEventListener {

	/**
	 * Inform listeners that a segment start index has been updated in a dataset or
	 * in a single cell
	 * 
	 * @param event
	 */
	void segmentStartIndexUpdateEventReceived(SegmentStartIndexUpdateEvent event);

	/**
	 * Inform listeners that segments should be merged in a dataset
	 * 
	 * @param event
	 */
	void segmentMergeEventReceived(SegmentMergeEvent event);

	/**
	 * Inform listeners that segments should be unmerged in a dataset
	 * 
	 * @param event
	 */
	void segmentUnmergeEventReceived(SegmentUnmergeEvent event);

	/**
	 * Inform listeners that segments should be split in a dataset
	 * 
	 * @param event
	 */
	void segmentSplitEventReceived(SegmentSplitEvent event);

	/**
	 * Inform listeners that the profile window proportion in a dataset has changed
	 * 
	 * @param event
	 */
	void profileWindowProportionUpdateEventReceived(ProfileWindowProportionUpdateEvent event);
}
