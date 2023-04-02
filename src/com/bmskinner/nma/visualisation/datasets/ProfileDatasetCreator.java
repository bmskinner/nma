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
package com.bmskinner.nma.visualisation.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.profiles.DefaultProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;
import com.bmskinner.nma.visualisation.options.ChartOptions;

/**
 * Create charting datasets from analysis datasets.
 * 
 * @author ben
 * @since 1.14.0
 *
 */
public class ProfileDatasetCreator extends AbstractDatasetCreator<ChartOptions> {

	private static final Logger LOGGER = Logger.getLogger(ProfileDatasetCreator.class.getName());

	/** The length that profiles will be normalised to by default for display */
	private static final int DEFAULT_PROFILE_LENGTH = 1000;

	public ProfileDatasetCreator(@NonNull ChartOptions options) {
		super(options);
	}

	/**
	 * Combine line series for profiles and difference series (for IQRs) in one
	 * class
	 * 
	 * @author bms41
	 * @since 1.14.0
	 *
	 */
	public class ProfileChartDataset {

		/** values that will be drawn witha line renderer */
		private final FloatXYDataset lines = new FloatXYDataset();

		/** values that will be drawn witha difference renderer */
		private final Map<Integer, XYSeriesCollection> ranges = new HashMap<>();

		private float maxYValue = -Float.MAX_VALUE;

		private int maxDomainValue = DEFAULT_PROFILE_LENGTH;

		/**
		 * Add a line series
		 * 
		 * @param seriesKey    the key
		 * @param data         the data
		 * @param datasetIndex the index of the dataset in the chart
		 */
		public void addLines(String seriesKey, float[][] data, int datasetIndex) {
			lines.addSeries(seriesKey, data, datasetIndex);
			maxYValue = Math.max(maxYValue, Stats.max(data[1]));
		}

		public int getMaxDomainValue() {
			return maxDomainValue;
		}

		public void setMaxNormalisedDomainValue(int i) {
			maxDomainValue = i;
		}

		public double maxRangeValue() {
			return maxYValue;
		}

		/**
		 * Add a range series
		 * 
		 * @param seriesKey    the key
		 * @param data         the data
		 * @param datasetIndex the index of the dataset in the chart
		 */
		public void addRanges(String seriesKey, float[][] data, int datasetIndex) {

			ranges.computeIfAbsent(datasetIndex, k -> new XYSeriesCollection());

			XYSeries series = new XYSeries(seriesKey);
			float[] x = data[0];
			float[] y = data[1];
			for (int j = 0; j < x.length; j++) {
				series.add(x[j], y[j]);
			}
			maxYValue = Math.max(maxYValue, Stats.max(y));
			ranges.get(datasetIndex).addSeries(series);
		}

		/**
		 * Get the number of unique datasets in this class
		 * 
		 * @return
		 */
		public int getDatasetCount() {
			return ranges.size();
		}

		public FloatXYDataset getLines() {
			return lines;
		}

		public XYSeriesCollection getRanges(int datasetIndex) {
			return ranges.get(datasetIndex);
		}

		@Override
		public String toString() {
			StringBuilder b = new StringBuilder("ProfileChartDataset:\nLines:\n");
			for (int i = 0; i < lines.getSeriesCount(); i++) {
				Comparable seriesKey = lines.getSeriesKey(i);

				int items = lines.getItemCount(i);

				double min = Double.MAX_VALUE;
				double max = -Double.MAX_VALUE;

				for (int j = 0; j < items; j++) {
					min = Math.min(lines.getX(i, j).doubleValue(), min);
					max = Math.max(lines.getX(i, j).doubleValue(), max);
				}

				b.append(String.format("\tSeries %s: %s\tDataset %s\tItems: %s\tX-range: %s-%s%n",
						i, seriesKey,
						lines.getDatasetIndex(seriesKey), items, min, max));
			}
			b.append("Ranges:\n");
			for (int i : ranges.keySet()) {
				b.append("\tDataset " + i + "\n");
			}
			return b.toString();
		}
	}

	/**
	 * For offsetting a raw profile to the right. Find the maximum length of median
	 * profile in the dataset.
	 * 
	 * @param list the datasets to check
	 * @return the maximum length
	 */
	private int getMaximumMedianProfileLength(@NonNull final List<IAnalysisDataset> list) {
		return list.stream().mapToInt(d -> d.getCollection().getMedianArrayLength()).max()
				.orElse(DEFAULT_PROFILE_LENGTH);
	}

