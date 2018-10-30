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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.DatasetUpdateEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEventHandler;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEventHandler;

public interface TabPanel extends EventListener {

    /**
     * Update the panel display based on the datasets selected in the global
     * list manager
     */
    void update();

    /**
     * Instruct the panel to update its display based on the given datasets
     * 
     * @param list
     *            the datasets with which to update the panel
     */
    void update(List<IAnalysisDataset> list);

    /**
     * Cause charts to redraw with new dimensions preserving aspect ratio
     */
    void updateSize();

    /**
     * Fetch the currently active dataset for the panel. Use when only one
     * dataset is expected to be visible; this simply accesses the first dataset
     * in the list provided
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
     * @return
     */
    boolean hasCellUpdate();
    
    /**
     * Set the cell update state for the panel
     * @param b
     */
    void setCellUpdate(boolean b);

    /**
     * Add the given panel as a sub-panel of this. The sub panel will be
     * notified of dataset updates, and interface and dataset events fired by
     * the sub panel will be passed upwards by this panel.
     * 
     * @param panel
     *            the panel to add
     */
    void addSubPanel(@NonNull TabPanel panel);
    
    TabPanel getParentPanel();
    
    /**
     * Add a listener for signal change events from this panel
     * 
     * @param l
     *            the listener
     */
    void addSignalChangeListener(EventListener l);

    /**
     * Remove a listener for signal change events from this panel
     * 
     * @param l
     *            the listener
     */
    void removeSignalChangeListener(EventListener l);

    /**
     * Add a listener for dataset events from this panel
     * 
     * @param l the listener
     */
    void addDatasetEventListener(EventListener l);

    /**
     * Remove a listener for dataset events from this panel
     * 
     * @param l the listener
     */
    void removeDatasetEventListener(EventListener l);

    /**
     * Add a listener for interface events from this panel
     * 
     * @param l
     *            the listener
     */
    void addInterfaceEventListener(EventListener l);

    /**
     * Remove a listener for interface events from this panel
     * 
     * @param l
     *            the listener
     */
    void removeInterfaceEventListener(EventListener l);

    /**
     * Add a listener for dataset update events from this panel
     * 
     * @param l
     *            the listener
     */
    void addDatasetUpdateEventListener(EventListener l);

    /**
     * Remove a listener for dataset update events from this panel
     * 
     * @param l
     *            the listener
     */
    void removeDatasetUpdateEventListener(EventListener l);

    /**
     * Get the list of sub panels
     * 
     * @return
     */
    public List<TabPanel> getSubPanels();

    /**
     * Check if this panel has sub panels
     * 
     * @return true if sub panels are present
     */
    boolean hasSubPanels();

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
    void setChartsAndTablesLoading();

    /**
     * Set the controls in this panel to enabled or disabled
     * 
     * @param b
     */
    void setEnabled(boolean b);

    /**
     * Remove all charts from the panel chart cache
     */
    void clearChartCache();

    /**
     * Remove all charts containing the datasets in the list from the panel
     * chart cache
     */
    void clearChartCache(List<IAnalysisDataset> list);

    /**
     * Remove all tables from the panel table cache
     */
    void clearTableCache();

    /**
     * Remove all tables containing the datasets in the list from the panel
     * table cache
     */
    void clearTableCache(List<IAnalysisDataset> list);

    /**
     * Remove all charts from the chart cache, then redraw the currently
     * selected dataset charts
     */
    void refreshChartCache();

    /**
     * Redraw redraw the charts for the given datasets
     * 
     * @param list
     *            the list of datasets to be redrawn
     */
    void refreshChartCache(List<IAnalysisDataset> list);

    /**
     * Remove all tables from the table cache, then redraw the currently
     * selected dataset tables
     */
    void refreshTableCache();

    /**
     * Redraw redraw the tables for the given datasets
     * 
     * @param list
     *            the list of datasets to be redrawn
     */
    void refreshTableCache(List<IAnalysisDataset> list);
    

    DatasetEventHandler getDatasetEventHandler();
    

    InterfaceEventHandler getInterfaceEventHandler();
    

    DatasetUpdateEventHandler getDatasetUpdateEventHandler();
    

    SignalChangeEventHandler getSignalChangeEventHandler();
    
    /**
     * Get the input supplier for user interaction with this panel
     * @return
     */
    InputSupplier getInputSupplier();

}
