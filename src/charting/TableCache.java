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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.swing.table.TableModel;

import charting.options.TableOptions;
import analysis.AnalysisDataset;


public class TableCache implements Cache {
	private Map<UUID, TableModel> tableMap = new HashMap<UUID, TableModel>();
	private Map<TableOptions, UUID> optionsMap = new HashMap<TableOptions, UUID>();
	
	public TableCache(){
		
	}
	
	public void addTable(TableOptions options, TableModel model){
		UUID id = UUID.randomUUID();
		tableMap.put(id, model);
		optionsMap.put(options, id);
	}
	
	public TableModel getTable(TableOptions options){
		for(TableOptions op : this.optionsMap.keySet()){
			if(op.equals(options)){
				UUID id = optionsMap.get(op);
				return tableMap.get(id);
			}
		}
		return null;
	}
	
	public boolean hasTable(TableOptions options){
		for(TableOptions op : this.optionsMap.keySet()){
			if(op.equals(options)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Remove all cached charts
	 */
	public void purge(){
		tableMap = new HashMap<UUID, TableModel>();
		optionsMap = new HashMap<TableOptions, UUID>();
	}
	
	/**
	 * Remove all cached charts
	 */
	public void refresh(){
		this.purge();
	}
	
	/**
	 * Remove caches containing any of the given datasets.
	 * These will be recalculated at next call
	 * @param list
	 */
	public void refresh(List<AnalysisDataset> list){
		List<TableOptions> toRemove = new ArrayList<TableOptions>();
		
		// Find the options with the datasets
		for(AnalysisDataset d : list){
			for(TableOptions op : this.optionsMap.keySet()){
				if(op.getDatasets().contains(d)){
					if(!toRemove.contains(op)){
						toRemove.add(op);
					}
				}
			}
		}
		
		//Remove the options with the datasets
		for(TableOptions op : toRemove){
			UUID id = optionsMap.get(op);
			tableMap.remove(id);
			optionsMap.remove(op);
		}
	}
}
