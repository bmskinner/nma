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
import java.util.UUID;
import java.util.logging.Level;

import org.jfree.chart.JFreeChart;

import charting.options.ChartOptions;
import analysis.AnalysisDataset;

/*
 * Store rendered charts in a cache, to avoid slowdowns when reselecting datasets
 * This needs to hold a UUID for any combination of datasets and display options,
 *  and map this uuid to the appropriate chart
 */
public class ChartCache implements Cache {
	
	private Map<ChartOptions, JFreeChart> chartMap = new HashMap<ChartOptions, JFreeChart>();
	
	public ChartCache(){
		
	}
	
	public void addChart(ChartOptions options, JFreeChart chart){
		chartMap.put(options, chart);
	}
	
	public JFreeChart getChart(ChartOptions options){
		if(chartMap.containsKey(options)){
			return chartMap.get(options);
		}
		return null;
	}
	
	public boolean hasChart(ChartOptions options){
		if(chartMap.containsKey(options)){
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see charting.Cache#purge()
	 */
	@Override
	public void purge(){
		chartMap   = new HashMap<ChartOptions, JFreeChart>();
	}
	
	/* (non-Javadoc)
	 * @see charting.Cache#refresh()
	 */
	@Override
	public void clear(){
		this.purge();
	}
	

	@Override
	public void clear(List<AnalysisDataset> list){
		
		// If the list is malformed, clear everything
		if(list==null || list.isEmpty()){
			purge();
			return;
		}
		
		// Make a list of the options that need removed
		// These are the options that contain the datasets in the list
		Set<ChartOptions> toRemove = new HashSet<ChartOptions>();
		
		// Find the options with the datasets
		for(AnalysisDataset d : list){
			for(ChartOptions op : this.chartMap.keySet()){
				if(op.hasDatasets()){
					if(op.getDatasets().contains(d)){
						toRemove.add(op);
						finest("Need to remove options with dataset "+d.getName());
					}
				}
			}
		}
		
//		try {
		
			//Remove the options with the datasets
			for(ChartOptions op : toRemove){
				finest("Clearing options");
				chartMap.remove(op);
			}
//		} catch(Exception e){
//			log(Level.FINE, "Error clearing chart cache", e );
//			purge();
//			return;
//		}
	}
	
	
}
