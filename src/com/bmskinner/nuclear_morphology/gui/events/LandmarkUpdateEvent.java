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

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;

/**
 * Landmark update events indicate a change to the position of a landmark in a
 * profile.
 * 
 * @author ben
 * @since 1.13.2
 *
 */
@SuppressWarnings("serial")
public class LandmarkUpdateEvent extends UserActionEvent {

	private Landmark lm;
	private int newIndex;
	private ICell c;

	/**
	 * Create an event from a source class, with the given message and datasets to
	 * process
	 * 
	 * @param source
	 * @param type
	 */
	public LandmarkUpdateEvent(@NonNull Object source, IAnalysisDataset dataset, @Nullable ICell cell, Landmark lm,
			int newIndex) {
		super(source, "LandmarkUpdate", List.of(dataset), null);
		this.c = cell;
		this.lm = lm;
		this.newIndex = newIndex;
	}

	public Landmark getLandmark() {
		return lm;
	}

	public int getNewIndex() {
		return newIndex;
	}

	public @Nullable ICell getCell() {
		return c;
	}

}
