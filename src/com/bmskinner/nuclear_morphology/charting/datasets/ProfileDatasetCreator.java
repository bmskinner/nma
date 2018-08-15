package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.charting.options.ChartOptions;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.FloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.gui.components.panels.ProfileAlignmentOptionsPanel.ProfileAlignment;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class ProfileDatasetCreator extends AbstractDatasetCreator<ChartOptions> {


	private static final double DEFAULT_PROFILE_LENGTH = 100;

	public static final String SEGMENT_SERIES_PREFIX  = "Seg_";
	public static final String NUCLEUS_SERIES_PREFIX  = "Nucleus_";
	public static final String QUARTILE_SERIES_PREFIX = "Q";
	public static final String PROFILE_SERIES_PREFIX  = "Profile_";

	public ProfileDatasetCreator(ChartOptions options) {
		super(options);
	}
	
	/**
	 * Combine line series for profiles and difference series (for IQRs) in one class
	 * @author bms41
	 *
	 */
	public class ProfileChartDataset {
		private final FloatXYDataset lines = new FloatXYDataset(); // values that will be drawn with a line renderer
		private final Map<Integer, XYSeriesCollection> ranges = new HashMap<>(); // values that will be drawn with a difference renderer
		
		/**
		 * Add a line series
		 * @param seriesKey the key
		 * @param data the data
		 * @param datasetIndex the index of the dataset in the chart
		 */
		public void addLines(String seriesKey, float[][] data, int datasetIndex) {
			lines.addSeries(seriesKey, data, datasetIndex);
		}
		
		/**
		 * Add a range series
		 * @param seriesKey the key
		 * @param data the data
		 * @param datasetIndex the index of the dataset in the chart
		 */
		public void addRanges(String seriesKey, float[][] data, int datasetIndex) {
			if(!ranges.containsKey(datasetIndex))
				ranges.put(datasetIndex, new XYSeriesCollection());
			
			XYSeries series = new XYSeries(seriesKey);
			float[] x = data[0];
			float[] y = data[1];
	        for (int j = 0; j < x.length; j++) {
	        	series.add(x[j], y[j]);
	        }
	        ranges.get(datasetIndex).addSeries(series);
		}
		
		/**
		 * Get the number of unique datasets in this class
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
	}

	/**
	 * For offsetting a raw profile to the right. Find the maximum length of
	 * median profile in the dataset.
	 * 
	 * @param list the datasets to check
	 * @return the maximum length
	 */
	private int getMaximumMedianProfileLength(final List<IAnalysisDataset> list) {
		return list.stream().mapToInt(d->d.getCollection().getMedianArrayLength())
				.max().orElse(100);
	}

	/**
	 * Get the maximum nucleus length in a collection
	 * 
	 * @param collection
	 * @return
	 */
	private int getMaximumNucleusProfileLength(ICellCollection collection) {
		return collection.streamCells().flatMap(c->c.getNuclei().stream())
				.mapToInt(n->n.getBorderLength())
				.max().orElse(100);
	}

	
	/**
	 * Create an appropriate chart dataset for the options
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public ProfileChartDataset createProfileDataset() throws ChartDatasetCreationException {
		ProfileChartDataset ds = new ProfileChartDataset();
		for(int i=0; i<options.getDatasets().size(); i++) {
			IAnalysisDataset d  = options.getDatasets().get(i);
			appendProfileDataset(ds, d, i);
		}
		return ds;
	}
	
	/**
	 * Create a chart series for a single analysis dataset and append it to the given
	 * chart dataset
	 * @param ds the chart dataset to be appended to 
	 * @param the analysis dataset to draw
	 * @param the index of the dataset for series naming
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	private void appendProfileDataset(ProfileChartDataset ds, IAnalysisDataset dataset, int i) throws ChartDatasetCreationException {

		ICellCollection collection = options.firstDataset().getCollection();
		boolean isNormalised       = options.isNormalised();
		ProfileAlignment alignment = options.getAlignment();
		Tag borderTag              = options.getTag();
		ProfileType type           = options.getType();
		boolean isSegmented        = collection.getProfileCollection().hasSegments();
		boolean isShowSegments     = !options.isHideProfiles() && options.isSingleDataset();
		boolean isShowNuclei       = options.isSingleDataset();	

		int maxLength = getMaximumNucleusProfileLength(collection);
		int medianProfileLength = collection.getMedianArrayLength();
		double offset = 0;
		try {
			IProfile medianProfile = collection.getProfileCollection().getProfile(type, options.getTag(), Stats.MEDIAN);

			IProfile xpoints = createXPositions(medianProfile, isNormalised ? 100 : medianProfileLength);

			if (alignment.equals(ProfileAlignment.RIGHT)) {
				offset = maxLength - collection.getMedianArrayLength();
				xpoints = xpoints.add(offset);
			}

			// rendering order will be first on top

			// add the segments if any exist and there is only a single dataset
			if(isSegmented && isShowSegments) {
				log("Drawing segments");
				List<IBorderSegment> segments = collection.getProfileCollection()
						.getSegmentedProfile(type, options.getTag(), Stats.MEDIAN)
						.getOrderedSegments();

				if (isNormalised) {
					addSegmentsFromProfile(segments, medianProfile, ds.getLines(), 100, 0, 0);
				} else {
					addSegmentsFromProfile(segments, medianProfile, ds.getLines(), collection.getMedianArrayLength(), offset, 0);
				}
			} else {
				log("Drawing only median");
				// add the median profile
				float[][] data50 = { xpoints.toFloatArray(), medianProfile.toFloatArray() };
				ds.addLines(PROFILE_SERIES_PREFIX+i, data50, i);
			}

			// make the IQR
			IProfile profile25 = collection.getProfileCollection().getProfile(type, borderTag, Stats.LOWER_QUARTILE);
			IProfile profile75 = collection.getProfileCollection().getProfile(type, borderTag, Stats.UPPER_QUARTILE);

			float[][] data25 = { xpoints.toFloatArray(), profile25.toFloatArray() };
			float[][] data75 = { xpoints.toFloatArray(), profile75.toFloatArray() };
			
			if(isShowSegments) { // IQR as lines only
				ds.addLines(QUARTILE_SERIES_PREFIX+"25_"+i, data25, i);
				ds.addLines(QUARTILE_SERIES_PREFIX+"75_"+i, data75, i);
			} else { // IQR as difference range
				ds.addRanges(QUARTILE_SERIES_PREFIX+"25_"+i, data25, i);
				ds.addRanges(QUARTILE_SERIES_PREFIX+"75_"+i, data75, i);
			}
			

			// add the first n individual nuclei - avoid slowing the UI with too many cells
			List<Nucleus> nuclei = collection.getNuclei().stream()
					.limit(MAX_PROFILE_CHART_ITEMS)
					.collect(Collectors.toList());
			
			if(isShowNuclei) {
				for (Nucleus n : nuclei) {

					int length = isNormalised ? xpoints.size() : n.getBorderLength();

					IProfile angles = isNormalised ? n.getProfile(type, borderTag).interpolate(length) : n.getProfile(type, borderTag);
					IProfile x = isNormalised ? xpoints : createXPositions(angles, n.getBorderLength());
					if (alignment.equals(ProfileAlignment.RIGHT))
						x = x.add(maxLength - n.getBorderLength());

					if (angles == null || x == null)
						throw new ChartDatasetCreationException("Nucleus profile is null");

					float[][] ndata = { x.toFloatArray(), angles.toFloatArray() };

					ds.addLines(NUCLEUS_SERIES_PREFIX + n.getSourceFileName() + "-" + n.getNucleusNumber(), ndata, i);
				}
			}
		} catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException | UnsegmentedProfileException e) {
			fine("Error getting profile from tag", e);
			throw new ChartDatasetCreationException("Unable to get median profile", e);
		}
	}

	/**
	 * Create a dataset for an individual nucleus profile. Segments are added
	 * if present
	 * 
	 * @param nucleus the nucleus to draw
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public ProfileChartDataset createProfileDataset(Nucleus nucleus) throws ChartDatasetCreationException {
		ProfileType type = options.getType();
		ProfileChartDataset ds = new ProfileChartDataset();

		try {
			IProfile profile = type.equals(ProfileType.FRANKEN) 
					? nucleus.getProfile(type)
							: nucleus.getProfile(type, Tag.REFERENCE_POINT);

					if(options.firstDataset().getCollection().getProfileCollection().hasSegments()) {
						List<IBorderSegment> segments = nucleus.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
								.getOrderedSegments();
						addSegmentsFromProfile(segments, profile, ds.getLines(), nucleus.getBorderLength(), 0, 0);
					} else {
						IProfile xpoints = createXPositions(profile, nucleus.getBorderLength());
						float[][] data = { xpoints.toFloatArray(), profile.toFloatArray() };
						ds.addLines(PROFILE_SERIES_PREFIX+nucleus.getNameAndNumber(), data, 0);
					}        	 

		} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
			throw new ChartDatasetCreationException("Cannot get segmented profile for " + nucleus.getNameAndNumber(), e);
		}
		return ds;
	}

	/**
	 * Create the x values for a profile chart based on the desired profile length
	 * @param profile
	 * @param newLength
	 * @return
	 */
	private static IProfile createXPositions(IProfile profile, int newLength){
		float[] result = new float[profile.size()];
		for (int i = 0; i < profile.size(); i++) {
			result[i] = (float) (profile.getFractionOfIndex(i) * newLength);
		}
		return new FloatProfile(result);
	}

	/**
	 * Add individual segments from a profile to a dataset. Offset them to the
	 * given length
	 * 
	 * @param segments the list of segments to add
	 * @param profile the profile against which to add them
	 * @param ds the dataset the segments are to be added to
	 * @param length the profile length
	 * @param offset an offset to the x position. Used to align plots to the right
	 * @param binSize the size of the ProfileAggregate bins, to adjust the offset of
	 *            the median
	 * @return the updated dataset
	 * @throws ProfileException
	 */
	private void addSegmentsFromProfile(List<IBorderSegment> segments, IProfile profile, FloatXYDataset ds,
			int length, double offset, int datasetIndex) throws ProfileException {

		IProfile xpoints = createXPositions(profile, length);
		xpoints = xpoints.add(offset);
		for (IBorderSegment seg : segments) {

			if (seg.wraps()) { // case when array wraps. We need to plot the two
				// ends as separate series

				if (seg.getEndIndex() == 0) {
					// no need to make two sections
					IProfile subProfile = profile.getSubregion(seg.getStartIndex(), profile.size() - 1);
					IProfile subPoints = xpoints.getSubregion(seg.getStartIndex(), profile.size() - 1);

					float[][] data = { subPoints.toFloatArray(), subProfile.toFloatArray() };

					// check if the series key is taken
					String seriesName = checkSeriesName(ds, seg.getName());

					ds.addSeries(seriesName, data, datasetIndex);

				} else {

					int lowerIndex = Math.min(seg.getEndIndex(), seg.getStartIndex());
					int upperIndex = Math.max(seg.getEndIndex(), seg.getStartIndex());

					// beginning of array
					IProfile subProfileA = profile.getSubregion(0, lowerIndex);
					IProfile subPointsA = xpoints.getSubregion(0, lowerIndex);

					float[][] dataA = { subPointsA.toFloatArray(), subProfileA.toFloatArray() };
					ds.addSeries(seg.getName() + "_A", dataA, 0);

					// end of array
					IProfile subProfileB = profile.getSubregion(upperIndex, profile.size() - 1);
					IProfile subPointsB = xpoints.getSubregion(upperIndex, profile.size() - 1);

					float[][] dataB = { subPointsB.toFloatArray(), subProfileB.toFloatArray() };
					ds.addSeries(seg.getName() + "_B", dataB, datasetIndex);
				}

				continue; // move on to the next segment

			}
			IProfile subProfile = profile.getSubregion(seg);
			IProfile subPoints = xpoints.getSubregion(seg);

			float[][] data = { subPoints.toFloatArray(), subProfile.toFloatArray() };

			// check if the series key is taken
			String seriesName = checkSeriesName(ds, seg.getName());

			ds.addSeries(seriesName, data, datasetIndex);
		}

	}

	/**
	 * Create a segmented dataset for an individual nucleus. Segments are added
	 * for all types except frankenprofiles, since the frankenprofile boundaries
	 * will not match
	 * 
	 * @param nucleus the nucleus to draw
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	private XYDataset createSegmentedProfileDataset(Nucleus nucleus) throws ChartDatasetCreationException {

		ProfileType type = options.getType();
		FloatXYDataset ds = new FloatXYDataset();

		ISegmentedProfile profile;

		try {

			if (type.equals(ProfileType.FRANKEN)) {
				profile = nucleus.getProfile(type);
			} else {

				finest("Getting XY positions along profile from reference point");
				profile = nucleus.getProfile(type, Tag.REFERENCE_POINT);

				// add the segments
				finest("Adding ordered segments from reference point");
				List<IBorderSegment> segments = nucleus.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
						.getOrderedSegments();
				addSegmentsFromProfile(segments, profile, ds, nucleus.getBorderLength(), 0, 0);
			}
		} catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
			fine("Error getting profile", e);
			throw new ChartDatasetCreationException("Cannot get segmented profile for " + nucleus.getNameAndNumber());
		}

		return ds;
	}

	/**
	 * Check if the string for the series key is aleady used. If so, append _1
	 * and check again
	 * 
	 * @param ds the dataset of series
	 * @param name the name to check
	 * @return a valid name
	 */
	private String checkSeriesName(XYDataset ds, String name) {
		String result = name;
		boolean ok = true;
		for (int i = 0; i < ds.getSeriesCount(); i++) {
			if (ds.getSeriesKey(i).equals(name))
				ok = false; // do not allow the same name to be added twice
		}
		if (!ok)
			result = checkSeriesName(ds, name + "_1");
		return result;

	}

}
