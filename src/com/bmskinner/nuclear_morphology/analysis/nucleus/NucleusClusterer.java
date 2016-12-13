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
package com.bmskinner.nuclear_morphology.analysis.nucleus;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.utility.Constants;

import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;


public class NucleusClusterer extends NucleusTreeBuilder {
	
	public static final int EM = 0; // expectation maximisation
	public static final int HIERARCHICAL = 1;
	
	private Map<Integer, ICellCollection> clusterMap = new HashMap<Integer, ICellCollection>();

		
	public NucleusClusterer(IAnalysisDataset dataset, ClusteringOptions options){
		super(dataset, options);		
		log(Level.FINEST, "Total set to "+this.getProgressTotal());
	}
	
	@Override
	protected Boolean doInBackground() {
		boolean ok = cluster(collection);
		log(Level.FINE, "Returning "+ok);
		return ok;
	}
	
	/**
	 * Fetch the cluster with the given number
	 * @param cluster
	 * @return
	 */
	public ICellCollection getCluster(int cluster){
		return this.clusterMap.get(cluster);
	}
	
	public ClusteringOptions getOptions(){
		return this.options;
	}
	
	/**
	 * If a tree is present (i.e clustering was hierarchical),
	 * return the string of the tree, otherwise return null
	 * @return
	 */
	@Override
	public String getNewickTree(){
		if(options.getType()==ClusteringMethod.HIERARCHICAL){
			return super.getNewickTree();
		} else{
			return null;
		}
	}
	
	/**
	 * Get the number of cluster found by the clusterer
	 * @return
	 */
	public int getNumberOfClusters(){
		return clusterMap.size();
	}

	/**
	 * Run the clustering on a collection
	 * @param collection
	 * @return success or fail
	 */
	public boolean cluster(ICellCollection collection){
		
//		this.logger = new Logger(collection.getDebugFile(), "NucleusClusterer");
		
		log(Level.FINE, "Beginning clustering of population");
				
		try {
						
			// create Instances to hold Instance
			Instances instances = makeInstances();
			

			// create the clusterer to run on the Instances
			String[] optionArray = this.options.getOptions();

			try {
				

				finer("Clusterer is type "+options.getType());
				for(String s : optionArray){
//					fileLogger.log(Level.FINE, "Clusterer options: "+s);
					finest("Clusterer options: "+s);
				}
				
				
				if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
					HierarchicalClusterer clusterer = new HierarchicalClusterer();
					
					clusterer.setOptions(optionArray);     // set the options
					clusterer.setDistanceFunction(new EuclideanDistance());
					clusterer.setDistanceIsBranchLength(true);
					clusterer.setNumClusters(1);
					
					log(Level.FINEST, "Building clusterer for tree");
					firePropertyChange("Cooldown", getProgress(), Constants.Progress.FINISHED.code());
					clusterer.buildClusterer(instances);    // build the clusterer with one cluster for the tree
					clusterer.setPrintNewick(true);
					
					this.newickTree = clusterer.graph();
					
					clusterer.setNumClusters(options.getClusterNumber());

					log(Level.FINEST, "Building clusterer for clustering");
					clusterer.buildClusterer(instances);    // build the clusterer
					assignClusters(clusterer, collection);		
					
				}
				
				if(options.getType().equals(ClusteringMethod.EM)){
					EM clusterer = new EM();   // new instance of clusterer
					clusterer.setOptions(optionArray);     // set the options
					clusterer.buildClusterer(instances);    // build the clusterer
					
					assignClusters(clusterer, collection);		
				}

			} catch (Exception e) {
				error("Error in clustering", e);
				return false;
			}
		} catch (Exception e) {
			error("Error in assignments", e);
			return false;
		}
		log(Level.FINE, "Clustering complete");
		return true;
	}
	
	/**
	 * Given a trained clusterer, put each nucleus within the collection into a cluster
	 * @param clusterer the clusterer to use
	 * @param collection the collection with nuclei to cluster
	 */
	private void assignClusters(Clusterer clusterer, ICellCollection collection){
		try {
			// construct new collections for each cluster
			fine("Assigning nuclei to clusters");
			fine("Clusters : "+clusterer.numberOfClusters());

			for(int i=0;i<clusterer.numberOfClusters();i++ ){
				fine("Cluster "+i+": " +	collection.getName()+"_Cluster_"+i);

				ICellCollection clusterCollection = new VirtualCellCollection(getDataset(), "Cluster_"+i);
				
				clusterCollection.setName("Cluster_"+i);
				clusterMap.put(i, clusterCollection);
			}

			int i = collection.size();
			for(Instance inst : cellToInstanceMap.keySet()){
				
				try{
					
					
					UUID cellID = cellToInstanceMap.get(inst);

					int clusterNumber = clusterer.clusterInstance(inst); // #pass each instance through the model
					
					finest("\tTesting instance "+i+": "+clusterNumber);
					
					ICellCollection cluster = clusterMap.get(clusterNumber);

					// should never be null
					if(collection.getCell(cellID)!=null){
						cluster.addCell( collection.getCell(cellID) );
					} else {
						warn("Error: cell with ID "+cellID+" is not found");
					}
					finest("\tInstance handled");
					publish(i++);
				} catch(Exception e){
					error("Error assigning instance to cluster", e);
				}
				 
			}
			finer("Assignment of clusters complete");
		} catch (Exception e) {
			warn("Error making clusters");
			fine("Error clustering", e);			
		}
	}

	


}
