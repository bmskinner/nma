package charting;

import java.util.List;

import analysis.AnalysisDataset;
import stats.PlottableStatistic;

public class SegmentStatsTableOptions extends NucleusStatsTableOptions{
	
	private String name;
	
	public SegmentStatsTableOptions(List<AnalysisDataset> list, PlottableStatistic stat, String name){
		
		super(list, stat);
		this.name = name;
		
	}

	public String getName() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj){
		if (!this.getClass().equals(obj.getClass())){
			return false;
		}
		
		SegmentStatsTableOptions b = (SegmentStatsTableOptions) obj;
		
		if(!this.getDatasets().equals(b.getDatasets())){
			return false;
		}
		
		if(!this.getStat().equals(b.getStat())){
			return false;
		}
		
		if(!this.getName().equals(b.getName())){
			return false;
		}

		return true;
	}

}
