package com.bmskinner.nma.components;

import java.util.EventObject;

/**
 * Interface for components that need to signal they have been updated
 * 
 */
public interface Updatable {
	
	/**
	 * Signal {@link ComponentUpdateListener} listeners that this component has updated
	 */
	void fireComponentUpdated();
	
	/**
	 * Add the given listener for updates from this component
	 * @param l
	 */
	void addComponentUpdateListener(ComponentUpdateListener l);
	
	/**
	 * Remove the given listener for updates from this component
	 * @param l
	 */
	void removeComponentUpdateListener(ComponentUpdateListener l);
	
	
	public class ComponentUpdateEvent extends EventObject {
		private static final long serialVersionUID = 1L;
		/**
		 * Create an event from a source, with the given message
		 * 
		 * @param source  the source of the event
		 */
		public ComponentUpdateEvent(final Object source) {
			super(source);
		}
	}

}
