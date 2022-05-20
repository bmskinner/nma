package com.bmskinner.nma.analysis.profiles;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

/**
 * Merge segments in a dataset collection and its children, as long as the
 * collection is real.
 * 
 * @author bs19022
 *
 */
public class SegmentMergeMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(SegmentMergeMethod.class.getName());
	private static final String SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL = "Segments are out of sync with median";

	private DatasetValidator dv = new DatasetValidator();

	private final UUID seg0Id;
	private final UUID seg1Id;

	/**
	 * Create a segment merger
	 * 
	 * @param dataset the dataset to update
	 * @param seg0Id  the first segment to merge
	 * @param seg1Id  the second segment to merge
	 */
	public SegmentMergeMethod(IAnalysisDataset dataset, @NonNull UUID seg0Id,
			@NonNull UUID seg1Id) {
		super(dataset);
		this.seg0Id = seg0Id;
		this.seg1Id = seg1Id;
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

	private void run() throws ProfileException, MissingComponentException {
		if (!dataset.isRoot()) {
			LOGGER.fine("Cannot merge segments in a virtual collection");
			return;
		}

		// Don't mess with an already broken dataset
		if (!dv.validate(dataset)) {
			LOGGER.warning("Cancelling merge: " + SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL);
			return;
		}

		// Give the new merged segment a new ID
		final UUID newID = UUID.randomUUID();

		LOGGER.fine(
				"Merging segments " + seg0Id + " and " + seg1Id + " in dataset " + dataset.getName()
						+ " to new segment " + newID);

		ISegmentedProfile medianProfile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		IProfileSegment seg1 = medianProfile.getSegment(seg0Id);
		IProfileSegment seg2 = medianProfile.getSegment(seg1Id);

		boolean ok = dataset.getCollection().getProfileManager().testSegmentsMergeable(seg1, seg2);

		if (ok) {
			dataset.getCollection().getProfileManager().mergeSegments(seg0Id, seg1Id, newID);
			fireProgressEvent();

			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				child.getCollection().getProfileManager().mergeSegments(seg0Id, seg1Id, newID);
				fireProgressEvent();
			}
		} else {
			LOGGER.warning("Segments are not mergable");
		}
	}
}
