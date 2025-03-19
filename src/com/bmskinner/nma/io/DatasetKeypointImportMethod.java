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
 * Method to read a keypoint file and update all landmarks in a dataset to match.
 * This file should take the same format as the keypoint export in {@link DatasetKeypointExportMethod}
 * 
 * @author bs19022
 *
 */
public class DatasetKeypointImportMethod extends MultipleDatasetAnalysisMethod implements Io {

	private static final Logger LOGGER = Logger
			.getLogger(DatasetKeypointImportMethod.class.getName());

	private static final String IMPORT_FILE_KEY = "IMPORT_FILE";

	private final HashOptions options;
	
	private final File inputFile;

	/**
	 * Create with a dataset of cells to update
	 * 
	 * @param dataset
	 * @param options
	 */
	public DatasetKeypointImportMethod(@NonNull IAnalysisDataset dataset, @NonNull File f,
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
	public DatasetKeypointImportMethod(@NonNull List<IAnalysisDataset> datasets, @NonNull File f,
			@NonNull HashOptions options) {
		super(datasets);
		this.options = options;
		this.inputFile = f;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		for (IAnalysisDataset d : datasets)
			importKeypoints(d);
		return new DefaultAnalysisResult(datasets);
	}

	private void importKeypoints(IAnalysisDataset d)
			throws Exception {


		try (FileInputStream fstream = new FileInputStream(inputFile);
				BufferedReader br = new BufferedReader(
						new InputStreamReader(fstream, StandardCharsets.ISO_8859_1));) {

			String strLine;
			boolean isHeader = true;

			// Following fields in {@link DatasetKeypointExportMethod}
			while ((strLine = br.readLine()) != null) {
				if(isHeader) {
					isHeader = false;
					continue;
				}
				String[] arr = strLine.split(Io.TAB);

				String datasetName = arr[0];
				// Image folder and file
				String imageFolder = arr[1];
				String imageFileName = arr[2];
				File imageFile = new File(imageFolder, imageFileName);
				
				String cellID = arr[3];
				String nucleusID = arr[4];

				// Bounding box
				int xmin = Double.valueOf(arr[5]).intValue();
				int xmax = Double.valueOf(arr[6]).intValue();
				int ymin = Double.valueOf(arr[7]).intValue();
				int ymax = Double.valueOf(arr[8]).intValue();
				Rectangle r = new Rectangle(xmin, ymin, xmax - xmin, ymax - ymin);

				Optional<Nucleus> oN = d.getCollection().getNuclei(imageFile).stream()
						.filter(n -> r.contains(n.getOriginalCentreOfMass().toPoint2D()))
						.findFirst();

				// If there is a nucleus at the given position, update the landmark
				if (oN.isPresent()) {

					// Landmark name
					String lmName = arr[9];
					
					
					float lmx = Float.valueOf(arr[10]);
					float lmy = Float.valueOf(arr[11]);

					Landmark l = new DefaultLandmark(lmName);
					IPoint lm = new FloatPoint(lmx, lmy);

					// Find the closest point to the new landmark in the border
					IPoint newLm = oN.get().getBorderList().stream()
							.min(Comparator
									.comparing(point -> Math.abs(point.getLengthTo(lm))))
							.get();

					// Get the index of this point and update the landmark to this index
					int oldIdx = oN.get().getBorderIndex(l);
					int newIdx = oN.get().getBorderIndex(newLm);
					oN.get().setLandmark(l, newIdx);
					LOGGER.fine("Updated '%s' in '%s' from %s to %s".formatted(lmName, imageFileName, oldIdx, newIdx));
				} else {
					LOGGER.fine("No nucleus found in '%s' at x %s-%s y %s-%s".formatted(imageFileName, xmin, xmax, ymin, ymax));
				}

				fireProgressEvent();
			}
		}

		// TODO update profile collections, consensus nuclei
		d.getCollection().getProfileCollection().calculateProfiles();
		fireProgressEvent();
	}
	
}
