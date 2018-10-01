package com.bmskinner.nuclear_morphology.documentation;

import java.awt.Component;
import java.lang.reflect.Field;

import com.bmskinner.nuclear_morphology.gui.main.DockableMainWindow;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;
import com.javadocking.dock.TabDock;

/**
 * Change between tab panels in a main window
 * @author bms41
 * @since 1.14.0
 *
 */
public class TabPanelSwitcher {
	
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
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public boolean hasNext() {
		return currentTab < totalTabs-1;
	}
	
	public DetailPanel nextTab() {
		currentTab++;
		dock.setSelectedDockable(dock.getDockable(currentTab));
		Component c = dock.getDockable(currentTab).getContent();			
		DetailPanel d = (DetailPanel)c;
		return d;
	}

}