	/**
	 * Get the maximum nucleus length in a collection
	 * 
	 * @param collection
	 * @return
	 */
	private int getMaximumNucleusProfileLength(@NonNull final ICellCollection collection) {
		return collection.streamCells().flatMap(c -> c.getNuclei().stream())
				.mapToInt(CellularComponent::getBorderLength)
				.max()
				.orElse(DEFAULT_PROFILE_LENGTH);
	}

	/**
	 * Create an appropriate chart dataset for the options
	 * 
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public ProfileChartDataset createProfileDataset() throws ChartDatasetCreationException {
		ProfileChartDataset ds = new ProfileChartDataset();
		int maxMedianLength = getMaximumMedianProfileLength(options.getDatasets());
		for (int i = 0; i < options.getDatasets().size(); i++) {
			appendProfileDataset(ds, i, maxMedianLength);
		}
		return ds;
	}

	/**
	 * Create a chart series for a single analysis dataset and append it to the
	 * given chart dataset
	 * 
	 * @param ds              the chart dataset to be appended to
	 * @param the             index of the dataset for series naming
	 * @param maxMedianLength the maximum median length in the datasets being
	 *                        plotted
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	private void appendProfileDataset(@NonNull final ProfileChartDataset ds, int i,
			int maxMedianLength)
			throws ChartDatasetCreationException {

		ICellCollection collection = options.getDatasets().get(i).getCollection();
		boolean isNormalised = options.isNormalised();
		ProfileAlignment alignment = options.getAlignment();
		OrientationMark om = options.getOrientationMark();
		ProfileType type = options.getType();
		boolean isSegmented = collection.getProfileCollection().hasSegments();
		boolean isShowSegments = isSegmented && options.isSingleDataset();
		boolean isShowNuclei = options.isSingleDataset() && options.isShowProfiles();
		boolean isShowIQR = options.isShowIQR(); // points only displayed for single lines

		int maxNucleusLength = getMaximumNucleusProfileLength(collection);
		int medianProfileLength = collection.getMedianArrayLength();
		int maxLength = isShowNuclei ? Math.max(maxMedianLength, maxNucleusLength)
				: maxMedianLength; // the maximum
									// length that
									// needs to be
									// drawn

		double offset = 0;

		int normalisedProfileLength = chooseNormalisedProfileLength();
		ds.setMaxNormalisedDomainValue(normalisedProfileLength);

		try {
			IProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(type,
					om,
					Stats.MEDIAN);

			IProfile xpoints = createXPositions(medianProfile,
					isNormalised ? normalisedProfileLength : medianProfileLength);

			// Offset the x positions depending on the alignment setting a
			if (alignment.equals(ProfileAlignment.RIGHT))
				offset = maxLength - collection.getMedianArrayLength();

			xpoints = xpoints.add(offset);

			// rendering order will be first on top

			// add the segments if any exist and there is only a single dataset
			if (isShowSegments) {
				List<IProfileSegment> segments = collection.getProfileCollection()
						.getSegmentedProfile(type, om, Stats.MEDIAN).getOrderedSegments();

				if (isNormalised) {
					addSegmentsFromProfile(segments, medianProfile, ds, normalisedProfileLength, 0,
							0);
				} else {
					addSegmentsFromProfile(segments, medianProfile, ds,
							collection.getMedianArrayLength(), offset, 0);
				}
			} else {
				// add the median profile
				float[][] data50 = { xpoints.toFloatArray(), medianProfile.toFloatArray() };
				ds.addLines(MEDIAN_SERIES_PREFIX + i, data50, i);
			}

			// make the IQR
			if (isShowIQR) {
				IProfile profile25 = collection.getProfileCollection().getProfile(type, om,
						Stats.LOWER_QUARTILE);
				IProfile profile75 = collection.getProfileCollection().getProfile(type, om,
						Stats.UPPER_QUARTILE);

				float[][] data25 = { xpoints.toFloatArray(), profile25.toFloatArray() };
				float[][] data75 = { xpoints.toFloatArray(), profile75.toFloatArray() };

				if (isShowSegments) { // IQR as lines only
					ds.addLines(QUARTILE_SERIES_PREFIX + "25_" + i, data25, i);
					ds.addLines(QUARTILE_SERIES_PREFIX + "75_" + i, data75, i);
				} else { // IQR as difference range
					ds.addRanges(QUARTILE_SERIES_PREFIX + "25_" + i, data25, i);
					ds.addRanges(QUARTILE_SERIES_PREFIX + "75_" + i, data75, i);
				}
			}

			if (isShowNuclei) {
				// add random sample of n individual nuclei - avoid slowing the UI with too many
				// cells
				List<Nucleus> nuclei = new ArrayList<>(collection.getNuclei());
				Collections.shuffle(nuclei);
				nuclei = collection.getNuclei().stream()
						.limit(MAX_PROFILE_CHART_ITEMS)
						.toList();
				for (int j = 0; j < nuclei.size(); j++) {
					Nucleus n = nuclei.get(j);

					int length = isNormalised ? normalisedProfileLength : n.getBorderLength();

					IProfile yValues = isNormalised
							? n.getProfile(type, om).interpolate(length)
							: n.getProfile(type, om);
					IProfile xValues = createXPositions(yValues, length);

					if (alignment.equals(ProfileAlignment.RIGHT))
						xValues = xValues.add(maxLength - n.getBorderLength());

					float[][] ndata = { xValues.toFloatArray(), yValues.toFloatArray() };

					ds.addLines(
							NUCLEUS_SERIES_PREFIX + j + "_" + n.getSourceFileName() + "_"
									+ n.getNucleusNumber(),
							ndata, i);
				}
			}
		} catch (MissingLandmarkException e) {
			LOGGER.fine("Landmark is not present: " + om);
		} catch (ProfileException | MissingProfileException e) {
			LOGGER.log(Loggable.STACK, "Error getting profile from tag", e);
			throw new ChartDatasetCreationException("Unable to get median profile", e);
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
			for (IAnalysisDataset d : options.getDatasets()) {
				for (Nucleus n : d.getCollection().getNuclei()) {
					int l = n.getProfile(ProfileType.ANGLE).size();
					if (l > profileLength)
						profileLength = (int) Math.ceil(l / (double) DEFAULT_PROFILE_LENGTH)
								* DEFAULT_PROFILE_LENGTH;
				}
			}
		} catch (MissingProfileException | ProfileException | MissingLandmarkException e) {
			LOGGER.fine("Unable to get a profile, defaulting to default profile length");
		}
		return profileLength;
	}

	/**
	 * Create a dataset for an individual nucleus profile. Segments are added if
	 * present
	 * 
	 * @param nucleus the nucleus to draw
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public ProfileChartDataset createProfileDataset(@NonNull Nucleus nucleus)
			throws ChartDatasetCreationException {
		ProfileType type = options.getType();
		ProfileChartDataset ds = new ProfileChartDataset();

		try {
			IProfile profile = nucleus.getProfile(type, OrientationMark.REFERENCE);

			if (options.firstDataset().getCollection().getProfileCollection().hasSegments()) {
				List<IProfileSegment> segments = nucleus
						.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE)
						.getOrderedSegments();
				addSegmentsFromProfile(segments, profile, ds, nucleus.getBorderLength(), 0, 0);
			} else {
				IProfile xpoints = createXPositions(profile, nucleus.getBorderLength());
				float[][] data = { xpoints.toFloatArray(), profile.toFloatArray() };
				ds.addLines(PROFILE_SERIES_PREFIX + nucleus.getNameAndNumber(), data, 0);
			}

		} catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
			throw new ChartDatasetCreationException(
					"Cannot get segmented profile for " + nucleus.getNameAndNumber(),
					e);
		}
		return ds;
	}

	/**
	 * Create a profile chart dataset for the given profile
	 * 
	 * @param profile
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public ProfileChartDataset createProfileDataset(@NonNull IProfile profile)
			throws ChartDatasetCreationException {
		ProfileChartDataset ds = new ProfileChartDataset();
		if (profile instanceof ISegmentedProfile segProfile) {

			if (segProfile.hasSegments()) {
				List<IProfileSegment> segments = segProfile.getOrderedSegments();
				addSegmentsFromProfile(segments, segProfile, ds, segProfile.size(), 0, 0);
			}
		} else {
			IProfile xpoints = createXPositions(profile, profile.size());
			float[][] data = { xpoints.toFloatArray(), profile.toFloatArray() };
			ds.addLines(PROFILE_SERIES_PREFIX, data, 0);
		}
		return ds;
	}

	/**
	 * Create the x values for a profile chart based on the desired profile length
	 * 
	 * @param profile   the profile
	 * @param newLength the length of the x-axis
	 * @return the position of each index in the profile after interpolation to
	 *         <code>newLength</code>
	 */
	private static IProfile createXPositions(IProfile profile, int newLength) {
		float[] result = new float[profile.size()];
		for (int i = 0; i < profile.size(); i++) {
			result[i] = (float) (profile.getFractionOfIndex(i) * newLength);
		}
		return new DefaultProfile(result);
	}

