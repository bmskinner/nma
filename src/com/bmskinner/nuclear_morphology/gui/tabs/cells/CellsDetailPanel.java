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
package com.bmskinner.nuclear_morphology.gui.tabs.cells;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

/**
 * Shows aggregate stats for the cells in datasets
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CellsDetailPanel extends DetailPanel {

	private static final String PANEL_TITLE_LBL = "Cell charts";

	private JTabbedPane tabPane;

	public CellsDetailPanel() {
		super(PANEL_TITLE_LBL);

		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane(JTabbedPane.TOP);

		DetailPanel boxplotPanel = new CellsBoxplotsPanel();

		tabPane.addTab(boxplotPanel.getPanelTitle(), boxplotPanel);

		this.add(tabPane, BorderLayout.CENTER);
	}

}
