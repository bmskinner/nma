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

import gui.GlobalOptions;
import gui.tabs.BoxplotsTabPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.logging.Level;

import javax.swing.JScrollPane;

import org.jfree.chart.JFreeChart;

import stats.SignalStatistic;
import charting.charts.ExportableChartPanel;
import charting.charts.ViolinChartFactory;
import charting.charts.ViolinChartPanel;
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
			ChartOptions options = builder
					.addStatistic(stat)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.build();

			JFreeChart chart = null;
			try {
				chart = ViolinChartFactory.getInstance().createStatisticPlot(options);
			} catch (Exception e) {
				log(Level.SEVERE, "Error creating boxplots panel", e);
			}

			ViolinChartPanel panel = new ViolinChartPanel(chart);
			panel.setPreferredSize(preferredSize);
			chartPanels.put(stat.toString(), panel);
			mainPanel.add(panel);
		}

		// add the scroll pane to the tab
		scrollPane  = new JScrollPane(mainPanel);
		this.add(scrollPane, BorderLayout.CENTER);

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		update(getDatasets());

	}

	@Override
	protected void updateSingle() {
		updateMultiple();

	}

	@Override
	protected void updateMultiple() {

		for(SignalStatistic stat : SignalStatistic.values()){

			ExportableChartPanel panel = chartPanels.get(stat.toString());

			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(getDatasets())
					.addStatistic(stat)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.build();
			
			JFreeChart chart = getChart(options);
			panel.setChart(chart);

			panel.setChart(chart);
		}

	}

	@Override
	protected void updateNull() {
		updateMultiple();
	}

}

