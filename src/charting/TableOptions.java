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
