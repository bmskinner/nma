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

import java.awt.Dimension;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;

import stats.SignalStatistic;
import charting.charts.HistogramChartFactory;
import charting.charts.panels.SelectableChartPanel;
import charting.options.DefaultChartOptions;
import charting.options.ChartOptionsBuilder;

@SuppressWarnings("serial")
public class SignalsHistogramPanel extends HistogramsTabPanel {
    	  	    	
	public SignalsHistogramPanel() throws Exception{
		super();
		
		try {

			Dimension preferredSize = new Dimension(400, 150);
			for(SignalStatistic stat : SignalStatistic.values()){

				ChartOptionsBuilder builder = new ChartOptionsBuilder();
				DefaultChartOptions options = builder
					.addStatistic(stat)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setUseDensity(false)
					.setSignalGroup(null)
					.build();
				
				SelectableChartPanel panel = new SelectableChartPanel(new HistogramChartFactory(options).createStatisticHistogram(), stat.toString());
				panel.setPreferredSize(preferredSize);
				panel.addSignalChangeListener(this);
				chartPanels.put(stat.toString(), panel);
				mainPanel.add(panel);

			}

		} catch(Exception e){
			log(Level.SEVERE, "Error creating histogram panel", e);
		}
		
		// Keep disabled until CPU use is fixed
//		useDensityPanel.setEnabled(false);
		
	}
	
	@Override
	protected void updateSingle() {
		this.setEnabled(true);
		
		// Keep disabled until CPU use is fixed
//		useDensityPanel.setEnabled(false);
		
		boolean useDensity = useDensityPanel.isSelected();
		
		for(SignalStatistic stat : SignalStatistic.values()){
			SelectableChartPanel panel = chartPanels.get(stat.toString());
			
//			XYPlot plot = (XYPlot) chart.getPlot();
//			plot.setDomainPannable(true);
//			plot.setRangePannable(true);

			JFreeChart chart = null;
			
			ChartOptionsBuilder builder = new ChartOptionsBuilder();
			DefaultChartOptions options = builder.setDatasets(getDatasets())
				.addStatistic(stat)
				.setScale(GlobalOptions.getInstance().getScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setUseDensity(useDensity)
				.setTarget(panel)
				.build();
			
			
			setChart(options);
			
			chart = getChart(options);

			

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
