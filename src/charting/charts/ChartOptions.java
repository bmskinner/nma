package charting.charts;

import java.util.ArrayList;
import java.util.List;

import analysis.AnalysisDataset;

/*
 * Hold options for drawing a chart
 */
public abstract class ChartOptions {
	
	protected List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();


//	// Profiles
//	private boolean normalised;
//	private ProfileAlignment alignment;
	
	public ChartOptions(List<AnalysisDataset> list){
		this.list = list;
	}
	
	
	public List<AnalysisDataset> getDatasets(){
		return this.list;
	}
	
}
