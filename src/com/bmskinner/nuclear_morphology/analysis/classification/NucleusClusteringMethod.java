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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultClusterGroup;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualDataset;
import com.bmskinner.nuclear_morphology.components.measure.DefaultMeasurement;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.measure.MeasurementDimension;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import weka.clusterers.Clusterer;
import weka.clusterers.EM;
import weka.clusterers.HierarchicalClusterer;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Run clustering of nuclei in a dataset based on a given set of options
 * 
 * @author ben
 * @since 1.8.0
 *
 */
public class NucleusClusteringMethod extends TreeBuildingMethod {
	
	private static final Logger LOGGER = Logger.getLogger(NucleusClusteringMethod.class.getName());

    public static final int EM           = 0; // expectation maximisation
    public static final int HIERARCHICAL = 1;

    /* Map cluster numbers to collections of cells */
    private Map<Integer, ICellCollection> clusterMap = new HashMap<>();

    /**
     * Construct from a dataset and options
     * 
     * @param dataset the analysis dataset with nuclei to cluster
     * @param options the clustering options
     */
    public NucleusClusteringMethod(@NonNull IAnalysisDataset dataset, @NonNull HashOptions options) {
        super(dataset, options);
    }

    @Override
    public IAnalysisResult call() throws Exception {

        run();

        // Save the clusters to the dataset
        List<IAnalysisDataset> list = new ArrayList<>();

        int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;

        // Create a group to store the clustered cells
        IClusterGroup group = new DefaultClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, options,
                newickTree);

        for (int cluster = 0; cluster < clusterMap.size(); cluster++) {

            ICellCollection c = clusterMap.get(cluster);

            if (c.hasCells()) {
                group.addDataset(c);
                c.setName(group.getName() + "_" + c.getName());

                dataset.addChildCollection(c);

                // attach the clusters to their parent collection
                LOGGER.fine("Cluster " + cluster + ": " + c.size() + " nuclei");
                IAnalysisDataset clusterDataset = dataset.getChildDataset(c.getId());

                // set shared counts
                c.setSharedCount(dataset.getCollection(), c.size());
                dataset.getCollection().setSharedCount(c, c.size());

                list.add(clusterDataset);
            } else {
            	LOGGER.info("No cells assigned to cluster "+cluster);
            }
        }
        LOGGER.fine("Profiles copied to all clusters");
        
        // Move tSNE values to their cluster group if needed
        if(options.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)) {
        	for(ICell c : dataset.getCollection()) {
        		for(Nucleus n : c.getNuclei()) {
        			n.setStatistic(new DefaultMeasurement("TSNE_1_"+group.getId(), MeasurementDimension.DIMENSIONLESS), n.getStatistic(Measurement.TSNE_1));
        			n.setStatistic(new DefaultMeasurement("TSNE_2_"+group.getId(), MeasurementDimension.DIMENSIONLESS), n.getStatistic(Measurement.TSNE_2));
        			n.setStatistic(Measurement.TSNE_1, Statistical.STAT_NOT_CALCULATED);
        			n.setStatistic(Measurement.TSNE_2, Statistical.STAT_NOT_CALCULATED);
        		}
        	}
        }
        
     // Move PCA values to their cluster group if needed        
        if(options.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY)) {
        	for(ICell c : dataset.getCollection()) {
        		for(Nucleus n : c.getNuclei()) {
        			int nPcs = (int) n.getStatistic(Measurement.PCA_N);
        			
        			n.setStatistic(Measurement.makePrincipalComponentNumber(group.getId()), nPcs);
        			n.clearStatistic(Measurement.PCA_N);
        			for(int i=1; i<=nPcs; i++) {
        				n.setStatistic(Measurement.makePrincipalComponent(i, group.getId()), n.getStatistic(Measurement.makePrincipalComponent(i)));
        				n.clearStatistic(Measurement.makePrincipalComponent(i));
        			}
        		}
        	}
        }
        
        
        dataset.addClusterGroup(group);
        return new ClusterAnalysisResult(list, group);
    }

    private void run() {
        boolean ok = cluster();
        LOGGER.fine("Returning " + ok);
    }

    /**
     * Run the clustering on a collection
     * 
     * @param collection
     * @return success or fail
     */
    public boolean cluster() {

        try {

            // create Instances to hold Instance
            Instances instances = makeInstances();

            // create the clusterer to run on the Instances
            String[] optionArray = createClustererOptions();

            for (String s : optionArray) {
                LOGGER.finest( "Clusterer options: " + s);
            }
            ClusteringMethod cm = ClusteringMethod.valueOf(options.getString(HashOptions.CLUSTER_METHOD_KEY));
            if (cm.equals(ClusteringMethod.HIERARCHICAL)) {
                HierarchicalClusterer hc1 = new HierarchicalClusterer();

                hc1.setOptions(optionArray);
                hc1.setDistanceFunction(new EuclideanDistance());
                hc1.setDistanceIsBranchLength(true);
                hc1.setNumClusters(1); // this is only to get the tree
                hc1.buildClusterer(instances);
                hc1.setPrintNewick(true);
                newickTree = hc1.graph();
                LOGGER.finest(newickTree);
                
                // Create a new clusterer with the desired number of clusters
                hc1.setNumClusters(options.getInt(HashOptions.CLUSTER_MANUAL_CLUSTER_NUMBER_KEY));
                hc1.buildClusterer(instances); // build the clusterer
                assignClusters(hc1);
            }

            if (cm.equals(ClusteringMethod.EM)) {
                EM clusterer = new EM(); // new instance of clusterer
                clusterer.setOptions(optionArray); // set the options
                clusterer.buildClusterer(instances); // build the clusterer
                assignClusters(clusterer);
            }

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error in cluster assignments", e);
            return false;
        }
        return true;
    }

    /**
     * Given a trained clusterer, put each nucleus within the collection into a
     * cluster
     * 
     * @param clusterer the clusterer to use
     * @throws ProfileException 
     * @throws MissingProfileException 
     */
    private void assignClusters(Clusterer clusterer) throws ProfileException, MissingProfileException {

    	int numberOfClusters = 0;
		try {
			numberOfClusters = clusterer.numberOfClusters();
		} catch (Exception e1) {
			LOGGER.warning("Unable to cluster cells: "+e1.getMessage());
			LOGGER.log(Loggable.STACK, "Unable to cluster cells", e1);
			return;
		}
		
		LOGGER.fine("Clustering found "+numberOfClusters+" clusters");

		// Create new empty collections to hold the cells for each cluster
		for (int i = 0; i <numberOfClusters ; i++) {
			ICellCollection c = new VirtualDataset(dataset, "Cluster_" + i);
			clusterMap.put(i, c);
		}

    	for(Entry<Instance,UUID> entry : cellToInstanceMap.entrySet()) {

    		try {

    			UUID cellID = entry.getValue();
    			int clusterNumber = clusterer.clusterInstance(entry.getKey());    			
    			ICellCollection cluster = clusterMap.get(clusterNumber);

    			// should never be null
    			if (collection.getCell(cellID) != null) {
    				cluster.addCell(collection.getCell(cellID));
    			} else {
    				LOGGER.warning("Error: cell with ID " + cellID + " is not found");
    			}
    			fireProgressEvent();
    		} catch (Exception e) {
    			LOGGER.log(Loggable.STACK, "Error assigning instance to cluster", e);
    		}

    	}
    	
    	// complete the new collections by profiling 
    	for(ICellCollection c : clusterMap.values()) {
    		c.createProfileCollection();
    		dataset.getCollection().getProfileManager().copySegmentsAndLandmarksTo(c);
    	}

    }

}
