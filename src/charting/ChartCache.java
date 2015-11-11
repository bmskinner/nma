package charting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jfree.chart.JFreeChart;

import charting.charts.ChartOptions;
import analysis.AnalysisDataset;

/*
 * Store rendered charts in a cache, to avoid slowdowns when reselecting datasets
 * This needs to hold a UUID for any combination of datasets and display options,
 *  and map this uuid to the appropriate chart
 */
public class ChartCache {
	
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
	
	/**
	 * Remove all cached charts
	 */
	public void purge(){
		chartMap = new HashMap<UUID, JFreeChart>();
		optionsMap = new HashMap<ChartOptions, UUID>();
	}
	
	public void refresh(){
		this.purge();
	}
	
	/**
	 * Remove caches containing any of the given datasets.
	 * These will be recalculated at next call
	 * @param list
	 */
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
