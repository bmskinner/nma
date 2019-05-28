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

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshFace;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.Profileable;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.stats.GenericStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;

import weka.clusterers.HierarchicalClusterer;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;

public class TreeBuildingMethod extends CellClusteringMethod {

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
        IClusterGroup group = new ClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, options,
                newickTree);
        IAnalysisResult r = new ClusterAnalysisResult(dataset, group);
        return r;
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

                finer("Clusterer is type " + options.getType());
                for (String s : optionArray) {

                    finest("Clusterer options: " + s);
                }

                // Cobweb clusterer = new Cobweb();
                HierarchicalClusterer clusterer = new HierarchicalClusterer();

                clusterer.setOptions(optionArray); // set the options
                clusterer.setDistanceFunction(new EuclideanDistance());
                clusterer.setDistanceIsBranchLength(true);
                clusterer.setNumClusters(1);
                clusterer.setDebug(true);

                finest("Building clusterer for tree");
                clusterer.buildClusterer(instances); // build the clusterer with
                                                     // one cluster for the tree
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
    
    private FastVector makeTsneAttributes() {
    	FastVector attributes = new FastVector(2);
    	attributes.addElement(new Attribute("tSNE_X"));
    	attributes.addElement(new Attribute("tSNE_Y"));
    	 if (options.getType().equals(ClusteringMethod.HIERARCHICAL)) {
             Attribute name = new Attribute("name", (FastVector) null);
             attributes.addElement(name);
         }
    	return attributes;
    }
    
    private FastVector makePCAttributes() {
    	
    	// From the first nucleus, find the number of PCs to cluster on
    	Nucleus n = dataset.getCollection().getCells().stream().findFirst().get().getNucleus();
    	int nPcs = (int) n.getStatistic(PlottableStatistic.PCA_N); 

    	FastVector attributes = new FastVector(nPcs);
    	for(int i=1; i<=nPcs; i++)
    		attributes.addElement(new Attribute("PC_"+i));

    	
    	 if (options.getType().equals(ClusteringMethod.HIERARCHICAL)) {
             Attribute name = new Attribute("name", (FastVector) null);
             attributes.addElement(name);
         }
    	return attributes;
    }

    @Override
	protected FastVector makeAttributes() throws ClusteringMethodException{
    	
    	// Shortcuts if dimensional reduction is chosen
    	if(options.getBoolean(IClusteringOptions.USE_TSNE_KEY))
        	return makeTsneAttributes();
    	
    	if(options.getBoolean(IClusteringOptions.USE_PCA_KEY))
        	return makePCAttributes();   	

        // Determine the number of attributes required
        int attributeCount = 0;
        int profileAttributeCount = 0;

        double profileWindow = Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION;

        if (options.isIncludeProfile()) { // An attribute for each angle in the
                                          // median profile, spaced <windowSize>
                                          // apart          
            if (dataset.hasAnalysisOptions()) 
                profileWindow = dataset.getAnalysisOptions().get().getProfileWindowProportion();

            profileAttributeCount = (int) Math.floor(1d / profileWindow);
            attributeCount += profileAttributeCount;
        }

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat))
                attributeCount++;
        }

        Mesh<Nucleus> mesh = null;

        if(options.isIncludeMesh() && collection.hasConsensus()){
            try {
				mesh = new DefaultMesh(collection.getConsensus());
				attributeCount += mesh.getFaces().size();
			} catch (MeshCreationException e) {
				e.printStackTrace();
				throw new ClusteringMethodException(e);
			}
            
        }

        if (options.getType().equals(ClusteringMethod.HIERARCHICAL)) {
            attributeCount++;
        }
        
        // Create the attributes

        FastVector attributes = new FastVector(attributeCount);

        if (options.isIncludeProfile()) {
            fine("Including profile " + options.getProfileType());
            for (int i = 0; i < profileAttributeCount; i++) {
                Attribute a = new Attribute("att_" + i);
                attributes.addElement(a);
            }
        }

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat)) {
                Attribute a = new Attribute(stat.toString());
                attributes.addElement(a);
            }
        }

        for (UUID segID : options.getSegments()) {
            if (options.isIncludeSegment(segID)) {
                finer("Including segment" + segID.toString());
                Attribute a = new Attribute("att_" + segID.toString());
                attributes.addElement(a);
            }
        }

        if (options.isIncludeMesh() && collection.hasConsensus() && mesh!=null ){

            for (MeshFace face : mesh.getFaces()) {
                finer("Including face " + face.toString());
                Attribute a = new Attribute("mesh_" + face.toString());
                attributes.addElement(a);
            }
        }

        if (options.getType().equals(ClusteringMethod.HIERARCHICAL)) {
            Attribute name = new Attribute("name", (FastVector) null);
            attributes.addElement(name);
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

        double windowProportion = Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION;
        if (dataset.hasAnalysisOptions())// Merged datasets may not have options
            windowProportion = dataset.getAnalysisOptions().get().getProfileWindowProportion();

        // Weka clustering uses a table in which columns are attributes and rows are instances
        FastVector attributes = makeAttributes();

        Instances instances = new Instances(collection.getName(), attributes, collection.size());

        Mesh<Nucleus> template = null;
        if (options.isIncludeMesh() && collection.hasConsensus()) {
            try {
				template = new DefaultMesh(collection.getConsensus());
			} catch (MeshCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new ClusteringMethodException(e);
			}
        }
        
        for(ICell c : collection) {
        	for(Nucleus n : c.getNuclei()) {
        		 try {
                     addNucleus(c, n, attributes, instances, template, windowProportion);
                 } catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException
                         | MeshCreationException e) {
                     // TODO Auto-generated catch block
                     e.printStackTrace();
                 }
        	}
        }
        return instances;

    }
    
    private void addTsneNucleus(ICell c, Nucleus n, FastVector attributes, Instances instances) {
    	int attNumber = 0;
    	Instance inst = new SparseInstance(attributes.size());
    	Attribute attX = (Attribute) attributes.elementAt(attNumber++);
    	inst.setValue(attX, n.getStatistic(PlottableStatistic.TSNE_1));
    	Attribute attY = (Attribute) attributes.elementAt(attNumber++);
    	inst.setValue(attY, n.getStatistic(PlottableStatistic.TSNE_2));
    	
    	if (options.getType().equals(ClusteringMethod.HIERARCHICAL)) {
            String uniqueName = c.getId().toString();
            Attribute att = (Attribute) attributes.elementAt(attNumber++);
            inst.setValue(att, uniqueName);
        }
    	 instances.add(inst);
         cellToInstanceMap.put(inst, c.getId());
         fireProgressEvent();
    }
    
    private void addPCANucleus(ICell c, Nucleus n, FastVector attributes, Instances instances) {
    	int attNumber = 0;
    	Instance inst = new SparseInstance(attributes.size());
    	
    	int nPcs = (int) n.getStatistic(PlottableStatistic.PCA_N);
    	for(int i=1; i<=nPcs; i++) {
    		Attribute att = (Attribute) attributes.elementAt(attNumber++);
    		double pc = n.getStatistic(PlottableStatistic.makePrincipalComponent(i));
    		inst.setValue(att, pc);
    	}
    	
    	if (options.getType().equals(ClusteringMethod.HIERARCHICAL)) {
            String uniqueName = c.getId().toString();
            Attribute att = (Attribute) attributes.elementAt(attNumber++);
            inst.setValue(att, uniqueName);
        }
    	 instances.add(inst);
         cellToInstanceMap.put(inst, c.getId());
         fireProgressEvent();
    }

    private void addNucleus(ICell c, Nucleus n, FastVector attributes, Instances instances, Mesh<Nucleus> template,
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

        if (options.isIncludeProfile()) {

            // Interpolate the profile to the median length
            IProfile p = n.getProfile(options.getProfileType(), Tag.REFERENCE_POINT);

            for (int profileAtt = 0; profileAtt < pointsToSample; profileAtt++) {
                Attribute att = (Attribute) attributes.elementAt(profileAtt);
                inst.setValue(att, p.get(profileAtt * windowProportion));
                attNumber++;
            }

        }

        for (PlottableStatistic stat : PlottableStatistic.getNucleusStats(collection.getNucleusType())) {
            if (options.isIncludeStatistic(stat)) {
                Attribute att = (Attribute) attributes.elementAt(attNumber++);
                inst.setValue(att, n.getStatistic(stat, MeasurementScale.MICRONS));

            }
        }

        for (UUID segID : options.getSegments()) {
            if (options.isIncludeSegment(segID)) {
                Attribute att = (Attribute) attributes.elementAt(attNumber++);
                double length = 0;
                try {
                    length = n.getProfile(ProfileType.ANGLE).getSegment(segID).length();
                } catch (UnavailableComponentException e) {
                    stack(e);
                }
                inst.setValue(att, length);
            }
        }

        if (options.isIncludeMesh() && collection.hasConsensus()) {

            Mesh<Nucleus> mesh = new DefaultMesh(n, template);
            for (MeshFace face : mesh.getFaces()) {
                Attribute att = (Attribute) attributes.elementAt(attNumber++);
                inst.setValue(att, face.getArea());
            }
        }

        if (options.getType().equals(ClusteringMethod.HIERARCHICAL)) {
            String uniqueName = c.getId().toString();
            Attribute att = (Attribute) attributes.elementAt(attNumber++);
            inst.setValue(att, uniqueName);
        }
        


        instances.add(inst);
        cellToInstanceMap.put(inst, c.getId());
        fireProgressEvent();
    }

}
