package com.bmskinner.nuclear_morphology.gui.tabs.segments;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.charts.AbstractChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.HistogramChartFactory;
import com.bmskinner.nuclear_morphology.charting.charts.panels.SelectableChartPanel;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptionsBuilder;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.stats.SegmentStatistic;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.gui.Labels;
import com.bmskinner.nuclear_morphology.gui.components.HistogramsTabPanel;

@SuppressWarnings("serial")
public class SegmentHistogramsPanel extends HistogramsTabPanel  {
	
	private Dimension preferredSize = new Dimension(200, 100);
	
	public SegmentHistogramsPanel(){
		super();
		
		JFreeChart chart = HistogramChartFactory.createHistogram(null, "Segment", "Length");		
		SelectableChartPanel panel = new SelectableChartPanel(chart, "null");
		panel.setPreferredSize(preferredSize);
		SegmentHistogramsPanel.this.chartPanels.put("null", panel);
		SegmentHistogramsPanel.this.mainPanel.add(panel);
		
	}
	
	@Override
	protected void updateSingle() {
		updateMultiple() ;
	}
	

	@Override
	protected void updateMultiple() {
		this.setEnabled(true);
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		boolean useDensity = this.useDensityPanel.isSelected();
		
		log(Level.FINEST, "Dataset list is not empty");

		// Check that all the datasets have the same number of segments
		if(IBorderSegment.segmentCountsMatch(getDatasets())){ // make a histogram for each segment

			ICellCollection collection = activeDataset().getCollection();
			
			List<IBorderSegment> segments;
			try {
				segments = collection.getProfileCollection()
						.getSegments(Tag.REFERENCE_POINT);
			} catch (UnavailableBorderTagException | ProfileException e) {
				warn("Cannot get segments");
				fine("Cannot get segments", e);
				return;
			}
			

			// Get each segment as a boxplot
			for(IBorderSegment seg : segments){
				
				JFreeChart chart = AbstractChartFactory.createLoadingChart();
				SelectableChartPanel chartPanel = new SelectableChartPanel(chart, seg.getName());
				chartPanel.setPreferredSize(preferredSize);
				mainPanel.add(chartPanel);	
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.addStatistic(SegmentStatistic.LENGTH)
					.setScale(GlobalOptions.getInstance().getScale())
					.setSwatch(GlobalOptions.getInstance().getSwatch())
					.setUseDensity(useDensity)
					.setSegPosition(seg.getPosition())
					.setTarget(chartPanel)
					.build();
				
				setChart(options);
						
			}



		} else { // different number of segments, blank chart
			this.setEnabled(false);
			mainPanel.setLayout(new FlowLayout());
			mainPanel.add(new JLabel(Labels.INCONSISTENT_SEGMENT_NUMBER, JLabel.CENTER));
			scrollPane.setViewportView(mainPanel);
		}
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
		
		
	}
	
	@Override
	protected void updateNull() {
		this.setEnabled(true);
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		JFreeChart chart = HistogramChartFactory.createHistogram(null, "Segment", "Length");		
		SelectableChartPanel panel = new SelectableChartPanel(chart, "null");
		panel.setPreferredSize(preferredSize);
		SegmentHistogramsPanel.this.chartPanels.put("null", panel);
		SegmentHistogramsPanel.this.mainPanel.add(panel);
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
		this.setEnabled(false);
	}
	
}
