/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshFace;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Profileable;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultClusterGroup;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.Tag;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class TreeBuildingMethod extends CellClusteringMethod {
	
	private static final Logger LOGGER = Logger.getLogger(TreeBuildingMethod.class.getName());

    protected String newickTree = null;

    /**
     * Construct from a dataset with a set of clustering options
     * 
     * @param dataset
     * @param options
     */
    public TreeBuildingMethod(@NonNull IAnalysisDataset dataset, @NonNull IClusteringOptions options) {
        super(dataset, options);
    }

    @Override
    public IAnalysisResult call() throws Exception {

    	makeTree();
        int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;
        IClusterGroup group = new DefaultClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, options,
                newickTree);
        return new ClusterAnalysisResult(dataset, group);
    }

    /**
     * If a tree is present (i.e clustering was hierarchical), return the string
     * of the tree, otherwise return null
     * 
     * @return
     */
    public String getNewickTree() {
        return newickTree;
    }



    /**
     * Run the clustering on a collection
     * 
     * @param collection
     * @return success or fail
     */
    protected boolean makeTree() {

        try {

            // create Instances to hold Instance
            Instances instances = makeInstances();

            // create the clusterer to run on the Instances
            String[] optionArray = options.getOptions();

            try {

                LOGGER.finer( "Clusterer is type " + options.getClusteringMethod());
                for (String s : optionArray) {
                    LOGGER.finest( "Clusterer options: " + s);
                }

                HierarchicalClusterer clusterer = new HierarchicalClusterer();

                clusterer.setOptions(optionArray); // set the options
                clusterer.setDistanceFunction(new EuclideanDistance());
                clusterer.setDistanceIsBranchLength(true);
                clusterer.setNumClusters(1);
                clusterer.setDebug(true);

                LOGGER.finest( "Building clusterer for tree");
                clusterer.buildClusterer(instances); // build the clusterer with
                                                     // one cluster for the tree
                clusterer.setPrintNewick(true);

                this.newickTree = clusterer.graph();

            } catch (Exception e) {
                LOGGER.log(Loggable.STACK, "Error in clustering", e);
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error in assignments", e);
            return false;
        }
        return true;
    }
    
    private ArrayList<Attribute> makeTsneAttributes() {
    	ArrayList<Attribute> attributes = new ArrayList<>();
    	attributes.add(new Attribute("tSNE_X"));
    	attributes.add(new Attribute("tSNE_Y"));
    	 if (options.getClusteringMethod().equals(ClusteringMethod.HIERARCHICAL)) {
             Attribute name = new Attribute("name", (List<String>) null);
             attributes.add(name);
         }
    	return attributes;
    }
    
    private ArrayList<Attribute> makePCAttributes() {
    	
    	// From the first nucleus, find the number of PCs to cluster on
    	Nucleus n = dataset.getCollection().getCells().stream().findFirst().orElseThrow(NullPointerException::new).getPrimaryNucleus();
    	int nPcs = (int) n.getStatistic(Measurement.PCA_N); 

    	ArrayList<Attribute> attributes = new ArrayList<>();
    	for(int i=1; i<=nPcs; i++)
    		attributes.add(new Attribute("PC_"+i));

    	
    	 if (options.getClusteringMethod().equals(ClusteringMethod.HIERARCHICAL)) {
             Attribute name = new Attribute("name", (List<String>) null);
             attributes.add(name);
         }
    	return attributes;
    }

    @Override
	protected ArrayList<Attribute> makeAttributes() throws ClusteringMethodException{
    	
    	// Shortcuts if dimensional reduction is chosen
    	LOGGER.finer("Checking if tSNE clustering is set");
    	if(options.getBoolean(IClusteringOptions.USE_TSNE_KEY))
        	return makeTsneAttributes();
    	
    	LOGGER.finer("Checking if PCA clustering is set");
    	if(options.getBoolean(IClusteringOptions.USE_PCA_KEY))
        	return makePCAttributes();   	

    	LOGGER.finer("Creating attribute count on values directly");
        // Determine the number of attributes required
        int attributeCount = 0;
        int profileAttributeCount = 0;

     // How many attributes per profile?  
        double profileWindow = Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION;
        if (dataset.hasAnalysisOptions()) 
        	profileWindow = dataset.getAnalysisOptions().orElseThrow(NullPointerException::new).getProfileWindowProportion();
        
        profileAttributeCount = (int) Math.floor(1d / profileWindow);

        // An attribute for each index in each selected profile, spaced <windowSize> apart   
        for(ProfileType t : ProfileType.displayValues()) {
        	if(options.getBoolean(t.toString()))    {	
        		LOGGER.finer("Creating attribute count for "+t.toString());
        		attributeCount += profileAttributeCount;
        	}
        }

        for (Measurement stat : Measurement.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat)) {
            	LOGGER.finer("Adding attribute count for "+stat);
                attributeCount++;
            }
        }

        Mesh<Nucleus> mesh = null;

        if(options.isIncludeMesh() && collection.hasConsensus()){
        	LOGGER.finer("Adding attribute count for mesh");
            try {
				mesh = new DefaultMesh(collection.getConsensus());
				attributeCount += mesh.getFaces().size();
			} catch (MeshCreationException e) {
				LOGGER.log(Loggable.STACK, "Cannot create mesh", e);
				throw new ClusteringMethodException(e);
			}
            
        }

        if (options.getClusteringMethod().equals(ClusteringMethod.HIERARCHICAL)) {
        	LOGGER.finer("Adding attribute count for name - hierarchical only");
            attributeCount++;
        }
        
        // Create the attributes
        LOGGER.finer("Creating attributes");
        ArrayList<Attribute> attributes = new ArrayList<>(attributeCount);
        int profileAttCounter = 0;
        for(ProfileType t : ProfileType.displayValues()) {
        	if(options.getBoolean(t.toString()))    {	
        		LOGGER.finer("Creating attributes for profile " + t);
                for (int i = 0; i < profileAttributeCount; i++) {
                    Attribute a = new Attribute("att_" + profileAttCounter);
                    attributes.add(a);
                    profileAttCounter++;
                }
        	}
        }

        for (Measurement stat : Measurement.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat)) {
            	LOGGER.finer("Creating attribute for "+stat);
                Attribute a = new Attribute(stat.toString());
                attributes.add(a);
            }
        }

        for (UUID segID : options.getSegments()) {
            if (options.isIncludeSegment(segID)) {
                LOGGER.finer( "Creating attribute for segment" + segID.toString());
                Attribute a = new Attribute("att_" + segID.toString());
                attributes.add(a);
            }
        }

        if (options.isIncludeMesh() && collection.hasConsensus() && mesh!=null ){

            for (MeshFace face : mesh.getFaces()) {
                LOGGER.finer( "Creating attribute for face " + face.toString());
                Attribute a = new Attribute("mesh_" + face.toString());
                attributes.add(a);
            }
        }

        if (options.getClusteringMethod().equals(ClusteringMethod.HIERARCHICAL)) {
        	LOGGER.finer("Creating attribute for name - hierarchical only");
            Attribute name = new Attribute("name", (List<String>) null);
            attributes.add(name);
        }
        return attributes;
    }

    /**
     * Create Instances using the interpolated profiles of nuclei
     * @return
     * @throws ClusteringMethodException 
     */
    @Override
	protected Instances makeInstances() throws ClusteringMethodException {
    	LOGGER.finer("Creating clusterable instances");
        double windowProportion = Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION;
        if (dataset.hasAnalysisOptions())// Merged datasets may not have options
            windowProportion = dataset.getAnalysisOptions().orElseThrow(NullPointerException::new).getProfileWindowProportion();

        // Weka clustering uses a table in which columns are attributes and rows are instances
        ArrayList<Attribute> attributes = makeAttributes();

        Instances instances = new Instances(collection.getName(), attributes, collection.size());

        Mesh<Nucleus> template = null;
        if (options.isIncludeMesh() && collection.hasConsensus()) {
            try {
				template = new DefaultMesh(collection.getConsensus());
			} catch (MeshCreationException e) {
				LOGGER.log(Loggable.STACK, "Cannot create mesh", e);
				throw new ClusteringMethodException(e);
			}
        }
        
        for(ICell c : collection) {
        	for(Nucleus n : c.getNuclei()) {
        		 try {
                     addNucleus(c, n, attributes, instances, template, windowProportion);
                 } catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException
                         | MeshCreationException e) {
                	 LOGGER.log(Loggable.STACK, "Cannot add nucleus data", e);
                 }
        	}
        }
        return instances;

    }
    
    private void addTsneNucleus(ICell c, Nucleus n, ArrayList<Attribute> attributes, Instances instances) {
    	int attNumber = 0;
    	Instance inst = new SparseInstance(attributes.size());
    	Attribute attX = attributes.get(attNumber++);
    	inst.setValue(attX, n.getStatistic(Measurement.TSNE_1));
    	Attribute attY = attributes.get(attNumber++);
    	inst.setValue(attY, n.getStatistic(Measurement.TSNE_2));
    	
    	if (options.getClusteringMethod().equals(ClusteringMethod.HIERARCHICAL)) {
            String uniqueName = c.getId().toString();
            Attribute att = attributes.get(attNumber++);
            inst.setValue(att, uniqueName);
        }
    	 instances.add(inst);
         cellToInstanceMap.put(inst, c.getId());
         fireProgressEvent();
    }
    
    private void addPCANucleus(ICell c, Nucleus n, ArrayList<Attribute> attributes, Instances instances) {
    	int attNumber = 0;
    	Instance inst = new SparseInstance(attributes.size());
    	
    	int nPcs = (int) n.getStatistic(Measurement.PCA_N);
    	for(int i=1; i<=nPcs; i++) {
    		Attribute att = attributes.get(attNumber++);
    		double pc = n.getStatistic(Measurement.makePrincipalComponent(i));
    		inst.setValue(att, pc);
    	}
    	
    	if (options.getClusteringMethod().equals(ClusteringMethod.HIERARCHICAL)) {
            String uniqueName = c.getId().toString();
            Attribute att = attributes.get(attNumber++);
            inst.setValue(att, uniqueName);
        }
    	 instances.add(inst);
         cellToInstanceMap.put(inst, c.getId());
         fireProgressEvent();
    }

    private void addNucleus(ICell c, Nucleus n, ArrayList<Attribute> attributes, Instances instances, Mesh<Nucleus> template,
            double windowProportion) throws UnavailableBorderTagException, UnavailableProfileTypeException,
            ProfileException, MeshCreationException {

        
        if(options.getBoolean(IClusteringOptions.USE_TSNE_KEY)) {
        	addTsneNucleus(c, n, attributes, instances);
        	return;
        }
        
        if(options.getBoolean(IClusteringOptions.USE_PCA_KEY)) {
        	addPCANucleus(c, n, attributes, instances);
        	return;
        }
        
    	int attNumber = 0;
        Instance inst = new SparseInstance(attributes.size());

        int pointsToSample = (int) Math.floor(1d / windowProportion);
        
        for(ProfileType t : ProfileType.displayValues()) {
        	
        	if(options.getBoolean(t.toString()))    {	
        		LOGGER.finer("Adding attribute for "+t.toString());
        		// Interpolate the profile to the median length
                IProfile p = n.getProfile(t, Tag.REFERENCE_POINT);

                for (int i = 0; i < pointsToSample; i++) {
                    Attribute att = attributes.get(attNumber);
                    inst.setValue(att, p.get(i * windowProportion));
                    attNumber++;
                }
        	}
        }

        for (Measurement stat : Measurement.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat)) {
                Attribute att = attributes.get(attNumber++);
                
                if(Measurement.VARIABILITY.equals(stat)) {
                	 inst.setValue(att, collection.getNormalisedDifferenceToMedian(Tag.REFERENCE_POINT, n));
                } else {
                	inst.setValue(att, n.getStatistic(stat, MeasurementScale.MICRONS));
                }

            }
        }

        for (UUID segID : options.getSegments()) {
            if (options.isIncludeSegment(segID)) {
                Attribute att = attributes.get(attNumber++);
                double length = 0;
                try {
                    length = n.getProfile(ProfileType.ANGLE).getSegment(segID).length();
                } catch (UnavailableComponentException e) {
                	LOGGER.log(Loggable.STACK, "Unable to find segment", e);
                }
                inst.setValue(att, length);
            }
        }

        if (options.isIncludeMesh() && collection.hasConsensus()) {

            Mesh<Nucleus> mesh = new DefaultMesh(n, template);
            for (MeshFace face : mesh.getFaces()) {
                Attribute att = attributes.get(attNumber++);
                inst.setValue(att, face.getArea());
            }
        }

        if (options.getClusteringMethod().equals(ClusteringMethod.HIERARCHICAL)) {
            String uniqueName = c.getId().toString();
            Attribute att = attributes.get(attNumber++);
            inst.setValue(att, uniqueName);
        }

        instances.add(inst);
        cellToInstanceMap.put(inst, c.getId());
        fireProgressEvent();
    }

}
