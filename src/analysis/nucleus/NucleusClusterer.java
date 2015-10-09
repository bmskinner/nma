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
package analysis.nucleus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import analysis.ClusteringOptions;
import components.Cell;
import components.CellCollection;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.nuclei.Nucleus;
import utility.Constants;
import utility.Constants.BorderTag;
import utility.DipTester;
import utility.Logger;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;


public class NucleusClusterer extends SwingWorker<Boolean, Integer> {
	
	public static final int EM = 0; // expectation maximisation
	public static final int HIERARCHICAL = 1;
	
	private Map<Instance, UUID> cellToInstanceMap = new HashMap<Instance, UUID>();
	private Map<Integer, CellCollection> clusterMap = new HashMap<Integer, CellCollection>();
	
	private String newickTree;	
	private Logger logger;
		
	private CellCollection collection;
	private ClusteringOptions options;
		
	public NucleusClusterer(CellCollection collection, ClusteringOptions options){
		this.options = options;
		this.collection = collection;
	}
	
	
	@Override
	protected void process(List<Integer> integers){
//		// get last published value
//		int amount = integers.get( integers.size() - 1 );
//		
//		// total number of nuclei
//		int total = dataset.getCollection().getNucleusCount();
//		
//		// express as percent as int
//		int progress = (int) (((double) amount / (double) total)*100);
//		setProgress(progress);
	}
	
	@Override
	protected Boolean doInBackground() {
		boolean ok = cluster(collection);
		return ok;
	}
	
