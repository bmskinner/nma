/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.io;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.image.GLCM.GLCMParameter;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.Statistical;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.logging.Loggable;

/**
 * Export all the stats from a dataset to a text file for downstream analysis
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetStatsExporter extends StatsExporter {

	private static final Logger LOGGER = Logger.getLogger(DatasetStatsExporter.class.getName());

	private boolean isIncludeProfiles = true;
	private boolean isIncludeSegments = false;
	private boolean isIncludeGlcm = false;

	/** How many samples should be taken from each profile? */
	private int profileSamples = 100;
	private int segCount = 0;

	/** The default length to which profiles should be normalised */
	private static final int DEFAULT_PROFILE_LENGTH = 1000;

	/** The length to which profiles should be normalised */
	private final int normProfileLength;

	private final List<Measurement> measurements;

	/**
	 * Create specifying the folder stats will be exported into
	 * 
	 * @param folder
	 */
	public DatasetStatsExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list,
			HashOptions options) {
		super(file, list, options);
		segCount = list.get(0).getCollection().getProfileManager().getSegmentCount();
		if (list.size() == 1) {
			isIncludeSegments = true;
		} else {
			isIncludeSegments = list.stream()
					.allMatch(d -> d.getCollection().getProfileManager()
							.getSegmentCount() == segCount);
		}
		profileSamples = options.getInt(Io.PROFILE_SAMPLES_KEY);

		// Only include if present in all cells of all datasets
		isIncludeGlcm = list.stream()
				.allMatch(d -> d.getCollection().getCells().stream().noneMatch(c -> c
						.getPrimaryNucleus().getMeasurement(
								GLCMParameter.SUM.toStat()) == Statistical.ERROR_CALCULATING_STAT));

		normProfileLength = chooseNormalisedProfileLength();

		measurements = chooseMeasurementsToExport();
	}

	/**
	 * Create specifying the folder stats will be exported into
	 * 
	 * @param folder
	 */
	public DatasetStatsExporter(@NonNull File file, @NonNull IAnalysisDataset dataset,
			HashOptions options) {
		super(file, dataset, options);
		segCount = dataset.getCollection().getProfileManager().getSegmentCount();
		isIncludeSegments = true;
		profileSamples = options.getInt(Io.PROFILE_SAMPLES_KEY);

		isIncludeGlcm = dataset.getCollection().getCells().stream()
				.noneMatch(c -> c.getPrimaryNucleus()
						.getMeasurement(
								GLCMParameter.SUM.toStat()) == Statistical.ERROR_CALCULATING_STAT);

		normProfileLength = chooseNormalisedProfileLength();

		measurements = chooseMeasurementsToExport();
	}

	/**
	 * Not all datasets may have the same measurement. Take the union of all
	 * possible measurements.
	 * 
	 * @return
	 */
	private List<Measurement> chooseMeasurementsToExport() {
		Set<Measurement> result = new HashSet<>();
		for (IAnalysisDataset d : datasets) {
			result.addAll(d.getAnalysisOptions().get().getRuleSetCollection()
					.getMeasurableValues());
		}
		return result.stream().toList();
	}

	/**
	 * Write a column header line to the StringBuilder. Only nuclear stats for now
	 * 
	 * @param outLine
	 */
	@Override
	protected void appendHeader(@NonNull StringBuilder outLine) {
		outLine.append("Dataset").append(TAB)
				.append("File").append(TAB)
				.append("CellID").append(TAB)
				.append("Component").append(TAB)
				.append("Folder").append(TAB)
				.append("Image").append(TAB)
				.append("Centre_of_mass").append(TAB);

		for (Measurement s : measurements) {

			String label = s.label(MeasurementScale.PIXELS).replace(" ", "_").replace("(", "_")
					.replace(")", "")
					.replace("__", "_");
			outLine.append(label + TAB);

			if (!s.isDimensionless() && !s.isAngle()) { // only give micron
														// measurements when
														// length or area

				label = s.label(MeasurementScale.MICRONS).replace(" ", "_").replace("(", "_")
						.replace(")", "")
						.replace("__", "_");

				outLine.append(label + TAB);
			}

		}

		if (isIncludeGlcm) {
			for (Measurement s : Measurement.getGlcmStats()) {
				String label = s.label(MeasurementScale.PIXELS).replace(" ", "_").replace("__",
						"_");
				outLine.append("GLCM_" + label + TAB);
			}
		}

		if (isIncludeProfiles) {
			for (ProfileType type : ProfileType.exportValues()) {
				String label = type.toString().replace(" ", "_");
				for (int i = 0; i < profileSamples; i++) {
					outLine.append(label + "_" + i + TAB);
				}
			}
		}

		if (isIncludeSegments) {
			String label = "Length_seg_";

			for (int i = 0; i < segCount; i++) {
				outLine.append(label + i + "_pixels" + TAB);
				outLine.append(label + i + "_microns" + TAB);
				outLine.append("Seg_" + i + "_start" + TAB);
				outLine.append("Seg_" + i + "_end" + TAB);
			}
		}

		// remove the final tab character
		if (outLine.length() > 0)
			outLine.setLength(outLine.length() - 1);

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
	protected void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine)
			throws Exception {

		for (ICell cell : d.getCollection().getCells()) {

			if (cell.hasNucleus()) {

				for (Nucleus n : cell.getNuclei()) {

					outLine.append(d.getName() + TAB)
							.append(d.getSavePath() + TAB)
							.append(cell.getId() + TAB)
							.append(CellularComponent.NUCLEUS + "_" + n.getNameAndNumber() + TAB)
							.append(n.getSourceFolder() + TAB)
							.append(n.getSourceFileName() + TAB)
							.append(n.getOriginalCentreOfMass().toString() + TAB);

					appendNucleusStats(outLine, d, n);

					if (isIncludeProfiles) {
						appendProfiles(outLine, n);
					}

					if (isIncludeSegments) {
						appendSegments(outLine, n);
					}

					// Remove final tab
					if (outLine.length() > 0)
						outLine.setLength(outLine.length() - 1);

					outLine.append(NEWLINE);
				}

			}

		}
	}

	private void appendNucleusStats(StringBuilder outLine, IAnalysisDataset d,
			CellularComponent c) {

		for (Measurement s : measurements) {
			double varP = 0;
			double varM = 0;

			if (s.equals(Measurement.VARIABILITY)) {

				try {
					varP = d.getCollection().getNormalisedDifferenceToMedian(
							OrientationMark.REFERENCE, (Taggable) c);
					varM = varP;
				} catch (MissingLandmarkException e) {
					LOGGER.log(Loggable.STACK, "Landmark not present in component", e);
					varP = -1;
					varM = -1;
				}
			} else {
				varP = c.getMeasurement(s, MeasurementScale.PIXELS);
				varM = c.getMeasurement(s, MeasurementScale.MICRONS);
			}

			outLine.append(varP + TAB);
			if (!s.isDimensionless() && !s.isAngle()) {
				outLine.append(varM + TAB);
			}
		}

		if (isIncludeGlcm) {
			for (Measurement s : Measurement.getGlcmStats()) {
				outLine.append(c.getMeasurement(s) + TAB);
			}
		}
	}

	/**
	 * Generate and append profiles for a component
	 * 
	 * @param outLine the string builder to append to
	 * @param c       the component to export
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 */
	private void appendProfiles(StringBuilder outLine, Taggable c)
			throws MissingLandmarkException, MissingProfileException, ProfileException {
		for (ProfileType type : ProfileType.exportValues()) {

			IProfile p = c.getProfile(type, OrientationMark.REFERENCE);

			for (int i = 0; i < profileSamples; i++) {
				double idx = ((double) i) / (double) profileSamples;

				double value = p.get(idx);
				outLine.append(value + TAB);
			}
		}
	}

	private void appendSegments(StringBuilder outLine, Taggable c)
			throws MissingLandmarkException, MissingProfileException, ProfileException {

		double varP = 0;
		double varM = 0;

		ISegmentedProfile p = c.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		ISegmentedProfile normalisedProfile = p.interpolate(normProfileLength); // Allows point
																				// indexes
		List<IProfileSegment> segs = p.getOrderedSegments();

		for (IProfileSegment segment : segs) {
			if (segment != null) {
				// Add the length of the segment
				int indexLength = segment.length();
				double fractionOfPerimeter = (double) indexLength
						/ (double) segment.getProfileLength();
				varP = fractionOfPerimeter
						* c.getMeasurement(Measurement.PERIMETER, MeasurementScale.PIXELS);
				varM = fractionOfPerimeter
						* c.getMeasurement(Measurement.PERIMETER, MeasurementScale.MICRONS);
				outLine.append(varP + TAB);
				outLine.append(varM + TAB);

				// Add the index of the segment start and end in the normalised profile.
				try {
					IProfileSegment normalisedSeg = normalisedProfile.getSegment(segment.getID());
					int start = normalisedSeg.getStartIndex();
					int end = normalisedSeg.getEndIndex();
					outLine.append(start + TAB);
					outLine.append(end + TAB);
				} catch (MissingComponentException e) {
					outLine.append("NA" + TAB);
					outLine.append("NA" + TAB);
				}
			}
		}
	}

	/**
	 * When handling large objects, the default normalised profile length may not be
	 * sufficient. Ensure the normalised length is a multiple of
	 * DEFAULT_PROFILE_LENGTH and greater than any individual profile.
	 * 
	 * @return
	 * @throws MissingProfileException
	 */
	private int chooseNormalisedProfileLength() {
		int profileLength = DEFAULT_PROFILE_LENGTH;

		try {
			for (IAnalysisDataset d : datasets) {
				for (Nucleus n : d.getCollection().getNuclei()) {
					int l = n.getProfile(ProfileType.ANGLE).size();
					if (l > profileLength)
						profileLength = (int) Math.ceil(l / DEFAULT_PROFILE_LENGTH)
								* DEFAULT_PROFILE_LENGTH;
				}
			}
		} catch (MissingProfileException | ProfileException | MissingLandmarkException e) {
			LOGGER.log(Loggable.STACK, "Unable to get profile: " + e.getMessage(), e);
			LOGGER.fine("Unable to get a profile, defaulting to default profile length of "
					+ DEFAULT_PROFILE_LENGTH);
		}
		return profileLength;
	}
}
