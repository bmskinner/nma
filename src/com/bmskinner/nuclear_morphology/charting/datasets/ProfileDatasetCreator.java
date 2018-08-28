package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
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

/**
 * Create charting datasets from analysis datasets.
 * @author ben
 * @since 1.14.0
 *
 */
public class ProfileDatasetCreator extends AbstractDatasetCreator<ChartOptions> {


	public static final int DEFAULT_PROFILE_LENGTH = 200;

	public static final String SEGMENT_SERIES_PREFIX  = "Seg_";
	public static final String NUCLEUS_SERIES_PREFIX  = "Nucleus_";
	public static final String QUARTILE_SERIES_PREFIX = "Q";
	public static final String PROFILE_SERIES_PREFIX  = "Profile_";
	public static final String MEDIAN_SERIES_PREFIX   = "Median_";

	public ProfileDatasetCreator(@NonNull ChartOptions options) {
		super(options);
	}
	
	/**
	 * Combine line series for profiles and difference series (for IQRs) in one class
	 * @author bms41
	 * @since 1.14.0
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
		
		@Override
		public String toString() {
			StringBuilder b = new StringBuilder("ProfileChartDataset:\nLines:\n");
			for(int i=0; i<lines.getSeriesCount(); i++) {
				Comparable seriesKey = lines.getSeriesKey(i);
				
				int items = lines.getItemCount(i);
				
				double min=Double.MAX_VALUE, max=-Double.MAX_VALUE;
				
				for(int j=0; j<items; j++) {
					min = Math.min(lines.getX(i, j).doubleValue(), min);
					max = Math.max(lines.getX(i, j).doubleValue(), max);
				}
				
				b.append(String.format("\tSeries %s: %s\tDataset %s\tItems: %s\tX-range: %s-%s\n", i, seriesKey, lines.getDatasetIndex(seriesKey), items, min, max ));		
			}
			b.append("Ranges:\n");
			for(int i : ranges.keySet()) {
				b.append("\tDataset "+i+"\n");
			}
			return b.toString();
		}
	}

	/**
	 * For offsetting a raw profile to the right. Find the maximum length of
	 * median profile in the dataset.
	 * 
	 * @param list the datasets to check
	 * @return the maximum length
	 */
	private int getMaximumMedianProfileLength(@NonNull final List<IAnalysisDataset> list) {
		return list.stream().mapToInt(d->d.getCollection().getMedianArrayLength())
				.max().orElse(DEFAULT_PROFILE_LENGTH);
	}

