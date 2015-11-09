package charting.charts;

import java.util.List;

import analysis.AnalysisDataset;
import components.generic.MeasurementScale;
import components.nuclear.NucleusStatistic;

public class BoxplotChartOptions extends ChartOptions {

	protected NucleusStatistic stat;
	protected MeasurementScale scale;

	public BoxplotChartOptions(List<AnalysisDataset> list, NucleusStatistic stat, MeasurementScale scale){
		super(list);
		this.stat = stat;
		this.scale = scale;
	}
	
	public NucleusStatistic getStat(){
		return this.stat;
	}
	
	public MeasurementScale getScale(){
		return this.scale;
	}
	
	@Override
	public boolean equals(Object obj){
		
		if (!this.getClass().equals(obj.getClass())){
			return false;
		}

		BoxplotChartOptions b = (BoxplotChartOptions) obj;
		
		if(!this.list.equals(b.getDatasets())){
			return false;
		}
		
		if(!this.getStat().equals(b.getStat())){
			return false;
		}
		
		if(!this.getScale().equals(b.getScale())){
			return false;
		}
		
		if(!this.getSwatch().equals(b.getSwatch())){
			return false;
		}
		
		return true;
	}

}