	/**
	 * Add individual segments from a profile to a dataset. Offset them to the given
	 * length
	 * 
	 * @param segments     the list of segments to add
	 * @param profile      the profile against which to add them
	 * @param ds           the dataset the segments are to be added to
	 * @param length       the profile length
	 * @param offset       an offset to the x position. Used to align plots to the
	 *                     right
	 * @param datasetIndex the index of the dataset for adding to the chart dataset
	 * @throws ProfileException
	 * @throws ChartDatasetCreationException
	 */
	private void addSegmentsFromProfile(List<IProfileSegment> segments, IProfile profile,
			ProfileChartDataset ds,
			int length, double offset, int datasetIndex)
			throws ChartDatasetCreationException {

		IProfile xpoints = createXPositions(profile, length).add(offset);

		float[] xvalues = xpoints.toFloatArray();
		float[] yvalues = profile.toFloatArray();

		for (IProfileSegment seg : segments) {
			int prevIndex = -1;

			Iterator<Integer> it = seg.iterator();

			int start = seg.getStartIndex();
			while (it.hasNext()) {

				int index = it.next();

				if (index < prevIndex) {
					// Start a new block if the segment is wrapping
					float[][] data = { Arrays.copyOfRange(xvalues, start, prevIndex + 1),
							Arrays.copyOfRange(yvalues, start, prevIndex + 1) };
					start = index;

					ds.addLines(seg.getName() + "_A", data, datasetIndex);
				}

				prevIndex = index;
			}

			try {
				if (prevIndex > -1) { // only happens when segmentation has gone wrong - but those
										// are the times we
										// need to check the chartstrek
					float[][] data = { Arrays.copyOfRange(xvalues, start, prevIndex + 1),
							Arrays.copyOfRange(yvalues, start, prevIndex + 1) };
					ds.addLines(seg.getName(), data, datasetIndex);
				}
			} catch (IllegalArgumentException e) {
				throw new ChartDatasetCreationException(
						String.format(
								"Cannot make segment range for indexes %s to %s in segment %s",
								start, prevIndex,
								seg.getDetail()));
			}

		}

	}

