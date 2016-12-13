package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import com.bmskinner.nuclear_morphology.charting.charts.panels.AbstractScatterChartPanel;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;

@SuppressWarnings("serial")
public class SignalScatterChartPanel extends AbstractScatterChartPanel {
	

	public SignalScatterChartPanel(){
		super(SignalStatistic.AREA);
		gateButton.setVisible(false); // filtering not enabled
	}
	
}

