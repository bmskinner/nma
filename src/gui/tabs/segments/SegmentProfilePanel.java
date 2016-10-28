package gui.tabs.segments;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.options.DefaultChartOptions;
import components.generic.ProfileType;
import gui.tabs.profiles.ProfileDisplayPanel;

@SuppressWarnings("serial")
public class SegmentProfilePanel extends ProfileDisplayPanel {
	
	public SegmentProfilePanel(){
		super(ProfileType.ANGLE);
		this.remove(buttonPanel); // customisation is not needed here
	}
	
	@Override
	protected JFreeChart createPanelChartType(DefaultChartOptions options) throws Exception {
		options.setShowMarkers(false);
		options.setShowAnnotations(false);
		options.setShowLines(true);
		return new MorphologyChartFactory(options).makeMultiSegmentedProfileChart();
	}

}
