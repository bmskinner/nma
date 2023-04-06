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
package com.bmskinner.nma.gui.tabs.comparisons;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;

import com.bmskinner.nma.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class ComparisonDetailPanel extends DetailPanel {

	JTabbedPane tabPanel;

	private static final String PANEL_TITLE_LBL = "Comparisons";
	private static final String PANEL_DESC_LBL = "Find which cells are common between datasets";

	public ComparisonDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		this.setLayout(new BorderLayout());

		tabPanel = new JTabbedPane(JTabbedPane.TOP);

		DetailPanel vennPanel = new VennDetailPanel();
		DetailPanel pairwiseVennPanel = new PairwiseVennDetailPanel();

		addPanel(tabPanel, vennPanel);
		addPanel(tabPanel, pairwiseVennPanel);

		this.add(tabPanel, BorderLayout.CENTER);
	}
}
