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
package com.bmskinner.nuclear_morphology.gui.tabs.segments;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.ProfileDisplayPanel;
import com.bmskinner.nuclear_morphology.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;

@SuppressWarnings("serial")
public class SegmentProfilePanel extends ProfileDisplayPanel {

	public SegmentProfilePanel() {
		super(ProfileType.ANGLE);
		this.remove(buttonPanel); // customisation is not needed here
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		return new ProfileChartFactory(options).createProfileChart();
	}

}
