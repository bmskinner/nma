package charting.charts;

import java.util.List;

import analysis.AnalysisDataset;
import components.generic.MeasurementScale;
import components.nuclear.NucleusStatistic;

public class HistogramChartOptions extends BoxplotChartOptions {

	protected boolean useDensity;
	
	public HistogramChartOptions(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale, boolean useDensity) {
		super(list, stat, scale);
		this.useDensity = useDensity;
	}
	
	public boolean isUseDensity(){
		return this.useDensity;
	}

	@Override
	public boolean equals(Object obj){
				
		if (!this.getClass().equals(obj.getClass())){
			return false;
		}

		HistogramChartOptions b = (HistogramChartOptions) obj;
		
		if(!this.list.equals(b.getDatasets())){
			return false;
		}
		
		if(!this.getStat().equals(b.getStat())){
			return false;
		}
		
		if(!this.getScale().equals(b.getScale())){
			return false;
		}
		
		if(this.isUseDensity()!=(b.isUseDensity())){
			return false;
		}
		
		return true;
	}
}
