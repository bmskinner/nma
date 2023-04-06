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
package com.bmskinner.nma.gui.tabs.segments;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JTabbedPane;

import com.bmskinner.nma.gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class SegmentsDetailPanel extends DetailPanel {

	private static final String PANEL_TITLE_LBL = "Nuclear segments";
	private static final String PANEL_DESC_LBL = "View segment lengths across nuclei";

	public SegmentsDetailPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		this.setLayout(new BorderLayout());

		JTabbedPane tabPanel = new JTabbedPane(JTabbedPane.TOP);

		DetailPanel segmentBoxplotsPanel = new SegmentBoxplotsPanel();
		DetailPanel segmentWilcoxonPanel = new SegmentWilcoxonPanel();
		DetailPanel segmentMagnitudePanel = new SegmentMagnitudePanel();

		Dimension minimumChartSize = new Dimension(100, 100);

		segmentBoxplotsPanel.setMinimumSize(minimumChartSize);
		segmentWilcoxonPanel.setMinimumSize(minimumChartSize);
		segmentMagnitudePanel.setMinimumSize(minimumChartSize);

		addPanel(tabPanel, segmentBoxplotsPanel);
		addPanel(tabPanel, segmentWilcoxonPanel);
		addPanel(tabPanel, segmentMagnitudePanel);

		this.add(tabPanel, BorderLayout.CENTER);

	}
}
