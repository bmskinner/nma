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
package charting.options;

import java.util.List;
import java.util.logging.Logger;

import stats.PlottableStatistic;
import analysis.AnalysisDataset;
import components.Cell;

/*
 * Hold the drawing options for a table. 
 * The appropriate options
 * are retrieved on table generation.
 */
public class TableOptions extends AbstractOptions {
	
	private TableType type = null;
	
	private Cell cell      = null;
	
	public TableOptions(List<AnalysisDataset> list) {
		super(list);

	}

	public TableOptions(List<AnalysisDataset> list, Logger programLogger){
		super(list, programLogger);

	}
	
	public void setType(TableType type){
		this.type = type;
	}

	
	public TableType getType(){
		return this.type;
	}
	
	

	public Cell getCell() {
		return cell;
	}

	public void setCell(Cell cell) {
		this.cell = cell;
	}



	public enum TableType {
		ANALYSIS_PARAMETERS,
		ANALYSIS_STATS,
		VENN, 
		PAIRWISE_VENN,
		WILCOXON,
		SIGNAL_STATS_TABLE,
		MERGE_SOURCES
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		
		result = prime * result
				+ ((cell == null) ? 0 : cell.hashCode());
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
		TableOptions other = (TableOptions) obj;
		if (type != other.type)
			return false;
		if (cell != other.cell)
			return false;
		return true;
	}
	
	
}
