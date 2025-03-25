package com.bmskinner.nma.analysis.profiles;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

/**
 * Merge segments in a dataset collection and its children, as long as the
 * collection is real.
 * 
 * @author Ben Skinner
 *
 */
public class SegmentMergeMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(SegmentMergeMethod.class.getName());
	private static final String SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL = "Segments are out of sync with median";

	private DatasetValidator dv = new DatasetValidator();

	private final UUID seg0Id;
	private final UUID seg1Id;
	private final UUID newId;

	/**
	 * Create a segment merger
	 * 
	 * @param dataset the dataset to update
	 * @param seg0Id  the first segment to merge
	 * @param seg1Id  the second segment to merge
	 */
	public SegmentMergeMethod(@NonNull IAnalysisDataset dataset, @NonNull UUID seg0Id,
			@NonNull UUID seg1Id) {
		this(dataset, seg0Id, seg1Id, null);
	}

	/**
	 * Create a segment merger
	 * 
	 * @param dataset the dataset to update
	 * @param seg0Id  the first segment to merge
	 * @param seg1Id  the second segment to merge
	 * @param newId   the id of the segment to create
	 */
	public SegmentMergeMethod(@NonNull IAnalysisDataset dataset, @NonNull UUID seg0Id,
			@NonNull UUID seg1Id, @Nullable UUID newId) {
		super(dataset);
		this.seg0Id = seg0Id;
		this.seg1Id = seg1Id;
		this.newId = newId == null ? UUID.randomUUID() : newId;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();

		if (!dv.validate(dataset))
			throw new AnalysisMethodException(
					"Unable to validate dataset after merging segments: "
							+ dv.getSummary() + "\n"
							+ dv.getErrors());
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws ProfileException, MissingDataException, SegmentUpdateException {
		if (!dataset.isRoot()) {
			LOGGER.fine("Cannot merge segments in a virtual collection");
			return;
		}

		// Don't mess with an already broken dataset
		if (!dv.validate(dataset)) {
			LOGGER.warning("Cancelling merge: " + SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL);
			return;
		}

		LOGGER.fine(
				() -> "Merging segments %s and %s in %s to new segment %s".formatted(seg0Id, seg1Id,
						dataset.getName(),
						newId));

		ISegmentedProfile medianProfile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		IProfileSegment seg1 = medianProfile.getSegment(seg0Id);
		IProfileSegment seg2 = medianProfile.getSegment(seg1Id);

		boolean ok = testSegmentsMergeable(seg1, seg2);

		if (ok) {
			mergeSegments(dataset.getCollection(), seg0Id, seg1Id, newId);
			fireProgressEvent();

			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				mergeSegments(child.getCollection(), seg0Id, seg1Id, newId);
				fireProgressEvent();
			}
		} else {
			LOGGER.warning("Segments are not mergable");
		}
	}

	/**
	 * Check that the given segment pair can be merged given the positions of core
	 * border tags
	 * 
	 * @param seg1 the first in the pair to merge
	 * @param seg2 the second in the pair to merge
	 * @return true if the merge is possible, false otherwise
	 * @throws MissingLandmarkException
	 */
	private boolean testSegmentsMergeable(IProfileSegment seg1, IProfileSegment seg2)
			throws MissingLandmarkException {

		if (!seg1.nextSegment().getID().equals(seg2.getID())) {
			return false;
		}

		// check the boundaries of the segment - we do not want to merge across the RP
		int tagIndex = dataset.getCollection().getProfileCollection()
				.getLandmarkIndex(OrientationMark.REFERENCE);
		if (seg1.getEndIndex() == tagIndex || seg2.getStartIndex() == tagIndex) {
			return false;
		}

		return true;
	}

	/**
	 * Merge the given segments from the median profile, and update each nucleus in
	 * the collection.
	 * 
	 * @param seg1  the first segment in the pair to merge
	 * @param seg2  the second segment in the pair to merge
	 * @param newID the id for the merged segment
	 * @throws ProfileException       if the update fails
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private void mergeSegments(@NonNull ICellCollection collection, @NonNull UUID seg1,
			@NonNull UUID seg2, @NonNull UUID newID)
			throws ProfileException, MissingDataException, SegmentUpdateException {
		// Note - we can't do the root check here. It must be at the segmentation
		// handler level
		// otherwise updating child datasets to match a root will fail

		ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		// Only try the merge if both segments are present in the profile
		if (!medianProfile.hasSegment(seg1))
			throw new ProfileException("Median profile does not have segment 1 with ID " + seg1);

		if (!medianProfile.hasSegment(seg2))
			throw new ProfileException("Median profile does not have segment 2 with ID " + seg2);

		// Note - validation is run in segmentation handler

		// merge the two segments in the median

		medianProfile.mergeSegments(seg1, seg2, newID);

		// put the new segment pattern back with the appropriate offset
		collection.getProfileCollection().setSegments(medianProfile.getSegments());

		/*
		 * With the median profile segments merged, also merge the segments in the
		 * individual nuclei
		 */
		if (collection.isReal()) {
			for (Nucleus n : collection.getNuclei()) {
				boolean wasLocked = n.isLocked();
				n.setLocked(false); // Merging segments is not destructive
				mergeSegments(n, seg1, seg2, newID);
				n.setLocked(wasLocked);
				fireProgressEvent();
			}
		}

		/* Update the consensus if present */
		if (collection.hasConsensus()) {
			Nucleus n = collection.getRawConsensus();
			mergeSegments(n, seg1, seg2, newID);
			fireProgressEvent();
		}
	}

	/**
	 * Merge the segments with the given IDs into a new segment with the given new
	 * ID
	 * 
	 * @param p     the object with a segmented profile to merge
	 * @param seg1  the first segment to be merged
	 * @param seg2  the second segment to be merged
	 * @param newID the new ID for the merged segment
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private void mergeSegments(@NonNull Nucleus p, @NonNull UUID seg1, @NonNull UUID seg2,
			@NonNull UUID newID)
			throws ProfileException, MissingDataException, SegmentUpdateException {
		ISegmentedProfile profile = p.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		// Only try the merge if both segments are present in the profile
		if (!profile.hasSegment(seg1))
			throw new ProfileException(
					p.getNameAndNumber() + " profile does not have segment 1 with ID " + seg1
							+ ". Profile is " + profile);

		if (!profile.hasSegment(seg2))
			throw new ProfileException(
					p.getNameAndNumber() + " profile does not have segment 2 with ID " + seg2
							+ ". Profile is " + profile);

		profile.mergeSegments(seg1, seg2, newID);
		p.setSegments(profile.getSegments());
	}
}
