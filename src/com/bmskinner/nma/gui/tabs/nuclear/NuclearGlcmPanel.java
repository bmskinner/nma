/*******************************************************************************
 * Copyright (C) 2020 Ben Skinner
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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.components.panels.ExportableChartPanel;
import com.bmskinner.nma.gui.components.panels.ViolinChartPanel;
import com.bmskinner.nma.gui.events.GLCMUpdateListener;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.tabs.BoxplotsTabPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Display GLCM values measured for nuclei
 * 
 * @author Ben Skinner
 * @since 1.18.0
 *
 */
@SuppressWarnings("serial")
public class NuclearGlcmPanel extends BoxplotsTabPanel
		implements SwatchUpdatedListener, GLCMUpdateListener {

	private static final Logger LOGGER = Logger.getLogger(NuclearGlcmPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "GLCM";
	private static final String PANEL_DESC_LBL = "Grey level co-ocurrance matrix measurements of image texture";

	public NuclearGlcmPanel() {
		super(CellularComponent.NUCLEUS, PANEL_TITLE_LBL, PANEL_DESC_LBL);

		Dimension preferredSize = new Dimension(200, 300);

		for (Measurement stat : Measurement.getGlcmStats()) {

			JFreeChart chart = AbstractChartFactory.createEmptyChart();
			ViolinChartPanel panel = new ViolinChartPanel(chart);
			panel.getChartRenderingInfo().setEntityCollection(null);
			panel.setPreferredSize(preferredSize);
			chartPanels.put(stat.toString(), panel);
			mainPanel.add(panel);
		}
		this.add(scrollPane, BorderLayout.CENTER);
		uiController.addSwatchUpdatedListener(this);
		uiController.addGlcmUpdatedEventListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update(getDatasets());
	}

	@Override
	protected synchronized void updateSingle() {
		updateMultiple();

	}

	@Override
	protected synchronized void updateMultiple() {

		// TODO: when GlobalOptions.IS_GLCM_INTERFACE_KEY is true, we try to visualise
		// GLCM values when a dataset is selected.
		// If GLCM has not yet been calculated manually, this will give a default value
		// of -1 (ERROR_CALCULATING_STAT)
		// This adds a series of -1s to the nucleus measurements
		// This changes the nucleus hashcode
		// This triggers a "Do you want to save the dataset" message on exit, even
		// though the dataset should not have changed just because we selected it.
		// This needs to only trigger displaying values if GLCM is
		// available

		for (Measurement stat : Measurement.getGlcmStats()) {

			ExportableChartPanel panel = chartPanels.get(stat.toString());

			ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets())
					.addStatistic(stat)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setTarget(panel).build();

			setChart(options);
		}

	}

	@Override
	protected synchronized void updateNull() {
		LOGGER.finest("Passing to update multiple in " + this.getClass().getName());
		updateMultiple();
	}

	@Override
	public synchronized void setLoading() {
		super.setLoading();

		for (Measurement stat : Measurement.getGlcmStats()) {
			ExportableChartPanel panel = chartPanels.get(stat.toString());
			panel.setChart(AbstractChartFactory.createLoadingChart());

		}
	}

	@Override
	public void globalPaletteUpdated() {
		update(getDatasets());
	}

	@Override
	public void colourUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void GLCMDataAdded(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

}
