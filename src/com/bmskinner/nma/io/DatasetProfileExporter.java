package com.bmskinner.nma.io;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;

/**
 * Class to export only profiles from nuclei
 * 
 * @author Ben Skinner
 * @since 1.17.2
 *
 */
public class DatasetProfileExporter extends MeasurementsExportMethod {

	private static final Logger LOGGER = Logger.getLogger(DatasetProfileExporter.class.getName());

	/**
	 * Create specifying the file or directory profiles will be exported into
	 * 
	 * @param file    the output file or directory for the export
	 * @param list    the datasets to export
	 * @param options other options for the export
	 */
	public DatasetProfileExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list,
			HashOptions options) {
		super(file, list, options);
	}

	/**
	 * Create specifying the file or directory profiles will be exported into
	 * 
	 * @param file    the output file or directory for the export
	 * @param dataset the dataset to export
	 * @param options other options for the export
	 */
	public DatasetProfileExporter(@NonNull File file, @NonNull IAnalysisDataset dataset,
			HashOptions options) {
		super(file, dataset, options);
	}

	@Override
	protected void appendHeader(@NonNull StringBuilder outLine) {
		outLine.append("Dataset\tCellID\tComponent\tFolder\tImage\tProfile");
		outLine.append(NEWLINE);
	}

	/**
	 * Append the given dataset stats into the string builder
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

		for (ICell cell : d.getCollection().getCells()) {

			StringBuilder outLine = new StringBuilder();

			if (cell.hasNucleus()) {

				for (Nucleus n : cell.getNuclei()) {

					outLine.append(d.getName() + TAB).append(cell.getId() + TAB)
							.append(CellularComponent.NUCLEUS + "_" + n.getNameAndNumber() + TAB)
							.append(n.getSourceFolder() + TAB).append(n.getSourceFileName() + TAB);

					appendProfiles(outLine, n);

					// Remove final separator
					if (outLine.length() > 0)
						outLine.setLength(outLine.length() - 1);

					outLine.append(NEWLINE);
				}
			}
			pw.write(outLine.toString());
		}
	}

	private void appendProfiles(StringBuilder outLine, Taggable c) {

		try {
			IProfile p = c.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

			for (int i : p) {
				double value = p.get(i);
				outLine.append(value + COMMA);
			}

		} catch (MissingDataException | SegmentUpdateException e) {
			LOGGER.severe("Unable to get profile for component " + c.getId());
		}
	}
}
