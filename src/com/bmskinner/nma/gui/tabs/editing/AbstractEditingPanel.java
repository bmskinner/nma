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
package com.bmskinner.nma.gui.tabs.editing;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.SegmentationHandler;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.events.UserActionController;
import com.bmskinner.nma.gui.events.UserActionEvent;
import com.bmskinner.nma.gui.tabs.DetailPanel;
import com.bmskinner.nma.gui.tabs.EditingTabPanel;

@SuppressWarnings("serial")
public abstract class AbstractEditingPanel extends DetailPanel implements EditingTabPanel {

	private static final Logger LOGGER = Logger.getLogger(AbstractEditingPanel.class.getName());

	public AbstractEditingPanel(String title) {
		super(title);
	}

	/**
	 * Check if any of the cells in the active collection are locked for editing. If
	 * so, ask the user whether to unlock all cells, or leave cells locked.
	 */
	@Override
	public void checkCellLock() {
		if (activeDataset() == null)
			return;
		ICellCollection collection = activeDataset().getCollection();

		if (collection.hasLockedCells()) {
			String[] options = { "Keep manual values", "Overwrite manual values" };

			try {
				int result = getInputSupplier().requestOptionAllVisible(options, 0,
						"Some cells have been manually segmented. Keep manual values?",
						"Keep manual values?");
				if (result != 0)
					collection.setCellsLocked(false);
			} catch (RequestCancelledException e) {
			} // no action
		}
	}

	/**
	 * Update the border tag in the median profile to the given index, and update
	 * individual nuclei to match.
	 * 
	 * @param tag
	 * @param newTagIndex
	 */
	@Override
	public void setBorderTagAction(@NonNull Landmark tag, int newTagIndex) {
		if (activeDataset() == null)
			return;
		if (activeDataset().getCollection().isVirtual() && tag.equals(OrientationMark.REFERENCE)) {
			LOGGER.warning("Cannot update core border tag for a child dataset");
			return;
		}

		checkCellLock();

		LOGGER.info("Updating " + tag + " to index " + newTagIndex);

		setAnalysing(true);

		SegmentationHandler sh = new SegmentationHandler(activeDataset());
		sh.setLandmark(tag, newTagIndex);

		refreshCache(); // immediate visualisation of result

		if (OrientationMark.REFERENCE.equals(tag)) {
			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.SEGMENTATION_ACTION, getDatasets()));
		} else {
			// TODO: get UI controller to fire here
		}

		this.setAnalysing(false);

	}

	/**
	 * This triggers a general chart recache for the active dataset and all its
	 * children, but performs the recache on the currnt tab first so results are
	 * showed at once
	 */
	protected void refreshEditingPanelCharts() {
		this.refreshCache();
	}

	/**
	 * Update the start index of the given segment to the given index in the median
	 * profile, and update individual nuclei to match
	 * 
	 * @param id
	 * @param index
	 * @throws Exception
	 */
	@Override
	public void updateSegmentStartIndexAction(@NonNull UUID id, int index) throws Exception {

		checkCellLock();

		SegmentationHandler sh = new SegmentationHandler(activeDataset());
		sh.updateSegmentStartIndexAction(id, index);

		refreshEditingPanelCharts();

		UserActionController.getInstance().userActionEventReceived(
				new UserActionEvent(this, UserActionEvent.APPLY_MEDIAN_TO_NUCLEI, getDatasets()));

	}
}
