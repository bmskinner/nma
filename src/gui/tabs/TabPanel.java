/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs;

import java.util.List;

import analysis.IAnalysisDataset;
import gui.DatasetEventListener;
import gui.DatasetUpdateEventListener;
import gui.InterfaceEventListener;
import gui.SignalChangeListener;


public interface TabPanel 
	extends DatasetUpdateEventListener {
	
	/**
	 * Instruct the panel to update its display based on the
	 * given datasets
	 * @param list the datasets with which to update the panel
	 */
	void update(List<IAnalysisDataset> list);
	
	
	/**
	 * Cause charts to redraw with new dimensions
	 * preserving aspect ratio
	 */
	void updateSize();
	
	/**
	 * Fetch the currently active dataset for the panel.
	 * Use when only one dataset is expected to be visible;
	 * this simply accesses the first dataset in the list provided
	 * @return
	 */
	IAnalysisDataset activeDataset();
	
	
	/**
	 * Test if the current tab is in the process of updating
	 * its display
	 * @return
	 */
	boolean isUpdating();
	
	/**
	 * Add the given panel as a sub-panel of this. The sub panel 
	 * will be notified of dataset updates, and interface and dataset 
	 * events fired by the sub panel will be passed upwards by this panel. 
	 * @param panel the panel to add
	 */
	void addSubPanel(TabPanel panel);

	
	void addSignalChangeListener(SignalChangeListener l);
	void removeSignalChangeListener(SignalChangeListener l);
	
	void addDatasetEventListener(DatasetEventListener l);
	void removeDatasetEventListener(DatasetEventListener l);
	
	void addInterfaceEventListener(InterfaceEventListener l);
	void removeInterfaceEventListener(InterfaceEventListener l);
	
	void addDatasetUpdateEventListener(DatasetUpdateEventListener l);
	void removeDatasetUpdateEventListener(DatasetUpdateEventListener l);
	
	/**
	 * Get the list of sub panels
	 * @return
	 */
	public List<TabPanel> getSubPanels();
	
	/**
	 * Check if this panel has sub panels
	 * @return true if sub panels are present
	 */
	boolean hasSubPanels();
	
	/**
	 * Set the analysing state. This sets the cursor over the panel
	 * and its sub-panels
	 * @param b
	 */
	void setAnalysing(boolean b);
	
	/**
	 * Set the panel state to show loading charts and tables.
	 */
	void setChartsAndTablesLoading();
	
	/**
	 * Set the controls in this panel to enabled or disabled
	 * @param b
	 */
	void setEnabled(boolean b);
	
	/**
	 * Remove all charts from the panel chart cache
	 */
	void clearChartCache();
	
	/**
	 * Remove all charts containing the datasets in the list
	 * from the panel chart cache
	 */
	void clearChartCache(List<IAnalysisDataset> list);
	
	/**
	 * Remove all tables from the panel table cache
	 */
	void clearTableCache();
	
	/**
	 * Remove all tables containing the datasets in the list
	 * from the panel table cache
	 */
	void clearTableCache(List<IAnalysisDataset> list);
	
	
	/**
	 * Remove all charts from the chart cache, then redraw the
	 * currently selected dataset charts
	 */
	void refreshChartCache();
	
	/**
	 * Redraw redraw the charts for the given datasets
	 * @param list the list of datasets to be redrawn
	 */
	void refreshChartCache(List<IAnalysisDataset> list);
	
	/**
	 * Remove all tables from the table cache, then redraw the
	 * currently selected dataset tables
	 */
	void refreshTableCache();
	
	/**
	 * Redraw redraw the tables for the given datasets
	 * @param list the list of datasets to be redrawn
	 */
	void refreshTableCache(List<IAnalysisDataset> list);
}
