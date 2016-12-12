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
package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import java.awt.Dimension;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.HistogramChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.SelectableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.charting.options.DefaultChartOptions;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.components.HistogramsTabPanel;
import com.bmskinner.nuclear_morphology.stats.SignalStatistic;

@SuppressWarnings("serial")
public class SignalsHistogramPanel extends HistogramsTabPanel {
    	  	    	
	public SignalsHistogramPanel() throws Exception{
		super();
		
		try {

			Dimension preferredSize = new Dimension(400, 150);
			for(SignalStatistic stat : SignalStatistic.values()){
				
				JFreeChart chart = HistogramChartFactory.createEmptyChart();
				SelectableChartPanel panel = new SelectableChartPanel(chart, stat.toString());
//				SelectableChartPanel panel = new SelectableChartPanel(new HistogramChartFactory(options).createStatisticHistogram(), stat.toString());
				panel.setPreferredSize(preferredSize);
				panel.addSignalChangeListener(this);
				chartPanels.put(stat.toString(), panel);
				mainPanel.add(panel);

			}

		} catch(Exception e){
			warn("Error creating signal histogram panel");
			stack("Error creating histogram panel", e);
		}
		
	}
	
	@Override
	protected void updateSingle() {
		this.setEnabled(true);
		
		boolean useDensity = useDensityPanel.isSelected();
		
		for(SignalStatistic stat : SignalStatistic.values()){
			SelectableChartPanel panel = chartPanels.get(stat.toString());

//			JFreeChart chart = null;

			ChartOptions options = new ChartOptionsBuilder()
				.setDatasets(getDatasets())
				.addStatistic(stat)
				.setScale(GlobalOptions.getInstance().getScale())
				.setSwatch(GlobalOptions.getInstance().getSwatch())
				.setUseDensity(useDensity)
				.setTarget(panel)
				.build();
			
			
			setChart(options);
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
