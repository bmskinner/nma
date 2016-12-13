package com.bmskinner.nuclear_morphology.gui.tabs.nuclear;

import com.bmskinner.nuclear_morphology.charting.charts.panels.AbstractScatterChartPanel;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;

@SuppressWarnings("serial")
public class NuclearScatterChartPanel extends AbstractScatterChartPanel {
	

	public NuclearScatterChartPanel(){
		super(NucleusStatistic.AREA);
	}

}
