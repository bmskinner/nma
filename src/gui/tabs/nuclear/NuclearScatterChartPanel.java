package gui.tabs.nuclear;

import charting.charts.AbstractScatterChartPanel;
import stats.NucleusStatistic;

@SuppressWarnings("serial")
public class NuclearScatterChartPanel extends AbstractScatterChartPanel {
	

	public NuclearScatterChartPanel(){
		super(NucleusStatistic.AREA);
	}

}
