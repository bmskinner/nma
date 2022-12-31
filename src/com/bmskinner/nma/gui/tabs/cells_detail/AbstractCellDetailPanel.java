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
package com.bmskinner.nma.gui.tabs.cells_detail;

import java.util.logging.Logger;

import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public abstract class AbstractCellDetailPanel extends DetailPanel
		implements CellEditingTabPanel {
	private static final Logger LOGGER = Logger.getLogger(AbstractCellDetailPanel.class.getName());
	private CellViewModel model;

	protected AbstractCellDetailPanel(final CellViewModel model, String title) {
		super(title);
		this.model = model;
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
	 * This triggers a general chart recache for the active dataset and all its
	 * children, but performs the recache on the currnt tab first so results are
	 * showed at once
	 */
	protected void refreshEditingPanelCharts() {
		this.refreshCache();
	}

	/**
	 * Update the charts and tables for the current cell and component
	 */
	@Override
	public abstract void update();

	/**
	 * Get the current cell view
	 * 
	 * @return
	 */
	@Override
	public synchronized CellViewModel getCellModel() {
		return model;
	}

	@Override
	public synchronized void setCellModel(CellViewModel model) {
		this.model = model;
	}

	/**
	 * Remove any charts that contain the current active cell, causing them to
	 * redraw on the next refresh
	 */
	@Override
	public synchronized void clearCellCharts() {
		this.getCache().clear(model.getCell());
	}

}
