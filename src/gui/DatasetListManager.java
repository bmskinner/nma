package gui;

import java.util.ArrayList;
import java.util.List;

import analysis.AnalysisDataset;

/**
 * Track the open datasets in the program
 * @author bms41
 *
 */
public final class DatasetListManager {
	
	private static DatasetListManager instance = null;
	
	private final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	
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
