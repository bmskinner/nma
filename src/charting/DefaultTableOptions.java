/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
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
