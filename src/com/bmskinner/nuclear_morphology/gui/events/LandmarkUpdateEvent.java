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

import java.util.EventObject;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;

/**
 * Landmark update events indicate a change to the position of a landmark in a
 * profile.
 * 
 * @author ben
 * @since 1.13.2
 *
 */
@SuppressWarnings("serial")
public class LandmarkUpdateEvent extends EventObject {

	public OrientationMark lm;
	public IAnalysisDataset dataset = null;
	public int newIndex;
	public ICell cell = null;

	/**
	 * Create an event from a source, with the given message
	 * 
	 * @param source        the source of the datasets
	 * @param id            the the segment to be updated
	 * @param newStartIndex the new index to apply via the update type
	 * @param type          the type of segment update to perform
	 */
	public LandmarkUpdateEvent(final Object source, final IAnalysisDataset dataset, final OrientationMark lm,
			final int newIndex) {
		super(source);
		this.lm = lm;
		this.newIndex = newIndex;
		this.dataset = dataset;
	}

	/**
	 * Create an event from a source, with the given message
	 * 
	 * @param source        the source of the datasets
	 * @param id            the the segment to be updated
	 * @param newStartIndex the new index to apply via the update type
	 * @param type          the type of segment update to perform
	 */
	public LandmarkUpdateEvent(final Object source, final ICell cell, final OrientationMark lm, final int newIndex) {
		super(source);
		this.lm = lm;
		this.newIndex = newIndex;
		this.cell = cell;
	}

	public boolean isDataset() {
		return dataset != null;
	}

	public boolean isCell() {
		return cell != null;
	}
}
