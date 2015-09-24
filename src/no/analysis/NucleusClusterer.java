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
package no.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import cell.Cell;
import no.collections.CellCollection;
import no.components.Profile;
import no.components.ProfileCollection;
import no.nuclei.Nucleus;
import utility.Constants;
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
	
	private int type = NucleusClusterer.EM;
	
	private Map<String, Object> optionMap;
	
	private Logger logger;
		
	private CellCollection collection;
	
	public NucleusClusterer(int type, CellCollection collection){
		this.type = type;
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
	
	public void setType(int type){
		
	}
	
	public CellCollection getCluster(int cluster){
		return this.clusterMap.get(cluster);
	}
	
	public String getNewickTree(){
		if(this.type==NucleusClusterer.HIERARCHICAL){
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
			String[] options = makeClusteringOptions();

			try {
				

				logger.log("Clusterer is type "+type);
				for(String s : options){
					logger.log("Clusterer options: "+s, Logger.DEBUG);
				}
				
				
				if(type==NucleusClusterer.HIERARCHICAL){
					HierarchicalClusterer clusterer = new HierarchicalClusterer();
					clusterer.setOptions(options);     // set the options
					clusterer.buildClusterer(instances);    // build the clusterer
					assignClusters(clusterer, collection);		
					this.newickTree = clusterer.graph();
				}
				
				if(type==NucleusClusterer.EM){
					EM clusterer = new EM();   // new instance of clusterer
					clusterer.setOptions(options);     // set the options
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
		int attributeCount = 3;

		Attribute area = new Attribute("area"); 
		//		Attribute perimeter = new Attribute("perimeter"); 
		//		Attribute point1 = new Attribute("point1"); 
		//		Attribute point2 = new Attribute("point2"); 
		//		Attribute point3 = new Attribute("point3"); 
		Attribute circularity = new Attribute("circularity"); 
		Attribute aspect = new Attribute("aspect"); 


		// hold the attributes in a Vector
		FastVector attributes = new FastVector(attributeCount);

		attributes.addElement(area);
		//		attributes.addElement(perimeter);
		//		attributes.addElement(point1);
		//		attributes.addElement(point2);
		//		attributes.addElement(point3);
		attributes.addElement(circularity);
		attributes.addElement(aspect);

		Instances instances = new Instances(collection.getName(), attributes, collection.getNucleusCount());

		try{

			//		ProfileCollection pc = collection.getFrankenCollection();
			//		String pointType = collection.getReferencePoint();

			// get the nucleus profiles
			//		List<Profile> profiles = pc.getNucleusProfiles(pointType);

			// get the regions with the highest variability within the population
			//		List<Integer> variableIndexes = pc.findMostVariableRegions(pointType);

			// these points are indexes in the frankenstein profile. Find the points in each nucleus profile that they
			// compare to 
			// interpolate the frankenprofile to the frankenmedian length. Then we can use the index point directly.


			// create Instance for each nucleus and add to Instances
			//		int i = 0;
			for(Cell c : collection.getCells()){

				Nucleus n = c.getNucleus();
				// instance holds data
				// Create empty instance with five attribute values
				Instance inst = new SparseInstance(attributeCount);

				// Set instance's values for the attributes
				inst.setValue(area, n.getArea());
				inst.setValue(circularity, n.getCircularity());
				inst.setValue(aspect, n.getAspectRatio());


				//			inst.setValue(perimeter, n.getPerimeter());

				// add the mean of the variability window
				//			Profile frankenProfile = profiles.get(i);
				//			Profile interpolatedProfile = frankenProfile.interpolate(pc.getProfile(pointType).size());
				//			
				//			int attributeIndex = 2;
				//			for(int index : variableIndexes){
				//				// get the points in a window centred on the index
				//				Profile window = interpolatedProfile.getWindow(index, 10);
				//
				//				double total = 0;
				//				for(int j=0; j<21;j++){ // index plus 10 positions to either side
				//					total += window.get(j);
				//				}
				//				total = total/21;
				//				
				//				Attribute point = (Attribute) attributes.elementAt(attributeIndex);
				//				inst.setValue(point, total);
				//				attributeIndex++;
				//			}


				instances.add(inst);
				cellToInstanceMap.put(inst, c.getCellId());

				//			i++;
			}
		} catch(Exception e){
			logger.error("Error making instances", e);
		}
		return instances;
	}
 	
 	public void setClusteringOptions(Map<String, Object> options){
 		this.optionMap = options;
 		this.type =  (Integer) options.get("type"); 
// 		IJ.log("Input type: "+this.type);
 	}
	
	private String[] makeClusteringOptions(){
		
		String[] options = null;
		if(this.type==NucleusClusterer.EM){
			options = new String[2];
			options[0] = "-I";                 // max. iterations
			options[1] = "100";
			//		options[2] = "-N";
			//		options[3] = "2";
		}
		if(this.type==NucleusClusterer.HIERARCHICAL){
			
			Object o = optionMap.get("-N");
			options = new String[4];
			options[0] = "-N";                 // number of clusters
			options[1] = String.valueOf((Integer) optionMap.get("-N"));
			options[2] = "-L";                 // algorithm
			options[3] = "WARD";
		}
		
		return options;
	}
	


}
