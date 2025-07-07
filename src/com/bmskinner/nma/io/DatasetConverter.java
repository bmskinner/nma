package com.bmskinner.nma.io;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Version;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.IClusterGroup;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.utility.ArrayUtils;

/**
 * Convert old version datasets to match the current version
 * 
 * @author Ben Skinner
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

		if (dataset.getVersionLastSaved().isOlderThan(Version.V_2_1_0)) {
			convert200To210(dataset);
		}

		if (dataset.getVersionLastSaved().isOlderThan(Version.V_2_3_0)) {
			convert220To230(dataset);
		}

	}

	/**
	 * Some alterations may need to be made to datasets on import
	 * 
	 */
	private static void convert220To230(IAnalysisDataset dataset) {

		// Update individual PCA etc. measurements to array measurements
		try {
			convertArrayMeasurements(dataset);
		} catch (MissingDataException | ComponentCreationException | SegmentUpdateException e) {
			LOGGER.log(Level.SEVERE, "Unable to convert dataset: %s".formatted(e.getMessage()), e);
		}

	}

	/**
	 * Array measurements were added in 2.3.0. We need to convert
	 * multi-measurements. These include: PCA, TSNE, UMAP and histograms.
	 * 
	 * @param dataset
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	private static void convertArrayMeasurements(IAnalysisDataset dataset)
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {

		// Get all the measurements in the dataset that should be array measurements
		final List<Measurement> updatedMeasurements = dataset.getCollection().stream()
				.flatMap(c -> c.getNuclei().stream())
				.flatMap(n -> n.getMeasurements().stream())
				.distinct()
				.toList();
		
		final List<Measurement> histogramMeasurements = updatedMeasurements.stream()
				.filter(m -> m.name().startsWith(Measurement.Names.PIXEL_HISTOGRAM))
				.sorted()
				.toList();

		final List<Measurement> pcaMeasurements = updatedMeasurements.stream()
				.filter(m -> m.name().startsWith("PC_"))
				.toList();
		
		final List<Measurement> tsneMeasurements = updatedMeasurements.stream()
				.filter(m -> m.name().startsWith(Measurement.Names.TSNE))
				.toList();
				
		final List<Measurement> umapMeasurements = updatedMeasurements.stream()
				.filter(m -> m.name().startsWith(Measurement.Names.UMAP))
				.toList();

		final List<IClusterGroup> clusterGroups = dataset.getClusterGroups();
				
		// Replace image histograms
		if (!histogramMeasurements.isEmpty()) {

			final List<Measurement> histogram = Measurement.getPixelHistogramMeasurements(
					dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get().getInt(HashOptions.CHANNEL));

			for (final Nucleus n : dataset.getCollection().getNuclei()) {
				final List<Double> values = new ArrayList<>();
				for (final Measurement m : histogram) {
					values.add(n.getMeasurement(m));
					n.clearMeasurement(m);
				}
				n.setMeasurement(Measurement.makeImageHistogram(CellularComponent.NUCLEUS), ArrayUtils.toArray(values));
			}
		}

		// Replace PCAs
		if (!pcaMeasurements.isEmpty()) {
			for (final IClusterGroup cg : clusterGroups) {
				final UUID id = cg.getId();

				// Get the PCA measurements for this cluster group
				final int nPCs = cg.getOptions().get().getInt(Measurement.Names.PCA_N);

				final List<Measurement> pcaOrdered = new ArrayList<>();
				for (int i = 0; i < nPCs; i++) {
					pcaOrdered.add(Measurement.makePrincipalComponent(i, id));
				}

				for (final Nucleus n : dataset.getCollection().getNuclei()) {
					final List<Double> values = new ArrayList<>();
					for (final Measurement m : pcaOrdered) {
						values.add(n.getMeasurement(m));
						n.clearMeasurement(m);
					}
					n.setMeasurement(Measurement.makePrincipalComponent(id), ArrayUtils.toArray(values));
				}
			}


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

		for (final IClusterGroup g : dataset.getClusterGroups()) {
			final HashOptions op = g.getOptions().get();

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

		for (final IAnalysisDataset d : dataset.getChildDatasets()) {
			setClusterOptions200To210(d);
		}
	}

	/**
	 * Consensus nucleus coordinates are scaled differently in 2.1.0 to use micron
	 * scaling. Older datasets need consensus regenerating for consistency.
	 */
	private static void recalculateConsensusNuclei(IAnalysisDataset dataset) {
		if (dataset.getCollection().hasConsensus()) {
			try {
				new ConsensusAveragingMethod(dataset).call();
			} catch (final Exception e) {
				LOGGER.log(Level.SEVERE, "Error remaking consensus", e);
			}
		}

		for (final IAnalysisDataset d : dataset.getChildDatasets()) {
			recalculateConsensusNuclei(d);
		}
	}

}
