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

import stats.DipTester;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import utility.Constants;
import utility.Utils;
import weka.clusterers.Cobweb;
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


public class NucleusTreeBuilder extends AnalysisWorker {
		
	protected Map<Instance, UUID> cellToInstanceMap = new HashMap<Instance, UUID>();
	
	protected String newickTree = null;	
		
	protected CellCollection collection;
	protected ClusteringOptions options;
		
	public NucleusTreeBuilder(AnalysisDataset dataset, ClusteringOptions options, Logger programLogger){
		super(dataset, programLogger);
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
	
	protected Instances makeInstances(){
		log(Level.FINER, "Making instances");

		Instances instances = null;

		if(options.isUseSimilarityMatrix()){
			instances = makeMatrixInstances(collection);
		} else {
			instances = makeStandardInstances(collection);
		}
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
		
	private List<Integer> getModalityIndexes(Profile indexes, int windowSize){
		List<Integer> previousValues = new ArrayList<Integer>();

		for(int i=0; i<indexes.size(); i++){
			int index = (int) indexes.get(i);
			log(Level.FINEST, "Testing p-value index "+i+": "+index);
			// Stop picking values if we have enough modalilty regions
			if(previousValues.size()>=options.getModalityRegions()){
				log(Level.FINEST, "Found enough modality points");
				break;
			}		
			
			// check that each value is not too close to the previous
			// values in the profile						
			boolean ok = true;
			
			if(!previousValues.isEmpty()){
				
				for(int testIndex = -windowSize; testIndex<windowSize;testIndex++){

					// Get the values wrapped to the array for testing
					int offsetTestIndex = Utils.wrapIndex(index + testIndex, indexes.size());

					for(int prev : previousValues){

						if(prev == offsetTestIndex){
							log(Level.FINEST, "Existing index "+prev+" is within window of "+index);
							ok=false; // too close to a previous value
						}
					}

				}
			}
			
			if(ok){
				log(Level.FINEST, "Added modality index "+previousValues.size()+": "+index);
				previousValues.add(index);
				
			}

		}
		log(Level.FINEST, "Found "+previousValues.size()+" usable modality points");
		return previousValues;
	}
	
 	/**
 	 * Make the attributes on which to cluster, and build the Weka instances used internally
 	 * @param collection the collection to cluster
 	 * @return a Weka Instances
 	 */
	private Instances makeStandardInstances(CellCollection collection){

		// Values to cluster on: area, circularity, aspect ratio (feret/min diameter)
		log(Level.FINER, "Creating attributes");
		int basicAttibuteCount = options.getType().equals(ClusteringMethod.HIERARCHICAL) ? 4 : 3;
		int attributeCount = basicAttibuteCount;
		if(options.isIncludeModality()){
			
			attributeCount += options.getModalityRegions();
			log(Level.FINER, "Included modality");
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
		
		
		log(Level.FINER, "Created attributes");

		Instances instances = new Instances(collection.getName(), attributes, collection.getNucleusCount());

		try{

			log(Level.FINER, "Building instances");
			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.FRANKEN);

			Profile pvals = DipTester.testCollectionGetPValues(collection, BorderTag.REFERENCE_POINT, ProfileCollectionType.FRANKEN);
			Profile medianProfile = pc.getProfile(BorderTag.REFERENCE_POINT, 50);
			Profile indexes = pvals.getSortedIndexes();
			

			List<Integer> modalityIndexes = getModalityIndexes(indexes, collection.getNuclei().get(0).getAngleProfileWindowSize());

			// create Instance for each nucleus and add to Instances
			//			List<String> stringList = new ArrayList<String>();
			int j=0;
			for(Cell c : collection.getCells()){
				log(Level.FINEST, "Adding cell "+j);

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
					String uniqueName =  makeUniqueName(n);
					inst.setValue(name,  uniqueName);
				}



				if(options.isIncludeModality()){

					Profile p = n.getAngleProfile(BorderTag.REFERENCE_POINT);
					Profile interpolated = p.interpolate(medianProfile.size());

					for(int index=0; index<modalityIndexes.size(); index++){

						Attribute att = (Attribute) attributes.elementAt(index+basicAttibuteCount);
						inst.setValue(att, interpolated.get(modalityIndexes.get(index)));

					}
				}

				instances.add(inst);
				cellToInstanceMap.put(inst, c.getId());
				publish(j++);

			}

			log(Level.FINER, "Built instances");
		} catch(Exception e){

			logError("Error making instances", e);
		}
		return instances;
	}
	
	private double[][] makeSimilarityMatrix(CellCollection collection) throws Exception{
		
		if(collection.hasNucleusSimilarityMatrix()){
			log(Level.FINER, "Found existing matrix");
			publish(collection.size());
			return collection.getNucleusSimilarityMatrix();
		} else {
			log(Level.FINER, "Creating similarity matrix");
			double[][] matrix = new double[collection.size()][collection.size()];

			List<Nucleus> nuclei = collection.getNuclei();
			int i = 0;
			for(Nucleus n1 : nuclei){

				int j = 0;
				for(Nucleus n2 : nuclei){
					
					/*
					 * TODO: We can cut this in half by flipping the matrix
					 */

					double score = n1.getAngleProfile(BorderTag.REFERENCE_POINT).absoluteSquareDifference(n2.getAngleProfile(BorderTag.REFERENCE_POINT));
										
					matrix[i][j] = score;

					j++;
				}
				publish(i++);
			}
			collection.setNucleusSimilarityMatrix(matrix);
			return matrix;
		}
	}
	
	/**
	 * Make a taxon name with quoting, suitable for use in a Newick tree.
	 * Uses the full nucleus original file path, so will work in merged datsets.
	 * @param n
	 * @return
	 */
	private String makeUniqueName(Nucleus n){
		return "'"+n.getSourceFile()+"-"+n.getNameAndNumber()+"'";
	}

	private Instances makeMatrixInstances(CellCollection collection){
		
//		int basicAttributeCount = collection.size();
		int basicAttributeCount = collection.size()/10;
		
		int attributeCount = options.getType().equals(ClusteringMethod.HIERARCHICAL) ? basicAttributeCount+3 : basicAttributeCount+2;

		log(Level.FINE, "Building instance matrix");
		
		FastVector attributes = new FastVector(attributeCount);
		for(int i=0; i<basicAttributeCount; i+=1){
			Attribute a = new Attribute("att_"+i); 
			attributes.addElement(a);
		}
		
		Attribute area = new Attribute("area"); 
		Attribute aspect = new Attribute("aspect"); 
		Attribute name = new Attribute("name", (FastVector) null); 
		
		if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
			attributes.addElement(name);
		}
		attributes.addElement(area);
		attributes.addElement(aspect);
		
		Instances instances = new Instances(collection.getName(), attributes, collection.getNucleusCount());
		

		try{
			double[][] matrix = makeSimilarityMatrix(collection);
			
			int i=0;
			for(Cell c : collection.getCells()){
				Nucleus n1 = c.getNucleus();
				Instance inst = new SparseInstance(attributeCount);

				
				int attNumber = 0;
				for(int j=0; j<collection.size(); j+=10){
//				for(Nucleus n2 : collection.getNuclei()){
//					Nucleus n2 = collection.getNuclei().get(j);
//					Attribute att = (Attribute) attributes.elementAt(j);
					Attribute att = (Attribute) attributes.elementAt(attNumber);
					double score = matrix[i][j];
//					
					score /= n1.getPerimeter();
					inst.setValue(att, score);
					attNumber++;
				}
				if(options.getType().equals(ClusteringMethod.HIERARCHICAL)){
					String uniqueName = makeUniqueName(n1);
					inst.setValue(name, uniqueName);
				}
				
				inst.setValue(area, n1.getArea());
				inst.setValue(aspect, n1.getAspectRatio());
				
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

