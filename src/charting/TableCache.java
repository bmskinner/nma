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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.TableModel;

import charting.options.TableOptions;
import analysis.AnalysisDataset;
import analysis.IAnalysisDataset;


public class TableCache implements Cache {
	private Map<TableOptions, TableModel> tableMap = new HashMap<TableOptions, TableModel>();
	
	public TableCache(){
		
	}
	
	public void addTable(TableOptions options, TableModel model){
		tableMap.put(options, model);
	}
	
	public TableModel getTable(TableOptions options){
		
		if(tableMap.containsKey(options)){
			return tableMap.get(options);
		}
		return null;
	}
	
	public boolean hasTable(TableOptions options){
		return tableMap.containsKey(options);
	}
	
	/**
	 * Remove all cached charts
	 */
	public void purge(){
		tableMap = new HashMap<TableOptions, TableModel>();
	}
	
	/**
	 * Remove all cached charts
	 */
	public void clear(){
		this.purge();
	}
	
	/**
	 * Remove caches containing any of the given datasets.
	 * These will be recalculated at next call
	 * @param list
	 */
	public void clear(List<IAnalysisDataset> list){
		
		if(list==null || list.isEmpty()){
			purge();
			return;
		}
		
		Set<TableOptions> toRemove = new HashSet<TableOptions>();
		
		// Find the options with the datasets
		for(IAnalysisDataset d : list){
			for(TableOptions op : tableMap.keySet()){
				
				if( ! op.hasDatasets()){
					continue;
				}
				if(op.getDatasets().contains(d)){
						toRemove.add(op);
				}
			}
		}
		
		//Remove the options with the datasets
		for(TableOptions op : toRemove){
			tableMap.remove(op);
		}
	}
}
