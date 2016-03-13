package gui.tabs.segments;

import org.jfree.chart.JFreeChart;

import charting.charts.MorphologyChartFactory;
import charting.options.ChartOptions;
import components.generic.ProfileType;
import gui.tabs.profiles.ProfileDisplayPanel;

@SuppressWarnings("serial")
public class SegmentProfilePanel extends ProfileDisplayPanel {
	
	public SegmentProfilePanel(){
		super(ProfileType.REGULAR);
		this.remove(buttonPanel); // customisation is not needed here
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options) throws Exception {
		return MorphologyChartFactory.makeMultiSegmentedProfileChart(options);
	}

}
