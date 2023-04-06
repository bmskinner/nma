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
import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.gui.components.panels.ProfileMarkersOptionsPanel;
import com.bmskinner.nma.gui.components.panels.ProfileTypeOptionsPanel;
import com.bmskinner.nma.gui.events.ProfilesUpdatedListener;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.tabs.ChartDetailPanel;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Display variability information about profiles
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class VariabilityDisplayPanel extends ChartDetailPanel
		implements ProfilesUpdatedListener, SwatchUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(VariabilityDisplayPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Variability";
	private static final String PANEL_DESC_LBL = "Which parts of a profile are most variable between nuclei";

	private JPanel buttonPanel = new JPanel(new FlowLayout());
	protected ExportableChartPanel chartPanel;

	private ProfileTypeOptionsPanel profileCollectionTypeSettingsPanel = new ProfileTypeOptionsPanel();

	private ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();

	public VariabilityDisplayPanel() {
		super(PANEL_TITLE_LBL, PANEL_DESC_LBL);
		this.setLayout(new BorderLayout());

		ChartOptions options = new ChartOptionsBuilder().setProfileType(ProfileType.ANGLE).build();

		JFreeChart chart = new ProfileChartFactory(options).createVariabilityChart();

		chartPanel = new ExportableChartPanel(chart);
		chartPanel.getChartRenderingInfo().setEntityCollection(null);
		this.add(chartPanel, BorderLayout.CENTER);

		profileCollectionTypeSettingsPanel.addActionListener(e -> update(getDatasets()));
		profileCollectionTypeSettingsPanel.setEnabled(false);
		buttonPanel.add(profileCollectionTypeSettingsPanel);

		buttonPanel.revalidate();

		this.add(buttonPanel, BorderLayout.NORTH);

		uiController.addProfilesUpdatedListener(this);
		uiController.addSwatchUpdatedListener(this);
	}

	@Override
	public void setEnabled(boolean b) {
		profileCollectionTypeSettingsPanel.setEnabled(b);
		profileMarkersOptionsPanel.setEnabled(b);
	}

	/**
	 * Update the profile panel with data from the given options
	 */
	private void updateProfiles(ChartOptions options) {
		try {
			setChart(options);
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error in plotting variability chart", e);
		}
	}

	@Override
	protected synchronized void updateSingle() {

		this.setEnabled(true);
		boolean showMarkers = profileMarkersOptionsPanel.isShowAnnotations();
		ProfileType type = profileCollectionTypeSettingsPanel.getSelected();

		ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.setNormalised(true)
				.setAlignment(ProfileAlignment.LEFT)
				.setLandmark(OrientationMark.REFERENCE)
				.setShowMarkers(showMarkers)
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setProfileType(type)
				.setTarget(chartPanel).build();

		updateProfiles(options);

	}

	@Override
	protected synchronized void updateMultiple() {
		updateSingle();
		// Don't allow marker selection for multiple datasets
		profileMarkersOptionsPanel.setEnabled(false);
	}

	@Override
	protected synchronized void updateNull() {
		updateSingle();
		this.setEnabled(false);
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();
		chartPanel.setChart(AbstractChartFactory.createLoadingChart());
	}

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		return new ProfileChartFactory(options).createVariabilityChart();
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
