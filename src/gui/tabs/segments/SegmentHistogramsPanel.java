package gui.tabs.segments;

import gui.GlobalOptions;
import gui.Labels;
import gui.components.HistogramsTabPanel;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.JFreeChart;

import stats.Quartile;
import stats.SegmentStatistic;
import charting.charts.HistogramChartFactory;
import charting.charts.panels.SelectableChartPanel;
import charting.options.DefaultChartOptions;
import charting.options.ChartOptionsBuilder;
import components.CellCollection;
import components.ICellCollection;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderSegment;
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
			
			List<IBorderSegment> segments = collection.getProfileCollection()
					.getSegments(Tag.REFERENCE_POINT);
//					.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN)
//					.getOrderedSegments();
			

			// Get each segment as a boxplot
			for(IBorderSegment seg : segments){
				
				JFreeChart chart = HistogramChartFactory.makeEmptyChart();
				SelectableChartPanel chartPanel = new SelectableChartPanel(chart, seg.getName());
				chartPanel.setPreferredSize(preferredSize);
				mainPanel.add(chartPanel);	
				
				DefaultChartOptions options = new ChartOptionsBuilder()
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
