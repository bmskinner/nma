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
package gui.tabs.segments;

import gui.ChartSetEvent;
import gui.ChartSetEventListener;
import gui.Labels;
import gui.tabs.BoxplotsTabPanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import charting.charts.ExportableChartPanel;
import charting.charts.MorphologyChartFactory;
import charting.charts.OutlineChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;

/**
 * Holds a series of outline panels showing the locations of the segment
 * starts for all nuclei within a dataset
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class SegmentPositionsPanel extends BoxplotsTabPanel implements ChartSetEventListener  {

	private Dimension preferredSize = new Dimension(300, 300);
			
	public SegmentPositionsPanel(){
		super();
		
		try {
			this.updateNull();
		} catch (Exception e) {
			error("Error creating segments posistion panel", e);
		}		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		update(getDatasets());
		
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception{
		return MorphologyChartFactory.getInstance().makeSegmentStartPositionChart(options);
	}


	@Override
	protected void updateSingle() {
		super.updateSingle();
		finest("Passing to update multiple datasets");
		updateMultiple();
	}
	

	


	@Override
	protected void updateMultiple() {
		super.updateMultiple();
		
		// Replace all the charts on the main panel
		
		finest("Creating new main panel");
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		finest("Checking segment counts");
				
		// Check that all the datasets have the same number of segments
		if(NucleusBorderSegment.segmentCountsMatch(getDatasets())){
			
			finest("Segment counts match");

			CellCollection collection = activeDataset().getCollection();
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.ANGLE)
					.getSegmentedProfile(BorderTagObject.REFERENCE_POINT)
					.getOrderedSegments();


			finest("Creating segment charts");
			
			// Get each segment as a boxplot
			for(NucleusBorderSegment seg : segments){

				finest("Creating chart for segment "+seg.getName());
				
				JFreeChart chart = OutlineChartFactory.getInstance().makeEmptyChart();
				ExportableChartPanel chartPanel = new ExportableChartPanel(chart);
				chartPanel.setFixedAspectRatio(true);
				chartPanel.addChartSetEventListener(this);
				chartPanel.setPreferredSize(preferredSize);
				chartPanel.setSize(preferredSize);
				chartPanels.put(seg.getName(), chartPanel);
				mainPanel.add(chartPanel);			
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setSegPosition(seg.getPosition())
					.setSegID(seg.getID())
					.setTarget(chartPanel)
					.build();
				
				setChart(options);

				
				finest("Adding new chart panel for segment "+seg.getName());

			}
			
			finest("Finshed creating segment charts");

		} else { // different number of segments, blank chart
			finest("Segment counts do not match");
			this.setEnabled(false);
			mainPanel.setLayout(new FlowLayout());
			mainPanel.add(new JLabel(Labels.INCONSISTENT_SEGMENT_NUMBER, JLabel.CENTER));
		}

		mainPanel.revalidate();
		mainPanel.repaint();
		
		scrollPane.setViewportView(mainPanel);
		
		/*
		 * Ensure charts maintain aspect ratio
		 */
		finest("Restoring aspect ratios");

	}
	



	@Override
	protected void updateNull() {
		super.updateNull();
		this.setEnabled(false);
		mainPanel.setLayout(new FlowLayout());
		mainPanel.add(new JLabel("No datasets selected", JLabel.CENTER));
		scrollPane.setViewportView(mainPanel);
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		((ExportableChartPanel) e.getSource()).restoreAutoBounds();
	}
}
