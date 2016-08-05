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

import gui.Labels;
import gui.tabs.BoxplotsTabPanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import charting.charts.FixedAspectRatioChartPanel;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class SegmentPositionsPanel extends BoxplotsTabPanel {

	private Dimension preferredSize = new Dimension(300, 300);
			
	public SegmentPositionsPanel(){
		super();
		
		// Not needed for a consensus panel
//		headerPanel.remove(measurementUnitSettingsPanel);

		try {
			this.updateNull();
		} catch (Exception e) {
			log(Level.SEVERE, "Error creating segments posistion panel", e);
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
		finest("Creating new main panel");
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		
		mainPanel.addComponentListener( new ComponentListener(){

			@Override
			public void componentHidden(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void componentResized(ComponentEvent e) {
				restoreAspectRatio();
				
			}

			@Override
			public void componentShown(ComponentEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			
		});

		finest("Checking segment counts");
				
		// Check that all the datasets have the same number of segments
		if(NucleusBorderSegment.segmentCountsMatch(getDatasets())){
			
			finest("Segment counts match");

			CellCollection collection = activeDataset().getCollection();
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.ANGLE)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getOrderedSegments();


			finest("Creating segment charts");
			
			// Get each segment as a boxplot
			for(NucleusBorderSegment seg : segments){

				finest("Creating chart for segment "+seg.getName());
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setSegPosition(seg.getPosition())
					.setSegID(seg.getID())
					.build();
				

				JFreeChart chart = getChart(options);

				FixedAspectRatioChartPanel chartPanel = new FixedAspectRatioChartPanel(chart);
				
				finest("Adding new chart panel for segment "+seg.getName());
				
				chartPanel.setPreferredSize(preferredSize);
				chartPanel.setSize(preferredSize);
				chartPanels.put(seg.getName(), chartPanel);
				mainPanel.add(chartPanel);			
			
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
		restoreAspectRatio();
	}
	
	private void restoreAspectRatio(){
		for(ChartPanel panel : chartPanels.values()){
			
			((FixedAspectRatioChartPanel) panel).restoreAutoBounds();
		}
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
}
