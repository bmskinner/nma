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
import java.util.logging.Logger;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.ClusterAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ClusterGroup;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.Statistical;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions.ClusteringMethod;
import com.bmskinner.nuclear_morphology.components.stats.GenericStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;
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
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

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
    public NucleusClusteringMethod(@NonNull IAnalysisDataset dataset, @NonNull IClusteringOptions options) {
        super(dataset, options);
    }

    @Override
    public IAnalysisResult call() throws Exception {

        run();

        // Save the clusters to the dataset
        List<IAnalysisDataset> list = new ArrayList<>();

        int clusterNumber = dataset.getMaxClusterGroupNumber() + 1;

        // Create a group to store the clustered cells
        IClusterGroup group = new ClusterGroup(IClusterGroup.CLUSTER_GROUP_PREFIX + "_" + clusterNumber, options,
                newickTree);

        for (int cluster = 0; cluster < clusterMap.size(); cluster++) {

            ICellCollection c = clusterMap.get(cluster);

            if (c.hasCells()) {
                LOGGER.finest( "Cluster " + cluster + ": " + c.getName());

                try {
                    dataset.getCollection().getProfileManager().copyCollectionOffsets(c);
                } catch (ProfileException e) {
                    LOGGER.warning("Error copying collection offsets");
                    LOGGER.log(Loggable.STACK, "Error in offsetting", e);
                }

                group.addDataset(c);
                c.setName(group.getName() + "_" + c.getName());

                dataset.addChildCollection(c);

                // attach the clusters to their parent collection
                LOGGER.info("Cluster " + cluster + ": " + c.size() + " nuclei");
                IAnalysisDataset clusterDataset = dataset.getChildDataset(c.getID());
                clusterDataset.setRoot(false);

                // set shared counts
                c.setSharedCount(dataset.getCollection(), c.size());
                dataset.getCollection().setSharedCount(c, c.size());

                list.add(clusterDataset);
            }
        }
        LOGGER.fine("Profiles copied to all clusters");
        
        // Move tSNE values to their cluster group if needed
        if(options.getBoolean(IClusteringOptions.USE_TSNE_KEY)) {
        	for(ICell c : dataset.getCollection()) {
        		for(Nucleus n : c.getNuclei()) {
        			n.setStatistic(new GenericStatistic("TSNE_1_"+group.getId(), StatisticDimension.DIMENSIONLESS), n.getStatistic(PlottableStatistic.TSNE_1));
        			n.setStatistic(new GenericStatistic("TSNE_2_"+group.getId(), StatisticDimension.DIMENSIONLESS), n.getStatistic(PlottableStatistic.TSNE_2));
        			n.setStatistic(PlottableStatistic.TSNE_1, Statistical.STAT_NOT_CALCULATED);
        			n.setStatistic(PlottableStatistic.TSNE_2, Statistical.STAT_NOT_CALCULATED);
        		}
        	}
        }
        
     // Move PCA values to their cluster group if needed        
        if(options.getBoolean(IClusteringOptions.USE_PCA_KEY)) {
        	for(ICell c : dataset.getCollection()) {
        		for(Nucleus n : c.getNuclei()) {
        			int nPcs = (int) n.getStatistic(PlottableStatistic.PCA_N);
        			
        			n.setStatistic(PlottableStatistic.makePrincipalComponentNumber(group.getId()), nPcs);
        			n.clearStatistic(PlottableStatistic.PCA_N);
        			for(int i=1; i<=nPcs; i++) {
        				n.setStatistic(PlottableStatistic.makePrincipalComponent(i, group.getId()), n.getStatistic(PlottableStatistic.makePrincipalComponent(i)));
        				n.clearStatistic(PlottableStatistic.makePrincipalComponent(i));
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
            String[] optionArray = this.options.getOptions();

            for (String s : optionArray) {
                LOGGER.finest( "Clusterer options: " + s);
            }

            if (options.getType().equals(ClusteringMethod.HIERARCHICAL)) {
                HierarchicalClusterer clusterer = new HierarchicalClusterer();

                clusterer.setOptions(optionArray); // set the options
                clusterer.setDistanceFunction(new EuclideanDistance());
                clusterer.setDistanceIsBranchLength(true);
                clusterer.setNumClusters(1);

                LOGGER.finest( "Building clusterer for tree");
                // firePropertyChange("Cooldown", getProgress(),
                // Constants.Progress.FINISHED.code());
                clusterer.buildClusterer(instances); // build the clusterer with
                                                     // one cluster for the tree
                clusterer.setPrintNewick(true);

                this.newickTree = clusterer.graph();

                clusterer.setNumClusters(options.getClusterNumber());
                clusterer.buildClusterer(instances); // build the clusterer
                assignClusters(clusterer);

            }

            if (options.getType().equals(ClusteringMethod.EM)) {
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
     */
    private void assignClusters(Clusterer clusterer) {

    	int numberOfClusters = 0;
		try {
			numberOfClusters = clusterer.numberOfClusters();
		} catch (Exception e1) {
			LOGGER.warning("Unable to cluster cells: "+e1.getMessage());
			LOGGER.log(Loggable.STACK, "Unable to cluster cells", e1);
			return;
		}

    	for (int i = 0; i <numberOfClusters ; i++) {
    		ICellCollection clusterCollection = new VirtualCellCollection(dataset, "Cluster_" + i);

    		clusterCollection.setName("Cluster_" + i);
    		clusterMap.put(i, clusterCollection);
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

    }

}
