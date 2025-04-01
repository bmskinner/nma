package com.bmskinner.nma.components;

import com.bmskinner.nma.components.Updatable.ComponentUpdateEvent;

/**
 * Listener for updates to a dataset or the cells they contain
 * 
 */
public interface ComponentUpdateListener {
	
	/**
	 * Signal the listener that there has been an update.
	 */
	public void componentUpdated(ComponentUpdateEvent e);
	
}
