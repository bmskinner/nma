package com.bmskinner.nuclear_morphology.gui.events.revamp;

import com.bmskinner.nuclear_morphology.gui.events.UserActionEvent;

/**
 * Interface for handlers of user actions
 * 
 * @author ben
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
