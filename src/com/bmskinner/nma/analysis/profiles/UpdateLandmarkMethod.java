package com.bmskinner.nma.analysis.profiles;

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.profiles.UnsegmentedProfileException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.stats.Stats;

/**
 * Update landmarks in a dataset
 * 
 * @author bs19022
 *
 */
public class UpdateLandmarkMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(UpdateLandmarkMethod.class.getName());

	private DatasetValidator dv = new DatasetValidator();

	private final Landmark lm;
	private final int newIndex;

	/**
	 * Create a landmark update
	 * 
	 * @param dataset  the dataset to update
	 * @param lm       the landmark to update
	 * @param newIndex the new index in the median profile
	 */
	public UpdateLandmarkMethod(@NonNull IAnalysisDataset dataset, @NonNull Landmark lm,
			int newIndex) {
		super(dataset);
		this.lm = lm;
		this.newIndex = newIndex;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();

		if (!dv.validate(dataset))
			throw new AnalysisMethodException(
					"Unable to validate dataset after moving landmark '" + lm + "': "
							+ dv.getSummary() + "\n"
							+ dv.getErrors());
		return new DefaultAnalysisResult(dataset);
	}

	private void run() throws MissingLandmarkException, MissingProfileException,
			IndexOutOfBoundsException, ProfileException, ComponentCreationException {
		Landmark rp = dataset.getCollection().getRuleSetCollection()
				.getLandmark(OrientationMark.REFERENCE).get();

		if (dataset.getCollection().isVirtual() && rp.equals(lm)) {
			LOGGER.warning("Cannot update core border tag for a child dataset");
			return;
		}

		LOGGER.fine(String.format("Updating '%s' to index %d in '%s'", lm, newIndex,
				dataset.getName()));

		// Try updating to an existing tag index. If this
		// succeeds, do nothing else
		if (couldUpdateLandmarkToExistingInDataset(lm, newIndex))
			return;

		// Otherwise, find the best fit for each child dataset
		double prop = dataset.getCollection().getProfileCollection()
				.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
				.getFractionOfIndex(newIndex);

		updateLandmark(dataset.getCollection(), newIndex);
		fireProgressEvent();

		for (IAnalysisDataset child : dataset.getAllChildDatasets()) {

			// Update each child median profile to the same proportional index
			int childIndex = child.getCollection().getProfileCollection()
					.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
					.getIndexOfFraction(prop);

			LOGGER.fine(String.format("Updating '%s' to index %d in '%s'", lm, childIndex,
					child.getName()));

			updateLandmark(child.getCollection(), childIndex);
			fireProgressEvent();
		}

	}

	/**
	 * If a landmark is to be updated to an index with an existing tag, don't
	 * perform alignments; just set the landmark to the same index directly. The
	 * user is expecting the landmark to lie at the same index in every nucleus.
	 * 
	 * @param landmark the tag to update
	 * @param index    the new index for the landmark
	 * @return
	 * @throws MissingLandmarkException
	 * @throws IndexOutOfBoundsException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 * @throws ComponentCreationException
	 */
	private synchronized boolean couldUpdateLandmarkToExistingInDataset(Landmark tag, int index)
			throws MissingLandmarkException, MissingProfileException, ProfileException,
			IndexOutOfBoundsException,
			ComponentCreationException {
		List<OrientationMark> tags = dataset.getCollection().getProfileCollection()
				.getOrientationMarks();
		for (OrientationMark existingTag : tags) {
			if (existingTag.equals(tag))
				continue;
			int existingTagIndex = dataset.getCollection().getProfileCollection()
					.getLandmarkIndex(existingTag);
			if (index == existingTagIndex) {
				updateLandmark(dataset.getCollection(), existingTagIndex);
				for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
					updateLandmark(child.getCollection(), existingTagIndex);
				}
				return true;
			}
		}
		return false;
	}

	/**
	 * When updating a landmark index, check if the index matches another landmark
	 * in the median profile. If so, we can skip profile best fits and just set
	 * directly in each nucleus
	 * 
	 * @param lm       the landmark to set
	 * @param newIndex the new landmark index in the median profile
	 * @throws MissingLandmarkException
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws IndexOutOfBoundsException
	 * @throws ComponentCreationException
	 */
	private boolean canUpdateLandmarkIndexToExistingLandmark(@NonNull ICellCollection collection,
			@NonNull Landmark lm,
			int newIndex)
			throws MissingLandmarkException, IndexOutOfBoundsException, MissingProfileException,
			ProfileException {

		List<OrientationMark> tags = collection.getProfileCollection().getOrientationMarks();
		for (OrientationMark existingTag : tags) {
			if (existingTag.equals(lm))
				continue;
			int existingTagIndex = collection.getProfileCollection().getLandmarkIndex(existingTag);
			if (newIndex == existingTagIndex) {
				int newIndexOffset = CellularComponent.wrapIndex(newIndex,
						collection.getMedianArrayLength());
				collection.getProfileCollection().setLandmark(lm, newIndexOffset);

				// update nuclei - allow possible parallel processing
				for (Nucleus n : collection.getNuclei()) {
					int existingIndex = n.getBorderIndex(existingTag);
					n.setLandmark(lm, existingIndex);
					n.clearMeasurements();

				}

				// Update consensus
				if (collection.hasConsensus()) {
					Nucleus n = collection.getRawConsensus();
					int existingIndex = n.getBorderIndex(existingTag);
					n.setLandmark(lm, existingIndex);
				}

				// Update signals as needed
				collection.getSignalManager().recalculateSignalAngles();
				return true;
			}
		}
		return false;
	}

	/**
	 * Update the location of the given border tag within the profile
	 * 
	 * @param lm    the landmark to be updated
	 * @param index the new index of the landmark in the median, with the RP at
	 *              index 0
	 * @throws MissingLandmarkException
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 */
	private void updateLandmark(@NonNull ICellCollection collection, int index)
			throws ProfileException, MissingLandmarkException, MissingProfileException,
			IndexOutOfBoundsException, ComponentCreationException {

		Landmark rp = collection.getRuleSetCollection().getLandmark(OrientationMark.REFERENCE)
				.orElseThrow(MissingLandmarkException::new);

		if (rp.equals(lm)) {
			updateReferencePointIndex(collection, index);
		} else {
			updateNonReferencePoint(collection, index);
		}
	}

	/**
	 * If the RP is moved, segment boundaries must be moved. It is left to calling
	 * classes to perform a resegmentation of the dataset.
	 * 
	 * @param index the new index of the tag in the median, relative to the current
	 *              RP
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws UnsegmentedProfileException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 * @throws SegmentUpdateException
	 */
	private void updateReferencePointIndex(@NonNull ICellCollection collection, int index)
			throws MissingLandmarkException, ProfileException, MissingProfileException,
			IndexOutOfBoundsException, ComponentCreationException {

		// Get the median zeroed on the RP
		ISegmentedProfile oldMedian = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		// This is the median we will use to update individual nuclei
		ISegmentedProfile newMedian = oldMedian.startFrom(index);

		Landmark rp = collection.getRuleSetCollection().getLandmark(OrientationMark.REFERENCE)
				.orElseThrow(MissingLandmarkException::new);

		// Update the nuclei to match the new median
		updateNucleiLandmarkToBestFit(collection, rp, ProfileType.ANGLE, newMedian);

		// Ensure landmarks in the profile collection are offset to preserve their
		// positions

		// TODO: consider what happens if profiles are reversed

		for (Landmark l : collection.getProfileCollection().getLandmarks()) {
			if (l.equals(rp))
				continue;

			// Index relative to the old reference point
			int oldIndex = collection.getProfileCollection().getLandmarkIndex(l);

			int newIndex = CellularComponent
					.wrapIndex(oldIndex - index, collection.getMedianArrayLength());

			LOGGER.fine(String.format("Moving %s from %d to %d", l, oldIndex, newIndex));
			collection.getProfileCollection().setLandmark(l, newIndex);
		}

		// Rebuild the profile aggregate in the collection using the existing landmarks
		collection.getProfileCollection().calculateProfiles();

		// Update signals as needed
		collection.getSignalManager().recalculateSignalAngles();
	}

	/**
	 * Update the landmarks other than the RP
	 * 
	 * @param lm    the landmark to be updated
	 * @param index the new index of the tag in the median, relative to the current
	 *              RP
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 */
	private void updateNonReferencePoint(@NonNull ICellCollection collection,
			int index)
			throws ProfileException,
			MissingLandmarkException, MissingProfileException, IndexOutOfBoundsException,
			ComponentCreationException {

		// If the new index for the landmark is the same as another, set directly
		// and return
		if (canUpdateLandmarkIndexToExistingLandmark(collection, lm, index))
			return;

		/*
		 * Otherwise, we need to do a best fit using profiles
		 * 
		 * Set the landmark in the median profile to the new index
		 */
		collection.getProfileCollection().setLandmark(lm,
				CellularComponent.wrapIndex(index, collection.getMedianArrayLength()));

		// Use the median profile to set the landmark in nuclei
		IProfile median = collection.getProfileCollection().getProfile(ProfileType.ANGLE, lm,
				Stats.MEDIAN);

		updateNucleiLandmarkToBestFit(collection, lm, ProfileType.ANGLE, median);

		/* Set the landmark in the consensus profile */
		if (collection.hasConsensus()) {
			Nucleus n = collection.getRawConsensus();
			int newIndex = n.getProfile(ProfileType.ANGLE).findBestFitOffset(median);
			n.setLandmark(lm, newIndex);
		}

		// Update signals as needed
		collection.getSignalManager().recalculateSignalAngles();

	}

	/**
	 * Update the given tag in each nucleus of the collection to the index with a
	 * best fit of the profile to the given median profile
	 * 
	 * @param lm       the landmark to fit
	 * @param type     the profile type to fit against
	 * @param template the template profile to offset against
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 * @throws
	 */
	private void updateNucleiLandmarkToBestFit(@NonNull ICellCollection collection,
			@NonNull Landmark lm, @NonNull ProfileType type, @NonNull IProfile template)
			throws MissingProfileException, ProfileException, MissingLandmarkException,
			IndexOutOfBoundsException, ComponentCreationException {

		for (Nucleus n : collection.getNuclei()) {
			if (n.isLocked())
				continue;

			// Get the nucleus profile starting at the landmark
			// Find the best offset needed to make it match the median profile
			int offset = n.getProfile(type, lm).findBestFitOffset(template);

			// Update the landmark position to the original index plus the offset
			n.setLandmark(lm, n.wrapIndex(n.getBorderIndex(lm) + offset));

			// Update measurements - many are based on orientation
			n.clearMeasurements();
		}

		// Update the consensus nucleus
		if (collection.hasConsensus()) {
			// Get the nucleus profile starting at the landmark
			// Find the best offset needed to make it match the median profile
			int offset = collection.getRawConsensus().getProfile(type, lm)
					.findBestFitOffset(template);

			// Update the landmark position to the original index plus the offset
			collection.getRawConsensus().setLandmark(lm, collection.getRawConsensus()
					.wrapIndex(collection.getRawConsensus().getBorderIndex(lm) + offset));

		}
	}
}
