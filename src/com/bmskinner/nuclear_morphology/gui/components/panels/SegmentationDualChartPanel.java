/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.components.panels;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.DraggableOverlayChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.gui.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;

public class SegmentationDualChartPanel extends DualChartPanel {

    public SegmentationDualChartPanel() {
        super();

        ChartOptions options = new ChartOptionsBuilder().setProfileType(ProfileType.ANGLE).setShowXAxis(false)
                .setShowYAxis(false).build();

        JFreeChart profileChart = ProfileChartFactory.makeEmptyChart(ProfileType.ANGLE);
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

	@Override
	public void eventReceived(InterfaceEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void eventReceived(ChartOptionsRenderedEvent event) {
		// TODO Auto-generated method stub
		
	}

}
