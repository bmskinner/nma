package com.bmskinner.nma.gui.events;

/**
 * Interface for handlers of user actions
 * 
 * @author Ben Skinner
 *
 */
public interface UserActionEventListener {

	/**
	 * Alert the listener that an action has been taken
	 * 
	 * @param e
	 */
	void userActionEventReceived(UserActionEvent e);

}
