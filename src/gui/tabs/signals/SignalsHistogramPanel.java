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
import gui.components.HistogramsTabPanel;
import gui.components.panels.MeasurementUnitSettingsPanel;

import java.awt.Dimension;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import stats.SignalStatistic;
import charting.charts.HistogramChartFactory;
import charting.charts.SelectableChartPanel;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.generic.MeasurementScale;

@SuppressWarnings("serial")
public class SignalsHistogramPanel extends HistogramsTabPanel {
    	  	    	
	public SignalsHistogramPanel() throws Exception{
		super();
		
		try {

			Dimension preferredSize = new Dimension(400, 150);
			for(SignalStatistic stat : SignalStatistic.values()){

				ChartOptionsBuilder builder = new ChartOptionsBuilder();
				ChartOptions options = builder
					.addStatistic(stat)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setUseDensity(false)
					.setSignalGroup(null)
					.build();
				
				SelectableChartPanel panel = new SelectableChartPanel(HistogramChartFactory.getInstance().createStatisticHistogram(options), stat.toString());
				panel.setPreferredSize(preferredSize);
				panel.addSignalChangeListener(this);
				chartPanels.put(stat.toString(), panel);
				mainPanel.add(panel);

			}

		} catch(Exception e){
			log(Level.SEVERE, "Error creating histogram panel", e);
		}
		
	}
	
	@Override
	protected void updateSingle() {
		this.setEnabled(true);
		
		boolean useDensity = useDensityPanel.isSelected();
		
		for(SignalStatistic stat : SignalStatistic.values()){
			SelectableChartPanel panel = chartPanels.get(stat.toString());

			JFreeChart chart = null;
			
			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(getDatasets())
				.addStatistic(stat)
				.setScale(GlobalOptions.getInstance().getScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setUseDensity(useDensity)
				.build();
			
			
			chart = getChart(options);

			XYPlot plot = (XYPlot) chart.getPlot();
			plot.setDomainPannable(true);
			plot.setRangePannable(true);

			panel.setChart(chart);
		}
		
		
	}

	@Override
	protected void updateMultiple() {
		updateSingle();
		
	}

	@Override
	protected void updateNull() {
		this.setEnabled(false);
		
	}
	
}
