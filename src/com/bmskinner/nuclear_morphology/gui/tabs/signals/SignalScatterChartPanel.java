package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractScatterChartPanel;

@SuppressWarnings("serial")
public class SignalScatterChartPanel extends AbstractScatterChartPanel {
	

	public SignalScatterChartPanel(){
		super(SignalStatistic.AREA);
		gateButton.setVisible(false); // filtering not enabled
	}
	
}

