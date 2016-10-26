package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import logging.Loggable;
import analysis.IAnalysisDataset;

/**
 * Track the open datasets in the program
 * @author bms41
 *
 */
public final class DatasetListManager implements Loggable {
	
	private static DatasetListManager instance = null;
	
	/**
	 * The list of root datasets currently loaded. The order of datasets
	 * within the list can be used to determine the order of root datasets within the
	 * populations panel.
	 */
	private final List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
	
	/**
	 * This map stores the UUID of a dataset as a key against the hashcode of the dataset.
	 * This is used to compare actual and saved hashcodes, and detect whether a dataset has changed
	 * since the last check.
	 */
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
	
	public synchronized List<IAnalysisDataset> getRootDatasets(){
		return new ArrayList<IAnalysisDataset>(list);
	}
	
	/**
	 * Get the index of the given dataset in the list. Returns
	 * -1 if the dataset is not root, not found, or null.
	 * @param d
	 * @return the index, or -1
	 */
	public synchronized int getPosition(IAnalysisDataset d){
		if(d.isRoot()){
			return list.indexOf(d);
		}
		return -1;
	}
	
	public synchronized boolean hasDatasets(){
		return map.size()>0;
	}
	
	/**
	 * Update the cluster groups for each root dataset and its children.
	 * This will remove any cluster groups with no member datasets. 
	 */
	public synchronized void refreshClusters(){
		try {
		finest("Refreshing clusters...");
		if(this.hasDatasets()){
			
			for(IAnalysisDataset rootDataset : this.getRootDatasets()){

				finest("  Root dataset "+rootDataset.getName());
				rootDataset.refreshClusterGroups();
				for(IAnalysisDataset child : rootDataset.getAllChildDatasets()){
					finest("    Child dataset "+child.getName());
					child.refreshClusterGroups();
				}
				
			}
		}
		} catch (Exception e){
			error("Error refreshing clusters", e);
		}
	}
	
	
	/**
	 * Get the parent dataset to the given dataset. If the 
	 * given dataset is root, returns itself
	 * @param d
	 * @return
	 */
	public synchronized IAnalysisDataset getParent(IAnalysisDataset d){
		
		if(d.isRoot()){
			return d;
		}
		
		IAnalysisDataset result = null;
		
		
		for(IAnalysisDataset root : this.getRootDatasets()){
			
			if(root.hasRecursiveChild(d)){
				
				// Get the child of the root dataset which is a parent
				// to the input dataset
				
				for(IAnalysisDataset parent : root.getAllChildDatasets()){
					if(parent.hasChild(d)){
						return parent;
					}
				}
				
			}
		}
		return result;
	}
	
	
	public synchronized Set<IAnalysisDataset> getAllDatasets(){
		
		Set<IAnalysisDataset> result = new HashSet<IAnalysisDataset>();
		for(IAnalysisDataset d : list){
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
	public synchronized boolean hasDataset(UUID id){
		for(IAnalysisDataset d : list){
			if(d.getUUID().equals(id)){
				return true;
			}
			
			for(IAnalysisDataset child : d.getAllChildDatasets()){
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
	public synchronized IAnalysisDataset getDataset(UUID id){
		for(IAnalysisDataset d : list){
			if(d.getUUID().equals(id)){
				return d;
			}
			
			for(IAnalysisDataset child : d.getAllChildDatasets()){
				if(child.getUUID().equals(id)){
					return child;
				}
			}
		}
		return null;
	}
	
	public synchronized void addDataset(IAnalysisDataset d){
		if(d.isRoot()){
			list.add(d);
			fine("Adding hash code: "+d.getName()+" - "+d.hashCode());
			map.put(d.getUUID(), d.hashCode());
		} else {
			finer("Not adding a root dataset");
		}
	}
	
	public synchronized void removeDataset(IAnalysisDataset d){
		
		if( ! d.isRoot()){
			return;
		}
		
		if( ! list.contains(d)){
			warn("Requested dataset "+d.getName()+" is not in list; checking UUIDs");
		}
		
		// The hashcode may have changed from what is stored in the list, so check
		if(hashCodeChanged(d)){
			finer("Dataset hashcode changed");
		}
		
		finer("List manager has "+list.size()+" root datasets and "+map.size()+" hashcodes");

		Iterator<IAnalysisDataset> it = list.iterator();
		while (it.hasNext()){
			IAnalysisDataset test = it.next();
			
			if(test.getUUID().equals(d.getUUID())){
				finer("Found id matching dataset");
				it.remove(); //TODO: figure out why this does not actually remove anything from the list
			}
		}
		map.remove(d.getUUID());
		
		finer("List manager now has "+list.size()+" root datasets and "+map.size()+" hashcodes");

	}
	
	
	/**
	 * Get the number of datasets loaded
	 * @return
	 */
	public synchronized int count(){
		return map.size();
	}
	
	/**
	 * Close all datasets without saving and clear them from memory
	 */
	public void clear(){
		list.clear();
		map.clear();
	}
	
	/**
	 * Check if the stored hashcode for the given dataset is different
	 * to the actual dataset hashcode
	 * @param d
	 * @return true if the hashcode is different to the stored value 
	 */
	public boolean hashCodeChanged(IAnalysisDataset d){
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

		for(IAnalysisDataset d : list){
			if(hashCodeChanged(d)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Update the stored hashcode for the given dataset to its 
	 * current actual value
	 * @param d
	 */
	public void updateHashCode(IAnalysisDataset d){
		if(d.isRoot()){
			map.put(d.getUUID(), d.hashCode());
		}
	}
	
	/**
	 * Update the stored hashcode for all root datasets to their 
	 * current actual values
	 * @param d
	 */
	public void updateHashCodes(){
		for(IAnalysisDataset d : list){
			updateHashCode(d);
		}
	}

}
