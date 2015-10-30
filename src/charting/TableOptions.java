package charting;

import java.util.ArrayList;
import java.util.List;
import analysis.AnalysisDataset;

/*
 * Hold options for drawing a table
 */
public abstract class TableOptions {


	protected List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();

	public TableOptions(List<AnalysisDataset> list){
		this.list = list;
	}


	public List<AnalysisDataset> getDatasets(){
		return this.list;
	}


}
