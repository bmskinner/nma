package com.bmskinner.nuclear_morphology.components.options;

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
    
    /**
     * If the given options contains a clustering method
     * key, get the value
     * @param o
     * @return
     */
    public static ClusteringMethod from(HashOptions o) {
    	return ClusteringMethod.valueOf(o.getString(HashOptions.CLUSTER_METHOD_KEY));
    }
}