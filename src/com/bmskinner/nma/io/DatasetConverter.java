package com.bmskinner.nma.io;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
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

	private static final Logger LOGGER = Logger.getLogger(DatasetConverter.class.getName());

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

		// Update missing clustering options
		setClusterOptions200To210(dataset);

		// Update consensus nuclei
		recalculateConsensusNuclei(dataset);

	}

	/**
	 * Need to update the dimensionality reduction options for display. Before 2.1.0
	 * all clustering with dimensional reduction used the values for clustering.
	 * After this, the values can be computed for display only
	 * 
	 * @param dataset
	 */
	private static void setClusterOptions200To210(IAnalysisDataset dataset) {

		for (IClusterGroup g : dataset.getClusterGroups()) {
			HashOptions op = g.getOptions().get();

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

		for (IAnalysisDataset d : dataset.getChildDatasets())
			setClusterOptions200To210(d);
	}

	/**
	 * Consensus nucleus coordinates are scaled differently in 2.1.0 to use micron
	 * scaling. Older datasets need consensus regenerating for consistency.
	 */
	private static void recalculateConsensusNuclei(IAnalysisDataset dataset) {
		if (dataset.getCollection().hasConsensus()) {
			try {
				new ConsensusAveragingMethod(dataset).call();
			} catch (Exception e) {
				LOGGER.log(Level.SEVERE, "Error remaking consensus", e);
			}
		}

		for (IAnalysisDataset d : dataset.getChildDatasets())
			recalculateConsensusNuclei(d);
	}

}
