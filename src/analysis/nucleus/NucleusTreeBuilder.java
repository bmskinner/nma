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
import stats.SegmentStatistic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import utility.Constants;
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
import components.generic.BorderTagObject;
import components.generic.MeasurementScale;
import components.generic.Profile;
import components.generic.ProfileType;
import components.nuclei.Nucleus;


public class NucleusTreeBuilder extends AnalysisWorker {
		
	protected Map<Instance, UUID> cellToInstanceMap = new HashMap<Instance, UUID>();
	
	protected String newickTree = null;	
		
	protected CellCollection collection;
	protected ClusteringOptions options;
		
	public NucleusTreeBuilder(AnalysisDataset dataset, ClusteringOptions options){
		super(dataset);
		this.options = options;
		this.collection = dataset.getCollection();
		this.setProgressTotal(dataset.getCollection().cellCount() * 2);
		
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
	public boolean makeTree(CellCollection collection){
		
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
				
	/**
	 * Make a taxon name with quoting, suitable for use in a Newick tree.
	 * Uses the full nucleus original file path, so will work in merged datsets.
	 * @param n
	 * @return
	 */
	private String makeUniqueName(Nucleus n){
		
		return "'"+n.getSourceFile()+"-"+n.getNameAndNumber()+"'";
//		return n.getID().toString();
	}
	
	private FastVector makeAttributes(CellCollection collection, int windowSize)throws Exception {
		
		
		int attributeCount        = 0;
		int profileAttributeCount = 0;
		
		if(options.isIncludeProfile()){ // An attribute for each angle in the median profile
			log(Level.FINEST, "Including profile");
			profileAttributeCount = collection.getProfileCollection(options.getProfileType()).getProfile(BorderTagObject.REFERENCE_POINT, 50).size();
			profileAttributeCount /= windowSize;
			attributeCount += profileAttributeCount;
		}
		
		for(NucleusStatistic stat : NucleusStatistic.values()){
			if(options.isIncludeStatistic(stat)){
				log(Level.FINEST, "Including "+stat.toString());
				attributeCount++;
			}
		}
		
		if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
			attributeCount++;
		}
		
		FastVector attributes = new FastVector(attributeCount);
		
		if(options.isIncludeProfile()){
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
				log(Level.FINEST, "Including segment"+segID.toString());
				Attribute a = new Attribute("att_"+segID.toString()); 
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
	private Instances makeProfileInstances(CellCollection collection)throws Exception {
		
		int profileSize = collection.getProfileCollection(options.getProfileType()).getProfile(BorderTagObject.REFERENCE_POINT, 50).size();
		int windowSize = collection.getNuclei().get(0).getAngleProfileWindowSize(); // use the first window size found for now
		
		
		FastVector attributes = makeAttributes(collection, windowSize);
		
		Instances instances = new Instances(collection.getName(), attributes, collection.getNucleusCount());
		
		int profilePointsToCount = profileSize/windowSize;

		try{
			
			int i=0;
			for(Cell c : collection.getCells()){
				Nucleus n1 = c.getNucleus();
				Instance inst = new SparseInstance(attributes.size());

				// Interpolate the profile to the median length
				Profile p = n1.getProfile(options.getProfileType(), BorderTagObject.REFERENCE_POINT).interpolate(profileSize);
				
				int attNumber=0;
				if(options.isIncludeProfile()){
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
				
				if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
//					String uniqueName = makeUniqueName(n1);
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

