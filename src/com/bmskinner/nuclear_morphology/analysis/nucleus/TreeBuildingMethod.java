package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshFace;
import com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMesh;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.ClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

public class TreeBuildingMethod extends AbstractAnalysisMethod {
	
protected Map<Instance, UUID> cellToInstanceMap = new HashMap<Instance, UUID>();
	
	protected String newickTree = null;	
		
	protected ICellCollection collection;
	protected IClusteringOptions options;
		
	/**
	 * Construct from a dataset with a set of clustering options
	 * @param dataset
	 * @param options
	 */
	public TreeBuildingMethod(IAnalysisDataset dataset, IClusteringOptions options){
		super(dataset);
		this.options = options;
		this.collection = dataset.getCollection();
		
	}
	
	@Override
	public IAnalysisResult call() throws Exception {

		run();		
		int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
		IClusterGroup group = new ClusterGroup("ClusterGroup_"+clusterNumber, options, newickTree);
		IAnalysisResult r = new ClusterAnalysisResult(dataset, group);
		return r;
	}
	
	private void run() {
		boolean ok = makeTree(collection);
	}
	
	public IClusteringOptions getOptions(){
		return this.options;
	}
	
	/**
	 * If a tree is present (i.e clustering was hierarchical),
	 * return the string of the tree, otherwise return null
	 * @return
	 */
	public String getNewickTree(){
		return newickTree;
	}
	
	protected Instances makeInstances()throws Exception{

		Instances instances = null;

		finer("Making profile instances");
		instances = makeProfileInstances(collection);

		finer( instances.toSummaryString());
		return instances;
		
	}
	

	/**
	 * Run the clustering on a collection
	 * @param collection
	 * @return success or fail
	 */
	protected boolean makeTree(ICellCollection collection){

		fine("Beginning tree building");
				
		try {
						
			// create Instances to hold Instance
			Instances instances = makeInstances();

			// create the clusterer to run on the Instances
			String[] optionArray = options.getOptions();

			try {
				

				finer("Clusterer is type "+options.getType());
				for(String s : optionArray){

					finest("Clusterer options: "+s);
				}
				
//				Cobweb clusterer = new Cobweb();
				HierarchicalClusterer clusterer = new HierarchicalClusterer();

				clusterer.setOptions(optionArray);     // set the options
				clusterer.setDistanceFunction(new EuclideanDistance());
				clusterer.setDistanceIsBranchLength(true);
				clusterer.setNumClusters(1);
				clusterer.setDebug(true);

				finest("Building clusterer for tree");
				clusterer.buildClusterer(instances);    // build the clusterer with one cluster for the tree
				clusterer.setPrintNewick(true);

				this.newickTree = clusterer.graph();

			} catch (Exception e) {
				error("Error in clustering", e);
				return false;
			}
		} catch (Exception e) {
			error("Error in assignments", e);
			return false;
		}
		return true;
	}
					
