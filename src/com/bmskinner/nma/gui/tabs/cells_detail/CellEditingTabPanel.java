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
package com.bmskinner.nma.gui.tabs.cells_detail;

import com.bmskinner.nma.gui.tabs.EditingTabPanel;

public interface CellEditingTabPanel extends EditingTabPanel {

	/**
	 * Update the charts and tables for the current cell and component
	 */
	@Override
	void update();

	/**
	 * Get the current cell view
	 * 
	 * @return
	 */
	CellViewModel getCellModel();

	void setCellModel(CellViewModel model);

	/**
	 * Remove any charts that contain the current active cell, causing them to
	 * redraw on the next refresh
	 */
	void clearCellCharts();

}
