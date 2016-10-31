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
	 * @param list
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
	
	void addSubPanel(TabPanel panel);

	
	void addSignalChangeListener(SignalChangeListener l);
	void removeSignalChangeListener(SignalChangeListener l);
	
	void addDatasetEventListener(DatasetEventListener l);
	void removeDatasetEventListener(DatasetEventListener l);
	
	void addInterfaceEventListener(InterfaceEventListener l);
	void removeInterfaceEventListener(InterfaceEventListener l);
	
	void addDatasetUpdateEventListener(DatasetUpdateEventListener l);
	void removeDatasetUpdateEventListener(DatasetUpdateEventListener l);
	
	public List<TabPanel> getSubPanels();
	
	boolean hasSubPanels();
	
	void setAnalysing(boolean b);
	
	void setChartsAndTablesLoading();
	
	void setEnabled(boolean b);
	
	void clearChartCache();
	
	void clearChartCache(List<IAnalysisDataset> list);
	
	void clearTableCache();
	
	void clearTableCache(List<IAnalysisDataset> list);
	
	void refreshChartCache();
	
	void refreshChartCache(List<IAnalysisDataset> list);
	
	void refreshTableCache();
	
	void refreshTableCache(List<IAnalysisDataset> list);
}
