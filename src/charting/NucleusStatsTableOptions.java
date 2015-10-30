package charting;

import java.util.List;

import analysis.AnalysisDataset;
import components.nuclear.NucleusStatistic;

public class NucleusStatsTableOptions extends TableOptions {

	private NucleusStatistic stat;
	
	public NucleusStatsTableOptions(List<AnalysisDataset> list, NucleusStatistic stat) {
		super(list);
		this.stat = stat;
	}

	public NucleusStatistic getStat() {
		return stat;
	}
	
	@Override
	public boolean equals(Object obj){
		if (!this.getClass().equals(obj.getClass())){
			return false;
		}

		NucleusStatsTableOptions b = (NucleusStatsTableOptions) obj;
		
		if(!this.getDatasets().equals(b.getDatasets())){
			return false;
		}
		
		if(!this.getStat().equals(b.getStat())){
			return false;
		}

		return true;
	}

}