	/**
	 * Create a chart of the variability in the interquartile ranges across the
	 * angle profiles of the given datasets
	 * 
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public ProfileChartDataset createProfileVariabilityDataset()
			throws ChartDatasetCreationException {
		if (options.isSingleDataset())
			return createSingleProfileVariabilityDataset();
		return createMultiProfileVariabilityDataset();
	}

	private ProfileChartDataset createSingleProfileVariabilityDataset()
			throws ChartDatasetCreationException {

		ProfileChartDataset ds = new ProfileChartDataset();

		try {

			ICellCollection collection = options.firstDataset().getCollection();

			IProfile profile = collection.getProfileCollection().getIQRProfile(options.getType(),
					options.getOrientationMark());

			if (collection.getProfileCollection().hasSegments()) {
				List<IProfileSegment> segments = collection.getProfileCollection()
						.getSegmentedProfile(options.getType(), options.getOrientationMark(),
								Stats.MEDIAN)
						.getOrderedSegments();
				addSegmentsFromProfile(segments, profile, ds, DEFAULT_PROFILE_LENGTH, 0, 0);
			} else {
				IProfile xpoints = createXPositions(profile, DEFAULT_PROFILE_LENGTH);
				float[][] data = { xpoints.toFloatArray(), profile.toFloatArray() };
				ds.addLines(MEDIAN_SERIES_PREFIX + "0", data, 0);
			}
		} catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
			LOGGER.log(Loggable.STACK,
					"Error creating single dataset variability data: %s".formatted(e.getMessage()),
					e);
			throw new ChartDatasetCreationException(
					"Error creating single dataset variability data", e);
		}
		return ds;
	}

	private ProfileChartDataset createMultiProfileVariabilityDataset()
			throws ChartDatasetCreationException {

		ProfileChartDataset ds = new ProfileChartDataset();

		for (int i = 0; i < options.getDatasets().size(); i++) {
			IAnalysisDataset dataset = options.getDatasets().get(i);
			ICellCollection collection = dataset.getCollection();

			IProfile profile;
			try {
				profile = collection.getProfileCollection().getIQRProfile(options.getType(),
						options.getOrientationMark());
			} catch (MissingLandmarkException | ProfileException | MissingProfileException e) {
				LOGGER.log(Loggable.STACK, "Error getting profile from tag", e);
				throw new ChartDatasetCreationException("Unable to get median profile", e);
			}
			IProfile xpoints = createXPositions(profile, DEFAULT_PROFILE_LENGTH);
			float[][] data = { xpoints.toFloatArray(), profile.toFloatArray() };
			ds.addLines(
					AbstractDatasetCreator.MEDIAN_SERIES_PREFIX + i + "_" + collection.getName(),
					data, i);
		}

		return ds;
	}
}
