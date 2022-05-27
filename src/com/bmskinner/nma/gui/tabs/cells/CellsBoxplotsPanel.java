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
package com.bmskinner.nma.gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JScrollPane;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.gui.events.ScaleUpdatedListener;
import com.bmskinner.nma.gui.events.SwatchUpdatedListener;
import com.bmskinner.nma.gui.tabs.BoxplotsTabPanel;
import com.bmskinner.nma.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nma.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nma.visualisation.charts.panels.ViolinChartPanel;
import com.bmskinner.nma.visualisation.options.ChartOptions;
import com.bmskinner.nma.visualisation.options.ChartOptionsBuilder;

/**
 * Display boxplots for whole cell data
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CellsBoxplotsPanel extends BoxplotsTabPanel
		implements ActionListener, ScaleUpdatedListener, SwatchUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(CellsBoxplotsPanel.class.getName());

	public CellsBoxplotsPanel() {
		super(CellularComponent.WHOLE_CELL);

		Dimension preferredSize = new Dimension(200, 300);

		for (Measurement stat : Measurement.getCellStats()) {

			JFreeChart chart = AbstractChartFactory.createEmptyChart();
			ViolinChartPanel panel = new ViolinChartPanel(chart);

			panel.setPreferredSize(preferredSize);
			chartPanels.put(stat.toString(), panel);
			mainPanel.add(panel);

		}

		// add the scroll pane to the tab
		scrollPane = new JScrollPane(mainPanel);
		this.add(scrollPane, BorderLayout.CENTER);

		uiController.addScaleUpdatedListener(this);
		uiController.addSwatchUpdatedListener(this);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		update(getDatasets());

	}

	@Override
	protected synchronized void updateSingle() {
		super.updateSingle();
		LOGGER.finest("Passing to update multiple in " + this.getClass().getName());
		updateMultiple();

	}

	@Override
	protected synchronized void updateMultiple() {
		super.updateMultiple();

		for (Measurement stat : Measurement.getCellStats()) {

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
		super.updateNull();
		updateMultiple();
	}

	@Override
	public void setLoading() {
		super.setLoading();

		for (Measurement stat : Measurement.getCellStats()) {
			ExportableChartPanel panel = chartPanels.get(stat.toString());
			panel.setChart(AbstractChartFactory.createLoadingChart());

		}
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

	@Override
	public void globalPaletteUpdated() {
		update(getDatasets());
	}

	@Override
	public void colourUpdated(IAnalysisDataset dataset) {
		refreshCache(dataset);
	}
}
