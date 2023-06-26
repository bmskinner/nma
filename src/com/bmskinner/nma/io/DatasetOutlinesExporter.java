package com.bmskinner.nma.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.utility.ArrayUtils;

/**
 * Export the outlines of cellular components
 * 
 * @author Ben Skinner
 * @since 1.18.0
 *
 */
public class DatasetOutlinesExporter extends StatsExporter {

	private static final Logger LOGGER = Logger.getLogger(DatasetOutlinesExporter.class.getName());

	/**
	 * Create specifying the folder profiles will be exported into
	 * 
	 * @param folder
	 */
	public DatasetOutlinesExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list,
			@NonNull HashOptions options) {
		super(file, list, options);
	}

	/**
	 * Create specifying the folder profiles will be exported into
	 * 
	 * @param folder
	 */
	public DatasetOutlinesExporter(@NonNull File file, @NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		super(file, dataset, options);
	}

	@Override
	protected void appendHeader(@NonNull StringBuilder outLine) {
		outLine.append("Dataset\tCellID\tComponent\tFolder\tImage\tCoordinates");
		outLine.append(NEWLINE);
	}

	/**
	 * Append the given dataset outlines into the string builder
	 * 
	 * @param d       the dataset to export
	 * @param outLine the string builder to append to
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	@Override
	protected void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine)
			throws Exception {

		for (ICell cell : d.getCollection().getCells()) {

			if (cell.hasNucleus()) {

				for (Nucleus n : cell.getNuclei()) {

					outLine.append(d.getName() + TAB)
							.append(cell.getId() + TAB)
							.append(CellularComponent.NUCLEUS + "_" + n.getNameAndNumber() + TAB)
							.append(n.getSourceFolder() + TAB)
							.append(n.getSourceFileName() + TAB);

					appendNucleusOutline(outLine, n);
				}
			}

			if (cell.hasCytoplasm()) {
				outLine.append(d.getName() + TAB)
						.append(cell.getId() + TAB)
						.append(CellularComponent.CYTOPLASM + "_" + cell.getCytoplasm().getID()
								+ TAB)
						.append(cell.getCytoplasm().getSourceFolder() + TAB)
						.append(cell.getCytoplasm().getSourceFileName() + TAB);

				appendCytoplasmOutline(outLine, cell.getCytoplasm());
			}
		}
	}

	private void appendNucleusOutline(StringBuilder outLine, Nucleus n) {

		try {

			// If a landmark to offset has been specified, lmOffset will not be null
			Landmark lmOffset = null;
			for (Landmark lm : n.getLandmarks()) {
				if (lm.getName().equals(
						options.getString(DefaultOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY))) {
					lmOffset = lm;
				}
			}

			// Get the borders offset to requested landmark (if present in options)
			List<IPoint> borderList = lmOffset == null ? n.getBorderList()
					: n.getBorderList(lmOffset);

			// Normalise border list - if required - to given number of points
			if (options.getBoolean(DefaultOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY)) {
				borderList = normaliseBorderList(borderList,
						options.getInt(DefaultOptions.EXPORT_OUTLINE_N_SAMPLES_KEY));
			}

			for (IPoint p : borderList) {
				outLine.append(p.getX() + PIPE + p.getY() + COMMA);
			}
			// Remove final separator and add newline
			if (outLine.length() > 0)
				outLine.setLength(outLine.length() - 1);

			outLine.append(NEWLINE);
		} catch (MissingLandmarkException | ProfileException e) {
			LOGGER.warning(() -> "Error creating outline to export for " + n.getNameAndNumber());
			outLine.append(NEWLINE);
		}
	}

	/**
	 * Given an input border list, sample n points equally spaced around the border
	 * 
	 * @param inputBorder
	 * @return
	 * @throws ProfileException
	 */
	private List<IPoint> normaliseBorderList(List<IPoint> inputBorder, int nPoints)
			throws ProfileException {

		if (nPoints == inputBorder.size())
			return inputBorder;

		// This is basically the same interpolation as a profile, but for two
		// dimensions, x and y. Convert to two profiles
		float[] xpoints = ArrayUtils
				.toFloat(inputBorder.stream().mapToDouble(IPoint::getX).toArray());
		float[] ypoints = ArrayUtils
				.toFloat(inputBorder.stream().mapToDouble(IPoint::getY).toArray());

		IProfile xprofile = new DefaultProfile(xpoints);
		IProfile yprofile = new DefaultProfile(ypoints);

		IProfile xScale = xprofile.interpolate(nPoints);
		IProfile yScale = yprofile.interpolate(nPoints);

		List<IPoint> result = new ArrayList<>();
		for (int i = 0; i < nPoints; i++) {
			result.add(new FloatPoint(xScale.get(i), yScale.get(i)));
		}

		return result;

	}

	private void appendCytoplasmOutline(StringBuilder outLine, CellularComponent c) {

		for (IPoint p : c.getBorderList()) {
			outLine.append(p.getX() + PIPE + p.getY() + COMMA);
		}
		// Remove final separator and add newline
		if (outLine.length() > 0)
			outLine.setLength(outLine.length() - 1);

		outLine.append(NEWLINE);
	}

}
