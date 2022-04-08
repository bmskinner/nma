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
package com.bmskinner.nuclear_morphology.gui.components.panels;

import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.DraggableOverlayChartPanel;

public class SegmentationDualChartPanel extends DualChartPanel {

	protected List<Object> listeners = new ArrayList<>();

	public SegmentationDualChartPanel() {
		super(false);

		JFreeChart profileChart = ProfileChartFactory.createEmptyChart(ProfileType.ANGLE);
		chartPanel = new DraggableOverlayChartPanel(profileChart, null, false);
		((DraggableOverlayChartPanel) chartPanel).addSignalChangeListener(this);
	}

	public void setProfile(ISegmentedProfile profile, boolean normalised) {

		((DraggableOverlayChartPanel) chartPanel).setProfile(profile, normalised);
		this.updateChartPanelRange();
	}

	public void setCharts(JFreeChart chart, ISegmentedProfile profile, boolean normalised, JFreeChart rangeChart) {

		((DraggableOverlayChartPanel) chartPanel).setChart(chart, profile, normalised);
		rangePanel.setChart(rangeChart);
		this.updateChartPanelRange();
	}

	@Override
	public void eventReceived(DatasetEvent event) {
		// TODO Auto-generated method stub

	}

	@Override
	public void eventReceived(DatasetUpdateEvent event) {
		// TODO Auto-generated method stub

	}

}
