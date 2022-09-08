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
 * Method to read a keypoint file and update all landmarks in a dataset to match
 * 
 * @author bs19022
 *
 */
public class DatasetKeypointImportMethod extends MultipleDatasetAnalysisMethod implements Io {

	private static final Logger LOGGER = Logger
			.getLogger(DatasetKeypointImportMethod.class.getName());

	private static final String IMPORT_FILE_KEY = "IMPORT_FILE";
	private static final String DELIMITER = "\t";

	private final HashOptions options;

	/**
	 * Create with a dataset of cells to update
	 * 
	 * @param dataset
	 * @param options
	 */
	public DatasetKeypointImportMethod(@NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		super(dataset);
		this.options = options;
	}

	/**
	 * Create with datasets of cells to update
	 * 
	 * @param datasets
	 * @param options
	 */
	public DatasetKeypointImportMethod(@NonNull List<IAnalysisDataset> datasets,
			@NonNull HashOptions options) {
		super(datasets);
		this.options = options;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		for (IAnalysisDataset d : datasets)
			importKeypoints(d);
		return new DefaultAnalysisResult(datasets);
	}

	private void importKeypoints(IAnalysisDataset d)
			throws Exception {

		File importFile = new File(options.getString(IMPORT_FILE_KEY));
		try (FileInputStream fstream = new FileInputStream(importFile);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(fstream, StandardCharsets.ISO_8859_1));) {

			String strLine;

			while ((strLine = br.readLine()) != null) {
				String[] arr = strLine.split(DELIMITER);

				// Image folder and file
				String imageFolder = arr[0];
				String imageFileName = arr[1];
				File imageFile = new File(imageFolder, imageFileName);

				// Bounding box
				int x1 = Integer.parseInt(arr[2]);
				int y1 = Integer.parseInt(arr[3]);
				int x2 = Integer.parseInt(arr[4]);
				int y2 = Integer.parseInt(arr[5]);
				Rectangle r = new Rectangle(x1, y1, x2 - x1, y2 - y1);

				Optional<Nucleus> oN = d.getCollection().getNuclei(imageFile).stream()
						.filter(n -> r.contains(n.getOriginalCentreOfMass().toPoint2D()))
						.findFirst();

				// If there is a nucleus at the given position, update the landmark
				if (oN.isPresent()) {

					// Landmark name
					String lmName = arr[6];
					int lmx = Integer.parseInt(arr[7]);
					int lmy = Integer.parseInt(arr[8]);

					Landmark l = new DefaultLandmark(lmName);
					IPoint lm = new FloatPoint(lmx, lmy);

					// Find the closest point to the new landmark in the border
					IPoint newLm = oN.get().getBorderList().stream()
							.min(Comparator
									.comparing(point -> Math.abs(point.getLengthTo(lm))))
							.get();

					// Get the index of this point and update the landmark to this index
					int newIdx = oN.get().getBorderIndex(newLm);
					oN.get().setLandmark(l, newIdx);
				}

				fireProgressEvent();
			}
		}

		// TODO update profile collections, consensus nuclei
		fireProgressEvent();
	}

}
