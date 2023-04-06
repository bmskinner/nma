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
package com.bmskinner.nma.gui.tabs.nuclear;

import java.awt.BorderLayout;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class NuclearStatisticsPanel extends DetailPanel {

	private static final String PANEL_TITLE_LBL = "Nuclear charts";
	private static final String PANEL_DESC_LBL = "View measurements from nuclei and filter datasets";

	private JTabbedPane tabPane;

	public NuclearStatisticsPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);

		this.setLayout(new BorderLayout());
		tabPane = new JTabbedPane(SwingConstants.TOP);

		DetailPanel nuclearStatsPanel = new NuclearStatsPanel();
		DetailPanel boxplotPanel = new NuclearBoxplotsPanel();
		DetailPanel wilcoxonPanel = new WilcoxonDetailPanel();
		DetailPanel nucleusMagnitudePanel = new NucleusMagnitudePanel();

		DetailPanel nuclearScatterChartPanel = new NuclearScatterChartPanel();

		addPanel(tabPane, nuclearStatsPanel);
		addPanel(tabPane, boxplotPanel);
		addPanel(tabPane, wilcoxonPanel);
		addPanel(tabPane, nucleusMagnitudePanel);
		addPanel(tabPane, nuclearScatterChartPanel);

		if (GlobalOptions.getInstance().getBoolean(GlobalOptions.IS_GLCM_INTERFACE_KEY)) {
			DetailPanel nuclearGlcmPanel = new NuclearGlcmPanel();
			addPanel(tabPane, nuclearGlcmPanel);
		}

		this.add(tabPane, BorderLayout.CENTER);
	}
}
