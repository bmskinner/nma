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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.events.NuclearSignalUpdatedListener;
import com.bmskinner.nma.gui.events.ScaleUpdatedListener;
import com.bmskinner.nma.gui.tabs.BoxplotsTabPanel;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.visualisation.charts.ViolinChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.charts.panels.ViolinChartPanel;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

@SuppressWarnings("serial")
public class SignalsBoxplotPanel extends BoxplotsTabPanel
		implements NuclearSignalUpdatedListener, ScaleUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(SignalsBoxplotPanel.class.getName());

	private static final String PANEL_TITLE_LBL = "Violin plots";
	private static final String PANEL_DESC_LBL = "Distributions of measured values with boxplots";

	public SignalsBoxplotPanel() {
		super(CellularComponent.NUCLEAR_SIGNAL, PANEL_TITLE_LBL, PANEL_DESC_LBL);
		createUI();
		uiController.addNuclearSignalUpdatedListener(this);
		uiController.addScaleUpdatedListener(this);

	}

	private void createUI() {

		this.setLayout(new BorderLayout());
		Dimension preferredSize = new Dimension(200, 300);

		for (Measurement stat : Measurement.getSignalStats()) {

			ChartOptions options = new ChartOptionsBuilder().addStatistic(stat)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.build();

			JFreeChart chart = null;
			try {
				chart = new ViolinChartFactory(options)
						.createStatisticPlot(CellularComponent.NUCLEAR_SIGNAL);
			} catch (Exception e) {
				LOGGER.log(Loggable.STACK, "Error creating boxplots panel", e);
			}

			ViolinChartPanel panel = new ViolinChartPanel(chart);
			panel.setPreferredSize(preferredSize);
			chartPanels.put(stat.toString(), panel);
			mainPanel.add(panel);
		}

		// add the scroll pane to the tab
		this.add(scrollPane, BorderLayout.CENTER);

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

		for (Measurement stat : Measurement.getSignalStats()) {

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
		updateMultiple();
	}

	@Override
	public void nuclearSignalUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void nuclearSignalUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void scaleUpdated(List<IAnalysisDataset> datasets) {
		refreshCache(datasets);
	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}

	@Override
	public void scaleUpdated() {
		update(getDatasets());
	}
}
