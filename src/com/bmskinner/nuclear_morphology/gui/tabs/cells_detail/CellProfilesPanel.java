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
package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileTypeOptionsPanel;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ProfilesUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.events.revamp.SwatchUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.ChartDetailPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.visualisation.charts.ProfileChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;

/**
 * Editing panel for the border tags of a single cell.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class CellProfilesPanel extends ChartDetailPanel
		implements CellEditingTabPanel, ProfilesUpdatedListener, SwatchUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(CellProfilesPanel.class.getName());

	private ExportableChartPanel chartPanel;

	private ProfileTypeOptionsPanel profileOptions = new ProfileTypeOptionsPanel();

	private JPanel buttonsPanel;
	private JButton reverseProfileBtn = new JButton(Labels.Cells.REVERSE_PROFILE_BTN_LBL);

	private CellViewModel model;

	public CellProfilesPanel(CellViewModel model) {
		super(Labels.Cells.PROFILES_PANEL_TITLE_LBL);
		this.model = model;

		this.setLayout(new BorderLayout());

		buttonsPanel = makeButtonPanel();
		this.add(buttonsPanel, BorderLayout.NORTH);
		setButtonsEnabled(false);

		chartPanel = new ExportableChartPanel(ProfileChartFactory.createEmptyChart());
		this.add(chartPanel, BorderLayout.CENTER);

		this.setBorder(null);

		setButtonsEnabled(false);

		uiController.addProfilesUpdatedListener(this);
	}

	private JPanel makeButtonPanel() {

		JPanel panel = new JPanel(new FlowLayout()) {
			@Override
			public void setEnabled(boolean b) {
				super.setEnabled(b);
				for (Component c : this.getComponents()) {
					c.setEnabled(b);
				}
			}
		};

		panel.add(profileOptions);
		profileOptions.addActionListener(e -> update());

		reverseProfileBtn.addActionListener(e -> reverseProfileAction());
		panel.add(reverseProfileBtn);

		return panel;

	}

	/**
	 * Reverse the profiles for the active cell. Tags and segments are also
	 * reversed.
	 */
	private void reverseProfileAction() {
		if (model.hasCell()) {
			try {
				for (Nucleus n : model.getCell().getNuclei()) {
					n.reverse();
				}

				model.updateViews();

				// Trigger refresh of dataset median profile and charts
				activeDataset().getCollection().getProfileManager().recalculateProfileAggregates();
			} catch (ProfileException | MissingComponentException e) {
				LOGGER.log(Loggable.STACK, "Error recalculating profile aggregate", e);
			}
		}
	}

	public void setButtonsEnabled(boolean b) {
		buttonsPanel.setEnabled(b);
	}

	@Override
	public void update() {

		try {

			ProfileType type = profileOptions.getSelected();

			if (!model.hasCell()) {
				chartPanel.setChart(ProfileChartFactory.createEmptyChart());
				buttonsPanel.setEnabled(false);

			} else {

				ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).setCell(model.getCell())
						.setNormalised(false).setAlignment(ProfileAlignment.LEFT).setLandmark(OrientationMark.REFERENCE)
						.setShowMarkers(true).setProfileType(type).setSwatch(GlobalOptions.getInstance().getSwatch())
						.setShowAnnotations(false).setShowPoints(true).setShowXAxis(false).setShowYAxis(false)
						.setTarget(chartPanel).build();

				setChart(options);
				buttonsPanel.setEnabled(true);

			}

		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error updating cell panel", e);
			chartPanel.setChart(ProfileChartFactory.createErrorChart());
			buttonsPanel.setEnabled(false);
		}

	}

	@Override
	public void setLoading() {
		super.setLoading();
		chartPanel.setChart(ProfileChartFactory.createLoadingChart());
	}

	@Override
	protected JFreeChart createPanelChartType(@NonNull ChartOptions options) {
		return new ProfileChartFactory(options).createProfileChart();
	}

	@Override
	public void refreshCache() {
		clearCache();
		this.update();
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
	public void swatchUpdated() {
		update(getDatasets());
	}

	@Override
	public void checkCellLock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBorderTagAction(@NonNull OrientationMark tag, int newTagIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateSegmentStartIndexAction(@NonNull UUID id, int index) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public CellViewModel getCellModel() {
		return model;
	}

	@Override
	public void setCellModel(CellViewModel model) {
		this.model = model;
	}

	@Override
	public void clearCellCharts() {
		getCache().clear(model.getCell());
	}
}
