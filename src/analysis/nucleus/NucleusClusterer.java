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

import ij.IJ;
import stats.DipTester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import utility.Constants;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import components.Cell;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileCollectionType;
import components.nuclei.Nucleus;


public class NucleusClusterer extends AnalysisWorker {
	
	public static final int EM = 0; // expectation maximisation
	public static final int HIERARCHICAL = 1;
	
	private Map<Instance, UUID> cellToInstanceMap = new HashMap<Instance, UUID>();
	private Map<Integer, CellCollection> clusterMap = new HashMap<Integer, CellCollection>();
	
	private String newickTree;	
		
	private CellCollection collection;
	private ClusteringOptions options;
		
	public NucleusClusterer(AnalysisDataset dataset, ClusteringOptions options, Logger programLogger){
		super(dataset, programLogger);
		this.options = options;
		this.collection = dataset.getCollection();
		this.setProgressTotal(dataset.getCollection().size() *2);
		
		programLogger.log(Level.FINEST, "Total set to "+this.getProgressTotal());
		fileLogger = Logger.getLogger(NucleusClusterer.class.getName());
		fileLogger.setLevel(Level.ALL);
		fileLogger.addHandler(dataset.getLogHandler());
	}
	
	@Override
	protected Boolean doInBackground() {
		boolean ok = cluster(collection);
		return ok;
	}
	
