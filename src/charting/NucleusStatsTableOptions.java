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

import charting.options.TableOptions;
import analysis.AnalysisDataset;
import stats.NucleusStatistic;
import stats.PlottableStatistic;

public class NucleusStatsTableOptions extends TableOptions {

	private PlottableStatistic stat;
	
	public NucleusStatsTableOptions(List<AnalysisDataset> list, PlottableStatistic stat) {
		super(list);
		this.stat = stat;
	}

	public PlottableStatistic getStat() {
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
