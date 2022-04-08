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
package com.bmskinner.nuclear_morphology.gui.tabs.cells;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JScrollPane;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.gui.events.revamp.ScaleUpdatedListener;
import com.bmskinner.nuclear_morphology.gui.tabs.BoxplotsTabPanel;
import com.bmskinner.nuclear_morphology.visualisation.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ExportableChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.charts.panels.ViolinChartPanel;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptions;
import com.bmskinner.nuclear_morphology.visualisation.options.ChartOptionsBuilder;

/**
 * Display boxplots for whole cell data
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CellsBoxplotsPanel extends BoxplotsTabPanel implements ActionListener, ScaleUpdatedListener {

	private static final Logger LOGGER = Logger.getLogger(CellsBoxplotsPanel.class.getName());

	public CellsBoxplotsPanel(@NonNull InputSupplier context) {
		super(context, CellularComponent.WHOLE_CELL);

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

			ChartOptions options = new ChartOptionsBuilder().setDatasets(getDatasets()).addStatistic(stat)
					.setScale(GlobalOptions.getInstance().getScale()).setSwatch(GlobalOptions.getInstance().getSwatch())
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
	public void setChartsAndTablesLoading() {
		super.setChartsAndTablesLoading();

		for (Measurement stat : Measurement.getCellStats()) {
			ExportableChartPanel panel = chartPanels.get(stat.toString());
			panel.setChart(MorphologyChartFactory.createLoadingChart());

		}
	}

	@Override
	public void scaleUpdated(List<IAnalysisDataset> datasets) {
		refreshChartCache(datasets);
	}

	@Override
	public void scaleUpdated(IAnalysisDataset dataset) {
		refreshChartCache(dataset);
	}

	@Override
	public void scaleUpdated() {
		update(getDatasets());
	}
}
