package gui.tabs.signals;

import charting.charts.AbstractScatterChartPanel;
import stats.SignalStatistic;

@SuppressWarnings("serial")
public class SignalScatterChartPanel extends AbstractScatterChartPanel {
	

	public SignalScatterChartPanel(){
		super(SignalStatistic.AREA);
		gateButton.setVisible(false); // filtering not enabled
	}
	
}

