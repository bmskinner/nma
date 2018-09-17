package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.event.WindowListener;

import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
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
	
    /**
     * Get the event handler that dispatches messages and analyses
     * 
     * @return
     */
	EventHandler getEventHandler();
	
	
    /**
     * Get the input supplier for requesting user input
     * 
     * @return
     */
	InputSupplier getInputSupplier();
	
	ProgressBarAcceptor getProgressAcceptor();

	WindowListener[] getWindowListeners();

}
