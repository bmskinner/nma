package com.bmskinner.nma.analysis.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.jujutsu.tsne.TSneConfiguration;
import com.jujutsu.tsne.barneshut.BHTSne;
import com.jujutsu.tsne.barneshut.BarnesHutTSne;
import com.jujutsu.utils.TSneUtils;

/**
 * This method takes a dataset and generates a t-SNE from cell stats. The
 * downside of this over exporting the data into R is that the Barnes-Hut t-SNE
 * implementation uses ThreadLocalRandom random number generation, so there is
 * no opportunity to set a seed. This means every run will give a different
 * result. Consequently, it will be better to use this for first pass analysis,
 * and decide what to export, rather than relying on this for full analysis. It
 * also does not easily allow for testing hyperparameters to choose the best
 * options.
 * 
 * @author bms41
 * @since 1.16.0
 *
 */
public class TsneMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(TsneMethod.class.getName());

	private static final int OUTPUT_DIMENSIONS = 2;

	public static final String PROFILE_TYPE_KEY = "Profile type";
	public static final String MAX_ITERATIONS_KEY = "Max iterations";
	public static final String PERPLEXITY_KEY = "Perplexity";
	public static final String INITIAL_DIMS_KEY = "Initial dimensions";

	private final HashOptions options;

	/**
	 * Create the t-SNE method with a dataset to analyse, and appropriate options
	 * 
	 * @param dataset     the dataset which tSNE should be run on
	 * @param tSneOptions the tSNE options
	 */
	public TsneMethod(@NonNull final IAnalysisDataset dataset, @NonNull HashOptions tSneOptions) {
		super(dataset);
		this.options = tSneOptions;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		int maxIterations = options.getInt(MAX_ITERATIONS_KEY);
		double perplexity = options.getDouble(PERPLEXITY_KEY);

		LOGGER.fine(() -> "Running tSNE with p %s and i %d".formatted(perplexity, maxIterations));

		// Calculate from options
		int initialDims = calculateNumberOfDimensions();

		// Create the matrix for profile values with consistent cell order
		List<Nucleus> nuclei = new ArrayList<>(dataset.getCollection().getNuclei());

		double[][] profileMatrix = makeProfileMatrix(nuclei, initialDims);

		TSneConfiguration config = TSneUtils.buildConfig(profileMatrix, OUTPUT_DIMENSIONS,
				initialDims, perplexity, maxIterations);
		BarnesHutTSne tsne = new BHTSne(); // ParallelBHTSne may not play well with the threading.
		// Note that using ParallelBHTSne does not play nice with the OpenJDK 12:
		// Potentially dangerous stack overflow in ReservedStackAccess annotated method
		// java.util.concurrent.locks.ReentrantLock$Sync.nonfairTryAcquire(I)Z
		double[][] tSneResult = tsne.tsne(config);

		// store this in the cell collection, attached to each cell. This is a temporary
		// store -
		// if used for clustering, it should be attached to the cluster id
		for (int i = 0; i < nuclei.size(); i++) {
			Nucleus n = nuclei.get(i);

			Measurement m1 = Measurement.makeTSNE(1,
					options.getUUID(HashOptions.CLUSTER_GROUP_ID_KEY));

			Measurement m2 = Measurement.makeTSNE(2,
					options.getUUID(HashOptions.CLUSTER_GROUP_ID_KEY));

			n.setMeasurement(m1, tSneResult[i][0]);
			n.setMeasurement(m2, tSneResult[i][1]);
		}

		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if (analysisOptions.isPresent()) {

			// We may run several clustering runs; ensure they are all stored appropriately
			// with the cluster id
			String optionsKey = IAnalysisOptions.TSNE + "_"
					+ options.getUUID(HashOptions.CLUSTER_GROUP_ID_KEY);
			analysisOptions.get().setSecondaryOptions(optionsKey, options);
		}

		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * Create the matrix of values for dimensional reduction
	 * 
	 * @param nuclei
	 * @return
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws MissingMeasurementException
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 */
	private double[][] makeProfileMatrix(List<Nucleus> nuclei, int initialDims)
			throws MissingDataException, SegmentUpdateException, ComponentCreationException {
		double[][] matrix = new double[nuclei.size()][initialDims];

		for (int i = 0; i < nuclei.size(); i++) {
			int j = 0;
			Nucleus n = nuclei.get(i);
			for (ProfileType t : ProfileType.displayValues()) {
				if (!options.getBoolean(t.toString()))
					continue;

				IProfile p = n.getProfile(t, OrientationMark.REFERENCE);
				for (int k = 0; k < 100; k++) {
					double idx = (k) / 100d;
					matrix[i][j++] = p.get(idx);
				}
			}

			for (Measurement stat : Measurement.getNucleusStats()) {
				if (!options.getBoolean(stat.toString()))
					continue;
				matrix[i][j++] = n.getMeasurement(stat);
			}
		}
		return matrix;
	}

	/**
	 * Count the number of dimensions to be reduced, based on the input options
	 * 
	 * @return
	 */
	private int calculateNumberOfDimensions() {
		int dimensions = 0;
		for (Measurement stat : Measurement.getNucleusStats())
			if (options.getBoolean(stat.toString()))
				dimensions++;

		for (ProfileType t : ProfileType.displayValues())
			if (options.getBoolean(t.toString()))
				dimensions += 100;

		return dimensions;

	}

}
