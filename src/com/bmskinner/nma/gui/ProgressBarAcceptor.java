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
package com.bmskinner.nma.gui;

import java.util.List;

import javax.swing.JProgressBar;

/**
 * Defines components that can have a progress bar added to them
 * @author bms41
 * @since 1.14.0
 *
 */
public interface ProgressBarAcceptor {
	
	/**
	 * Add the given progress bar to the component
	 * @param bar the bar to add
	 */
	void addProgressBar(JProgressBar bar);
	
	/**
	 * Remove the given progress bar from the component
	 * @param bar the bar to remove
	 */
	void removeProgressBar(JProgressBar bar);
	
	/**
	 * Get all progress bars in this component
	 * @return the bars in the component
	 */
	List<JProgressBar> getProgressBars();

}
