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

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nma.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Display a profile chart
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class ProfileDisplayPanel extends AbstractProfileDisplayPanel {

	public ProfileDisplayPanel(ProfileType type, @NonNull String panelTitle,
			@NonNull String panelDesc) {
		super(type, panelTitle, panelDesc);

		JFreeChart chart = ProfileChartFactory.createEmptyChart(type);
		chartPanel.setChart(chart);
	}

	@Override
	protected void updateSingle() {
		super.updateSingle();
		updateChart();

	}

	@Override
	protected void updateMultiple() {
		super.updateMultiple();
		updateChart();
	}

	@Override
	protected void updateNull() {
		super.updateNull();
		JFreeChart chart = ProfileChartFactory.createEmptyChart(type);
		chartPanel.setChart(chart);

	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		return new ProfileChartFactory(options).createProfileChart();
	}

	private void updateChart() {
		ChartOptions options = makeOptions();
		setChart(options);
	}

	private ChartOptions makeOptions() {

		boolean normalised = profileAlignmentOptionsPanel.isNormalised();
		ProfileAlignment alignment = normalised ? ProfileAlignment.LEFT
				: profileAlignmentOptionsPanel.getSelected();

		boolean showMarkers = profileMarkersOptionsPanel.isShowAnnotations();
		boolean hideProfiles = profileMarkersOptionsPanel.isShowNuclei();

		ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
				.setNormalised(normalised)
				.setAlignment(alignment).setLandmark(OrientationMark.REFERENCE)
				.setShowAnnotations(showMarkers)
				.setShowProfiles(hideProfiles).setSwatch(GlobalOptions.getInstance().getSwatch())
				.setProfileType(type)
				.setTarget(chartPanel).build();
		return options;
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
