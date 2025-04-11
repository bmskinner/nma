package com.bmskinner.nma.io;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.DefaultLandmark;
import com.bmskinner.nma.components.profiles.Landmark;

/**
 * Method to read a landmark file and update all landmarks in a dataset to
 * match. This is a tab-separated file in long format with the following
 * columns:
 * 
 * <pre>
 * 0 - Image file path
 * 1 - Nucleus bounding box x minimum
 * 2 - Nucleus bounding box x maximum
 * 3 - Nucleus bounding box y minimum
 * 4 - Nucleus bounding box y maximum
 * 5 - Landmark name
 * 6 - Landmark x float coordinate
 * 7 - Landmark y float coordinate
 * </pre>
 * 
 * The names of the columns are not used - column contents is inferred from
 * column index only. A column header is expected (i.e. the first row of the
 * file is skipped when reading. The nucleus bounding box is used to identify
 * the nucleus within the image.
 * 
 * @author Ben Skinner
 *
 */
public class DatasetLandmarkImportMethod extends MultipleDatasetAnalysisMethod implements Io {

	private static final Logger LOGGER = Logger.getLogger(DatasetLandmarkImportMethod.class.getName());

	private final HashOptions options;

	private final File inputFile;

	/**
	 * Create with a dataset of cells to update
	 * 
	 * @param dataset
	 * @param options
	 */
	public DatasetLandmarkImportMethod(@NonNull IAnalysisDataset dataset, @NonNull File f,
			@NonNull HashOptions options) {
		super(dataset);
		this.options = options;
		this.inputFile = f;
	}

	/**
	 * Create with datasets of cells to update
	 * 
	 * @param datasets
	 * @param options
	 */
	public DatasetLandmarkImportMethod(@NonNull List<IAnalysisDataset> datasets, @NonNull File f,
			@NonNull HashOptions options) {
		super(datasets);
		this.options = options;
		this.inputFile = f;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		for (IAnalysisDataset d : datasets)
			importLandmarks(d);
		return new DefaultAnalysisResult(datasets);
	}

	private void importLandmarks(IAnalysisDataset d) throws Exception {

		try (FileInputStream fstream = new FileInputStream(inputFile);
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

				Optional<Nucleus> oN = d.getCollection().getNuclei(imageFile).stream()
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

					// Get the index of this point and update the landmark to this index
					int oldIdx = oN.get().getBorderIndex(l);
					int newIdx = oN.get().getBorderIndex(newLm);
					oN.get().setLandmark(l, newIdx);
					LOGGER.fine("Updated '%s' in '%s' from %s to %s".formatted(lmName, imageFile, oldIdx, newIdx));
				} else {
					LOGGER.fine(
							"No nucleus found in '%s' at x %s-%s y %s-%s".formatted(imageFile, xmin, xmax, ymin, ymax));
				}

				fireProgressEvent();
			}
		} catch(NumberFormatException e) {
			LOGGER.severe(
					"When reading x and y coordinates, unable to parse a string as a number. %s. Check input file columns are in the correct order."
							.formatted(e.getMessage()));
			throw new AnalysisMethodException("Unable to read object coordinates from file", e);
		}

		// TODO update profile collections, consensus nuclei
		d.getCollection().getProfileCollection().calculateProfiles();
		fireProgressEvent();
	}

}