	private FastVector makeAttributes(ICellCollection collection, int windowSize)throws Exception {
		
		// Determine the number of attributes required
		
		int attributeCount        = 0;
		int profileAttributeCount = 0;
		
		if(options.isIncludeProfile()){ // An attribute for each angle in the median profile, spaced <windowSize> apart
			finest("Including profile");
			profileAttributeCount = collection.getProfileCollection().getProfile(options.getProfileType(), Tag.REFERENCE_POINT, 50).size();
			profileAttributeCount /= windowSize;
			attributeCount += profileAttributeCount;
		}
		
		for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())){
			if(options.isIncludeStatistic(stat)){
				finest("Including "+stat.toString());
				attributeCount++;
			}
		}
		
		Mesh<Nucleus> mesh = null;
		if(options.isIncludeMesh() && collection.hasConsensus()){	
			mesh = new NucleusMesh(collection.getConsensus());
			attributeCount+= mesh.getFaces().size();
		}
		
		if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
			attributeCount++;
		}
		
		// Create the attributes
		
		FastVector attributes = new FastVector(attributeCount);
		
		if(options.isIncludeProfile()){
			finer("Including profile");
			for(int i=0; i<profileAttributeCount; i++){
				Attribute a = new Attribute("att_"+i); 
				attributes.addElement(a);
			}
		}
		
		for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())){
			if(options.isIncludeStatistic(stat)){
				Attribute a = new Attribute(stat.toString()); 
				attributes.addElement(a);

			}
		}
		
		for(UUID segID : options.getSegments()){
			if(options.isIncludeSegment(segID)){
				finer("Including segment"+segID.toString());
				Attribute a = new Attribute("att_"+segID.toString()); 
				attributes.addElement(a);
			}
		}
		
		if(options.isIncludeMesh() && collection.hasConsensus()){
			
			for(MeshFace face : mesh.getFaces()){
				finer("Including face "+face.toString());
				Attribute a = new Attribute("mesh_"+face.toString()); 
				attributes.addElement(a);
			}
		}
		
		if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
			Attribute name = new Attribute("name", (FastVector) null); 
			attributes.addElement(name);
		}
		
		
		
		return attributes;
	}
	
	/**
	 * Create Instances using the interpolated profiles of nuclei
	 * @param collection
	 * @return
	 */
	private Instances makeProfileInstances(ICellCollection collection)throws Exception {
		
		int profileSize = collection.getProfileCollection().getProfile(options.getProfileType(), Tag.REFERENCE_POINT, 50).size();
		int windowSize = collection.getProfileManager().getProfileWindowSize(ProfileType.ANGLE);
		
		
		FastVector attributes = makeAttributes(collection, windowSize);
		
		Instances instances = new Instances(collection.getName(), attributes, collection.size());
		
		int profilePointsToCount = profileSize/windowSize;
		
		Mesh<Nucleus> template = null;
		if(options.isIncludeMesh() && collection.hasConsensus()){
			template = new NucleusMesh(collection.getConsensus());
		}

		try{
			
			int i=0;
			for(ICell c : collection.getCells()){
				Nucleus n1 = c.getNucleus();
				Instance inst = new SparseInstance(attributes.size());

				
				int attNumber=0;
				
				
				if(options.isIncludeProfile()){
					// Interpolate the profile to the median length
					IProfile p = n1.getProfile(options.getProfileType(), Tag.REFERENCE_POINT).interpolate(profileSize);
					
					for(attNumber=0; attNumber<profilePointsToCount; attNumber++){
						Attribute att = (Attribute) attributes.elementAt(attNumber);
						inst.setValue(att, p.get(attNumber*windowSize));
					}
				}
				
				for(PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())){
					if(options.isIncludeStatistic(stat)){
						Attribute att = (Attribute) attributes.elementAt(attNumber++);
						inst.setValue(att, n1.getStatistic(stat, MeasurementScale.MICRONS));

					}
				}
				
				for(UUID segID : options.getSegments()){
					if(options.isIncludeSegment(segID)){
						Attribute att = (Attribute) attributes.elementAt(attNumber++);
						double length = n1.getProfile(ProfileType.ANGLE).getSegment(segID).length();
						inst.setValue(att, length);
					}
				}
				
				if(options.isIncludeMesh() && collection.hasConsensus()){
					
					
					Mesh<Nucleus> mesh = new NucleusMesh(n1, template);
					for(MeshFace face : mesh.getFaces()){
						Attribute att = (Attribute) attributes.elementAt(attNumber++);
						inst.setValue(att, face.getArea());
					}
				}
				
				if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
					String uniqueName = c.getId().toString();
					Attribute att = (Attribute) attributes.elementAt(attNumber++);
					inst.setValue(att, uniqueName);
				}
				
				
				

				instances.add(inst);
				cellToInstanceMap.put(inst, c.getId());
				fireProgressEvent();
			}

		} catch(Exception e){
			error("Error making instances", e);
		}
		return instances;
		
	}
	

}
