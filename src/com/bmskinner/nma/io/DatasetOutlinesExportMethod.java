package com.bmskinner.nma.io;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.utility.ArrayUtils;

/**
 * Export the outlines of cellular components
 * 
 * @author Ben Skinner
 * @since 1.18.0
 *
 */
public class DatasetOutlinesExportMethod extends MeasurementsExportMethod {

	private static final Logger LOGGER = Logger.getLogger(DatasetOutlinesExportMethod.class.getName());

	/**
	 * Create specifying the folder profiles will be exported into
	 * 
	 * @param folder
	 */
	public DatasetOutlinesExportMethod(@NonNull File file, @NonNull List<IAnalysisDataset> list,
			@NonNull HashOptions options) {
		super(file, list, options);
	}

	/**
	 * Create specifying the folder profiles will be exported into
	 * 
	 * @param folder
	 */
	public DatasetOutlinesExportMethod(@NonNull File file, @NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		super(file, dataset, options);
	}

	@Override
	protected void appendHeader(@NonNull StringBuilder outLine) {
		outLine.append("Dataset").append(TAB)
				.append("CellID").append(TAB)
				.append("Component").append(TAB)
				.append("Folder").append(TAB)
				.append("Image").append(TAB)
				.append("RawX").append(TAB)
				.append("RawY").append(TAB)
				.append("OrientedX").append(TAB)
				.append("OrientedY")
				.append(NEWLINE);
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
	protected void append(@NonNull IAnalysisDataset d, @NonNull PrintWriter pw)
			throws Exception {

		String datasetCols = d.getName() + TAB; 

		for (ICell cell : d.getCollection().getCells()) {

			String cellCols = cell.getId() + TAB;

			for (Nucleus n : cell.getNuclei()) {
				StringBuilder constantCols = new StringBuilder()
						.append(datasetCols)
						.append(cellCols)
						.append(CellularComponent.NUCLEUS + "_" + n.getNameAndNumber() + TAB)
						.append(n.getSourceFolder() + TAB)
						.append(n.getSourceFile().getAbsolutePath() + TAB);

				String nString = appendNucleusOutline(constantCols.toString(), n);
				pw.write(nString);
				fireProgressEvent();
			}
		}
	}


	private String appendNucleusOutline(String constantColumns, Nucleus n) {
		
		StringBuilder outLine = new StringBuilder();
		try {
			// Get normalised coordinates for raw and oriented nuclei
			List<IPoint> rawCoords = getOutlinePoints(n);
			
			Nucleus o = n.getOrientedNucleus();
			o.moveCentreOfMass(IPoint.atOrigin());
			List<IPoint> orientedCoords = getOutlinePoints(o);
			
			// Add the outline coordinates to the output line
			for(int i=0; i<rawCoords.size(); i++) {
				
				IPoint raw = rawCoords.get(i);
				IPoint ori = orientedCoords.get(i);

				outLine.append(constantColumns)
					.append(raw.getX()).append(TAB)
					.append(raw.getY()).append(TAB)
					.append(ori.getX()).append(TAB)
					.append(ori.getY()).append(NEWLINE);

			}

		} catch (MissingLandmarkException | ComponentCreationException | SegmentUpdateException e) {
			LOGGER.warning(() -> "Error creating outline to export for " + n.getNameAndNumber());
			outLine.append(NEWLINE);
		}
		return outLine.toString();
	}

	/**
	 * @param n
	 * @return
	 * @throws MissingLandmarkException
	 * @throws SegmentUpdateException
	 */
	/**
	 * @param n
	 * @return
	 * @throws MissingLandmarkException
	 * @throws SegmentUpdateException
	 */
	private List<IPoint> getOutlinePoints(Nucleus n)
			throws MissingLandmarkException, SegmentUpdateException {
		// If a landmark to offset has been specified, lmOffset will not be null
		OrientationMark lmOffset = null;
		for (OrientationMark lm : n.getOrientationMarks()) {
			if (lm.name().equals(
					options.getString(HashOptions.EXPORT_OUTLINE_STARTING_LANDMARK_KEY))) {
				lmOffset = lm;
			}
		}

		// Get the borders offset to requested landmark (if present in options)
		List<IPoint> borderList = lmOffset == null ? n.getBorderList()
				: n.getBorderList(lmOffset);

		// Normalise border list - if required - to given number of points
		if (options.getBoolean(HashOptions.EXPORT_OUTLINE_IS_NORMALISED_KEY)) {
			borderList = changeBorderLength(borderList,
					options.getInt(HashOptions.EXPORT_OUTLINE_N_SAMPLES_KEY));
		}

		return borderList;
	}

	/**
	 * Given an input border list, sample n points equally spaced around the border
	 * 
	 * @param inputBorder
	 * @return
	 * @throws SegmentUpdateException
	 */
	private List<IPoint> changeBorderLength(List<IPoint> inputBorder, int nPoints)
			throws SegmentUpdateException {

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

		List<IPoint> r = new ArrayList<>();
		for (int i = 0; i < nPoints; i++) {
			r.add(new FloatPoint(xScale.get(i), yScale.get(i)));
		}

		return r;

	}

}
