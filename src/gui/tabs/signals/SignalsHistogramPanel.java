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

import gui.components.HistogramsTabPanel;
import gui.components.SelectableChartPanel;

import java.awt.Dimension;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;

import stats.SignalStatistic;
import charting.charts.HistogramChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.generic.MeasurementScale;

@SuppressWarnings("serial")
public class SignalsHistogramPanel extends HistogramsTabPanel {
    	  	    	
	public SignalsHistogramPanel(Logger programLogger) throws Exception{
		super(programLogger);
		
		try {

			MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();
			Dimension preferredSize = new Dimension(400, 150);
			for(SignalStatistic stat : SignalStatistic.values()){

				ChartOptionsBuilder builder = new ChartOptionsBuilder();
				ChartOptions options = builder.setDatasets(null)
					.setLogger(programLogger)
					.setStatistic(stat)
					.setScale(scale)
					.setUseDensity(false)
					.setSignalGroup(null)
					.build();
				
				SelectableChartPanel panel = new SelectableChartPanel(HistogramChartFactory.createStatisticHistogram(options), stat.toString());
				panel.setPreferredSize(preferredSize);
				panel.addSignalChangeListener(this);
				chartPanels.put(stat.toString(), panel);
				mainPanel.add(panel);

			}

		} catch(Exception e){
			programLogger.log(Level.SEVERE, "Error creating histogram panel", e);
		}
		
	}
	
	@Override
	protected void updateSingle() throws Exception {
		this.setEnabled(true);
		
		MeasurementScale scale  = measurementUnitSettingsPanel.getSelected();
		boolean useDensity = useDensityPanel.isSelected();
		
		for(SignalStatistic stat : SignalStatistic.values()){
			SelectableChartPanel panel = chartPanels.get(stat.toString());

			JFreeChart chart = null;
			
			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			ChartOptions options = builder.setDatasets(getDatasets())
				.setLogger(programLogger)
				.setStatistic(stat)
				.setScale(scale)
				.setUseDensity(useDensity)
				.build();
			
			
			chart = getChart(options);

//			if(this.getChartCache().hasChart(options)){
//				programLogger.log(Level.FINEST, "Using cached histogram: "+stat.toString());
//				chart = getChartCache().getChart(options);
//
//			} else { // No cache
//
//				chart = HistogramChartFactory.createStatisticHistogram(options);
//				getChartCache().addChart(options, chart);
//
//
//				programLogger.log(Level.FINEST, "Added cached histogram chart: "+stat);
//			}

			XYPlot plot = (XYPlot) chart.getPlot();
			plot.setDomainPannable(true);
			plot.setRangePannable(true);

			panel.setChart(chart);
		}
		
		
	}

	@Override
	protected void updateMultiple() throws Exception {
		 updateNull();
		
	}

	@Override
	protected void updateNull() throws Exception {
		this.setEnabled(false);
		
	}
	
}
