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

import java.awt.BorderLayout;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import com.bmskinner.nma.gui.tabs.DetailPanel;
import com.bmskinner.nma.gui.tabs.signals.warping.SignalWarpingMainPanel;


/**
 * The top level tab panel showing information on signals at the dataset level
 * 
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public class SignalsDetailPanel extends DetailPanel {

	private static final Logger LOGGER = Logger.getLogger(SignalsDetailPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Nuclear signals";
	private static final String PANEL_DESC_LBL = "Display FISH signals / other detected objects";
	private JTabbedPane signalsTabPane;

	/**
	 * Create the panel.
	 */
	public SignalsDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		try {

			this.setLayout(new BorderLayout());

			signalsTabPane = new JTabbedPane(SwingConstants.TOP);

			DetailPanel overviewPanel = new SignalsOverviewPanel();
			DetailPanel countsPanel = new SignalCountsPanel();
			DetailPanel boxplotPanel = new SignalsBoxplotPanel();
			DetailPanel shellsPanel = new SignalShellsPanel();
			DetailPanel detectionSettingsPanel = new SignalsAnalysisPanel();
			DetailPanel signalScatterChartPanel = new SignalScatterChartPanel();
			DetailPanel colocalistionPanel = new SignalsColocalisationPanel();
			DetailPanel warpingPanel = new SignalWarpingMainPanel();

			addPanel(signalsTabPane, overviewPanel);
			addPanel(signalsTabPane, detectionSettingsPanel);
			addPanel(signalsTabPane, countsPanel);
			addPanel(signalsTabPane, boxplotPanel);

			addPanel(signalsTabPane, signalScatterChartPanel);
			addPanel(signalsTabPane, shellsPanel);
			addPanel(signalsTabPane, colocalistionPanel);
			addPanel(signalsTabPane, warpingPanel);

			this.add(signalsTabPane, BorderLayout.CENTER);

		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Error making signal panel", e);
		}
	}
}
