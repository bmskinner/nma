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
package com.bmskinner.nma.gui.tabs.signals;

import javax.swing.BoxLayout;

import com.bmskinner.nma.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class SignalsOverviewPanel extends DetailPanel {

	private static final String PANEL_TITLE_LBL = "Overview";
	private static final String PANEL_DESC_LBL = "Locations of signals in the consensus nucleus";

	/**
	 * Create with an input supplier
	 * 
	 * @param inputSupplier the input supplier
	 */
	public SignalsOverviewPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		add(new SignalTablePanel());
		add(new SignalConsensusPanel());
	}
}
