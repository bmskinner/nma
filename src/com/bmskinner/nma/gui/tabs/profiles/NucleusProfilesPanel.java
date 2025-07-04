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
package com.bmskinner.nma.gui.tabs.profiles;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.gui.events.ProfilesUpdatedListener;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NucleusProfilesPanel extends DetailPanel
		implements ProfilesUpdatedListener, SwatchUpdatedListener {

	JTabbedPane tabPanel;

	private static final String PANEL_TITLE_LBL = "Nuclear profiles";
	private static final String PANEL_DESC_LBL = "View angle profiles and other profiles";

	public NucleusProfilesPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		this.setLayout(new BorderLayout());
		tabPanel = new JTabbedPane(SwingConstants.TOP);

		for (ProfileType type : ProfileType.displayValues()) {

			DetailPanel panel = new ProfileDisplayPanel(type, type.getLabel(), type.getLabel());
			addPanel(tabPanel, panel);
		}

		DetailPanel variabilityChartPanel = new VariabilityDisplayPanel();
		addPanel(tabPanel, variabilityChartPanel);

		this.add(tabPanel, BorderLayout.CENTER);

		uiController.addProfilesUpdatedListener(this);
		uiController.addSwatchUpdatedListener(this);
	}

	@Override
	public void profilesUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void profilesUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void globalPaletteUpdated() {
		update(getDatasets());
	}

	@Override
	public void colourUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}
}
