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
package gui.tabs.profiles;

import gui.components.ExportableChartPanel;
import gui.components.panels.BorderTagOptionsPanel;
import gui.components.panels.ProfileAlignmentOptionsPanel;
import gui.components.panels.ProfileMarkersOptionsPanel;
import gui.tabs.DetailPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;

import stats.StatisticDimension;
import components.generic.ProfileType;
import charting.charts.MorphologyChartFactory;

@SuppressWarnings("serial")
public abstract class AbstractProfileDisplayPanel extends DetailPanel implements ActionListener {
		
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);

		protected JPanel buttonPanel = new JPanel(new FlowLayout());
		protected ExportableChartPanel chartPanel;
		
		protected BorderTagOptionsPanel borderTagOptionsPanel = new BorderTagOptionsPanel();
		protected ProfileAlignmentOptionsPanel profileAlignmentOptionsPanel = new ProfileAlignmentOptionsPanel();
		protected ProfileMarkersOptionsPanel profileMarkersOptionsPanel = new ProfileMarkersOptionsPanel();
		
		protected ProfileType type;
		
		public AbstractProfileDisplayPanel(ProfileType type){
			super();
			this.type = type;
			
			this.setLayout(new BorderLayout());
			JFreeChart rawChart = MorphologyChartFactory.getInstance().makeEmptyChart();
			chartPanel = makeProfileChartPanel(rawChart);
			
			
			
			chartPanel.setMinimumDrawWidth( 0 );
			chartPanel.setMinimumDrawHeight( 0 );
			this.setMinimumSize(minimumChartSize);
			this.setPreferredSize(preferredChartSize);
			this.add(chartPanel, BorderLayout.CENTER);
					
			// add the alignments panel to the tab
			
			buttonPanel.add(profileAlignmentOptionsPanel);
			profileAlignmentOptionsPanel.addActionListener(this);
			profileAlignmentOptionsPanel.setEnabled(false);
			
			buttonPanel.add(borderTagOptionsPanel);
			borderTagOptionsPanel.addActionListener(this);
			borderTagOptionsPanel.setEnabled(false);
			
			buttonPanel.add(profileMarkersOptionsPanel);
			profileMarkersOptionsPanel.addActionListener(this);
			profileMarkersOptionsPanel.setEnabled(false);
						
			this.add(buttonPanel, BorderLayout.NORTH);
		}
		
		private ExportableChartPanel makeProfileChartPanel(JFreeChart chart){
			ExportableChartPanel panel = new ExportableChartPanel(chart){
				@Override
				public void restoreAutoBounds() {
					XYPlot plot = (XYPlot) this.getChart().getPlot();
					
					int length = 100;
					for(int i = 0; i<plot.getDatasetCount();i++){
						XYDataset dataset = plot.getDataset(i);
						Number maximum = DatasetUtilities.findMaximumDomainValue(dataset);
						length = maximum.intValue() > length ? maximum.intValue() : length;
					}
					
					if(type.getDimension().equals(StatisticDimension.ANGLE)){
						plot.getRangeAxis().setRange(0, 360);
					} else {
						plot.getRangeAxis().setAutoRange(true);
					}
					
					plot.getDomainAxis().setRange(0, length);				
					return;
				} 
			};
			return panel;
		}
		
		public void setEnabled(boolean b){
			profileAlignmentOptionsPanel.setEnabled(b);
			borderTagOptionsPanel.setEnabled(b);
			profileMarkersOptionsPanel.setEnabled(b);
		}
		
		@Override
		protected void updateSingle() {
			
			this.setEnabled(true);

		}
		
		@Override
		protected void updateMultiple() {
			// Don't allow marker selection for multiple datasets
			this.setEnabled(true);
			profileMarkersOptionsPanel.setEnabled(false);
		}
		
		@Override
		protected void updateNull() {
			this.setEnabled(false);		
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			update(getDatasets());
		}
	}


