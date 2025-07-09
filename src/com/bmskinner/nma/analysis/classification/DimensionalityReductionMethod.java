package com.bmskinner.nma.analysis.classification;

import com.bmskinner.nma.components.options.HashOptions;

/**
 * The types of dimensionality reduction that can be used
 */
public enum DimensionalityReductionMethod {

	NONE("None"),
	PCA("PCA"),
	TSNE("TSNE"),
	UMAP("UMAP");

	private final String name;

	DimensionalityReductionMethod(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	/**
	 * Given clustering options, determine which dimensionality reduction method was
	 * used, if any
	 * 
	 * @param clusterGroupOptions the options to search
	 * @return
	 */
	public static DimensionalityReductionMethod fromClusterGroupOptions(HashOptions clusterGroupOptions) {
		if (clusterGroupOptions.hasBoolean(HashOptions.CLUSTER_USE_PCA_KEY)
				&& clusterGroupOptions.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY))
			return PCA;
		if (clusterGroupOptions.hasBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)
				&& clusterGroupOptions.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY))
			return TSNE;

		if (clusterGroupOptions.hasBoolean(HashOptions.CLUSTER_USE_UMAP_KEY)
				&& clusterGroupOptions.getBoolean(HashOptions.CLUSTER_USE_UMAP_KEY))
			return UMAP;

		return NONE;

	}
}
