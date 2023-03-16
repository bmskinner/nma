package com.bmskinner.nma.analysis.classification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.logging.Loggable;

import tagbio.umap.Umap;

public class UMAPMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(UMAPMethod.class.getName());

	private static final int OUTPUT_DIMENSIONS = 2;

	public static final String PROFILE_TYPE_KEY = "Profile type";
	public static final String N_NEIGHBOUR_KEY = "Nearest neighbours";
	public static final String INITIAL_DIMS_KEY = "Initial dimensions";
	public static final String MIN_DISTANCE_KEY = "Min distance";

	private final HashOptions options;

	/**
	 * Create the UMAP method with a dataset to analyse, and appropriate options
	 * 
	 * @param dataset     the dataset which tSNE should be run on
	 * @param tSneOptions the UMAP options
	 */
	public UMAPMethod(@NonNull final IAnalysisDataset dataset, @NonNull HashOptions options) {
		super(dataset);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		int neighbours = options.getInt(N_NEIGHBOUR_KEY);
		float minDist = options.getFloat(MIN_DISTANCE_KEY);

		LOGGER.fine(
				() -> "Running UMAP using Euclidian metric with %s nearest neighbours and %s min distance"
						.formatted(neighbours, minDist));

		// Calculate from options
		int initialDims = calculateNumberOfDimensions();

		// Create the matrix for profile values with consistent cell order
		List<Nucleus> nuclei = new ArrayList<>(dataset.getCollection().getNuclei());

		double[][] profileMatrix = makeProfileMatrix(nuclei, initialDims);

		final Umap umap = new Umap();
		umap.setNumberComponents(OUTPUT_DIMENSIONS); // number of dimensions in result
		umap.setNumberNearestNeighbours(neighbours);
		umap.setMinDist(minDist);
		umap.setThreads(Math.max(1, Runtime.getRuntime().availableProcessors() - 2)); // run as fast
																						// as we can

		final double[][] umapResult = umap.fitTransform(profileMatrix);

		// store this in the cell collection, attached to each cell. This is a temporary
		// store -
		// if used for clustering, it should be attached to the cluster id
		for (int i = 0; i < nuclei.size(); i++) {
			Nucleus n = nuclei.get(i);
			n.setMeasurement(Measurement.UMAP_1, umapResult[i][0]);
			n.setMeasurement(Measurement.UMAP_2, umapResult[i][1]);
		}

		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if (analysisOptions.isPresent())
			analysisOptions.get().setSecondaryOptions(IAnalysisOptions.UMAP, options);

		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * Create the matrix of values for dimensional reduction
	 * 
	 * @param nuclei
	 * @return
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	private double[][] makeProfileMatrix(List<Nucleus> nuclei, int initialDims)
			throws ProfileException, MissingComponentException {
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

			for (IProfileSegment s : dataset.getCollection().getProfileCollection()
					.getSegments(OrientationMark.REFERENCE)) {
				if (!options.getBoolean(s.getID().toString()))
					continue;

				IProfileSegment seg = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE)
						.getSegment(s.getID());
				double proportionPerimeter = (double) seg.length()
						/ (double) seg.getProfileLength();
				matrix[i][j++] = n.getMeasurement(Measurement.PERIMETER) * proportionPerimeter;
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

		try {
			for (IProfileSegment s : dataset.getCollection().getProfileCollection()
					.getSegments(OrientationMark.REFERENCE))
				if (options.getBoolean(s.getID().toString()))
					dimensions++;

		} catch (ProfileException | MissingLandmarkException e) {
			LOGGER.log(Loggable.STACK, "Unable to get segments", e);
		}

		return dimensions;

	}

}
