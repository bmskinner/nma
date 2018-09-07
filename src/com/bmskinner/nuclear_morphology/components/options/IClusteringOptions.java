/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.options;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * This interface describes the options available for clustering cells within
 * datasets.
 * 
 * @author bms41
 *
 */
public interface IClusteringOptions extends Serializable, HashOptions {
	
	static final String USE_SIMILARITY_MATRIX_KEY = "USE_SIMILARITY_MATRIX";
	static final String MANUAL_CLUSTER_NUMBER_KEY = "MANUAL_CLUSTER_NUMBER";
	static final String CLUSTER_METHOD_KEY        = "CLUSTER_METHOD";
	static final String HIERARCHICAL_METHOD_KEY   = "HIERARCHICAL_METHOD";
	static final String EM_ITERATIONS_KEY         = "EM_ITERATIONS";
	static final String MODALITY_REGIONS_KEY      = "MODALITY_REGIONS";
	static final String USE_MODALITY_KEY          = "USE_MODALITY";
	static final String INCLUDE_PROFILE_KEY       = "INCLUDE_PROFILE";
	static final String INCLUDE_MESH_KEY          = "INCLUDE_MESH";
	static final String PROFILE_TYPE_KEY          = "PROFILE_TYPE";

    static final int                       DEFAULT_MANUAL_CLUSTER_NUMBER = 2;
    static final ClusteringMethod          DEFAULT_CLUSTER_METHOD        = ClusteringMethod.HIERARCHICAL;
    static final HierarchicalClusterMethod DEFAULT_HIERARCHICAL_METHOD   = HierarchicalClusterMethod.WARD;
    static final ProfileType               DEFAULT_PROFILE_TYPE          = ProfileType.ANGLE;
    static final int                       DEFAULT_EM_ITERATIONS         = 100;
    static final int                       DEFAULT_MODALITY_REGIONS      = 2;
    static final boolean                   DEFAULT_USE_MODALITY          = true;
    static final boolean                   DEFAULT_USE_SIMILARITY_MATRIX = false;
    static final boolean                   DEFAULT_INCLUDE_PROFILE       = true;
    static final boolean                   DEFAULT_INCLUDE_MESH          = false;

    /**
     * The available types of hierarchical clustering for the Weka clusterer
     */
    public enum HierarchicalClusterMethod {
        WARD("Ward", "WARD"), 
        SINGLE("Single", "SINGLE"), 
        COMPLETE("Complete", "COMPLETE"), 
        AVERAGE("Average", "AVERAGE"), 
        MEAN("Mean", "MEAN"), 
        CENTROID("Centroid", "CENTROID"), 
        ADJCOMPLETE("Adjusted complete", "ADJCOMPLETE"), 
        NEIGHBOR_JOINING("Neighbour joining", "NEIGHBOR_JOINING");

        private final String name;
        private final String code;

        HierarchicalClusterMethod(String name, String code) {
            this.name = name;
            this.code = code;
        }

        @Override
		public String toString() {
            return this.name;
        }

        public String code() {
            return this.code;
        }

    }

    /**
     * The available types of clustering for the Weka clusterer
     */
    public enum ClusteringMethod {
        EM("Expectation maximisation", 0),
        HIERARCHICAL("Hierarchical", 1),
        MANUAL("Manual", 2);

        private final String name;
        private final int    code;

        ClusteringMethod(String name, int code) {
            this.name = name;
            this.code = code;
        }

        @Override
		public String toString() {
            return this.name;
        }

        public int code() {
            return this.code;
        }
    }
    
    IClusteringOptions duplicate();
    
    /**
     * Check if the given segment is to be included in the clustering
     * 
     * @param stat
     * @return
     */
    boolean isIncludeSegment(UUID i);

//    boolean useSegments();

    /**
     * Get all the segments that are saved in this options object
     * 
     * @return
     */
    Set<UUID> getSegments();

    /**
     * Check if the given statistic is to be included in the clustering
     * 
     * @param stat
     * @return
     */
    boolean isIncludeStatistic(PlottableStatistic stat);

    /**
     * Get all the statistics that are saved in this options object
     * 
     * @return
     */
//    Set<PlottableStatistic> getSavedStatistics();

    boolean isIncludeProfile();

    boolean isUseSimilarityMatrix();

    ClusteringMethod getType();

    /**
     * Get the desired number of hierarchical clusters
     * 
     * @return
     */
    int getClusterNumber();

    /**
     * Get the chosen hierarchical method of clustering
     * 
     * @return
     */
    HierarchicalClusterMethod getHierarchicalMethod();

    int getIterations();

    ProfileType getProfileType();

    boolean isIncludeMesh();

    /**
     * Get a string array of the options set here suitable for the Weka
     * HierarchicalClusterer
     * 
     * @return
     */
    String[] getOptions();
    
    void setClusterNumber(int defaultManualClusterNumber);

    void setHierarchicalMethod(HierarchicalClusterMethod defaultHierarchicalMethod);

    void setIterations(int defaultEmIterations);

    void setUseSimilarityMatrix(boolean defaultUseSimilarityMatrix);

    void setIncludeProfile(boolean defaultIncludeProfile);

    void setProfileType(ProfileType defaultProfileType);

    void setIncludeMesh(boolean defaultIncludeMesh);

    void setIncludeStatistic(PlottableStatistic stat, boolean selected);

    void setIncludeSegment(UUID id, boolean selected);

    void setType(ClusteringMethod hierarchical);

}
