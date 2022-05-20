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
 * Unmerge segments in a dataset collection and its children, as long as the
 * collection is real.
 * 
 * @author bs19022
 *
 */
public class SegmentUnmergeMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(SegmentUnmergeMethod.class.getName());
	private static final String SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL = "Segments are out of sync with median";

	private DatasetValidator dv = new DatasetValidator();

	private final UUID segId;

	/**
	 * Create a segment merger
	 * 
	 * @param dataset the dataset to update
	 * @param seg0Id  the first segment to merge
	 * @param seg1Id  the second segment to merge
	 */
	public SegmentUnmergeMethod(IAnalysisDataset dataset, @NonNull UUID segId) {
		super(dataset);
		this.segId = segId;
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
		LOGGER.fine("Requested unmerge of segment " + segId + " in dataset " + dataset.getName());

		if (!dataset.isRoot()) {
			LOGGER.fine("Cannot unmerge segments in a virtual collection");
			return;
		}

		// Don't mess with a broken dataset
		if (!dv.validate(dataset)) {
			LOGGER.warning(SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL);
			LOGGER.warning("Cancelling unmerge");
			return;
		}

		ISegmentedProfile medianProfile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
						Stats.MEDIAN);

		IProfileSegment seg = medianProfile.getSegment(segId);

		if (!seg.hasMergeSources()) {
			LOGGER.warning("Segment is not a merge; cannot unmerge");
			return;
		}

		// Unmerge in the dataset
		dataset.getCollection().getProfileManager().unmergeSegments(segId);
		fireProgressEvent();

		// Unmerge children
		for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
			child.getCollection().getProfileManager().unmergeSegments(segId);
			fireProgressEvent();
		}

	}

}
