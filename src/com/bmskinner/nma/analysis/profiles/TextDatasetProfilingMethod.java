package com.bmskinner.nma.analysis.profiles;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.ComponentMeasurer;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.DefaultLandmark;
import com.bmskinner.nma.components.profiles.DefaultProfileAggregate;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.utility.ArrayUtils;
import com.bmskinner.nma.utility.StreamUtils;

/**
 * This class allows dataset profiling without landmark detection. It is
 * designed for nuclei and landmarks identified outside NMA, whose coordinates
 * have been provided via a text file. Contrast with
 * {@link DefaultDatasetProfilingMethod}, the original combined profiling and
 * landmark detection.
 * 
 * @since 2.3.0
 */
public class TextDatasetProfilingMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(TextDatasetProfilingMethod.class.getName());

	private DatasetValidator dv = new DatasetValidator();

	/**
	 * Create a profiler for the given dataset
	 * 
	 * @param dataset
	 */
	public TextDatasetProfilingMethod(@NonNull IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();

		if (!dv.validate(dataset))
			throw new AnalysisMethodException(
					"Unable to validate dataset after profiling: " + dv.getSummary() + "\n" + dv.getErrors());
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws Exception {
		if (!dataset.hasAnalysisOptions()) {
			LOGGER.warning("Unable to run profiling method, no analysis options in dataset " + dataset.getName());
			return;
		}

		this.fireUpdateProgressTotalLength(dataset.size() * 6); // TODO when size known

		// Nuclei have been detected. We are only going to build profiles from them.
		ICellCollection collection = dataset.getCollection();

		HashOptions nucleusOptions = dataset.getAnalysisOptions().get().getNucleusDetectionOptions().get();

		// We need to locate the RP in each nucleus. This is defined in the landmark
		// file.
		File landmarkFile = nucleusOptions.getFile(HashOptions.LANDMARK_LOCATION_FILE_KEY);

		// Which landmark is the RP?
		Landmark rp = new DefaultLandmark(nucleusOptions.getString(HashOptions.LANDMARK_RP_NAME));

		if (rp.getName() == null) {
			throw new MissingLandmarkException("RP landmark was not named");
		}

		// Assign landmarks in the file
		readLandmarks(landmarkFile);

//		 Remove any nuclei that do not have an RP
		List<ICell> cellsWithoutRP = dataset.getCollection().getCells().stream()
				.filter(c -> !c.getPrimaryNucleus().hasLandmark(rp)).collect(Collectors.toList());

		for (ICell c : cellsWithoutRP) {
			dataset.getCollection().remove(c);
		}

		findLandmarksInMedian(rp);

		// Now all cells have RP placed. Make collection profiles
		dataset.getCollection().getProfileCollection().calculateProfiles();

		// Clear all calculated measured values and force recalculation
		// in each nucleus since some measurements use the landmarks
		// for orientation
		for (Nucleus n : dataset.getCollection().getNuclei()) {
			for (Measurement m : dataset.getAnalysisOptions().orElseThrow(MissingOptionException::new)
					.getRuleSetCollection().getMeasurableValues()) {
				n.setMeasurement(m, ComponentMeasurer.calculate(m, n));
			}
			fireProgressEvent();
		}

		fireIndeterminateState();
		
		// Clear all calculated median values in the collection and
		// recalculate. This ensures any values dependent on landmarks
		// (e.g. bounding dimensions) are correct
		for (Measurement m : dataset.getAnalysisOptions().orElseThrow(MissingOptionException::new)
				.getRuleSetCollection().getMeasurableValues()) {
			collection.clear(m, CellularComponent.NUCLEUS);
			// Force recalculation
			collection.getMedian(m, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
		}
	}

	/**
	 * Assign all landmarks from the given file to the nuclei in the dataset.
	 * 
	 * @param landmarkFile the tsv file with landmark locations. Fields are given in
	 *                     {@link DatasetLandmarkImporttMethod}
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws AnalysisMethodException
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 * @throws IndexOutOfBoundsException
	 */
	private void readLandmarks(File landmarkFile) throws FileNotFoundException, IOException, AnalysisMethodException,
			IndexOutOfBoundsException, MissingDataException, SegmentUpdateException {

		try (FileInputStream fstream = new FileInputStream(landmarkFile);
				BufferedReader br = new BufferedReader(new InputStreamReader(fstream, StandardCharsets.ISO_8859_1));) {

			String strLine;
			boolean isHeader = true;

			while ((strLine = br.readLine()) != null) {

				String[] arr = strLine.split(Io.TAB);
				if (isHeader) {
					if (arr.length < 8)
						throw new AnalysisMethodException(
								"Not enough tab-separated input columns in landmark file. Expected 8, found %s"
										.formatted(arr.length));
					isHeader = false;
					continue;
				}

				// Image folder and object id
				File imageFile = new File(arr[0]);

				// Bounding box
				int xmin = Double.valueOf(arr[1]).intValue();
				int xmax = Double.valueOf(arr[2]).intValue();
				int ymin = Double.valueOf(arr[3]).intValue();
				int ymax = Double.valueOf(arr[4]).intValue();
				Rectangle r = new Rectangle(xmin, ymin, xmax - xmin, ymax - ymin);

				Optional<Nucleus> oN = dataset.getCollection().getNuclei(imageFile).stream()
						.filter(n -> r.contains(n.getOriginalCentreOfMass().toPoint2D())).findFirst();

				// If there is a nucleus at the given position, update the landmark
				if (oN.isPresent()) {

					// Landmark name
					String lmName = arr[5];

					float lmx = Float.valueOf(arr[6]);
					float lmy = Float.valueOf(arr[7]);

					Landmark l = new DefaultLandmark(lmName);
					IPoint lm = new FloatPoint(lmx, lmy);

					// Find the closest point to the new landmark in the border
					IPoint newLm = oN.get().getBorderList().stream()
							.min(Comparator.comparing(point -> Math.abs(point.getLengthTo(lm)))).get();

					// Get the index of this point
					int newIdx = oN.get().getBorderIndex(newLm);
					oN.get().setLandmark(l, newIdx);
					LOGGER.finer("Set '%s' in '%s' to %s".formatted(lmName, imageFile, newIdx));
				} else {
					LOGGER.fine(
							"No nucleus found in '%s' at x %s-%s y %s-%s".formatted(imageFile, xmin, xmax, ymin, ymax));
				}

				fireProgressEvent();
			}
		} catch (NumberFormatException e) {
			LOGGER.severe(
					"When reading x and y coordinates, unable to parse a string as a number. %s. Check input file columns are in the correct order."
							.formatted(e.getMessage()));
			throw new AnalysisMethodException("Unable to read object coordinates from file", e);
		}

	}

	/**
	 * Find the location of landmarks in the median profile. This uses the best fit
	 * offsets of nucleus landmark profiles to the median RP
	 * 
	 * @param rp
	 * @throws NoDetectedIndexException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private void findLandmarksInMedian(Landmark rp)
			throws NoDetectedIndexException, MissingDataException, SegmentUpdateException {

		Integer[] borders = dataset.getCollection().getNuclei().stream().map(Nucleus::getBorderLength)
				.toArray(Integer[]::new);

		double medianProfileLength = Stats.median(ArrayUtils.unbox(borders));

		// Find the unique landmarks in the nuclei
		Set<Landmark> lms = dataset.getCollection().getNuclei().stream().flatMap(n -> n.getLandmarks().stream())
				.distinct().collect(Collectors.toSet());

		// Create a profile aggregate for each landmark starting with RP
		DefaultProfileAggregate dpa = new DefaultProfileAggregate((int) medianProfileLength, borders.length);

		List<ISegmentedProfile> profiles = dataset.getCollection().getNuclei().stream()
				.map(n -> StreamUtils.uncheckCall(() -> n.getProfile(ProfileType.ANGLE, rp)))
				.collect(Collectors.toList());

		for (ISegmentedProfile p : profiles) {
			dpa.addValues(p);
		}

		IProfile rpMedian = dpa.getMedian();

		// Locate each landmark in the median
		for (Landmark lm : lms) {
			// Don't identify the RP again
			if (rp.equals(lm))
				continue;

			DefaultProfileAggregate lmdpa = new DefaultProfileAggregate((int) medianProfileLength, borders.length);

			List<ISegmentedProfile> lmProfiles = dataset.getCollection().getNuclei().stream()
					.map(n -> StreamUtils.uncheckCall(() -> n.getProfile(ProfileType.ANGLE, lm)))
					.collect(Collectors.toList());

			for (ISegmentedProfile p : lmProfiles) {
				lmdpa.addValues(p);
			}

			IProfile lmMedian = lmdpa.getMedian();

			int index = rpMedian.findBestFitOffset(lmMedian);

			// Add the index to the median profiles
			dataset.getCollection().getProfileCollection().setLandmark(lm,
					CellularComponent.wrapIndex(index, dataset.getCollection().getMedianArrayLength()));

		}

	}
}
