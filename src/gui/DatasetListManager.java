package gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import logging.Loggable;
import analysis.AnalysisDataset;

/**
 * Track the open datasets in the program
 * @author bms41
 *
 */
public final class DatasetListManager implements Loggable {
	
	private static DatasetListManager instance = null;
	
	private final Set<AnalysisDataset> list = new HashSet<AnalysisDataset>();
	
	private final Map<UUID, Integer> map = new HashMap<UUID, Integer>(); // store the hash for a dataset id
	
	
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
	
	public boolean hasDatasets(){
		return list.size()>0;
	}
	
	
	public Set<AnalysisDataset> getAllDatasets(){
		
		Set<AnalysisDataset> result = new HashSet<AnalysisDataset>();
		for(AnalysisDataset d : list){
			result.add(d);
			result.addAll(d.getAllChildDatasets());
		}
		return result;
		
	}
	
	/**
	 * Get the dataset with the given id, or null
	 * @param id
	 * @return
	 */
	public boolean hasDataset(UUID id){
		for(AnalysisDataset d : list){
			if(d.getUUID().equals(id)){
				return true;
			}
			
			for(AnalysisDataset child : d.getAllChildDatasets()){
				if(child.getUUID().equals(id)){
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Get the dataset with the given id, or null
	 * @param id
	 * @return
	 */
	public AnalysisDataset getDataset(UUID id){
		for(AnalysisDataset d : list){
			if(d.getUUID().equals(id)){
				return d;
			}
			
			for(AnalysisDataset child : d.getAllChildDatasets()){
				if(child.getUUID().equals(id)){
					return child;
				}
			}
		}
		return null;
	}
	
	public void addDataset(AnalysisDataset d){
		if(d.isRoot()){
			list.add(d);
			fine("Adding hash code: "+d.getName()+" - "+d.hashCode());
			map.put(d.getUUID(), d.hashCode());
		} else {
			finer("Not adding a root dataset");
		}
	}
	
	public void removeDataset(AnalysisDataset d){
		map.remove(d);
	}
	
	public int count(){
		return map.size();
	}
	
	public void clear(){
		list.clear();
		map.clear();
	}
	
	public boolean hashCodeChanged(AnalysisDataset d){
		if(d.isRoot()){
			
			if(map.containsKey(d.getUUID())){
				return d.hashCode()!=map.get(d.getUUID());
			} else {
				warn("Missing root dataset hashcode");
			}
			
		}
		return false;
	}
	
	/**
	 * Check if any of the root datasets have a different hashcode
	 * to their last save
	 * @return
	 */
	public boolean hashCodeChanged(){

		for(AnalysisDataset d : list){
			if(hashCodeChanged(d)){
				return true;
			}
		}
		return false;
	}
	
	public void updateHashCode(AnalysisDataset d){
		if(d.isRoot()){
			map.put(d.getUUID(), d.hashCode());
		}
	}
	
	public void updateHashCodes(){
		for(AnalysisDataset d : list){
			updateHashCode(d);
		}
	}

}
