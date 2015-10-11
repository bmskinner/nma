package components;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import analysis.AnalysisDataset;
import analysis.ClusteringOptions;

/**
 * This holds the ids of datasets created by clustering,
 * plus the clustering options that were used to generate
 * the clusters
 *
 */

public class ClusterGroup implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private List<UUID> ids = new ArrayList<UUID>(0); // hold the ids of datasets in a cluster
	private ClusteringOptions options; // store the options that were used to generate the cluster
	private String name;
	private String newickTree = null;
	
	/**
	 * Create a new cluster group
	 * @param name the group name (informal)
	 * @param options the options used to create the cluster
	 */
	public ClusterGroup(String name, ClusteringOptions options){
		this.name = name;
		this.options = options;
	}
	
	/**
	 * Create a new cluster group with a tree
	 * @param name the group name (informal)
	 * @param options the options used to create the cluster
	 * @param tree the Newick tree for the cluster as a String
	 */
	public ClusterGroup(String name, ClusteringOptions options, String tree){
		this(name, options);
		this.newickTree = tree;
	}
	
	public String getName(){
		return this.name;
	}
	
	public int size(){
		return this.ids.size();
	}
	
	/**
	 * Get the Newick tree for the cluster if set, or null
	 * @return
	 */
	public String getTree(){
		return this.newickTree;
	}
	
	public List<UUID> getUUIDs(){
		return this.ids;
	}
	
	public void addDataset(AnalysisDataset dataset){
		this.ids.add(dataset.getUUID());
	}
	
	public void addDataset(CellCollection collection){
		this.ids.add(collection.getID());
	}
	
	public ClusteringOptions getOptions(){
		return this.options;
	}
	

	/** 
	 * Test if this group contains the given dataset id
	 * @param id
	 * @return
	 */
	public boolean hasDataset(UUID id){
		if(this.ids.contains(id)){
			return true;
		} else {
			return false;
		}
	}

}
