package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.event.WindowListener;

import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;

/**
 * Interface for the main frame of the program. Used while debugging the dockable framework
 * @author ben
 * @since 1.14.0
 *
 */
public interface MainView {
	
	void dispose();
	
	boolean isStandalone();
	
	EventHandler getEventHandler();
	
	ProgressBarAcceptor getProgressAcceptor();

	WindowListener[] getWindowListeners();

}
