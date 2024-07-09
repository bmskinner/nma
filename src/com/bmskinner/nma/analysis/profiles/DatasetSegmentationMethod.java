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
package com.bmskinner.nma.analysis.profiles;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.ProgressEvent;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.DefaultSegmentedProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

/**
 * Run the segmentation of datasets. This allows for the median profile of a
 * collection to be segmented, and the segments to be assigned to each nucleus
 * in the dataset.
 * 
 * The complete analysis pipeline operates via the following pseudocode: 1) From
 * the collection median, find the representative median 2) Segment the
 * representative median 3) For each nucleus, use iterative segment fitting to
 * map the segments
 * 
 * @author bms41
 *
 */
public class DatasetSegmentationMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger
			.getLogger(DatasetSegmentationMethod.class.getName());

	private ICellCollection sourceCollection = null;

	private final ICellCollection collection;

	private MorphologyAnalysisMode mode = MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH;

	/**
	 * The types of segmentation that can be performed
	 * 
	 * @author bs19022
	 *
	 */
	public enum MorphologyAnalysisMode {

		/** Segment the median and update nuclei to match */
		SEGMENT_FROM_SCRATCH,

		/** Copy the segment pattern from another dataset and update nuclei to match */
		COPY_FROM_OTHER_DATASET,

		/** Keep the current median segmentation, and update nuclei to match */
		APPLY_MEDIAN_TO_NUCLEI
	}

	/**
	 * Segment a dataset with the given mode
	 * 
	 * @param dataset the dataset to be segmented
	 * @param mode    the analysis mode to run
	 * @throws AnalysisMethodException
	 */
	public DatasetSegmentationMethod(@NonNull IAnalysisDataset dataset,
			@NonNull MorphologyAnalysisMode mode)
			throws AnalysisMethodException {
		super(dataset);
		if (!dataset.isRoot())
			throw new AnalysisMethodException("Dataset is not root, cannot segment");

		collection = dataset.getCollection();
		this.mode = mode;
	}

	/**
	 * Copy the segmentation pattern from the source collection onto the given
	 * dataset
	 * 
	 * @param dataset the dataset to be segmented
	 * @param source  the collection to copy segment patterns from
	 * @throws AnalysisMethodException
	 */
	public DatasetSegmentationMethod(@NonNull IAnalysisDataset dataset,
			@NonNull ICellCollection source)
			throws AnalysisMethodException {
		this(dataset, MorphologyAnalysisMode.COPY_FROM_OTHER_DATASET);
		this.sourceCollection = source;
	}

	@Override
	public IAnalysisResult call() throws Exception {

		if (!dataset.getAnalysisOptions().get().getProfilingOptions()
				.getBoolean(HashOptions.IS_SEGMENT_PROFILES)) {
			LOGGER.fine(() -> "Segmentation is skipped for dataset %s"
					.formatted(dataset.getName()));
			return new DefaultAnalysisResult(dataset);
		}

		result = switch (mode) {
		case COPY_FROM_OTHER_DATASET -> runCopyAnalysis();
		case SEGMENT_FROM_SCRATCH -> runNewAnalysis();
		case APPLY_MEDIAN_TO_NUCLEI -> applyMedianToNuclei();
		default -> null;
		};

		// Ensure segments are copied appropriately to verticals
		// Ensure hook statistics are generated appropriately
		for (Nucleus n : dataset.getCollection().getNuclei()) {
			// Initialise all measurements that do not already exist
			for (Measurement m : dataset.getAnalysisOptions()
					.orElseThrow(MissingOptionException::new)
					.getRuleSetCollection().getMeasurableValues()) {
				n.getMeasurement(m);
			}
		}
		DatasetValidator dv = new DatasetValidator();
		if (!dv.validate(dataset)) {
			LOGGER.warning("Segmentation failed; resulting dataset did not validate");
			throw new AnalysisMethodException(
					"Segmentation failed; resulting dataset did not validate: " + dv.getErrors());
		}

		return result;
	}

	private IAnalysisResult applyMedianToNuclei() throws Exception {
		ISegmentedProfile median = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE,
						OrientationMark.REFERENCE, Stats.MEDIAN);

		if (median.getSegmentCount() <= 0) {
			throw new AnalysisMethodException("Error finding segments in median profile");
		}

		if (median.getSegmentCount() == 1)
			LOGGER.info("No segments in median profile - creating single segment");

		assignSegmentsToNuclei(median);// fit the segments to nuclei

		// Consensus nucleus should reflect the median profile directly
		if (collection.hasConsensus()) {
			List<IProfileSegment> newSegs = IProfileSegment.scaleSegments(median.getSegments(),
					collection.getRawConsensus().getBorderLength());
			IProfileSegment.linkSegments(newSegs);
			collection.getRawConsensus().setSegments(newSegs);
		}

		// Copy segmentation to child datasets and their consensus nuclei
		if (dataset.hasChildren()) {
			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				dataset.getCollection().getProfileManager()
						.copySegmentsAndLandmarksTo(child.getCollection());
				if (child.getCollection().hasConsensus()) {
					List<IProfileSegment> newSegs = IProfileSegment.scaleSegments(
							median.getSegments(),
							child.getCollection().getRawConsensus().getBorderLength());
					IProfileSegment.linkSegments(newSegs);
					child.getCollection().getRawConsensus()
							.setSegments(newSegs);
				}

			}
		}
		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * The main analysis method. Segment the median, apply the segments to nuclei,
	 * generate the franken-median, and adjust the nuclear segments based on
	 * comparison of franken-profiles.
	 * 
	 * @return an analysis result with the segmented dataset
	 * @throws Exception
	 */
	private IAnalysisResult runNewAnalysis() throws Exception {
		if (!dataset.isRoot()) {
			throw new AnalysisMethodException("Dataset is not root, cannot segment");
		}

		dataset.getCollection().setConsensus(null); // clear if present
		LOGGER.finer("Before segmentation median length: " + collection.getMedianArrayLength());
		ISegmentedProfile median = createSegmentsInMedian(); // segment the median profile

		if (median.getSegmentCount() <= 0) {
			throw new AnalysisMethodException("Error finding segments in median profile");
		}

		if (median.getSegmentCount() == 1)
			LOGGER.info("No segments in median profile - creating single segment");

		// Make a new collection and aggregate to invalidate previous cached data,
		// possibly
		// with different profile lengths
		dataset.getCollection().getProfileCollection().calculateProfiles();

		dataset.getCollection().getProfileCollection().setSegments(median.getSegments());

		assignSegmentsToNuclei(median);// 4 - fit the segments to nuclei

		// The best segmentation pattern has been found
		// Copy segmentation to child datasets and invalidate any consensus nuclei
		if (dataset.hasChildren()) {
			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				child.getCollection().setConsensus(null);
				dataset.getCollection().getProfileManager()
						.copySegmentsAndLandmarksTo(child.getCollection());
			}
		}
		return new DefaultAnalysisResult(dataset);
	}

	private IAnalysisResult runCopyAnalysis() throws Exception {
		if (sourceCollection == null) {
			LOGGER.warning("Cannot copy: source collection is null");
			return null;
		}

		if (!sourceCollection.getProfileCollection().hasSegments()) {
			LOGGER.fine("Cannot copy segments: source collection has no segments");
			dataset.getCollection().getProfileCollection().calculateProfiles();
			return new DefaultAnalysisResult(dataset);
		}

		reapplyProfiles();
		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * When a population needs to be reanalysed do not offset nuclei or recalculate
	 * best fits; just get the new median profile
	 * 
	 * @param collection       the collection of nuclei
	 * @param sourceCollection the collection with segments to copy
	 */
	private void reapplyProfiles() throws Exception {
		sourceCollection.getProfileManager().copySegmentsAndLandmarksTo(collection);
	}

	/**
	 * Use a segmenter to segment the median profile of the collection starting from
	 * the reference point
	 * 
	 * @param collection
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	private ISegmentedProfile createSegmentsInMedian()
			throws SegmentUpdateException, MissingDataException {

		// choose the best subset of nuclei and make a median profile from them
		LOGGER.finer("Collection median length " + collection.getMedianArrayLength());

		RepresentativeMedianFinder finder = new RepresentativeMedianFinder(collection);

		final IProfile median = finder.findMedian();
		LOGGER.finer(() -> "Representative median length %d".formatted(median.size()));

		ProfileSegmenter segmenter = new ProfileSegmenter(median);
		List<IProfileSegment> segments = segmenter.segment();
		return new DefaultSegmentedProfile(median, segments);
	}

	/**
	 * Assign the segments in the given profile to the nuclei within the collection.
	 * The template profile is assumed to be indexed at the reference point. This
	 * will unlock nuclei as needed to ensure that segments are consistent through
	 * the dataset.
	 * 
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 * @throws ProfileException
	 */
	private void assignSegmentsToNuclei(@NonNull ISegmentedProfile template)
			throws ProfileException, MissingDataException, SegmentUpdateException {
		IterativeSegmentFitter fitter = new IterativeSegmentFitter(template);
		for (Nucleus n : collection.getNuclei()) {

			// Ensure new segments can be assigned
			boolean wasLocked = n.isLocked();
			n.setLocked(false);

			IProfile nucleusProfile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			ISegmentedProfile segProfile = fitter.fit(nucleusProfile);
			n.setSegments(segProfile.getSegments());

			if (segProfile.getSegmentCount() != template.getSegmentCount())
				throw new ProfileException("Segments could not be fitted to nucleus");

			if (template.getSegmentCount() == 1) {
				ISegmentedProfile test = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
				IProfileSegment seg = test.getSegment(IProfileCollection.DEFAULT_SEGMENT_ID);
				if (seg.getStartIndex() != 0) {
					throw new ProfileException("Single segment does not start at RP in nucleus");
				}
			}
			n.setLocked(wasLocked);
		}
	}

	@Override
	public void progressEventReceived(ProgressEvent event) {
		fireProgressEvent();
	}

}
