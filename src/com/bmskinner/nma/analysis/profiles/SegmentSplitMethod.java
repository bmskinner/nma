package com.bmskinner.nma.analysis.profiles;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * Split a segment in a dataset collection and its children, as long as the
 * collection is real.
 * 
 * @author Ben Skinner
 *
 */
public class SegmentSplitMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(SegmentSplitMethod.class.getName());
	private static final String SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL = "Segments are out of sync with median";

	private DatasetValidator dv = new DatasetValidator();

	private final UUID segId;

	/**
	 * Create a segment splitter
	 * 
	 * @param dataset the dataset to update
	 * @param seg0Id  the segment to split
	 */
	public SegmentSplitMethod(@NonNull IAnalysisDataset dataset, @NonNull UUID segId) {
		super(dataset);
		this.segId = segId;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();

		if (!dv.validate(dataset))
			throw new AnalysisMethodException(
					"Unable to validate dataset after splitting segments: "
							+ dv.getSummary() + "\n"
							+ dv.getErrors());
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws MissingDataException, SegmentUpdateException {
		if (!dataset.isRoot()) {
			LOGGER.fine(() -> String.format("'%s': Cannot split segments in a virtual dataset",
					dataset.getName()));
			return;
		}

		// Don't mess with a broken dataset
		if (!dv.validate(dataset)) {
			LOGGER.warning("Canceling segment split: " + SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL);
			return;
		}

		ISegmentedProfile medianProfile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
						Stats.MEDIAN);

		IProfileSegment seg = medianProfile.getSegment(segId);

		UUID newID1 = UUID.randomUUID();
		UUID newID2 = UUID.randomUUID();

		LOGGER.fine(() -> String.format("Splitting segment %s in root '%s' into %s and %s", segId,
				dataset.getName(), newID1, newID2));
		boolean ok = splitSegment(dataset.getCollection(), seg, newID1,
				newID2);
		fireProgressEvent();

		if (ok) {
			// Child datasets should all be virtual
			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				LOGGER.fine(() -> String.format("Splitting segment  %s in child '%s'", seg.getID(),
						child.getName()));
				boolean cOk = splitSegment(child.getCollection(), seg,
						newID1, newID2);
				fireProgressEvent();
				if (!cOk)
					LOGGER.warning(() -> String.format("Splitting segment %s failed in child '%s'",
							seg.getID(),
							child.getName()));
			}
		} else {
			LOGGER.warning(
					() -> String.format("Splitting segment in '%s' failed", dataset.getName()));
		}

	}

	/**
	 * Split the given segment into two segments. The split is made at the given
	 * index
	 * 
	 * @param seg    the segment to split
	 * @param newID1 the id for the first new segment. Can be null.
	 * @param newID2 the id for the second new segment. Can be null.
	 * @return true if the split succeeded, false otherwise
	 * @throws ProfileException
	 * @throws MissingDataException   if the reference point tag is missing, or the
	 *                                segment is missing
	 * @throws SegmentUpdateException
	 */
	private boolean splitSegment(@NonNull ICellCollection collection, @NonNull IProfileSegment seg,
			@NonNull UUID newID1,
			@NonNull UUID newID2)
			throws MissingDataException, SegmentUpdateException {

		ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		// Replace the segment with the actual median profile segment - eg when
		// updating child datasets
		seg = medianProfile.getSegment(seg.getID());
		int index = seg.getMidpointIndex();

		if (!seg.contains(index)) {
			LOGGER.warning("Segment cannot be split: does not contain index " + index);
			return false;
		}

		double proportion = seg.getIndexProportion(index);

		// Validate that all nuclei have segments long enough to be split
		// This only applies to real datasets - we never touch the nuclei in virtual
		// datasets
		if (collection.isReal() && !isCollectionSplittable(collection, seg.getID(), proportion)) {
			LOGGER.warning(String.format(
					"Segment %s cannot be split in '%s': not all nuclei have splittable segment",
					seg.getID(), collection.getName()));
			return false;

		}

		// split the two segments in the median
		medianProfile.splitSegment(seg, index, newID1, newID2);

		// put the new segment pattern back with the appropriate offset
		collection.getProfileCollection().setSegments(medianProfile.getSegments());

		/*
		 * Split the segments in the individual nuclei. Requires proportional alignment
		 */
		if (collection.isReal()) {
			for (Nucleus n : collection.getNuclei())
				splitNucleusSegment(n, seg.getID(), proportion, newID1, newID2);
		}

		/* Update the consensus if present */
		if (collection.hasConsensus()) {
			Nucleus n = collection.getRawConsensus();
			splitNucleusSegment(n, seg.getID(), proportion, newID1, newID2);
		}
		return true;
	}

	/**
	 * Split the segment in the given nucleus, preserving lock state
	 * 
	 * @param n          the nucleus
	 * @param segId      the segment to split
	 * @param proportion the proportion of the segment to split at (0-1)
	 * @param newId1     the first new segment id
	 * @param newId2     the second new segment id
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private void splitNucleusSegment(@NonNull Nucleus n, @NonNull UUID segId, double proportion,
			@NonNull UUID newId1,
			@NonNull UUID newId2)
			throws MissingDataException, SegmentUpdateException {
		boolean wasLocked = n.isLocked();
		n.setLocked(false); // not destructive
		splitSegment(n, segId, proportion, newId1, newId2);
		n.setLocked(wasLocked);
		fireProgressEvent();
	}

	/**
	 * Test all the nuclei of the collection to see if all segments can be split
	 * before we carry out the split.
	 * 
	 * @param id         the segment to test
	 * @param proportion the proportion of the segment at which to split, from 0-1
	 * @return true if the segment can be split at the index equivalent to the
	 *         proportion, false otherwise
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private boolean isCollectionSplittable(@NonNull ICellCollection collection, @NonNull UUID id,
			double proportion)
			throws MissingDataException, SegmentUpdateException {

		if (collection.isVirtual())
			return false;

		ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		int index = medianProfile.getSegment(id).getProportionalIndex(proportion);

		if (!medianProfile.isSplittable(id, index)) {
			return false;
		}

		// check consensus //TODO replace with remove consensus
		if (collection.hasConsensus()) {
			Nucleus n = collection.getRawConsensus();
			if (!isSplittable(n, id, proportion)) {
				return false;
			}
		}

		return collection.getNuclei().parallelStream()
				.allMatch(n -> isSplittable(n, id, proportion));
	}

	private boolean isSplittable(Taggable t, UUID id, double proportion) {

		try {
			ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
			IProfileSegment nSeg = profile.getSegment(id);
			int targetIndex = nSeg.getProportionalIndex(proportion);
			return profile.isSplittable(id, targetIndex);
		} catch (MissingDataException | SegmentUpdateException e) {
			LOGGER.log(Loggable.STACK, "Error getting profile", e);
			return false;
		}

	}

	/**
	 * Split a segment in the given taggable object. The segment will be split at
	 * the proportion given.
	 * 
	 * @param t          the object containing the segment
	 * @param idToSplit  the id of the segment to be split
	 * @param proportion the proportion of the segment length at which to split
	 * @param newID1     the id for the first new segment
	 * @param newID2     the id for the second new segment
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private void splitSegment(@NonNull Taggable t, @NonNull UUID idToSplit, double proportion,
			@NonNull UUID newID1,
			@NonNull UUID newID2)
			throws MissingDataException, SegmentUpdateException {

		ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);
		IProfileSegment nSeg = profile.getSegment(idToSplit);

		int targetIndex = nSeg.getProportionalIndex(proportion);
		profile.splitSegment(nSeg, targetIndex, newID1, newID2);
		t.setSegments(profile.getSegments());

	}

}
