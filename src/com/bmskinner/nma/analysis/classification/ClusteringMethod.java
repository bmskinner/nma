package com.bmskinner.nma.analysis.classification;

import java.util.logging.Logger;

import com.bmskinner.nma.components.options.HashOptions;

/**
 * The available types of clustering for the Weka clusterer
 */
public enum ClusteringMethod {
	EM("Expectation maximisation"),
	HIERARCHICAL("Hierarchical"),
	IMPORTED("Imported"),
	MANUAL("Manual");

	private static final Logger LOGGER = Logger.getLogger(ClusteringMethod.class.getName());

	private final String name;

	ClusteringMethod(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	public static ClusteringMethod from(String s) {
		for (final ClusteringMethod c : ClusteringMethod.values()) {
			if (c.name.toUpperCase().equals(s.toUpperCase()))
				return c;
			// Handle instances of "EM" instead of "Expectation maximisation" in older
			// versions
			if (c.name().toUpperCase().equals(s.toUpperCase()))
				return c;
		}
		throw new IllegalArgumentException("There is no clustering method named %s".formatted(s));

	}

	/**
	 * If the given options contains a clustering method key, get the value
	 * 
	 * @param o
	 * @return
	 */
	public static ClusteringMethod from(HashOptions o) {
		return ClusteringMethod.from(o.getString(HashOptions.CLUSTER_METHOD_KEY));
	}
}