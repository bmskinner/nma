package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.gui.tabs.AbstractScatterChartPanel;

@SuppressWarnings("serial")
public class NuclearScatterChartPanel extends AbstractScatterChartPanel {
	

	public NuclearScatterChartPanel(){
		super(NucleusStatistic.AREA);
	}

}
