package com.bmskinner.nma.analysis.classification;

import com.bmskinner.nma.components.options.HashOptions;

/**
 * The available types of clustering for the Weka clusterer
 */
public enum ClusteringMethod {
	EM("Expectation maximisation"),
	HIERARCHICAL("Hierarchical"),
	IMPORTED("Imported"),
	MANUAL("Manual");

	private final String name;

	ClusteringMethod(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * If the given options contains a clustering method key, get the value
	 * 
	 * @param o
	 * @return
	 */
	public static ClusteringMethod from(HashOptions o) {
		return ClusteringMethod.valueOf(o.getString(HashOptions.CLUSTER_METHOD_KEY).toUpperCase());
	}
}