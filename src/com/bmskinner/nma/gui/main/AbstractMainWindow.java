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
package com.bmskinner.nma.gui.main;

import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.tabs.TabPanel;

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
		this.addWindowStateListener((e) -> {

				Runnable r = () -> {
					try {
						// If the update is called immediately, the chart size has
						// not yet changed, and therefore will render at the wrong aspect
						// ratio
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						LOGGER.log(Level.WARNING, "Error in window state listener", e1);
						Thread.currentThread().interrupt();
						return;
					}
					for (TabPanel d : detailPanels)
						d.updateSize();
				};
				ThreadManager.getInstance().submit(r);

			
		});

		this.setDropTarget(new MainDragAndDropTarget());
	}

	protected abstract void createUI();

}
