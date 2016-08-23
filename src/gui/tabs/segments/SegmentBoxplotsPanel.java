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
import gui.GlobalOptions;
import gui.Labels;
import gui.tabs.BoxplotsTabPanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import stats.SegmentStatistic;
import charting.charts.BoxplotChartFactory;
import charting.charts.ExportableChartPanel;
import charting.charts.ViolinChartPanel;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;

@SuppressWarnings("serial")
public class SegmentBoxplotsPanel extends BoxplotsTabPanel implements ActionListener, ChartSetEventListener {

	private Dimension preferredSize = new Dimension(200, 300);
			
	public SegmentBoxplotsPanel(){
		super();

		JFreeChart boxplot = BoxplotChartFactory.getInstance().makeEmptyChart();
		

		ExportableChartPanel chartPanel = new ExportableChartPanel(boxplot);
		chartPanel.setPreferredSize(preferredSize);
		chartPanels.put("null", chartPanel);

		mainPanel.add(chartPanel);

		
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		update(getDatasets());
		
	}


	@Override
	protected void updateSingle() {
		updateMultiple();
		
	}


	@Override
	protected void updateMultiple() {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
				
		log(Level.FINEST, "Dataset list is not empty");

		// Check that all the datasets have the same number of segments
		if(NucleusBorderSegment.segmentCountsMatch(getDatasets())){ // make a boxplot for each segment
			
			CellCollection collection = activeDataset().getCollection();
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.ANGLE)
					.getSegmentedProfile(BorderTagObject.REFERENCE_POINT)
					.getOrderedSegments();
			

			// Get each segment as a boxplot
			for(NucleusBorderSegment seg : segments){
				
				JFreeChart chart = BoxplotChartFactory.getInstance().makeEmptyChart();
				ViolinChartPanel chartPanel = new ViolinChartPanel(chart);
				chartPanel.addChartSetEventListener(this);
				chartPanel.setPreferredSize(preferredSize);
				chartPanels.put(seg.getName(), chartPanel);
				mainPanel.add(chartPanel);	
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.addStatistic(SegmentStatistic.LENGTH)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setSegPosition(seg.getPosition())
					.setTarget(chartPanel)
					.build();

				
				setChart(options);
			}

			
		} else { // different number of segments, blank chart
			mainPanel.setLayout(new FlowLayout());
			mainPanel.add(new JLabel(Labels.INCONSISTENT_SEGMENT_NUMBER, JLabel.CENTER));
		}
		mainPanel.revalidate();
		mainPanel.repaint();
				
		scrollPane.setViewportView(mainPanel);
	}


	@Override
	protected void updateNull() {
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
		
		ChartPanel chartPanel = new ChartPanel(BoxplotChartFactory.getInstance().makeEmptyChart());
		mainPanel.add(chartPanel);
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
	}

	@Override
	public void chartSetEventReceived(ChartSetEvent e) {
		((ViolinChartPanel) e.getSource()).restoreAutoBounds();
	}
}