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
package com.bmskinner.nma.gui.tabs;

/**
 * Methods for editing datasets via a tab panel
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface EditingTabPanel extends TabPanel {

	/**
	 * Check the lock state of all cells in the dataset being edited. If a lock has
	 * been set, get confirmation from the user whether cells should be unlocked for
	 * editing, or kept locked and protected from impending changes.
	 */
	void checkCellLock();

}
