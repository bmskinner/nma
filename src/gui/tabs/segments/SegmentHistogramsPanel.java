package gui.tabs.segments;

import gui.components.HistogramsTabPanel;
import gui.components.SelectableChartPanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import stats.SegmentStatistic;
import charting.charts.HistogramChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.nuclear.NucleusBorderSegment;

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
	protected void updateSingle() throws Exception {
		updateMultiple() ;
	}
	

	@Override
	protected void updateMultiple() throws Exception {
		this.setEnabled(true);
		mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		MeasurementScale scale = this.measurementUnitSettingsPanel.getSelected();
		boolean useDensity = this.useDensityPanel.isSelected();
		
		log(Level.FINEST, "Dataset list is not empty");

		// Check that all the datasets have the same number of segments
		if(checkSegmentCountsMatch(getDatasets())){ // make a histogram for each segment

			CellCollection collection = activeDataset().getCollection();
			
			List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR)
					.getSegmentedProfile(BorderTag.ORIENTATION_POINT)
					.getOrderedSegments();
			

			// Get each segment as a boxplot
			for(NucleusBorderSegment seg : segments){
				
				ChartOptions options = new ChartOptionsBuilder()
					.setDatasets(getDatasets())
					.setStatistic(SegmentStatistic.LENGTH)
					.setScale(scale)
					.setUseDensity(useDensity)
					.setSegPosition(seg.getPosition())
					.build();
				
				
				JFreeChart chart = getChart(options);
				
				SelectableChartPanel chartPanel = new SelectableChartPanel(chart, seg.getName());
				chartPanel.setPreferredSize(preferredSize);
				mainPanel.add(chartPanel);							
			}



		} else { // different number of segments, blank chart
			this.setEnabled(false);
			mainPanel.setLayout(new FlowLayout());
			mainPanel.add(new JLabel("Segment number is not consistent across datasets", JLabel.CENTER));
			scrollPane.setViewportView(mainPanel);
		}
		mainPanel.revalidate();
		mainPanel.repaint();
		scrollPane.setViewportView(mainPanel);
		
		
	}
	
	@Override
	protected void updateNull() throws Exception {
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
