package com.bmskinner.nuclear_morphology.analysis.classification;

import com.bmskinner.nuclear_morphology.components.options.HashOptions;

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
    
    /**
     * If the given options contains a clustering method
     * key, get the value
     * @param o
     * @return
     */
    public static HierarchicalClusterMethod from(HashOptions o) {
    	return HierarchicalClusterMethod.valueOf(o.getString(HashOptions.CLUSTER_HIERARCHICAL_METHOD_KEY));
    }

}