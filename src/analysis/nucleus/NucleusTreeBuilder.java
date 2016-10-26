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

import stats.NucleusStatistic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import utility.Constants;
import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import analysis.AnalysisWorker;
import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import analysis.IAnalysisDataset;
import analysis.mesh.NucleusMesh;
import analysis.mesh.NucleusMeshFace;
import components.ICell;
import components.ICellCollection;
import components.generic.IProfile;
import components.generic.MeasurementScale;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclei.Nucleus;


public class NucleusTreeBuilder extends AnalysisWorker {
		
	protected Map<Instance, UUID> cellToInstanceMap = new HashMap<Instance, UUID>();
	
	protected String newickTree = null;	
		
	protected ICellCollection collection;
	protected ClusteringOptions options;
		
	public NucleusTreeBuilder(IAnalysisDataset dataset, ClusteringOptions options){
		super(dataset);
		this.options = options;
		this.collection = dataset.getCollection();
		this.setProgressTotal(dataset.getCollection().size() * 2);
		
		log(Level.FINEST, "Total set to "+this.getProgressTotal());
	}
	
	@Override
	protected Boolean doInBackground() {
		boolean ok = makeTree(collection);
		return ok;
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
		return this.newickTree;
	}
	
	protected Instances makeInstances()throws Exception{

		Instances instances = null;

		log(Level.FINER, "Making profile instances");
		instances = makeProfileInstances(collection);

		log(Level.FINEST, instances.toSummaryString());
		return instances;
		
	}
	

	/**
	 * Run the clustering on a collection
	 * @param collection
	 * @return success or fail
	 */
	public boolean makeTree(ICellCollection collection){
		
//		this.logger = new Logger(collection.getDebugFile(), "NucleusClusterer");
		
		log(Level.FINE, "Beginning tree building");
				
		try {
						
			// create Instances to hold Instance
			Instances instances = makeInstances();

			// create the clusterer to run on the Instances
			String[] optionArray = this.options.getOptions();

			try {
				

				log(Level.FINER, "Clusterer is type "+options.getType());
				for(String s : optionArray){
//					fileLogger.log(Level.FINE, "Clusterer options: "+s);
					log(Level.FINEST, "Clusterer options: "+s);
				}
				
//				Cobweb clusterer = new Cobweb();
				HierarchicalClusterer clusterer = new HierarchicalClusterer();

				clusterer.setOptions(optionArray);     // set the options
				clusterer.setDistanceFunction(new EuclideanDistance());
				clusterer.setDistanceIsBranchLength(true);
				clusterer.setNumClusters(1);
				clusterer.setDebug(true);

				log(Level.FINEST, "Building clusterer for tree");
				firePropertyChange("Cooldown", getProgress(), Constants.Progress.FINISHED.code());
				clusterer.buildClusterer(instances);    // build the clusterer with one cluster for the tree
				clusterer.setPrintNewick(true);

				this.newickTree = clusterer.graph();

			} catch (Exception e) {
				logError("Error in clustering", e);
				return false;
			}
		} catch (Exception e) {
			logError("Error in assignments", e);
			return false;
		}
		return true;
	}
					
	private FastVector makeAttributes(ICellCollection collection, int windowSize)throws Exception {
		
		// Determine the number of attributes required
		
		int attributeCount        = 0;
		int profileAttributeCount = 0;
		
		if(options.isIncludeProfile()){ // An attribute for each angle in the median profile, spaced <windowSize> apart
			log(Level.FINEST, "Including profile");
			profileAttributeCount = collection.getProfileCollection(options.getProfileType()).getProfile(Tag.REFERENCE_POINT, 50).size();
			profileAttributeCount /= windowSize;
			attributeCount += profileAttributeCount;
		}
		
		for(NucleusStatistic stat : NucleusStatistic.values()){
			if(options.isIncludeStatistic(stat)){
				log(Level.FINEST, "Including "+stat.toString());
				attributeCount++;
			}
		}
		
		NucleusMesh mesh = null;
		if(options.isIncludeMesh() && collection.hasConsensusNucleus()){	
			mesh = new NucleusMesh(collection.getConsensusNucleus());
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
		
		for(NucleusStatistic stat : NucleusStatistic.values()){
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
		
		if(options.isIncludeMesh() && collection.hasConsensusNucleus()){
			
			for(NucleusMeshFace face : mesh.getFaces()){
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
		
		int profileSize = collection.getProfileCollection(options.getProfileType()).getProfile(Tag.REFERENCE_POINT, 50).size();
		int windowSize = collection.getProfileManager().getProfileWindowSize(ProfileType.ANGLE);
		
		
		FastVector attributes = makeAttributes(collection, windowSize);
		
		Instances instances = new Instances(collection.getName(), attributes, collection.size());
		
		int profilePointsToCount = profileSize/windowSize;
		
		NucleusMesh template = null;
		if(options.isIncludeMesh() && collection.hasConsensusNucleus()){
			template = new NucleusMesh(collection.getConsensusNucleus());
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
				
				for(NucleusStatistic stat : NucleusStatistic.values()){
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
				
				if(options.isIncludeMesh() && collection.hasConsensusNucleus()){
					
					
					NucleusMesh mesh = new NucleusMesh(n1, template);
					for(NucleusMeshFace face : mesh.getFaces()){
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
				publish(i++);
			}

		} catch(Exception e){
			logError("Error making instances", e);
		}
		return instances;
		
	}


}

