package charting;

import java.util.List;

import charting.charts.HistogramChartOptions;
import analysis.AnalysisDataset;

/**
 * The DefaultTableOptions provides a concrete access
 * to the TableOptions. It holds a list of datasets, 
 * and the type of table being created
 * @author bms41
 *
 */
public class DefaultTableOptions extends TableOptions {
	
	private TableType type;

	public DefaultTableOptions(List<AnalysisDataset> list, TableType type) {
		super(list);
		this.type = type;
	}
	
	public TableType getType(){
		return this.type;
	}

	
	public enum TableType {
		ANALYSIS_PARAMETERS,
		ANALYSIS_STATS,
		VENN, 
		WILCOXON
	}
	
	@Override
	public boolean equals(Object obj){
		if (!this.getClass().equals(obj.getClass())){
			return false;
		}

		DefaultTableOptions b = (DefaultTableOptions) obj;
		
		if(!this.getDatasets().equals(b.getDatasets())){
			return false;
		}
		
		if(!this.getType().equals(b.getType())){
			return false;
		}

		return true;
	}
}
