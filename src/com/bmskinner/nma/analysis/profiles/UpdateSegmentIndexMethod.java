package com.bmskinner.nma.analysis.profiles;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

/**
 * Update the start index of the given segment to the given index in the median
 * profile, and update individual nuclei to match. Removes any segment merge
 * sources to prevent scaling issues.
 * 
 * @param id
 * @param index
 * @throws ProfileException
 * @throws MissingComponentException
 * @throws MissingProfileException
 * @throws SegmentUpdateException
 * @throws Exception
 */
public class UpdateSegmentIndexMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(UpdateSegmentIndexMethod.class.getName());

	private DatasetValidator dv = new DatasetValidator();

	private final UUID segmentId;
	private final int newIndex;

	/**
	 * Create a segment index update
	 * 
	 * @param dataset   the dataset to update
	 * @param segmentId the segment to update
	 * @param newIndex  the new index in the median profile
	 */
	public UpdateSegmentIndexMethod(@NonNull IAnalysisDataset dataset, @NonNull UUID segmentId,
			int newIndex) {
		super(dataset);
		this.segmentId = segmentId;
		this.newIndex = newIndex;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();

		if (!dv.validate(dataset))
			throw new AnalysisMethodException(
					"Unable to validate dataset after updating segment index: " + dv.getSummary()
							+ "\n"
							+ dv.getErrors());
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws MissingComponentException, ProfileException, SegmentUpdateException {
		LOGGER.fine(() -> "Requested update of segment %s to index %d in dataset %s".formatted(
				segmentId, newIndex, dataset.getName()));

//			Don't update segment boundaries at the reference point. This should only be performed by moving the RP directly
		IProfileSegment segToUpdate = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
						Stats.MEDIAN)
				.getSegment(segmentId);

		if (segToUpdate.getStartIndex() == dataset.getCollection().getProfileCollection()
				.getLandmarkIndex(OrientationMark.REFERENCE)) {
			LOGGER.warning("Cannot move segment boundary at reference point");
			return;
		}

		// Get the updated profile
		double prop = dataset.getCollection().getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
				.getFractionOfIndex(newIndex);

		// Update the median profile
		dataset.getCollection().getProfileManager().updateMedianProfileSegmentStartIndex(segmentId,
				newIndex);

		// Get the updated profile
		ISegmentedProfile newProfile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
						Stats.MEDIAN);

		// Apply the updated profile to the cells
		for (ICell c : dataset.getCollection()) {
			List<IProfileSegment> newSegs = IProfileSegment.scaleSegments(
					newProfile.getSegments(), c.getPrimaryNucleus().getBorderLength());
			IProfileSegment.linkSegments(newSegs);
			c.getPrimaryNucleus().setSegments(newSegs);
			fireProgressEvent();
		}

		// Update consensus if present
		if (dataset.getCollection().hasConsensus()) {
			Nucleus n = dataset.getCollection().getRawConsensus();
			List<IProfileSegment> newSegs = IProfileSegment.scaleSegments(
					newProfile.getSegments(), n.getBorderLength());
			IProfileSegment.linkSegments(newSegs);
			n.setSegments(newSegs);
			fireProgressEvent();
		}

		for (IAnalysisDataset child : dataset.getAllChildDatasets()) {

			// Update each child median profile to the same proportional
			// index

			int childIndex = child.getCollection().getProfileCollection()
					.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
					.getIndexOfFraction(prop);

			child.getCollection().getProfileManager().updateMedianProfileSegmentStartIndex(
					segmentId,
					childIndex);

			// Update consensus if present
			if (child.getCollection().hasConsensus()) {
				Nucleus n = child.getCollection().getRawConsensus();
				List<IProfileSegment> newSegs = IProfileSegment.scaleSegments(
						newProfile.getSegments(), n.getBorderLength());
				IProfileSegment.linkSegments(newSegs);
				n.setSegments(newSegs);
				fireProgressEvent();
			}

			fireProgressEvent();
		}
	}

}
