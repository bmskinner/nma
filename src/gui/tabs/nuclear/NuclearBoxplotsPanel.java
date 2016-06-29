/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.tabs.nuclear;

import gui.components.ExportableChartPanel;
import gui.components.panels.MeasurementUnitSettingsPanel;
import gui.tabs.BoxplotsTabPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;

import javax.swing.JScrollPane;

import org.jfree.chart.JFreeChart;

import stats.NucleusStatistic;
import charting.charts.BoxplotChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.generic.MeasurementScale;

@SuppressWarnings("serial")
public class NuclearBoxplotsPanel extends BoxplotsTabPanel implements ActionListener {

		public NuclearBoxplotsPanel() {
			super();

			Dimension preferredSize = new Dimension(200, 300);
			
			for(NucleusStatistic stat : NucleusStatistic.values()){
				
				ChartOptionsBuilder builder = new ChartOptionsBuilder();
				ChartOptions options = builder.setDatasets(getDatasets())
					.addStatistic(stat)
					.setScale(MeasurementScale.PIXELS)
					.build();

				JFreeChart chart = null;
				try {
					chart = BoxplotChartFactory.getInstance().createStatisticBoxplot(options);
				} catch (Exception e) {
					log(Level.SEVERE, "Error creating boxplots panel", e);
				}
				
				ExportableChartPanel panel = new ExportableChartPanel(chart);
				panel.setPreferredSize(preferredSize);
				chartPanels.put(stat.toString(), panel);
				mainPanel.add(panel);
				
			}
			
			// add the scroll pane to the tab
			scrollPane  = new JScrollPane(mainPanel);
			this.add(scrollPane, BorderLayout.CENTER);
			
//			measurementUnitSettingsPanel.addActionListener(this);
//			measurementUnitSettingsPanel.setEnabled(false);
//			this.add(measurementUnitSettingsPanel, BorderLayout.NORTH);
			
		}
								
		@Override
		public void actionPerformed(ActionEvent e) {
			
			update(getDatasets());
			
		}

		@Override
		protected void updateSingle() {
			super.updateSingle();
			finest("Passing to update multiple in "+this.getClass().getName());
			updateMultiple();
			
		}

		@Override
		protected void updateMultiple() {
			super.updateMultiple();
//			measurementUnitSettingsPanel.setEnabled(true);
//			MeasurementScale scale  = this.measurementUnitSettingsPanel.getSelected();

			for(NucleusStatistic stat : NucleusStatistic.values()){

				ExportableChartPanel panel = chartPanels.get(stat.toString());
				
				ChartOptionsBuilder builder = new ChartOptionsBuilder();
				ChartOptions options = builder.setDatasets(getDatasets())
					.addStatistic(stat)
					.setScale(MeasurementUnitSettingsPanel.getInstance().getSelected())
					.build();
				
				JFreeChart chart = getChart(options);
				panel.setChart(chart);
			}
			
		}

		@Override
		protected void updateNull() {
			super.updateNull();
			finest("Passing to update multiple in "+this.getClass().getName());
			updateMultiple();
//			measurementUnitSettingsPanel.setEnabled(false);
		}
		
		
		
	}
