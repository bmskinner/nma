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

import org.jfree.chart.JFreeChart;

import charting.options.ChartOptions;
import analysis.AnalysisDataset;

/*
 * Store rendered charts in a cache, to avoid slowdowns when reselecting datasets
 * This needs to hold a UUID for any combination of datasets and display options,
 *  and map this uuid to the appropriate chart
 */
public class ChartCache implements Cache {
	
	private Map<UUID, JFreeChart> chartMap = new HashMap<UUID, JFreeChart>();
	private Map<ChartOptions, UUID> optionsMap = new HashMap<ChartOptions, UUID>();
	
	public ChartCache(){
		
	}
	
	public void addChart(ChartOptions options, JFreeChart chart){
		UUID id = UUID.randomUUID();
		chartMap.put(id, chart);
		optionsMap.put(options, id);
	}
	
	public JFreeChart getChart(ChartOptions options){
		for(ChartOptions op : this.optionsMap.keySet()){
			if(op.equals(options)){
				UUID id = optionsMap.get(op);
				return chartMap.get(id);
			}
		}
		return null;
	}
	
	public boolean hasChart(ChartOptions options){
		for(ChartOptions op : this.optionsMap.keySet()){
			if(op.equals(options)){
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see charting.Cache#purge()
	 */
	@Override
	public void purge(){
		chartMap   = new HashMap<UUID, JFreeChart>();
		optionsMap = new HashMap<ChartOptions, UUID>();
	}
	
	/* (non-Javadoc)
	 * @see charting.Cache#refresh()
	 */
	@Override
	public void refresh(){
		this.purge();
	}
	
	/* (non-Javadoc)
	 * @see charting.Cache#refresh(java.util.List)
	 */
	@Override
	public void refresh(List<AnalysisDataset> list){
		List<ChartOptions> toRemove = new ArrayList<ChartOptions>();
		
		// Find the options with the datasets
		for(AnalysisDataset d : list){
			for(ChartOptions op : this.optionsMap.keySet()){
				if(op.getDatasets().contains(d)){
					if(!toRemove.contains(op)){
						toRemove.add(op);
					}
				}
			}
		}
		
		//Remove the options with the datasets
		for(ChartOptions op : toRemove){
			UUID id = optionsMap.get(op);
			chartMap.remove(id);
			optionsMap.remove(op);
		}
	}
	
	
}
