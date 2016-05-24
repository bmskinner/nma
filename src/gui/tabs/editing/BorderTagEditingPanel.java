package gui.tabs.editing;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.table.TableModel;

import org.jfree.chart.JFreeChart;

import components.generic.BorderTag;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import charting.options.ChartOptionsBuilder;
import charting.options.TableOptions;
import gui.SignalChangeEvent;
import gui.components.DraggableOverlayChartPanel;
import gui.components.PositionSelectionChartPanel;
import gui.components.ColourSelecter.ColourSwatch;
import gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import gui.tabs.DetailPanel;

@SuppressWarnings("serial")
public class BorderTagEditingPanel extends DetailPanel {
	
	private DraggableOverlayChartPanel chartPanel; // for displaying the legnth of a given segment
	private PositionSelectionChartPanel rangePanel; // a small chart to show the entire profile
	
	public BorderTagEditingPanel() {
		
		super();
		
		this.setLayout(new BorderLayout());
		Dimension minimumChartSize = new Dimension(50, 100);
		Dimension preferredChartSize = new Dimension(400, 300);
		
		JFreeChart profileChart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);
		chartPanel = new DraggableOverlayChartPanel(profileChart, null, true);
		
		chartPanel.setMinimumSize(minimumChartSize);
		chartPanel.setPreferredSize(preferredChartSize);
		chartPanel.setMinimumDrawWidth( 0 );
		chartPanel.setMinimumDrawHeight( 0 );
		chartPanel.addSignalChangeListener(this);
		this.add(chartPanel, BorderLayout.CENTER);
				
		
		/*
		 * TESTING: A second chart panel at the south
		 * with a domain overlay crosshair to define the 
		 * centre of the zoomed range on the 
		 * centre chart panel 
		 */
		JFreeChart rangeChart = MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR);
		rangePanel = new PositionSelectionChartPanel(rangeChart);
		rangePanel.setPreferredSize(minimumChartSize);
		rangePanel.addSignalChangeListener(this);
		this.add(rangePanel, BorderLayout.SOUTH);
		updateChartPanelRange();

	}
	
	/**
	 * Set the main chart panel domain range to centre on the 
	 * position in the range panel, +- 10
	 */
	private void updateChartPanelRange(){
		double xValue = rangePanel.getDomainCrosshairPosition();
		finest("Range panel crosshair is at "+xValue);
		
		double min = xValue-10;
		double max = xValue+10;
		chartPanel.getChart().getXYPlot().getDomainAxis().setRange(min, max);
	}
	
	@Override
	protected void updateSingle() throws Exception {
		SegmentedProfile profile = null;
		
		ChartOptions options = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setNormalised(true)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(BorderTag.REFERENCE_POINT)
			.setShowMarkers(false)
			.setProfileType( ProfileType.REGULAR)
			.setShowPoints(true)
			.build();
		
					
		JFreeChart chart = getChart(options);
		
		profile = activeDataset().getCollection()
				.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(BorderTag.REFERENCE_POINT);
		
		chartPanel.setChart(chart, profile, true);
		updateChartPanelRange();
		
		
		/*
		 * Create the chart for the range panel
		 */
		
		ChartOptions rangeOptions = new ChartOptionsBuilder()
			.setDatasets(getDatasets())
			.setNormalised(true)
			.setAlignment(ProfileAlignment.LEFT)
			.setTag(BorderTag.REFERENCE_POINT)
			.setShowMarkers(false)
			.setProfileType( ProfileType.REGULAR)
			.setSwatch(ColourSwatch.NO_SWATCH)
			.setShowPoints(false)
			.build();
		
		JFreeChart rangeChart = getChart(rangeOptions);
		
		rangePanel.setChart(rangeChart);
	}
	

	@Override
	protected void updateMultiple() throws Exception {
		updateNull();
		
		
	}
	
	@Override
	protected void updateNull() throws Exception {
		chartPanel.setChart(MorphologyChartFactory.makeEmptyProfileChart(ProfileType.REGULAR));
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return MorphologyChartFactory.makeMultiSegmentedProfileChart(options);
	}
	
	@Override
	protected TableModel createPanelTableType(TableOptions options) throws Exception{
		return null;
	}
		
	@Override
	public void signalChangeReceived(SignalChangeEvent event) {
		fireSignalChangeEvent(event);
	}

}
