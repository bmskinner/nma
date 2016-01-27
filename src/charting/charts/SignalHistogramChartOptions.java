package charting.charts;

import java.util.List;

import analysis.AnalysisDataset;
import components.generic.MeasurementScale;
import stats.PlottableStatistic;

public class SignalHistogramChartOptions extends HistogramChartOptions {
	
	private int signalGroup;
	
	public SignalHistogramChartOptions(List<AnalysisDataset> list, PlottableStatistic stat, MeasurementScale scale, boolean useDensity, int signalGroup) {
		super(list, stat, scale, useDensity);
		this.signalGroup = signalGroup;
	}
	
	public int getSignalGroup(){
		return this.signalGroup;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + signalGroup;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		SignalHistogramChartOptions other = (SignalHistogramChartOptions) obj;
		if (signalGroup != other.signalGroup)
			return false;
		return true;
	}
	
	

}