	/**
	 * Fetch the cluster with the given number
	 * @param cluster
	 * @return
	 */
	public CellCollection getCluster(int cluster){
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
	public String getNewickTree(){
		if(options.getType()==ClusteringMethod.HIERARCHICAL){
			return this.newickTree;
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
	public boolean cluster(CellCollection collection){
		
//		this.logger = new Logger(collection.getDebugFile(), "NucleusClusterer");
		
		fileLogger.log(Level.INFO, "Beginning clustering of population");
				
		try {
						
			// create Instances to hold Instance
			
			Instances instances = null;
			
			if(options.isUseSimilarityMatrix()){
				instances = makeMatrixInstances(collection);
			} else {
				 instances = makeAttributesAndInstances(collection);
			}
			

			// create the clusterer to run on the Instances
			String[] optionArray = this.options.getOptions();

			try {
				

				fileLogger.log(Level.INFO, "Clusterer is type "+options.getType());
				for(String s : optionArray){
					fileLogger.log(Level.FINE, "Clusterer options: "+s);
					programLogger.log(Level.FINEST, "Clusterer options: "+s);
				}
				
				
				if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
					HierarchicalClusterer clusterer = new HierarchicalClusterer();
					
					clusterer.setOptions(optionArray);     // set the options
					clusterer.setDistanceFunction(new EuclideanDistance());
					clusterer.setDistanceIsBranchLength(true);
					clusterer.setNumClusters(1);
					clusterer.buildClusterer(instances);    // build the clusterer with one cluster for the tree
					clusterer.setPrintNewick(true);
					
					this.newickTree = clusterer.graph();
					
					clusterer.setNumClusters(options.getClusterNumber());
					
//					firePropertyChange("Cooldown", getProgress(), Constants.Progress.FINISHED.code());
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
				fileLogger.log(Level.SEVERE, "Error in clustering", e);
				programLogger.log(Level.SEVERE, "Error in clustering", e);
				return false;
			}
		} catch (Exception e) {
			fileLogger.log(Level.SEVERE, "Error in assignments", e);
			programLogger.log(Level.SEVERE, "Error in assignments", e);
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
			fileLogger.log(Level.FINE, "Assigning nuclei to clusters");
			fileLogger.log(Level.FINE, "Clusters : "+clusterer.numberOfClusters());

			for(int i=0;i<clusterer.numberOfClusters();i++ ){
				fileLogger.log(Level.FINE, "Cluster "+i+": " +	collection.getName()+"_Cluster_"+i);
				CellCollection clusterCollection = new CellCollection(collection.getFolder(), 
						collection.getOutputFolderName(), 
						collection.getName()+"_Cluster_"+i, 
						collection.getDebugFile(), 
						collection.getNucleusType());
				
				clusterCollection.setName(collection.getName()+"_Cluster_"+i);
				clusterMap.put(i, clusterCollection);
			}

			int i = collection.size();
			for(Instance inst : cellToInstanceMap.keySet()){
				
				try{
					UUID id = cellToInstanceMap.get(inst);

					int clusterNumber = clusterer.clusterInstance(inst); // #pass each instance through the model
					//			 IJ.log("instance "+index+" is in cluster "+ clusterNumber)  ; //       #pretty print results
					CellCollection cluster = clusterMap.get(clusterNumber);

					// should never be null
					if(collection.getCell(id)!=null){
						cluster.addCell(new Cell (collection.getCell(id)));
					} else {
						fileLogger.log(Level.SEVERE, "Error: cell with ID "+id+" is not found");
					}
					publish(i);
					i++;
				} catch(Exception e){
					fileLogger.log(Level.SEVERE, "Error assigning instance to cluster", e);
				}
				 
			}
		} catch (Exception e) {
			fileLogger.log(Level.SEVERE, "Error clustering", e);
		}
	}
	
 	/**
 	 * Make the attributes on which to cluster, and build the Weka instances used internally
 	 * @param collection the collection to cluster
 	 * @return a Weka Instances
 	 */
	private Instances makeAttributesAndInstances(CellCollection collection){

		// Values to cluster on: area, circularity, aspect ratio (feret/min diameter)
		programLogger.log(Level.FINER, "Creating attributes");
		int basicAttibuteCount = options.getType().equals(ClusteringMethod.HIERARCHICAL) ? 4 : 3;
		int attributeCount = basicAttibuteCount;
		if(options.isIncludeModality()){
			
			attributeCount += options.getModalityRegions();
			programLogger.log(Level.FINER, "Included modality");
		}
		 

		Attribute area = new Attribute("area"); 
		Attribute circularity = new Attribute("circularity"); 
		Attribute aspect = new Attribute("aspect"); 
		
		Attribute name = new Attribute("name", (FastVector) null); 
				
		// hold the attributes in a Vector
		FastVector attributes = new FastVector(attributeCount);
		attributes.addElement(area);
		attributes.addElement(circularity);
		attributes.addElement(aspect);
		
		if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
			attributes.addElement(name);
		}
		
		if(options.isIncludeModality()){
			
			for(int i=0; i<options.getModalityRegions(); i++){
				Attribute modality = new Attribute("modality_"+i); // use the point least likely to be unimodal
				attributes.addElement(modality);
			}
			
		}
		
		
		programLogger.log(Level.FINER, "Created attributes");

		Instances instances = new Instances(collection.getName(), attributes, collection.getNucleusCount());

		try{

			programLogger.log(Level.FINER, "Building instances");
			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.FRANKEN);

			Profile pvals = DipTester.testCollectionGetPValues(collection, BorderTag.REFERENCE_POINT, ProfileCollectionType.FRANKEN);
			Profile medianProfile = pc.getProfile(BorderTag.REFERENCE_POINT, 50);
			Profile indexes = pvals.getSortedIndexes();
			
			// create Instance for each nucleus and add to Instances
//			List<String> stringList = new ArrayList<String>();
			int j=0;
			for(Cell c : collection.getCells()){
				programLogger.log(Level.FINEST, "Adding cell "+j);

				Nucleus n = c.getNucleus();
				// instance holds data
				// Create empty instance with five attribute values
				Instance inst = new SparseInstance(attributeCount);

				// Set instance's values for the attributes
				inst.setValue(area, n.getArea());
				inst.setValue(circularity, n.getCircularity());
				inst.setValue(aspect, n.getAspectRatio());
//				stringList.add(n.getNameAndNumber());
				if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
					inst.setValue(name,  n.getNameAndNumber());
				}
				
				
				
				if(options.isIncludeModality()){
					
					Profile p = n.getAngleProfile(BorderTag.REFERENCE_POINT);
					Profile interpolated = p.interpolate(medianProfile.size());

					for(int i=0; i<options.getModalityRegions(); i++){
						
						int index = (int) indexes.get(i);
						
						Attribute att = (Attribute) attributes.elementAt(i+basicAttibuteCount);
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
				publish(j++);

			}
			
			
			programLogger.log(Level.FINER, "Built instances");
		} catch(Exception e){
			fileLogger.log(Level.SEVERE, "Error making instances", e);
			programLogger.log(Level.SEVERE, "Error making instances", e);
		}
		return instances;
	}
	
	private double[][] makeSimilarityMatrix(CellCollection collection) throws Exception{
		
		double[][] matrix = new double[collection.size()][collection.size()];
		
		int i = 0;
		for(Nucleus n1 : collection.getNuclei()){
			
			int j = 0;
			for(Nucleus n2 : collection.getNuclei()){
				
				double score = n1.getAngleProfile(BorderTag.REFERENCE_POINT).absoluteSquareDifference(n2.getAngleProfile(BorderTag.REFERENCE_POINT));
				matrix[i][j] = score;
				
				j++;
			}
			i++;
		}
		
		return matrix;
	}

	private Instances makeMatrixInstances(CellCollection collection){
		
		int basicAttributeCount = collection.size();
		int attributeCount = options.getType().equals(ClusteringMethod.HIERARCHICAL) ? basicAttributeCount+1 : basicAttributeCount;

		fileLogger.log(Level.FINE, "Building instance matrix");
		
		FastVector attributes = new FastVector(attributeCount);
		for(int i=0; i<attributeCount; i++){
			Attribute a = new Attribute("att_"+i); 
			attributes.addElement(a);
		}
		
		Attribute name = new Attribute("name", (FastVector) null); 
		
		if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
			attributes.addElement(name);
		}
		
		Instances instances = new Instances(collection.getName(), attributes, collection.getNucleusCount());

		try{

			int i=0;
			for(Cell c : collection.getCells()){
				Nucleus n1 = c.getNucleus();
				Instance inst = new SparseInstance(attributeCount);

				
				int j=0;
				for(Nucleus n2 : collection.getNuclei()){
					
					Attribute att = (Attribute) attributes.elementAt(j);
					double score = n1.getAngleProfile(BorderTag.REFERENCE_POINT).absoluteSquareDifference(n2.getAngleProfile(BorderTag.REFERENCE_POINT));
					score /= n1.getPerimeter();
					inst.setValue(att, score);
					j++;
				}
				if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
					inst.setValue(name,  n1.getNameAndNumber());
				}
				
				instances.add(inst);
				cellToInstanceMap.put(inst, c.getId());
				publish(i++);
			}

		} catch(Exception e){
			fileLogger.log(Level.SEVERE, "Error making instances", e);
		}
		fileLogger.log(Level.FINE, "Instance matrix: "+instances.toSummaryString());
		return instances;
	}

}
