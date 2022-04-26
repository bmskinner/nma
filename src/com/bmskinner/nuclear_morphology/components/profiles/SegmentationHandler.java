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
package com.bmskinner.nuclear_morphology.components.profiles;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.DatasetValidator;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This coordinates updates to segmentation between datasets and their children.
 * When a UI request is made to update segmentation, this handler is responsible
 * for keeping all child datasets in sync
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class SegmentationHandler {

	private static final Logger LOGGER = Logger.getLogger(SegmentationHandler.class.getName());

	private static final String SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL = "Segments are out of sync with median";
	private final IAnalysisDataset dataset;

	/**
	 * Create with a dataset that can be adjusted
	 * 
	 * @param d
	 */
	public SegmentationHandler(final IAnalysisDataset d) {
		dataset = d;
	}

	/**
	 * Merge segments with the given IDs in this collection and its children, as
	 * long as the collection is real.
	 * 
	 * @param segID1 the segment ID to be merged
	 * @param segID2 the segment ID to be merged
	 * @throws MissingComponentException
	 * @throws ProfileException
	 */
	public synchronized void mergeSegments(@NonNull UUID segID1, @NonNull UUID segID2)
			throws ProfileException, MissingComponentException {

		if (!dataset.isRoot()) {
			LOGGER.fine("Cannot merge segments in a virtual collection");
			return;
		}

		// Don't mess with a broken dataset
		DatasetValidator dv = new DatasetValidator();
		if (!dv.validate(dataset)) {
			LOGGER.warning("Cancelling merge: " + SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL);
			return;
		}

		// Give the new merged segment a new ID
		final UUID newID = UUID.randomUUID();

		LOGGER.fine("Merging segments " + segID1 + " and " + segID2 + " in dataset " + dataset.getName()
				+ " to new segment " + newID);

		ISegmentedProfile medianProfile = dataset.getCollection().getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

		IProfileSegment seg1 = medianProfile.getSegment(segID1);
		IProfileSegment seg2 = medianProfile.getSegment(segID2);

		boolean ok = dataset.getCollection().getProfileManager().testSegmentsMergeable(seg1, seg2);

		if (ok) {
			dataset.getCollection().getProfileManager().mergeSegments(segID1, segID2, newID);

			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				child.getCollection().getProfileManager().mergeSegments(segID1, segID2, newID);
			}
		} else {
			LOGGER.warning("Segments are not mergable");
		}

		if (!dv.validate(dataset)) {
			LOGGER.warning("Merging failed; resulting dataset did not validate");
			for (String s : dv.getErrors())
				LOGGER.warning(s);
		}
	}

	/**
	 * Unmerge segments with the given ID in this collection and its children, as
	 * long as the collection is real. Restore the original state on error.
	 * 
	 * @param segID the segment ID to be unmerged
	 */
	public synchronized void unmergeSegments(@NonNull final UUID segID) {

		LOGGER.fine("Requested unmerge of segment " + segID + " in dataset " + dataset.getName());

		if (!dataset.isRoot()) {
			LOGGER.fine("Cannot unmerge segments in a virtual collection");
			return;
		}

		// Don't mess with a broken dataset
		DatasetValidator dv = new DatasetValidator();
		if (!dv.validate(dataset)) {
			LOGGER.warning(SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL);
			LOGGER.warning("Canceling unmerge");
			return;
		}

		try {

			ISegmentedProfile medianProfile = dataset.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

			IProfileSegment seg = medianProfile.getSegment(segID);

			if (!seg.hasMergeSources()) {
				LOGGER.warning("Segment is not a merge; cannot unmerge");
				return;
			}

			// Unmerge in the dataset
			dataset.getCollection().getProfileManager().unmergeSegments(segID);

			// Unmerge children
			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
				child.getCollection().getProfileManager().unmergeSegments(segID);
			}

			if (!dv.validate(dataset)) {
				LOGGER.warning("Unmerging failed; resulting dataset did not validate");
				for (String s : dv.getErrors())
					LOGGER.warning(s);
			}

		} catch (ProfileException | MissingComponentException e) {
			LOGGER.log(Loggable.STACK, "Error unmerging segments", e);
		}
	}

	/**
	 * Split the segment with the given ID in this collection and its children, as
	 * long as the collection is real.
	 * 
	 * @param segID the segment ID to be split
	 */
	public synchronized void splitSegment(@NonNull UUID segID) {

		if (!dataset.isRoot()) {
			LOGGER.fine("Cannot split segments in a virtual collection");
			return;
		}

		// Don't mess with a broken dataset
		DatasetValidator dv = new DatasetValidator();
		if (!dv.validate(dataset)) {
			LOGGER.warning("Canceling segment split: " + SEGMENTS_ARE_OUT_OF_SYNC_WITH_MEDIAN_LBL);
			return;
		}

		try {

			ISegmentedProfile medianProfile = dataset.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

			IProfileSegment seg = medianProfile.getSegment(segID);

			UUID newID1 = UUID.randomUUID();
			UUID newID2 = UUID.randomUUID();

			LOGGER.fine("Splitting segment " + segID + " in root dataset " + dataset.getName() + " into new segments "
					+ newID1 + " and " + newID2);
			boolean ok = dataset.getCollection().getProfileManager().splitSegment(seg, newID1, newID2);

			if (ok) {
				// Child datasets should all be virtual
				for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
					LOGGER.fine("Splitting segment in " + child.getName());
					boolean cOk = child.getCollection().getProfileManager().splitSegment(seg, newID1, newID2);
					if (!cOk)
						LOGGER.warning("Splitting segment failed for child dataset " + child.getName());
				}
			} else {
				LOGGER.warning("Splitting segment cancelled");
			}

		} catch (ProfileException | MissingComponentException e) {
			LOGGER.warning("Error splitting segments");
			LOGGER.log(Loggable.STACK, e.getMessage(), e);

		}

		if (!dv.validate(dataset)) {
			LOGGER.warning("Splitting segment failed; resulting dataset did not validate");
			for (String s : dv.getErrors())
				LOGGER.warning(s);
		}
	}

	/**
	 * Update the start index of the given segment to the given index in the median
	 * profile, and update individual nuclei to match.
	 * 
	 * @param id
	 * @param index
	 * @throws Exception
	 */
	public synchronized void updateSegmentStartIndexAction(UUID id, int index) {

		LOGGER.fine("Requested update of segment " + id + " to index " + index + " in dataset " + dataset.getName());

		try {

			double prop = dataset.getCollection().getProfileCollection()
					.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN).getFractionOfIndex(index);

			// Update the median profile
			dataset.getCollection().getProfileManager().updateMedianProfileSegmentStartIndex(id, index);

			// Get the updated profile
			ISegmentedProfile newProfile = dataset.getCollection().getProfileCollection()
					.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN);

			// Apply the updated profile to the cells
			for (ICell c : dataset.getCollection()) {
				c.getPrimaryNucleus().setSegments(IProfileSegment.scaleSegments(newProfile.getSegments(),
						c.getPrimaryNucleus().getBorderLength()));
			}

			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {

				// Update each child median profile to the same proportional
				// index

				int childIndex = child.getCollection().getProfileCollection()
						.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
						.getIndexOfFraction(prop);

				child.getCollection().getProfileManager().updateMedianProfileSegmentStartIndex(id, childIndex);
			}

			// Lock all the segments except the one to change
			dataset.getCollection().getProfileManager().setLockOnAllNucleusSegments(true);
			dataset.getCollection().getProfileManager().setLockOnSegment(id, false);

		} catch (ProfileException | MissingComponentException e) {
			LOGGER.warning("Error updating index of segments");
			LOGGER.log(Loggable.STACK, e.getMessage(), e);

		}

	}

	/**
	 * Update the border tag in the median profile to the given index, and update
	 * individual nuclei to match.
	 * 
	 * @param tag
	 * @param newTagIndex
	 */
	public synchronized void setLandmark(OrientationMark tag, int index) {

		if (tag == null)
			throw new IllegalArgumentException("Tag is null");

		LOGGER.fine("Requested " + tag + " set to index " + index + " in dataset " + dataset.getName());

//		if (dataset.getCollection().isVirtual()) {
//			LOGGER.fine("Cannot update tag in virtual collection");
//			return;
//		}
		try {
			// Try updating to an existing tag index. If this
			// succeeds, do nothing else
			if (couldUpdateTagToExistingTagIndex(tag, index))
				return;

			// Otherwise, find the best fit for each child dataset
			double prop = dataset.getCollection().getProfileCollection()
					.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN).getFractionOfIndex(index);

			dataset.getCollection().getProfileManager().updateLandmark(tag, index);

			for (IAnalysisDataset child : dataset.getAllChildDatasets()) {

				// Update each child median profile to the same proportional index
				int childIndex = child.getCollection().getProfileCollection()
						.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
						.getIndexOfFraction(prop);

				child.getCollection().getProfileManager().updateLandmark(tag, childIndex);
			}

		} catch (IndexOutOfBoundsException | ProfileException | MissingLandmarkException | MissingProfileException e) {
			LOGGER.warning("Unable to update border tag index");
			LOGGER.log(Loggable.STACK, "Profiling error", e);
		} catch (Exception e) {
			LOGGER.warning("Unexpected error");
			LOGGER.log(Loggable.STACK, "Unexpected error", e);
		}
	}

	/**
	 * If a tag is to be updated to an index with an existing tag, don't perform
	 * alignments; just set the tag to the same index directly. The user is
	 * expecting the tags to lie at the same index in every nucleus.
	 * 
	 * @param tag   the tag to update
	 * @param index the new index for the tag
	 * @return
	 * @throws MissingLandmarkException
	 * @throws IndexOutOfBoundsException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 * @throws ComponentCreationException
	 */
	private synchronized boolean couldUpdateTagToExistingTagIndex(OrientationMark tag, int index)
			throws MissingLandmarkException, MissingProfileException, ProfileException, IndexOutOfBoundsException,
			ComponentCreationException {
		List<OrientationMark> tags = dataset.getCollection().getProfileCollection().getOrientationMarks();
		for (OrientationMark existingTag : tags) {
			if (existingTag.equals(tag))
				continue;
			int existingTagIndex = dataset.getCollection().getProfileCollection().getLandmarkIndex(existingTag);
			if (index == existingTagIndex) {
				dataset.getCollection().getProfileManager().updateLandmark(tag, existingTagIndex);
				for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
					child.getCollection().getProfileManager().updateLandmark(tag, existingTagIndex);
				}
				return true;
			}
		}
		return false;
	}

}