	/**
	 * Get the maximum nucleus length in a collection
	 * 
	 * @param collection
	 * @return
	 */
	private int getMaximumNucleusProfileLength(@NonNull final ICellCollection collection) {
		return collection.streamCells().flatMap(c->c.getNuclei().stream())
				.mapToInt(n->n.getBorderLength())
				.max().orElse(DEFAULT_PROFILE_LENGTH);
	}

	
	/**
	 * Create an appropriate chart dataset for the options
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	public ProfileChartDataset createProfileDataset() throws ChartDatasetCreationException {
		ProfileChartDataset ds = new ProfileChartDataset();
		int maxMedianLength = getMaximumMedianProfileLength(options.getDatasets());
		for(int i=0; i<options.getDatasets().size(); i++) {
			appendProfileDataset(ds, i, maxMedianLength);
		}
		return ds;
	}
	
	/**
	 * Create a chart series for a single analysis dataset and append it to the given
	 * chart dataset
	 * @param ds the chart dataset to be appended to 
	 * @param the index of the dataset for series naming
	 * @param maxMedianLength the maximum median length in the datasets being plotted
	 * @return
	 * @throws ChartDatasetCreationException
	 */
	private void appendProfileDataset(@NonNull final ProfileChartDataset ds, int i, int maxMedianLength) throws ChartDatasetCreationException {

		ICellCollection collection = options.getDatasets().get(i).getCollection();
		boolean isNormalised       = options.isNormalised();
		ProfileAlignment alignment = options.getAlignment();
		Tag borderTag              = options.getTag();
		ProfileType type           = options.getType();
		boolean isSegmented        = collection.getProfileCollection().hasSegments();
		boolean isShowSegments     = isSegmented && options.isSingleDataset();
		boolean isShowNuclei       = options.isSingleDataset() && options.isShowProfiles();
		boolean isShowIQR          = options.isShowIQR(); // points only displayed for single lines

		int maxNucleusLength    = getMaximumNucleusProfileLength(collection);
		int medianProfileLength = collection.getMedianArrayLength();
		int maxLength = isShowNuclei ? Math.max(maxMedianLength, maxNucleusLength) : maxMedianLength; // the maximum length that needs to be drawn
		
		double offset = 0;
		try {
			IProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(type, borderTag, Stats.MEDIAN);

			IProfile xpoints = createXPositions(medianProfile, isNormalised ? DEFAULT_PROFILE_LENGTH : medianProfileLength);

			// Offset the x positions depending on the alignment setting a
			if (alignment.equals(ProfileAlignment.RIGHT))
				offset = maxLength - collection.getMedianArrayLength();

			xpoints = xpoints.add(offset);
			
			// rendering order will be first on top

			// add the segments if any exist and there is only a single dataset
			if(isShowSegments) {
//				System.out.println(String.format("Drawing segments for %s", borderTag));
				List<IBorderSegment> segments = collection.getProfileCollection()
						.getSegmentedProfile(type, borderTag, Stats.MEDIAN)
						.getOrderedSegments();
				
//				System.out.println(String.format("Fetched %s median segments for %s", segments.size(), borderTag));

				if (isNormalised) {
					addSegmentsFromProfile(segments, medianProfile, ds.getLines(), DEFAULT_PROFILE_LENGTH, 0, 0);
				} else {
					addSegmentsFromProfile(segments, medianProfile, ds.getLines(), collection.getMedianArrayLength(), offset, 0);
				}
			} else {
				// add the median profile
				float[][] data50 = { xpoints.toFloatArray(), medianProfile.toFloatArray() };
				ds.addLines(MEDIAN_SERIES_PREFIX+i, data50, i);
			}

			// make the IQR
			if(isShowIQR) {
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
			}

			if(isShowNuclei) {
				// add the first n individual nuclei - avoid slowing the UI with too many cells
				List<Nucleus> nuclei = collection.getNuclei().stream()
						.limit(MAX_PROFILE_CHART_ITEMS)
						.collect(Collectors.toList());
				for (int j=0; j<nuclei.size(); j++) {
					Nucleus n = nuclei.get(j);

					int length = isNormalised ? DEFAULT_PROFILE_LENGTH : n.getBorderLength();

					IProfile yValues = isNormalised ? n.getProfile(type, borderTag).interpolate(length) : n.getProfile(type, borderTag);
					IProfile xValues = createXPositions(yValues, length);

					if (alignment.equals(ProfileAlignment.RIGHT))
						xValues = xValues.add(maxLength - n.getBorderLength());

					float[][] ndata = { xValues.toFloatArray(), yValues.toFloatArray() };

					ds.addLines(NUCLEUS_SERIES_PREFIX + j+ "_"+n.getSourceFileName() + "_" + n.getNucleusNumber(), ndata, i);
				}
			}
		} catch (UnavailableBorderTagException e) {
			fine("Border tag is not present: "+borderTag);
			return;
		}catch (ProfileException | UnavailableProfileTypeException | UnsegmentedProfileException e) {
			stack("Error getting profile from tag", e);
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
	 * @param profile the profile 
	 * @param newLength the length of the x-axis
	 * @return the position of each index in the profile after interpolation to <code>newLength</code> 
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
	 * @param datasetIndex the index of the dataset for adding to the chart dataset
	 * @throws ProfileException
	 * @throws ChartDatasetCreationException 
	 */
	private void addSegmentsFromProfile(List<IBorderSegment> segments, IProfile profile, FloatXYDataset ds,
			int length, double offset, int datasetIndex) throws ProfileException, ChartDatasetCreationException {

		IProfile xpoints = createXPositions(profile, length).add(offset);
		
		float[] xvalues = xpoints.toFloatArray();
		float[] yvalues = profile.toFloatArray();
		
		for (IBorderSegment seg : segments) {
			int prevIndex = -1;
						
			Iterator<Integer> it = seg.iterator();
			
			int start = seg.getStartIndex();
			while(it.hasNext()){

				int index = it.next();

				if(index<prevIndex) {
					// Start a new block if the segment is wrapping
					float[][] data = {Arrays.copyOfRange(xvalues, start, prevIndex+1),
							          Arrays.copyOfRange(yvalues, start, prevIndex+1)};
					start = index;
					
					ds.addSeries(seg.getName() + "_A", data, datasetIndex);
				}
				
				prevIndex = index;
			}
			
			try {
				if(prevIndex>-1) { // only happens when segmentation has gone wrong - but those are the times we need to check the chartstrek
					float[][] data = {Arrays.copyOfRange(xvalues, start, prevIndex+1),
							          Arrays.copyOfRange(yvalues, start, prevIndex+1)};
					ds.addSeries(seg.getName(), data, datasetIndex);
				}
			} catch(IllegalArgumentException e) {
				throw new ChartDatasetCreationException(String.format("Cannot make segment range for indexes %s to %s in segment %s", start, prevIndex, seg.getDetail()));
			}
			
			
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

	
    /**
     * Create a chart of the variability in the interquartile ranges across the
     * angle profiles of the given datasets
     * 
     * @return
     * @throws ChartDatasetCreationException
     */
    public ProfileChartDataset createProfileVariabilityDataset() throws ChartDatasetCreationException {
        if (options.isSingleDataset())
            return createSingleProfileVariabilityDataset();
		return createMultiProfileVariabilityDataset();
    }

    private ProfileChartDataset createSingleProfileVariabilityDataset() throws ChartDatasetCreationException {

    	ProfileChartDataset ds = new ProfileChartDataset();

        try {

            ICellCollection collection = options.firstDataset().getCollection();

            IProfile profile = collection.getProfileCollection().getIQRProfile(options.getType(), options.getTag());

            if(collection.getProfileCollection().hasSegments()) {
            	List<IBorderSegment> segments = collection.getProfileCollection()
                        .getSegmentedProfile(options.getType(), options.getTag(), Stats.MEDIAN).getOrderedSegments();
            	addSegmentsFromProfile(segments, profile, ds.getLines(), DEFAULT_PROFILE_LENGTH, 0, 0);
            } else {
            	IProfile xpoints = createXPositions(profile, DEFAULT_PROFILE_LENGTH);
				float[][] data = { xpoints.toFloatArray(), profile.toFloatArray() };
				ds.addLines(MEDIAN_SERIES_PREFIX+"0", data, 0);
            }
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException
                | UnsegmentedProfileException e) {
            fine("Error creating single dataset variability data", e);
            throw new ChartDatasetCreationException("Error creating single dataset variability data", e);
        }
        return ds;
    }

    private ProfileChartDataset createMultiProfileVariabilityDataset() throws ChartDatasetCreationException {

    	ProfileChartDataset ds = new ProfileChartDataset();

        for (int i=0; i<options.getDatasets().size(); i++) {
        	IAnalysisDataset dataset = options.getDatasets().get(i);
            ICellCollection collection = dataset.getCollection();

            IProfile profile;
            try {
                profile = collection.getProfileCollection().getIQRProfile(options.getType(), options.getTag());
            } catch (UnavailableBorderTagException | ProfileException | UnavailableProfileTypeException e) {
                fine("Error getting profile from tag", e);
                throw new ChartDatasetCreationException("Unable to get median profile", e);
            }
            IProfile xpoints = createXPositions(profile, DEFAULT_PROFILE_LENGTH);
            float[][] data = { xpoints.toFloatArray(), profile.toFloatArray() };
            ds.addLines(ProfileDatasetCreator.MEDIAN_SERIES_PREFIX + i + "_" + collection.getName(), data, i);
        }

        return ds;
    }
}
