package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.utility.Constants;

import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;

public class ClusteringMethod extends TreeBuildingMethod {
	
	public static final int EM = 0; // expectation maximisation
	public static final int HIERARCHICAL = 1;
	
	private Map<Integer, ICellCollection> clusterMap = new HashMap<Integer, ICellCollection>();

		
	public ClusteringMethod(IAnalysisDataset dataset, IClusteringOptions options){
		super(dataset, options);		
	}
	
	@Override
	public IAnalysisResult call() throws Exception {

		run();		
		
		// Save the clusters to the dataset
		List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();

		finest("Getting group number");
		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
		finest("Cluster group number chosen: "+clusterNumber);

		IClusterGroup group = new ClusterGroup(Constants.CLUSTER_GROUP_PREFIX+"_"+clusterNumber, options, newickTree);

		for(int cluster=0;cluster<clusterMap.size();cluster++){

			ICellCollection c = clusterMap.get(cluster);

			if(c.hasCells()){
				finest("Cluster "+cluster+": "+c.getName());
				
				try {
					dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
				} catch (ProfileException e) {
					warn("Error copying collection offsets");
					stack("Error in offsetting", e);
				}

				
				
				group.addDataset(c);
				c.setName(group.getName()+"_"+c.getName());

				dataset.addChildCollection(c);
								
				
				// attach the clusters to their parent collection
				log("Cluster "+cluster+": "+c.size()+" nuclei");
				IAnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
				clusterDataset.setRoot(false);
				
				// set shared counts
				c.setSharedCount(dataset.getCollection(), c.size());
				dataset.getCollection().setSharedCount(c, c.size());
				
				list.add(clusterDataset);
			}


		}
		fine("Profiles copied to all clusters");
		dataset.addClusterGroup(group);
		IAnalysisResult r = new ClusterAnalysisResult(list, group);
		return r;
	}
	

	private void run() {
		boolean ok = cluster(collection);
		fine("Returning "+ok);
	}
	
//	/**
//	 * Fetch the cluster with the given number
//	 * @param cluster
//	 * @return
//	 */
//	public ICellCollection getCluster(int cluster){
//		return this.clusterMap.get(cluster);
//	}
	
	
	/**
	 * If a tree is present (i.e clustering was hierarchical),
	 * return the string of the tree, otherwise return null
	 * @return
	 */
//	@Override
//	public String getNewickTree(){
//		if(options.getType() == ClusteringMethod.HIERARCHICAL){
//			return super.getNewickTree();
//		} else{
//			return null;
//		}
//	}
	
//	/**
//	 * Get the number of cluster found by the clusterer
//	 * @return
//	 */
//	public int getNumberOfClusters(){
//		return clusterMap.size();
//	}

	/**
	 * Run the clustering on a collection
	 * @param collection
	 * @return success or fail
	 */
	public boolean cluster(ICellCollection collection){

		fine("Beginning clustering of population");
				
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
					
					finest( "Building clusterer for tree");
//					firePropertyChange("Cooldown", getProgress(), Constants.Progress.FINISHED.code());
					clusterer.buildClusterer(instances);    // build the clusterer with one cluster for the tree
					clusterer.setPrintNewick(true);
					
					this.newickTree = clusterer.graph();
					
					clusterer.setNumClusters(options.getClusterNumber());

					finest("Building clusterer for clustering");
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
		fine("Clustering complete");
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

				ICellCollection clusterCollection = new VirtualCellCollection(dataset, "Cluster_"+i);
				
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
					fireProgressEvent();
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
