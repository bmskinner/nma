package no.analysis;

import ij.IJ;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import no.collections.NucleusCollection;
import no.components.Profile;
import no.components.ProfileCollection;
import no.nuclei.Nucleus;
import utility.Logger;
import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;


public class NucleusClusterer {
	
	public static final int EM = 0; // expectation maximisation
	public static final int HIERARCHICAL = 1;
	
	private Map<Instance, UUID> nucleusMap = new HashMap<Instance, UUID>();
	private Map<Integer, NucleusCollection> clusterMap = new HashMap<Integer, NucleusCollection>();
	
	private String newickTree;
	
	private int type = NucleusClusterer.EM;
	
	private Map<String, Object> optionMap;
	
	private Logger logger;
		
	public NucleusClusterer(int type){
		this.type = type;
	}
	
	public void setType(int type){
		
	}
	
	public NucleusCollection getCluster(int cluster){
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

	public boolean cluster(NucleusCollection collection){
		
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
				logger.log("Error in clustering: "+e.getMessage(), Logger.ERROR);
				for(StackTraceElement el : e.getStackTrace()){
					logger.log(el.toString(), Logger.STACK);
				}
				return false;
			}
		} catch (Exception e) {
			logger.log("Error in assignments: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		}
		return true;
	}
	
	private void assignClusters(Clusterer clusterer, NucleusCollection collection){
		try {
			// construct new collections for each cluster
			Constructor<?> collectionConstructor = collection.getClass().getConstructor(new Class<?>[]{File.class, String.class, String.class, File.class});
			
			logger.log("Clusters : "+clusterer.numberOfClusters(), Logger.DEBUG);

			for(int i=0;i<clusterer.numberOfClusters();i++ ){
				NucleusCollection clusterCollection = (NucleusCollection) collectionConstructor.newInstance(collection.getFolder(), 
						collection.getOutputFolderName(), 
						collection.getType(), 
						collection.getDebugFile());
				clusterCollection.setName(collection.getName()+"_Cluster_"+i);
				clusterMap.put(i, clusterCollection);
			}

			for(Instance inst : nucleusMap.keySet()){
				
				UUID id = nucleusMap.get(inst);
//			Instance inst = enumerated_instances.instance(index);
				int clusterNumber = clusterer.clusterInstance(inst); // #pass each instance through the model
//			 IJ.log("instance "+index+" is in cluster "+ clusterNumber)  ; //       #pretty print results
				 NucleusCollection cluster = clusterMap.get(clusterNumber);
				 
				 // should never be null
				 if(collection.getNucleus(id)!=null){
					 cluster.addNucleus(collection.getNucleus(id));
				 } else {
					 logger.log("Error: nucleus with ID "+id+" is not found", Logger.ERROR);
				 }
				 
			}
		} catch (NoSuchMethodException e) {
			logger.log("Error: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (SecurityException e) {
			logger.log("Error: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (InstantiationException e) {
			logger.log("Error: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (IllegalAccessException e) {
			logger.log("Error: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (IllegalArgumentException e) {
			logger.log("Error: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (InvocationTargetException e) {
			logger.log("Error: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		} catch (Exception e) {
			logger.log("Error: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
		}
	}
	
 	private Instances makeAttributesAndInstances(NucleusCollection collection){
		// make Attributes on which to cluster
		//		Attribute id = new Attribute("id", List); 

		Attribute area = new Attribute("area"); 
		Attribute perimeter = new Attribute("perimeter"); 
		Attribute point1 = new Attribute("point1"); 
		Attribute point2 = new Attribute("point2"); 
		Attribute point3 = new Attribute("point3"); 


		// hold the attributes in a Vector
		FastVector attributes = new FastVector(5);

		//		attributes.addElement(id);
		attributes.addElement(area);
		attributes.addElement(perimeter);
		attributes.addElement(point1);
		attributes.addElement(point2);
		attributes.addElement(point3);

		Instances instances = new Instances(collection.getName(), attributes, collection.getNucleusCount());


		ProfileCollection pc = collection.getFrankenCollection();
		String pointType = collection.getReferencePoint();

		// get the nucleus profiles
		List<Profile> profiles = pc.getNucleusProfiles(pointType);

		// get the regions with the highest variability within the population
		List<Integer> variableIndexes = pc.findMostVariableRegions(pointType);

		// these points are indexes in the frankenstein profile. Find the points in each nucleus profile that they
		// compare to 
		// interpolate the frankenprofile to the frankenmedian length. Then we can use the index point directly.


		// create Instance for each nucleus and add to Instances
		int i = 0;
		for(Nucleus n : collection.getNuclei()){

			// instance holds data
			// Create empty instance with five attribute values
			Instance inst = new SparseInstance(5);

			// Set instance's values for the attributes
			//			inst.setValue(id, n.getID().toString());
			inst.setValue(area, n.getArea());
			inst.setValue(perimeter, n.getPerimeter());

			// add the mean of the variability window
			Profile frankenProfile = profiles.get(i);
			Profile interpolatedProfile = frankenProfile.interpolate(pc.getProfile(pointType).size());
			
			int attributeIndex = 2;
			for(int index : variableIndexes){
				// get the points in a window centred on the index
				Profile window = interpolatedProfile.getWindow(index, 10);

				double total = 0;
				for(int j=0; j<21;j++){ // index plus 10 positions to either side
					total += window.get(j);
				}
				total = total/21;
				
				Attribute point = (Attribute) attributes.elementAt(attributeIndex);
				inst.setValue(point, total);
				attributeIndex++;
			}

//
//
//				
//				inst.setValue(point2, Math.random());
//				inst.setValue(point3, Math.random());
				instances.add(inst);
				nucleusMap.put(inst, n.getID());
				
			i++;
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
