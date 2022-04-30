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
package com.bmskinner.nma.gui.tabs;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.InputSupplier;

public interface TabPanel {

	/**
	 * Update the panel display based on the datasets selected in the global list
	 * manager
	 */
	void update();

	/**
	 * Instruct the panel to update its display based on the given datasets
	 * 
	 * @param list the datasets with which to update the panel
	 */
	void update(List<IAnalysisDataset> list);

	/**
	 * Cause charts to redraw with new dimensions preserving aspect ratio
	 */
	void updateSize();

	/**
	 * Fetch the currently active dataset for the panel. Use when only one dataset
	 * is expected to be visible; this simply accesses the first dataset in the list
	 * provided
	 * 
	 * @return
	 */
	IAnalysisDataset activeDataset();

	/**
	 * Test if the current tab is in the process of updating its display
	 * 
	 * @return
	 */
	boolean isUpdating();

	/**
	 * Check if any cells have been updated since the last UI change.
	 * 
	 * @return
	 */
	boolean hasCellUpdate();

	/**
	 * Set the cell update state for the panel
	 * 
	 * @param b
	 */
	void setCellUpdate(boolean b);

	/**
	 * Set the analysing state. This sets the cursor over the panel and its
	 * sub-panels
	 * 
	 * @param b
	 */
	void setAnalysing(boolean b);

	/**
	 * Set the panel state to show loading charts and tables.
	 */
	void setLoading();

	/**
	 * Set the controls in this panel to enabled or disabled
	 * 
	 * @param b
	 */
	void setEnabled(boolean b);

	/**
	 * Remove all charts from the panel chart cache
	 */
	void clearCache();

	/**
	 * Remove all charts containing the datasets in the list from the panel chart
	 * cache
	 */
	void clearCache(List<IAnalysisDataset> list);

	/**
	 * Remove all charts containing the datasets in the list from the panel chart
	 * cache
	 */
	void clearCache(IAnalysisDataset dataset);

	/**
	 * Remove all charts from the chart cache, then redraw the currently selected
	 * dataset charts
	 */
	void refreshCache();

	/**
	 * Redraw the charts for the given datasets
	 * 
	 * @param list the list of datasets to be redrawn
	 */
	void refreshCache(List<IAnalysisDataset> list);

	/**
	 * Redraw the charts for the given datasets
	 */
	void refreshCache(IAnalysisDataset dataset);

	/**
	 * Get the input supplier for user interaction with this panel
	 * 
	 * @return
	 */
	InputSupplier getInputSupplier();

}
