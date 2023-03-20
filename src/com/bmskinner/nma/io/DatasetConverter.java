package com.bmskinner.nma.io;

import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.options.HashOptions;

/**
 * Convert old version datasets to match the current version
 * 
 * @author bs19022
 *
 */
public class DatasetConverter {

	/**
	 * Convert the dataset provided to the current program version
	 * 
	 * @param dataset
	 */
	public static void convert(IAnalysisDataset dataset) {

		if (dataset.getVersionLastSaved().equals(new Version(2, 0, 0))) {
			convert200To210(dataset);
		}

	}

	/**
	 * Some alterations may need to be made to datasets on import
	 * 
	 */
	private static void convert200To210(IAnalysisDataset dataset) {

		// Need to update the dimensionality reduction options for display
		// Before 2.1.0 all clustering with dim red used the values for clustering
		// After this, the values can be computed for display only
		for (IAnalysisDataset d : dataset.getAllChildDatasets()) {
			for (IClusterGroup g : d.getClusterGroups()) {
				HashOptions op = g.getOptions().get();
				setClusterOptions200To210(op);
			}
		}

		for (IClusterGroup g : dataset.getClusterGroups()) {
			HashOptions op = g.getOptions().get();
			setClusterOptions200To210(op);
		}

	}

	private static void setClusterOptions200To210(HashOptions op) {
		if (op.getBoolean(HashOptions.CLUSTER_USE_PCA_KEY) ||
				op.getBoolean(HashOptions.CLUSTER_USE_TSNE_KEY)) {

			// If the dataset was saved in 2.1.0 with clusters then reopened in 2.0.0 for
			// clustering we could have previous cluster options present. Don't overwrite
			// them
			if (op.hasBoolean(HashOptions.CLUSTER_USE_DIM_RED_KEY)) {
				op.setBoolean(HashOptions.CLUSTER_USE_DIM_RED_KEY,
						op.getBoolean(HashOptions.CLUSTER_USE_DIM_RED_KEY));
			} else {
				op.setBoolean(HashOptions.CLUSTER_USE_DIM_RED_KEY, true);
			}
		}
	}

}
