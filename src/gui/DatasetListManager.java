package gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import analysis.AnalysisDataset;

/**
 * Track the open datasets in the program
 * @author bms41
 *
 */
public final class DatasetListManager {
	
	private static DatasetListManager instance = null;
	
	private final Set<AnalysisDataset> list = new HashSet<AnalysisDataset>();
	
	protected DatasetListManager(){}
	
	/**
	 * Fetch an instance of the factory
	 * @return
	 */
	public static DatasetListManager getInstance(){
		if(instance==null){
			instance = new DatasetListManager();
		}
		return instance;
	}
	
	public Set<AnalysisDataset> getRootDatasets(){
		return list;
	}
	
	
	public Set<AnalysisDataset> getAllDatasets(){
		
		Set<AnalysisDataset> result = new HashSet<AnalysisDataset>();
		for(AnalysisDataset d : list){
			result.addAll(d.getAllChildDatasets());
		}
		return result;
		
	}
	
	public void addDataset(AnalysisDataset d){
		list.add(d);
	}
	
	public void removeDataset(AnalysisDataset d){
		list.remove(d);
	}
	
	public int count(){
		return list.size();
	}

}
