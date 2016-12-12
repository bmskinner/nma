package com.bmskinner.nuclear_morphology.gui.tabs.segments;

import org.jfree.chart.JFreeChart;

import com.bmskinner.nuclear_morphology.charting.charts.MorphologyChartFactory;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.gui.tabs.profiles.ProfileDisplayPanel;

@SuppressWarnings("serial")
public class SegmentProfilePanel extends ProfileDisplayPanel {
	
	public SegmentProfilePanel(){
		super(ProfileType.ANGLE);
		this.remove(buttonPanel); // customisation is not needed here
	}
	
	@Override
	protected JFreeChart createPanelChartType(ChartOptions options){
//		options.setShowMarkers(false);
//		options.setShowAnnotations(false);
//		options.setShowLines(true);
		return new MorphologyChartFactory(options).makeMultiSegmentedProfileChart();
	}

}
