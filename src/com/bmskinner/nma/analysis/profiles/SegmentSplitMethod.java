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
 * Split a segment in a dataset collection and its children, as long as the
 * collection is real.
 * 
 * @author bs19022
 *
 */
public class SegmentSplitMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(SegmentUnmergeMethod.class.getName());
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

	private void run() throws ProfileException, MissingComponentException {
		if (!dataset.isRoot()) {
			LOGGER.fine(String.format("'%s': Cannot split segments in a virtual dataset",
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

		LOGGER.fine(String.format("Splitting segment %s in root '%s' into %s and %s", segId,
				dataset.getName(), newID1, newID2));
		boolean ok = dataset.getCollection().getProfileManager().splitSegment(seg, newID1,
				newID2);
		fireProgressEvent();

		if (ok) {
			// Child datasets should all be virtual
			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				LOGGER.fine(
						String.format("Splitting segment  %s in child '%s'", seg.getID(),
								child.getName()));
				boolean cOk = child.getCollection().getProfileManager().splitSegment(seg,
						newID1, newID2);
				fireProgressEvent();
				if (!cOk)
					LOGGER.warning(
							String.format("Splitting segment %s failed in child '%s'", seg.getID(),
									child.getName()));
			}
		} else {
			LOGGER.warning(String.format("Splitting segment in '%s' failed", dataset.getName()));
		}

	}

}
