package com.bmskinner.nuclear_morphology.gui.tabs.signals;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractScatterChartPanel;

@SuppressWarnings("serial")
public class SignalScatterChartPanel extends AbstractScatterChartPanel {
	

	public SignalScatterChartPanel(){
		super(CellularComponent.NUCLEAR_SIGNAL);
		gateButton.setVisible(false); // filtering not enabled
	}
	
}

