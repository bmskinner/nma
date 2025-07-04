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

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.gui.events.CellUpdatedEventListener;
import com.bmskinner.nma.gui.events.DatasetSelectionUpdatedListener;
import com.bmskinner.nma.gui.events.UIController;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.Cache;

/**
 * The DetailPanels hold chart and table caches, and track other DetailPanels
 * beneath them. Any dataset, interface or signal events generated by a
 * sub-panel are passed upwards to the parent detail panel.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class DetailPanel extends JPanel
		implements TabPanel, CellUpdatedEventListener, DatasetSelectionUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(DetailPanel.class.getName());

	/** Handle UI update requests */
	protected final transient UIController uiController;

	protected static final int SINGLE_CLICK = 1;
	protected static final int DOUBLE_CLICK = 2;

	private final transient InputSupplier inputSupplier = new DefaultInputSupplier();

	/** Holds rendered tables for all selected options */
	protected transient Cache cache;

	private static final String DEFAULT_TAB_TITLE = "Default";

	/** Panel title */
	private final String panelTabTitleLbl;

	/** What the panel is for; used in tooltips */
	private final String panelTabDescription;

	/** Track if the panel is currently in the process of updating */
	private AtomicBoolean isUpdating = new AtomicBoolean(false);

	/** Perform cosmetic operations on datasets - renaming, changing colours etc. */
	protected final transient CosmeticHandler cosmeticHandler = new CosmeticHandler(this);

	private boolean isCellUpdateMade = false; // for editing panels to batch UI update requests

	/**
	 * Create with the default title.
	 * 
	 */
	protected DetailPanel() {
		this(DEFAULT_TAB_TITLE);
	}

	/**
	 * Create with a title. The panel description will use the title also.
	 * 
	 */
	protected DetailPanel(@NonNull final String title) {
		this(title, title);
	}

	/**
	 * Create with a title and description for tooltips.
	 * 
	 * @param title
	 */
	protected DetailPanel(@NonNull final String title, @NonNull final String description) {
		panelTabTitleLbl = title;
		panelTabDescription = description;

		uiController = UIController.getInstance();
		uiController.addDatasetSelectionUpdatedListener(this);
	}

	/**
	 * Get the preferred name of the panel for use in tabs
	 * 
	 * @return the title
	 */
	public String getPanelTitle() {
		return panelTabTitleLbl;
	}

	/**
	 * Get the description of the panel for use in tooltips
	 * 
	 * @return the description
	 */
	public String getPanelDescription() {
		return panelTabDescription;
	}

	@Override
	public InputSupplier getInputSupplier() {
		return inputSupplier;
	}

	/**
	 * Add a panel to a tab pane
	 * 
	 * @param panel
	 */
	protected static void addPanel(JTabbedPane tabPane, DetailPanel panel) {
		tabPane.addTab(panel.getPanelTitle(), null, panel, panel.getPanelDescription());
	}

	/**
	 * Fetch the currently active dataset for the panel. Use when only one dataset
	 * is expected to be visible; this simply accesses the first dataset in the list
	 * provided
	 * 
	 * @return
	 */
	@Override
	public synchronized IAnalysisDataset activeDataset() {
		return DatasetListManager.getInstance().getActiveDataset();
	}

	/**
	 * Test if only a single dataset is selected
	 * 
	 * @return
	 */
	public synchronized boolean isSingleDataset() {
		return DatasetListManager.getInstance().isSingleSelectedDataset();
	}

	/**
	 * Test if multiple datasets are selected
	 * 
	 * @return
	 */
	public synchronized boolean isMultipleDatasets() {
		return DatasetListManager.getInstance().isMultipleSelectedDatasets();
	}

	public synchronized boolean hasDatasets() {
		return DatasetListManager.getInstance().hasSelectedDatasets();
	}

	/**
	 * Get the datasets currently displayed in this panel
	 * 
	 * @return a list of datasets
	 */
	protected synchronized List<IAnalysisDataset> getDatasets() {
		return DatasetListManager.getInstance().getSelectedDatasets();
	}

	@Override
	public synchronized boolean isUpdating() {
		return isUpdating.get();
	}

	protected synchronized void setUpdating(boolean b) {
		this.isUpdating.set(b);
	}

	/**
	 * Toggle wait cursor on element
	 * 
	 * @param b
	 */
	@Override
	public synchronized void setAnalysing(boolean b) {
		if (b) {

			for (Component c : this.getComponents()) {
				c.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}

			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		} else {

			for (Component c : this.getComponents()) {
				c.setCursor(Cursor.getDefaultCursor());
			}
			this.setCursor(Cursor.getDefaultCursor());
		}
	}

	/**
	 * Force any chart panels currently visible on screen to redraw, allowing text
	 * to be rendered with the appropriate aspect ratio
	 */
	@Override
	public synchronized void updateSize() {
		updateSize(this);
	}

	/**
	 * Carries out the resize - recursively search all containers for chart panels,
	 * and refresh the chart cache if any are found.
	 * 
	 * @param container
	 */
	private synchronized void updateSize(Container container) {
		for (Component c : container.getComponents()) {
			if (c instanceof ExportableChartPanel && c.isShowing()) {
				this.refreshCache();
				return;
			}

			if (c instanceof Container con) {
				updateSize(con);
			}

		}
	}

	@Override
	public void update() {

		Runnable r = () -> {
			setUpdating(true);
			List<IAnalysisDataset> list = DatasetListManager.getInstance().getSelectedDatasets();
			updateDetail(list);
		};
		ThreadManager.getInstance().submitUIUpdate(r);

	}

	@Override
	public void update(final List<IAnalysisDataset> list) {

		Runnable r = () -> {
			setUpdating(true);
			updateDetail(list);
		};
		ThreadManager.getInstance().submitUIUpdate(r);
	}

	/**
	 * This method sets which of the overriden handling methods are run by extending
	 * classes.
	 */
	private void updateDetail(@NonNull final List<IAnalysisDataset> list) {

		try {
			if (list.isEmpty()) {
				updateNull();
				return;
			}

			if (list.size() > 1) {
				updateMultiple();
				return;
			}
			updateSingle();

		} catch (Exception e) {
			LOGGER.fine("Error updating panel " + this.getClass().getName());
			LOGGER.log(Loggable.STACK, "Error updating panel", e); // save detail for fine
																	// logging

			try {
				updateNull();
			} catch (Exception e1) {
				LOGGER.fine(this.getClass().getName()
						+ ": Error recovering from error updating panel");
				LOGGER.log(Loggable.STACK, "Error recovering from error updating panel", e1);
			}
		} finally {
			setUpdating(false);
			setAnalysing(false);
		}
	}

	/**
	 * This method must be overridden by the extending class to perform the actual
	 * update when a single dataset is selected
	 */
	protected synchronized void updateSingle() {

	}

	/**
	 * This method must be overridden by the extending class to perform the actual
	 * update when multiple datasets are selected
	 */
	protected synchronized void updateMultiple() {
	}

	/**
	 * This method must be overridden by the extending class to perform the actual
	 * update when no datasets are selected
	 */
	protected synchronized void updateNull() {
	}

	/**
	 * Get the chart cache for the panel
	 * 
	 * @return
	 */
	public synchronized Cache getCache() {
		return this.cache;
	}

	/**
	 * Remove all charts from the cache. Does not invoke an update
	 * 
	 * @param list
	 */
	@Override
	public synchronized void clearCache() {
		if (cache != null)
			cache.clear();
	}

	/**
	 * Remove all charts from the cache. Does not invoke an update
	 * 
	 * @param list
	 */
	@Override
	public synchronized void clearCache(final List<IAnalysisDataset> list) {
		if (cache != null)
			cache.clear(list);
	}

	@Override
	public synchronized void clearCache(final IAnalysisDataset dataset) {
		if (cache != null)
			cache.clear(dataset);
	}

	/**
	 * Remove all charts from the cache. Then call an update of the panel
	 * 
	 * @param list
	 */
	@Override
	public synchronized void refreshCache() {
		Runnable r = () -> {
			clearCache();
			update(getDatasets());
		};
		ThreadManager.getInstance().submitUIUpdate(r);
	}

	/**
	 * Remove all charts from the cache containing datasets in the given list, so
	 * they will be recalculated. This allows a refresh of some of the charts in the
	 * chache, without recalculating everything. The charts will only be regenerated
	 * if they are currently being displayed.
	 * 
	 * @param list
	 */
	@Override
	public synchronized void refreshCache(final IAnalysisDataset dataset) {
		Runnable r = () -> {
			clearCache(dataset);
			if (getDatasets().stream().anyMatch(d -> dataset.getId().equals(d.getId())))
				update(getDatasets());
		};
		ThreadManager.getInstance().submitUIUpdate(r);
	}

	/**
	 * Remove all charts from the cache containing datasets in the given list, so
	 * they will be recalculated. This allows a refresh of some of the charts in the
	 * chache, without recalculating everything. The charts will only be regenerated
	 * if they are currently being displayed.
	 * 
	 * @param list
	 */
	@Override
	public synchronized void refreshCache(final List<IAnalysisDataset> list) {
		Runnable r = () -> {
			clearCache(list);
			if (getDatasets().stream().anyMatch(list::contains))
				update(getDatasets());
		};
		ThreadManager.getInstance().submitUIUpdate(r);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gui.tabs.TabPanel#setChartsAndTablesLoading() This sets all sub panels
	 * to invoke the loading state. Any implementing class must still override this
	 * method to provide the expected behaviour for the panel and call
	 * super.setChartsAndTablesLoading()
	 */
	@Override
	public synchronized void setLoading() {
	}

	@Override
	public void cellUpdatedEventReceived(CellUpdatedEvent event) {
		isCellUpdateMade = true;
	}

	@Override
	public boolean hasCellUpdate() {
		return isCellUpdateMade;
	}

	@Override
	public void setCellUpdate(boolean b) {
		isCellUpdateMade = b;
	}

	@Override
	public void datasetSelectionUpdated(List<IAnalysisDataset> datasets) {
		this.update(datasets);
	}

	@Override
	public void datasetSelectionUpdated(IAnalysisDataset d) {
		this.update(List.of(d));
	}

}
