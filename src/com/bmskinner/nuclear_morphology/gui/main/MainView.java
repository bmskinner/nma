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