	@Override
	protected void done(){
		try {
			if(this.get()){
				firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());
			} else {
				firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
			}
		} catch (InterruptedException e) {
			logger.error("Error in clustering", e);
		} catch (ExecutionException e) {
			logger.error("Error in clustering", e);
		}
	}

	
	public CellCollection getCluster(int cluster){
		return this.clusterMap.get(cluster);
	}
	
	public String getNewickTree(){
		if(options.getType()==NucleusClusterer.HIERARCHICAL){
			return this.newickTree;
		} else{
			return null;
		}
	}
	
	public int getNumberOfClusters(){
		return clusterMap.size();
	}

	public boolean cluster(CellCollection collection){
		
		this.logger = new Logger(collection.getDebugFile(), "NucleusClusterer");
		
		logger.log("Beginning clustering of population");
				
		try {
						
			// create Instances to hold Instance
			Instances instances = makeAttributesAndInstances(collection);

			// create the clusterer to run on the Instances
//			String[] options = makeClusteringOptions();
			String[] optionArray = this.options.getOptions();

			try {
				

				logger.log("Clusterer is type "+options.getType());
				for(String s : optionArray){
					logger.log("Clusterer options: "+s, Logger.DEBUG);
				}
				
				
				if(options.getType()==NucleusClusterer.HIERARCHICAL){
					HierarchicalClusterer clusterer = new HierarchicalClusterer();
					clusterer.setOptions(optionArray);     // set the options
					clusterer.buildClusterer(instances);    // build the clusterer
					assignClusters(clusterer, collection);		
					this.newickTree = clusterer.graph();
				}
				
				if(options.getType()==NucleusClusterer.EM){
					EM clusterer = new EM();   // new instance of clusterer
					clusterer.setOptions(optionArray);     // set the options
					clusterer.buildClusterer(instances);    // build the clusterer
					assignClusters(clusterer, collection);		
				}

			} catch (Exception e) {
				logger.error("Error in clustering", e);
				return false;
			}
		} catch (Exception e) {
			logger.error("Error in assignments", e);
			return false;
		}
		return true;
	}
	
	/**
	 * Given a trained clusterer, put each nucleus within the collection into a cluster
	 * @param clusterer the clusterer to use
	 * @param collection the collection with nuclei to cluster
	 */
	private void assignClusters(Clusterer clusterer, CellCollection collection){
		try {
			// construct new collections for each cluster

			logger.log("Clusters : "+clusterer.numberOfClusters(), Logger.DEBUG);

			for(int i=0;i<clusterer.numberOfClusters();i++ ){
				CellCollection clusterCollection = new CellCollection(collection.getFolder(), 
						collection.getOutputFolderName(), 
						collection.getName()+"_Cluster_"+i, 
						collection.getDebugFile(), 
						collection.getNucleusClass());
				
				clusterCollection.setName(collection.getName()+"_Cluster_"+i);
				clusterMap.put(i, clusterCollection);
			}

			for(Instance inst : cellToInstanceMap.keySet()){
				
				try{
					UUID id = cellToInstanceMap.get(inst);

					int clusterNumber = clusterer.clusterInstance(inst); // #pass each instance through the model
					//			 IJ.log("instance "+index+" is in cluster "+ clusterNumber)  ; //       #pretty print results
					CellCollection cluster = clusterMap.get(clusterNumber);

					// should never be null
					if(collection.getCell(id)!=null){
						cluster.addCell(collection.getCell(id));
					} else {
						logger.log("Error: cell with ID "+id+" is not found", Logger.ERROR);
					}
				} catch(Exception e){
					logger.error("Error assigning instance to cluster", e);
				}
				 
			}
		} catch (Exception e) {
			logger.error("Error clustering", e);
		}
	}
	
 	/**
 	 * Make the attributes on which to cluster, and build the Weka instances used internally
 	 * @param collection the collection to cluster
 	 * @return a Weka Instances
 	 */
	private Instances makeAttributesAndInstances(CellCollection collection){

		// Values to cluster on: area, circularity, aspect ratio (feret/min diameter)
		int attributeCount;
		if(options.isIncludeModality()){
			
			attributeCount = options.getModalityRegions() + 3;
			
		} else {
			attributeCount = 3;
		}
		 

		Attribute area = new Attribute("area"); 
		Attribute circularity = new Attribute("circularity"); 
		Attribute aspect = new Attribute("aspect"); 
		
		// hold the attributes in a Vector
		FastVector attributes = new FastVector(attributeCount);
		attributes.addElement(area);
		attributes.addElement(circularity);
		attributes.addElement(aspect);
		
		if(options.isIncludeModality()){
			
			for(int i=0; i<options.getModalityRegions(); i++){
				Attribute modality = new Attribute("modality_"+i); // use the point least likely to be unimodal
				attributes.addElement(modality);
			}
			
		}

		Instances instances = new Instances(collection.getName(), attributes, collection.getNucleusCount());

		try{

			
			ProfileCollection pc = collection.getProfileCollection();

			Profile pvals = DipTester.testCollectionGetPValues(collection, BorderTag.REFERENCE_POINT);
			Profile medianProfile = pc.getProfile(collection.getPoint(BorderTag.REFERENCE_POINT));
			Profile indexes = pvals.getSortedIndexes();
			
			// find the index of the point in the median profile closest to not being unimodal
//			int lowestPvalueIndex = pvals.getIndexOfMin();

			// create Instance for each nucleus and add to Instances
			for(Cell c : collection.getCells()){

				Nucleus n = c.getNucleus();
				// instance holds data
				// Create empty instance with five attribute values
				Instance inst = new SparseInstance(attributeCount);

				// Set instance's values for the attributes
				inst.setValue(area, n.getArea());
				inst.setValue(circularity, n.getCircularity());
				inst.setValue(aspect, n.getAspectRatio());

				
				
				if(options.isIncludeModality()){
					
					Profile p = n.getAngleProfile(n.getReferencePoint());
					Profile interpolated = p.interpolate(medianProfile.size());

					for(int i=0; i<options.getModalityRegions(); i++){
						
						int index = (int) indexes.get(i);
						
						Attribute att = (Attribute) attributes.elementAt(i+3);
						inst.setValue(att, interpolated.get(index));
					}
				}
				
				/*
				 * We now have an interpolated profile matching the length of the 
				 * median profile of the collection. The lowestPvalueIndex should now
				 * correspond to the correct point in the interpolated profile.
				 */

				instances.add(inst);
				cellToInstanceMap.put(inst, c.getId());

			}
		} catch(Exception e){
			logger.error("Error making instances", e);
		}
		return instances;
	}

}
