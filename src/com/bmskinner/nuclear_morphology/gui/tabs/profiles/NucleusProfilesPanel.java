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
package com.bmskinner.nuclear_morphology.gui.tabs.profiles;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JTabbedPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ProfilesUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NucleusProfilesPanel extends DetailPanel implements ProfilesUpdatedListener {

	JTabbedPane tabPanel;

	private static final String PANEL_TITLE_LBL = "Nuclear profiles";

	public NucleusProfilesPanel(@NonNull InputSupplier context) {
		super(context, PANEL_TITLE_LBL);
		this.setLayout(new BorderLayout());
		tabPanel = new JTabbedPane(JTabbedPane.TOP);

		for (ProfileType type : ProfileType.displayValues()) {

			DetailPanel panel = new ProfileDisplayPanel(context, type);
			tabPanel.addTab(panel.getPanelTitle(), panel);
		}

		DetailPanel variabilityChartPanel = new VariabilityDisplayPanel(context);

		tabPanel.addTab(variabilityChartPanel.getPanelTitle(), variabilityChartPanel);
		this.add(tabPanel, BorderLayout.CENTER);

		uiController.addProfilesUpdatedListener(this);
	}

	@Override
	public void profilesUpdated(List<IAnalysisDataset> datasets) {
		refreshChartCache(datasets);
	}

	@Override
	public void profilesUpdated(IAnalysisDataset dataset) {
		refreshChartCache(dataset);
	}
}
