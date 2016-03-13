/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs.signals;

import gui.components.ExportableChartPanel;
import gui.tabs.BoxplotsTabPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JScrollPane;

import org.jfree.chart.JFreeChart;

import stats.NucleusStatistic;
import stats.SignalStatistic;
import components.generic.MeasurementScale;
import charting.charts.BoxplotChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;

@SuppressWarnings("serial")
public class SignalsBoxplotPanel extends BoxplotsTabPanel {

	public SignalsBoxplotPanel(){
		super();
		createUI();
	}

	private void createUI(){

		this.setLayout(new BorderLayout());
		Dimension preferredSize = new Dimension(200, 300);

		for(SignalStatistic stat : SignalStatistic.values()){

			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(null)
					.setLogger(programLogger)
					.setStatistic(stat)
					.setScale(MeasurementScale.PIXELS)
					.build();

			JFreeChart chart = null;
			try {
				chart = BoxplotChartFactory.createStatisticBoxplot(options);
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error creating boxplots panel", e);
			}

			ExportableChartPanel panel = new ExportableChartPanel(chart);
			panel.setPreferredSize(preferredSize);
			chartPanels.put(stat.toString(), panel);
			mainPanel.add(panel);
		}

		// add the scroll pane to the tab
		scrollPane  = new JScrollPane(mainPanel);
		this.add(scrollPane, BorderLayout.CENTER);

		measurementUnitSettingsPanel.addActionListener(this);
		measurementUnitSettingsPanel.setEnabled(false);
		this.add(measurementUnitSettingsPanel, BorderLayout.NORTH);
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		update(getDatasets());

	}

	@Override
	protected void updateSingle() throws Exception {
		updateMultiple();

	}

	@Override
	protected void updateMultiple() throws Exception {
		measurementUnitSettingsPanel.setEnabled(true);
		MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();

		for(SignalStatistic stat : SignalStatistic.values()){

			ExportableChartPanel panel = chartPanels.get(stat.toString());

			JFreeChart chart = null;

			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(getDatasets())
					.setLogger(programLogger)
					.setStatistic(stat)
					.setScale(scale)
					.build();

			if(getChartCache().hasChart(options)){
				programLogger.log(Level.FINEST, "Using cached boxplot chart: "+stat.toString());
				chart = getChartCache().getChart(options);

			} else { // No cache

				chart = BoxplotChartFactory.createStatisticBoxplot(options);
				getChartCache().addChart(options, chart);
				programLogger.log(Level.FINEST, "Added cached boxplot chart: "+stat.toString());
			}

			panel.setChart(chart);
		}

	}

	@Override
	protected void updateNull() throws Exception {
		updateMultiple();
		measurementUnitSettingsPanel.setEnabled(false);
	}

}

