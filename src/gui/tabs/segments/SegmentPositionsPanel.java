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

import gui.components.FixedAspectRatioChartPanel;
import gui.tabs.BoxplotsTabPanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import charting.charts.ConsensusNucleusChartFactory;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class SegmentPositionsPanel extends BoxplotsTabPanel {

	private Dimension preferredSize = new Dimension(300, 300);
			
	public SegmentPositionsPanel(){
		super();
		
		// Not needed for a consensus panel
		headerPanel.remove(measurementUnitSettingsPanel);

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
		return MorphologyChartFactory.makeSegmentStartPositionChart(options);
	}


	@Override
	protected void updateSingle() throws Exception {
		updateMultiple();
	}
	

	


	@Override
	protected void updateMultiple() throws Exception {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));

		log(Level.FINEST, "Dataset list is not empty");
		
		// Check that all the datasets have the same number of segments
		if(checkSegmentCountsMatch(getDatasets())){

			CellCollection collection = activeDataset().getCollection();
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.REFERENCE_POINT)
					.getOrderedSegments();


			// Get each segment as a boxplot
			for(NucleusBorderSegment seg : segments){

				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setSegPosition(seg.getPosition())
					.setSegID(seg.getID())
					.build();
				
				log(Level.FINEST, "Making segment start position chart for seg "+seg.getName());

				JFreeChart chart = getChart(options);

				FixedAspectRatioChartPanel chartPanel = new FixedAspectRatioChartPanel(chart);
				
				chartPanel.setPreferredSize(preferredSize);
				chartPanel.setSize(preferredSize);
				chartPanels.put(seg.getName(), chartPanel);
				mainPanel.add(chartPanel);			
//				
			}

		} else { // different number of segments, blank chart
			this.setEnabled(false);
			mainPanel.setLayout(new FlowLayout());
			mainPanel.add(new JLabel("Segment number is not consistent across datasets", JLabel.CENTER));
//			scrollPane.setViewportView(mainPanel);
		}

		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
		
		/*
		 * Ensure charts maintain aspect ratio
		 */
		for(ChartPanel panel : chartPanels.values()){
			
			panel.restoreAutoBounds();
		}
	}


	@Override
	protected void updateNull() throws Exception {
		this.setEnabled(false);
		mainPanel.setLayout(new FlowLayout());
		mainPanel.add(new JLabel("No datasets selected", JLabel.CENTER));
		scrollPane.setViewportView(mainPanel);
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
	}
}
