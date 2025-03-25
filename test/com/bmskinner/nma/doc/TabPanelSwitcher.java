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
package com.bmskinner.nma.doc;

import java.awt.Component;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nma.gui.main.DockableMainWindow;
import com.bmskinner.nma.gui.tabs.DetailPanel;
import com.javadocking.dock.TabDock;

/**
 * Change between tab panels in a main window
 * 
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
public class TabPanelSwitcher {

	private static final Logger LOGGER = Logger.getLogger(TabPanelSwitcher.class.getName());

	private DockableMainWindow mw;
	private TabDock dock;
	private int currentTab = 0;
	private int totalTabs = 0;

	public TabPanelSwitcher(DockableMainWindow mw) {
		this.mw = mw;

		Field tabDock;
		try {
			tabDock = mw.getClass().getDeclaredField("tabDock");
			tabDock.setAccessible(true);
			dock = (TabDock) tabDock.get(mw);
			totalTabs = dock.getDockableCount();
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException
				| IllegalAccessException e) {
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
		}

	}

	/**
	 * Test if there is another tab to visit
	 * 
	 * @return true if there is another tab, false otherwise
	 */
	public boolean hasNext() {
		return currentTab <= totalTabs - 1;
	}

	/**
	 * Fetch the DetailPanel in the next tab
	 * 
	 * @return
	 */
	public DetailPanel nextTab() {
		dock.setSelectedDockable(dock.getDockable(currentTab));
		Component c = dock.getDockable(currentTab).getContent();
		DetailPanel d = (DetailPanel) c;
		currentTab++;
		return d;
	}

}
