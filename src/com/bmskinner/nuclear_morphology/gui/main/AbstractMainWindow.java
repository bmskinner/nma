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
package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;

/**
 * Base class for main windows
 * 
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractMainWindow extends JFrame implements MainView {

	private static final String PROGRAM_TITLE_BAR_LBL = "Nuclear Morphology Analysis v"
			+ Version.currentVersion().toString();

	/** Panels displaying dataset information */
	protected final List<TabPanel> detailPanels = new ArrayList<>();

	private static final Logger LOGGER = Logger.getLogger(AbstractMainWindow.class.getName());

	/**
	 * Create the frame.
	 * 
	 * @param standalone is the frame a standalone app, or launched within ImageJ?
	 */
	protected AbstractMainWindow() {
		setTitle(PROGRAM_TITLE_BAR_LBL);
	}

	/**
	 * Create the listeners that handle dataset saving when the main window is
	 * closed
	 * 
	 */
	protected void createWindowListeners() {

		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

		this.addWindowListener(new MainWindowCloseAdapter(this));

		// Add a listener for panel size changes. This will cause
		// charts to redraw at the new aspect ratio rather than stretch.
		this.addWindowStateListener(new WindowStateListener() {
			@Override
			public void windowStateChanged(WindowEvent e) {

				Runnable r = () -> {
					try {
						// If the update is called immediately, the chart size has
						// not yet changed, and therefore will render at the wrong aspect
						// ratio
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						return;
					}
					for (TabPanel d : detailPanels)
						d.updateSize();
				};
				ThreadManager.getInstance().submit(r);

			}
		});

		this.setDropTarget(new MainDragAndDropTarget());
	}

	protected abstract void createUI();

//	protected abstract void createEventHandling();

//	public List<TabPanel> getTabPanels() {
//		return this.detailPanels;
//	}

	/**
	 * Remove all charts and tables from caches, but do not redraw them
	 */
//	protected synchronized void clearChartCache() {
//		for (TabPanel panel : getTabPanels()) {
//			panel.clearCache();
//		}
//	}

	/**
	 * Remove charts and tables from caches which contain the given datasets
	 * 
	 * @param list
	 */
//	protected synchronized void clearChartCache(final List<IAnalysisDataset> list) {
//
//		if (list == null || list.isEmpty()) {
//			LOGGER.log(Level.WARNING, "A cache clear was requested for a specific list, which was null or empty");
//			clearChartCache();
//			return;
//		}
//		for (TabPanel panel : getTabPanels()) {
//			panel.clearCache(list);
//		}
//	}

	/*
	 * Trigger a recache of all charts and tables
	 */
//	protected synchronized void recacheCharts() {
//		InterfaceUpdater task = () -> {
//			for (TabPanel panel : getTabPanels()) {
//				panel.refreshCache();
//			}
//		};
//		ThreadManager.getInstance().execute(task);
//	}

	/*
	 * Trigger a recache of all charts and tables which contain the given datasets
	 */
//	protected synchronized void recacheCharts(final List<IAnalysisDataset> list) {
//
//		InterfaceUpdater task = () -> {
//			for (TabPanel panel : getTabPanels()) {
//				panel.refreshCache(list);
//			}
//		};
//		ThreadManager.getInstance().execute(task);
//
//	}

	@Override
	public void dispose() {
		super.dispose();
	}

	/**
	 * Send panel update requests to all panels
	 * 
	 * @author ben
	 *
	 */
//	public class PanelUpdater implements CancellableRunnable, InterfaceUpdater {
//		private final List<IAnalysisDataset> list = new ArrayList<>();
//
//		private final AtomicBoolean isCancelled = new AtomicBoolean(false);
//
//		public PanelUpdater(final @NonNull List<IAnalysisDataset> datasets) {
//			this.list.addAll(datasets);
//		}
//
//		@Override
//		public synchronized void run() {
//
//			// Set the loading state
//			for (TabPanel p : getTabPanels()) {
//				p.setLoading();
//			}
//
//			// Fire the update to each listener
//			DatasetUpdateEvent e = new DatasetUpdateEvent(this, list);
//			Iterator<EventListener> iterator = updateListeners.iterator();
//			while (iterator.hasNext()) {
//				if (isCancelled.get())
//					return;
////				iterator.next().eventReceived(e);
//			}
//
//		}
//
//		@Override
//		public void cancel() {
//			isCancelled.set(true);
//		}
//
//	}
}
