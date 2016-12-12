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
package com.bmskinner.nuclear_morphology.gui.tabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.datasets.AbstractDatasetCreator;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.TableOptions;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentBoxplotsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentHistogramsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentMagnitudePanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentPositionsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentProfilePanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentStatsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentWilcoxonPanel;

@SuppressWarnings("serial")
public class SegmentsDetailPanel extends DetailPanel {
		
	private SegmentStatsPanel 		segmentStatsPanel;		// Hold the start and end points of each segment
	private SegmentProfilePanel		segmentProfilePanel;	// draw the segments on the median profile
	private SegmentBoxplotsPanel 	segmentBoxplotsPanel;	// draw boxplots of segment lengths
	private SegmentHistogramsPanel 	segmentHistogramsPanel;	// draw boxplots of segment lengths
	private SegmentWilcoxonPanel	segmentWilcoxonPanel;	// stats between datasets
	private SegmentMagnitudePanel	segmentMagnitudePanel;  // magnitude differences between segments

	private JTabbedPane 			tabPanel;
	
	public SegmentsDetailPanel() {
		super();
		this.setLayout(new BorderLayout());
		
		tabPanel = new JTabbedPane(JTabbedPane.TOP);
		tabPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		JPanel panel = new JPanel(new GridBagLayout());
		
		Dimension minimumChartSize = new Dimension(100, 100);
		segmentProfilePanel  = new SegmentProfilePanel();
		this.addSubPanel(segmentProfilePanel);
		segmentProfilePanel.setMinimumSize(minimumChartSize);
		segmentProfilePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		segmentBoxplotsPanel = new SegmentBoxplotsPanel();
		segmentBoxplotsPanel.setMinimumSize(minimumChartSize);
		this.addSubPanel(segmentBoxplotsPanel);
		tabPanel.addTab("Boxplots", segmentBoxplotsPanel);
		
		
		segmentHistogramsPanel = new SegmentHistogramsPanel();
		segmentHistogramsPanel.setMinimumSize(minimumChartSize);
		this.addSubPanel(segmentHistogramsPanel);
		tabPanel.addTab("Histograms", segmentHistogramsPanel);
		
		segmentWilcoxonPanel = new SegmentWilcoxonPanel();
		segmentWilcoxonPanel.setMinimumSize(minimumChartSize);
		this.addSubPanel(segmentWilcoxonPanel);
		tabPanel.addTab("Stats", segmentWilcoxonPanel);
		
		segmentMagnitudePanel = new SegmentMagnitudePanel();
		segmentMagnitudePanel.setMinimumSize(minimumChartSize);
		this.addSubPanel(segmentMagnitudePanel);
		tabPanel.addTab("Magnitude", segmentMagnitudePanel);
		
		segmentStatsPanel = new SegmentStatsPanel();
		this.addSubPanel(segmentStatsPanel);
		segmentStatsPanel.setMinimumSize(minimumChartSize);
		segmentStatsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.weightx = 1;
		constraints.weighty = 0.5;
		constraints.anchor = GridBagConstraints.CENTER;
		
		panel.add(segmentStatsPanel, constraints);
		
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridheight = 1;
		constraints.gridwidth = 1;
		constraints.weighty = 1;
		panel.add(segmentProfilePanel, constraints);
		
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridheight = 2;
		constraints.gridwidth = 1;
		panel.add(tabPanel, constraints);
		
		this.add(panel, BorderLayout.CENTER);
		
		
	}			

	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) {
		return null;
	}

	@Override
	protected TableModel createPanelTableType(TableOptions options){
		return null;
	}
}
