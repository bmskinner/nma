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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This class is designed to simplify operations on cell collections involving
 * copying and refreshing of ProfileCollections and ProfileAggregates. It
 * handles movement of tag indexes within the median and the nuclei
 * 
 * @author bms41
 *
 */
public class ProfileManager {

	private static final Logger LOGGER = Logger.getLogger(ProfileManager.class.getName());
	private final ICellCollection collection;

	public ProfileManager(final ICellCollection collection) {
		this.collection = collection;
	}

	/**
	 * Update the given tag in each nucleus of the collection to the index with a
	 * best fit of the profile to the given median profile
	 * 
	 * @param lm     the landmark to fit
	 * @param type   the profile type to fit against
	 * @param median the template profile to offset against
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 * @throws
	 */
	public void updateLandmarkToMedianBestFit(@NonNull Landmark lm, @NonNull ProfileType type, @NonNull IProfile median)
			throws MissingProfileException, ProfileException, MissingLandmarkException, IndexOutOfBoundsException,
			ComponentCreationException {

		for (Nucleus n : collection.getNuclei()) {
			if (n.isLocked())
				continue;

			// Get the nucleus profile starting at the landmark
			// Find the best offset needed to make it match the median profile
			int offset = n.getProfile(type, lm).findBestFitOffset(median);

			// Update the landmark position to the original index plus the offset
			n.setLandmark(lm, n.wrapIndex(n.getBorderIndex(lm) + offset));

			// Update any stats that are based on orientation
			if (lm.equals(Landmark.TOP_VERTICAL) || lm.equals(Landmark.BOTTOM_VERTICAL)) {
				n.clearMeasurements();
				setOpUsingTvBv(n);
			}
		}
	}

	/**
	 * Add the given offset to each of the profile types in the ProfileCollection
	 * except for the frankencollection
	 * 
	 * @param lm    the landmark to change
	 * @param index the index to set the landmark to in
	 */
	public void updateLandmarkInProfileCollection(@NonNull Landmark lm, int index) {
		// check the index for wrapping - observed problem when OP==RP in
		// rulesets
		index = CellularComponent.wrapIndex(index, collection.getMedianArrayLength());
		collection.getProfileCollection().setLandmark(lm, index);
	}

	/**
	 * Use the collection's ruleset to calculate the positions of the top and bottom
	 * verticals in the median profile, and assign these to the nuclei
	 * 
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 */
	public void calculateTopAndBottomVerticals() throws MissingProfileException, ProfileException,
			MissingLandmarkException, IndexOutOfBoundsException, ComponentCreationException {

		try {
			int topIndex = ProfileIndexFinder.identifyIndex(collection, Landmark.TOP_VERTICAL);
			int btmIndex = ProfileIndexFinder.identifyIndex(collection, Landmark.BOTTOM_VERTICAL);

			updateLandmarkInProfileCollection(Landmark.TOP_VERTICAL, topIndex);
			updateLandmarkInProfileCollection(Landmark.BOTTOM_VERTICAL, btmIndex);

		} catch (NoDetectedIndexException e) {
			LOGGER.fine("Cannot find TV or BV in median profile");
			return;
		}

		IProfile topMedian = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Landmark.TOP_VERTICAL,
				Stats.MEDIAN);

		IProfile btmMedian = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Landmark.BOTTOM_VERTICAL,
				Stats.MEDIAN);
		updateLandmarkToMedianBestFit(Landmark.TOP_VERTICAL, ProfileType.ANGLE, topMedian);
		updateLandmarkToMedianBestFit(Landmark.BOTTOM_VERTICAL, ProfileType.ANGLE, btmMedian);
	}

	/**
	 * Copy the tag index from cells in the given source collection to cells with
	 * the same ID in this collection. This is intended to be use to ensure tag
	 * indexes are consistent between cells after a collection has been duplicated
	 * (e.g. after a merge of datasets)
	 * 
	 * @param source the collection to take tag indexes from
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws IndexOutOfBoundsException
	 */
	public void copyTagIndexesToCells(@NonNull ICellCollection source)
			throws IndexOutOfBoundsException, MissingProfileException, MissingLandmarkException, ProfileException {
		for (Nucleus n : collection.getNuclei()) {
			if (!source.contains(n))
				continue;

			Nucleus template = source.getNucleus(n.getID()).get();

			Map<Landmark, Integer> tags = template.getLandmarks();
			for (Entry<Landmark, Integer> entry : tags.entrySet()) {

				// RP should never change in re-segmentation, so don't
				// affect it here. This would risk moving RP off a
				// segment boundary
				if (entry.getKey().equals(Landmark.REFERENCE_POINT))
					continue;
				n.setLandmark(entry.getKey(), entry.getValue());
			}
		}
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
	public void updateLandmark(@NonNull Landmark lm, int index) throws ProfileException, MissingLandmarkException,
			MissingProfileException, IndexOutOfBoundsException, ComponentCreationException {

		if (Landmark.REFERENCE_POINT.equals(lm)) {
			updateCoreBorderTagIndex(lm, index);
		} else {
			updateExtendedBorderTagIndex(lm, index);
		}
	}

	/**
	 * Create a new landmark in a collection. The landmark is placed at the RP by
	 * default.
	 * 
	 * @param lm the landmark to create
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws IndexOutOfBoundsException
	 */
	private void createNewLandmark(@NonNull Landmark lm)
			throws IndexOutOfBoundsException, MissingProfileException, MissingLandmarkException, ProfileException {
		LOGGER.finer("Landmark does not exist and will be created in each nucleus");
		for (Nucleus n : collection.getNuclei()) {
			n.setLandmark(lm, n.getBorderIndex(Landmark.REFERENCE_POINT));
		}
		if (collection.hasConsensus())
			collection.getRawConsensus().setLandmark(lm,
					collection.getRawConsensus().getBorderIndex(Landmark.REFERENCE_POINT));
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
	private boolean canUpdateLandmarkIndexToExistingLandmark(@NonNull Landmark lm, int newIndex)
			throws MissingLandmarkException, IndexOutOfBoundsException, MissingProfileException, ProfileException,
			ComponentCreationException {
		List<Landmark> tags = collection.getProfileCollection().getLandmarks();
		for (Landmark existingTag : tags) {
			if (existingTag.equals(lm))
				continue;
			int existingTagIndex = collection.getProfileCollection().getLandmarkIndex(existingTag);
			if (newIndex == existingTagIndex) {
				updateLandmarkInProfileCollection(lm, newIndex);

				// update nuclei - allow possible parallel processing
				for (Nucleus n : collection.getNuclei()) {
					int existingIndex = n.getBorderIndex(existingTag);
					n.setLandmark(lm, existingIndex);
					if (lm.equals(Landmark.TOP_VERTICAL) || lm.equals(Landmark.BOTTOM_VERTICAL)) {
						n.clearMeasurements();
						setOpUsingTvBv(n);
					}
				}

				// Update consensus
				if (collection.hasConsensus()) {
					Nucleus n = collection.getRawConsensus();
					int existingIndex = n.getBorderIndex(existingTag);
					n.setLandmark(lm, existingIndex);
					setOpUsingTvBv(n);
				}

				// Update signals as needed
				collection.getSignalManager().recalculateSignalAngles();
				return true;
			}
		}
		return false;
	}

	/**
	 * Update the extended border tags that don't need resegmenting
	 * 
	 * @param lm    the extended tag to be updated
	 * @param index the new index of the tag in the median, relative to the current
	 *              RP
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 */
	private void updateExtendedBorderTagIndex(@NonNull Landmark lm, int index) throws ProfileException,
			MissingLandmarkException, MissingProfileException, IndexOutOfBoundsException, ComponentCreationException {

		try {
			// Check if the landmark exists
			collection.getProfileCollection().getLandmarkIndex(lm);
		} catch (MissingLandmarkException e) {
			createNewLandmark(lm);
		}

		// If the new index for the landmark is the same as another, set directly
		// and return
		if (canUpdateLandmarkIndexToExistingLandmark(lm, index))
			return;

		/*
		 * Otherwise, we need to do a best fit using profiles
		 * 
		 * Set the landmark in the median profile to the new index
		 */
		updateLandmarkInProfileCollection(lm, index);

		// Use the median profile to set the landmark in nuclei
		IProfile median = collection.getProfileCollection().getProfile(ProfileType.ANGLE, lm, Stats.MEDIAN);

		updateLandmarkToMedianBestFit(lm, ProfileType.ANGLE, median);

		/* Set the landmark in the consensus profile */
		if (collection.hasConsensus()) {
			Nucleus n = collection.getRawConsensus();
			int newIndex = n.getProfile(ProfileType.ANGLE).findBestFitOffset(median);
			n.setLandmark(lm, newIndex);
			setOpUsingTvBv(n);
		}

		// Update signals as needed
		collection.getSignalManager().recalculateSignalAngles();

	}

	/**
	 * If the TV and BV are present, move the OP to a more sensible position for
	 * signal angle measurement: the border point directly below the centre of mass.
	 * 
	 * @param n the nucleus to alter
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws IndexOutOfBoundsException
	 * @throws ComponentCreationException
	 */
	private void setOpUsingTvBv(@NonNull final Nucleus n) throws IndexOutOfBoundsException, MissingProfileException,
			MissingLandmarkException, ProfileException, ComponentCreationException {
		// also update the OP to be directly below the CoM in vertically oriented
		// nucleus
		if (n.hasLandmark(Landmark.TOP_VERTICAL) && n.hasLandmark(Landmark.BOTTOM_VERTICAL)) {
			Nucleus vertN = n.getOrientedNucleus();
			IPoint bottom = vertN.getBorderList().stream().filter(p -> p.getY() < vertN.getCentreOfMass().getY())
					.min(Comparator.comparing(p -> Math.abs(p.getX() - vertN.getCentreOfMass().getX()))).get();
			int newOp = vertN.getBorderIndex(bottom);
			n.setLandmark(Landmark.ORIENTATION_POINT, newOp);
		}
	}

	/**
	 * If a core border tag is moved, segment boundaries must be moved. It is left
	 * to calling classes to perform a resegmentation of the dataset.
	 * 
	 * @param tag   the core tag to be updated
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
	private void updateCoreBorderTagIndex(@NonNull Landmark tag, int index)
			throws MissingLandmarkException, ProfileException, MissingProfileException, UnsegmentedProfileException,
			IndexOutOfBoundsException, ComponentCreationException {

		LOGGER.finer("Updating core border tag index");

		// Get the median zeroed on the RP
		ISegmentedProfile oldMedian = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
				Landmark.REFERENCE_POINT, Stats.MEDIAN);

		moveRp(index, oldMedian);

		// Update signals as needed
		collection.getSignalManager().recalculateSignalAngles();
	}

	/**
	 * Move the RP index in nuclei. Update segments flanking the RP without moving
	 * any other segments.
	 * 
	 * @param newRpIndex the new index for the RP relative to the old RP
	 * @param oldMedian  the old median profile zeroed on the old RP
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 */
	private void moveRp(int newRpIndex, @NonNull ISegmentedProfile oldMedian) throws ProfileException,
			MissingProfileException, MissingLandmarkException, IndexOutOfBoundsException, ComponentCreationException {
		// This is the median we will use to update individual nuclei
		ISegmentedProfile newMedian = oldMedian.startFrom(newRpIndex);

		updateLandmarkToMedianBestFit(Landmark.REFERENCE_POINT, ProfileType.ANGLE, newMedian);

		// Rebuild the profile aggregate in the collection
		collection.getProfileCollection().calculateProfiles();
	}

	/**
	 * Get the number of segments in the regular profile of the collection. On error
	 * return 0
	 * 
	 * @return
	 */
	public int getSegmentCount() {
		IProfileCollection pc = collection.getProfileCollection();
		try {
			return pc.getSegments(Landmark.REFERENCE_POINT).size();
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK, "Error getting segment count from collection " + collection.getName(), e);
			return 0;
		}
	}

	/**
	 * Regenerate the profile aggregate in each of the profile types of the
	 * collection. The length is set to the angle profile length. The zero index of
	 * the profile aggregate is the RP.
	 * 
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 * 
	 * @throws Exception
	 */
	public void recalculateProfileAggregates()
			throws ProfileException, MissingLandmarkException, MissingProfileException {
		collection.getProfileCollection().calculateProfiles();

	}

	/**
	 * Copy profile offsets from this collection to the destination and build the
	 * median profiles for all profile types. Also copy the segments from the
	 * regular angle profile onto all other profile types
	 * 
	 * @param destination the collection to update
	 * @throws ProfileException        if the copy fails
	 * @throws MissingProfileException
	 */
	public void copySegmentsAndLandmarksTo(@NonNull final ICellCollection destination)
			throws ProfileException, MissingProfileException {

		// Get the corresponding profile collection from the template
		IProfileCollection sourcePC = collection.getProfileCollection();
		try {
			List<IProfileSegment> segments = sourcePC.getSegments(Landmark.REFERENCE_POINT);
			if (segments.isEmpty())
				throw new ProfileException("No segments in profile of " + collection.getName());

			// Create a new profile collection for the destination, so profiles are
			// refreshed
			IProfileCollection destPC = destination.getProfileCollection();
			destPC.calculateProfiles();

			// Copy the tags from the source collection
			// Use proportional indexes to allow for a changed aggregate length
			// Note: only the RP must be at a segment boundary. Some mismatches may occur

			for (Landmark key : sourcePC.getLandmarks()) {
				double prop = sourcePC.getProportionOfIndex(key);
				int adj = destPC.getIndexOfProportion(prop);
				destPC.setLandmark(key, adj);
			}

			// Copy the segments, also adjusting the lengths using profile interpolation
			ISegmentedProfile sourceMedian = sourcePC.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT,
					Stats.MEDIAN);
			ISegmentedProfile interpolatedMedian = sourceMedian.interpolate(destination.getMedianArrayLength());

			destPC.setSegments(interpolatedMedian.getSegments());

			// Final sanity check - did the segment IDs get copied properly?
			List<IProfileSegment> newSegs;
			try {
				newSegs = destPC.getSegments(Landmark.REFERENCE_POINT);
			} catch (MissingLandmarkException e1) {
				LOGGER.warning("RP not found in destination collection");
				LOGGER.log(Loggable.STACK, "Error getting destination segments from RP", e1);
				return;
			}

			if (segments.size() != newSegs.size())
				throw new ProfileException("Segments are not consistent with the old profile: were " + segments.size()
						+ ", now " + newSegs.size());

			for (int i = 0; i < newSegs.size(); i++) {
				// Start and end points can change, but id and lock state should be consistent
				// Check ids are in correct order
				if (!segments.get(i).getID().equals(newSegs.get(i).getID())) {
					throw new ProfileException("Segment IDs are not consistent with the old profile");
				}

				// Check lock state preserved
				if (segments.get(i).isLocked() != newSegs.get(i).isLocked()) {
					throw new ProfileException("Segment lock state not consistent with the old profile");
				}
			}

		} catch (MissingLandmarkException e1) {
			LOGGER.warning("RP not found in source collection");
			LOGGER.log(Loggable.STACK, "Error getting segments from RP", e1);
		}
	}

	/**
	 * Set the lock state for the given segment across the collection
	 * 
	 * @param segId
	 * @param lockState
	 */
	public void setLockOnSegment(@NonNull UUID segId, boolean lockState) {
		collection.getNuclei().forEach(n -> n.setSegmentStartLock(lockState, segId));
	}

	/**
	 * Set the lock on the start index of all segments of all profile types in all
	 * nuclei of the collection
	 * 
	 * @param b the segment lock state for all segments
	 */
	public void setLockOnAllNucleusSegments(boolean b) {
		List<UUID> ids = collection.getProfileCollection().getSegmentIDs();
		collection.getNuclei().forEach(n -> ids.forEach(segID -> n.setSegmentStartLock(b, segID)));
	}

	/**
	 * Update the start index of a segment in the angle profile of the given cell.
	 * 
	 * @param cell  the cell to alter
	 * @param id    the segment id
	 * @param index the new start index of the segment
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	public void updateCellSegmentStartIndex(@NonNull ICell cell, @NonNull UUID id, int index)
			throws ProfileException, MissingComponentException {

		Nucleus n = cell.getPrimaryNucleus();
		ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);

		IProfileSegment seg = profile.getSegment(id);

		int startPos = seg.getStartIndex();
		int newStart = index;
		int newEnd = seg.getEndIndex();

		int rawOldIndex = n.getIndexRelativeTo(Landmark.REFERENCE_POINT, startPos);

		try {
			if (profile.update(seg, newStart, newEnd)) {
				n.setSegments(profile.getSegments());

				/*
				 * Check the landmarks - if they overlap the old index replace them.
				 */
				int rawIndex = n.getIndexRelativeTo(Landmark.REFERENCE_POINT, index);

				Landmark landmarkToUpdate = n.getBorderTag(rawOldIndex);
				n.setLandmark(landmarkToUpdate, rawIndex);
				n.clearMeasurements();

			} else {
				LOGGER.warning(String.format("Updating %s start index from %s to %s failed", seg.getName(),
						seg.getStartIndex(), index));
			}
		} catch (SegmentUpdateException e) {
			LOGGER.warning(String.format("Updating %s start index from %s to %s failed", seg.getName(),
					seg.getStartIndex(), index));
		}
	}

	/**
	 * Update the given median profile index in the given segment to a new value
	 * 
	 * @param start whether the start index or end index of the segment is updated
	 * @param id    the id of the segment to update
	 * @param index the median profile index for the new segment start
	 * @throws UnsegmentedProfileException
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	public void updateMedianProfileSegmentStartIndex(UUID id, int index)
			throws ProfileException, MissingComponentException {

		ISegmentedProfile oldProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
				Landmark.REFERENCE_POINT, Stats.MEDIAN);

		IProfileSegment seg = oldProfile.getSegment(id);

		// Select the new endpoints for the segment
		int newStart = index;
		int newEnd = seg.getEndIndex();

		// if the segment start is on the RP, we must move the RP as well
		if (seg.getStartIndex() == collection.getProfileCollection().getLandmarkIndex(Landmark.REFERENCE_POINT)) {
			collection.getProfileCollection().setLandmark(Landmark.REFERENCE_POINT, index);
		}

		// Move the boundaries in the median profile.
		try {
			if (oldProfile.update(seg, newStart, newEnd)) {
				collection.getProfileCollection().setSegments(oldProfile.getSegments());
			} else {
				LOGGER.warning("Updating " + seg.getStartIndex() + " to index " + index + " failed");
			}
		} catch (SegmentUpdateException e) {
			LOGGER.log(Loggable.STACK, "Error updating segments", e);
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
	public boolean testSegmentsMergeable(IProfileSegment seg1, IProfileSegment seg2) throws MissingLandmarkException {

		if (!seg1.nextSegment().getID().equals(seg2.getID())) {
			return false;
		}

		// check the boundaries of the segment - we do not want to merge across the RP
		for (Landmark tag : DefaultLandmark.values(LandmarkType.CORE)) {

			// Find the position of the border tag in the median profile
			int tagIndex = collection.getProfileCollection().getLandmarkIndex(tag);
			if (seg1.getEndIndex() == tagIndex || seg2.getStartIndex() == tagIndex) {
				return false;
			}
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
	 * @throws UnsegmentedProfileException if the median profile is not segmented
	 * @throws ProfileException            if the update fails
	 * @throws MissingComponentException
	 */
	public void mergeSegments(@NonNull UUID seg1, @NonNull UUID seg2, @NonNull UUID newID)
			throws ProfileException, MissingComponentException {
		// Note - we can't do the root check here. It must be at the segmentation
		// handler level
		// otherwise updating child datasets to match a root will fail

		ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
				Landmark.REFERENCE_POINT, Stats.MEDIAN);

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
		for (Nucleus n : collection.getNuclei()) {
			boolean wasLocked = n.isLocked();
			n.setLocked(false); // Merging segments is not destructive
			mergeSegments(n, seg1, seg2, newID);
			n.setLocked(wasLocked);
		}

		/* Update the consensus if present */
		if (collection.hasConsensus()) {
			Nucleus n = collection.getRawConsensus();
			mergeSegments(n, seg1, seg2, newID);
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
	 * @throws MissingComponentException
	 */
	private void mergeSegments(@NonNull Taggable p, @NonNull UUID seg1, @NonNull UUID seg2, @NonNull UUID newID)
			throws ProfileException, MissingComponentException {
		ISegmentedProfile profile = p.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);

		// Only try the merge if both segments are present in the profile
		if (!profile.hasSegment(seg1))
			throw new ProfileException(p.getClass().getSimpleName() + ":" + p.getID()
					+ " profile does not have segment 1 with ID " + seg1);

		if (!profile.hasSegment(seg2))
			throw new ProfileException(p.getClass().getSimpleName() + ":" + p.getID()
					+ " profile does not have segment 2 with ID " + seg2);

		profile.mergeSegments(seg1, seg2, newID);
		p.setSegments(profile.getSegments());
	}

	/**
	 * Split the given segment into two segments. The split is made at the given
	 * index
	 * 
	 * @param seg    the segment to split
	 * @param newID1 the id for the first new segment. Can be null.
	 * @param newID2 the id for the second new segment. Can be null.
	 * @return
	 * @throws UnsegmentedProfileException
	 * @throws ProfileException
	 * @throws MissingComponentException   if the reference point tag is missing, or
	 *                                     the segment is missing
	 */
	public boolean splitSegment(@NonNull IProfileSegment seg, @NonNull UUID newID1, @NonNull UUID newID2)
			throws ProfileException, UnsegmentedProfileException, MissingComponentException {

		ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
				Landmark.REFERENCE_POINT, Stats.MEDIAN);

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
		if (!isCollectionSplittable(seg.getID(), proportion)) {
			LOGGER.warning("Segment cannot be split: not all nuclei have splittable segment");
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
			for (Nucleus n : collection.getNuclei()) {
				splitNucleusSegment(n, seg.getID(), proportion, newID1, newID2);
			}
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
	 * @throws MissingComponentException
	 */
	private void splitNucleusSegment(@NonNull Nucleus n, @NonNull UUID segId, double proportion, @NonNull UUID newId1,
			@NonNull UUID newId2) throws ProfileException, MissingComponentException {
		boolean wasLocked = n.isLocked();
		n.setLocked(false); // not destructive
		splitSegment(n, segId, proportion, newId1, newId2);
		n.setLocked(wasLocked);
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
	 * @throws MissingComponentException
	 * @throws UnsegmentedProfileException
	 */
	private boolean isCollectionSplittable(@NonNull UUID id, double proportion)
			throws ProfileException, MissingComponentException, UnsegmentedProfileException {

		ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
				Landmark.REFERENCE_POINT, Stats.MEDIAN);

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

		if (collection.isReal()) {
			return collection.getNuclei().parallelStream().allMatch(n -> isSplittable(n, id, proportion));
		}
		return true;
	}

	private boolean isSplittable(Taggable t, UUID id, double proportion) {

		try {
			ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
			IProfileSegment nSeg = profile.getSegment(id);
			int targetIndex = nSeg.getProportionalIndex(proportion);
			return profile.isSplittable(id, targetIndex);
		} catch (MissingComponentException | ProfileException e) {
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
	 * @throws MissingComponentException
	 */
	private void splitSegment(@NonNull Taggable t, @NonNull UUID idToSplit, double proportion, @NonNull UUID newID1,
			@NonNull UUID newID2) throws ProfileException, MissingComponentException {

		ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		IProfileSegment nSeg = profile.getSegment(idToSplit);

		int targetIndex = nSeg.getProportionalIndex(proportion);
		profile.splitSegment(nSeg, targetIndex, newID1, newID2);
		t.setSegments(profile.getSegments());

	}

	/**
	 * Unmerge the given segment into two segments
	 * 
	 * @param seg the segment to unmerge
	 * @return
	 * @throws UnsegmentedProfileException
	 * @throws ProfileException
	 * @throws MissingComponentException
	 */
	public void unmergeSegments(@NonNull UUID segId)
			throws ProfileException, UnsegmentedProfileException, MissingComponentException {

		ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
				Landmark.REFERENCE_POINT, Stats.MEDIAN);

		// Get the segments to merge
		IProfileSegment test = medianProfile.getSegment(segId);
		if (!test.hasMergeSources()) {
			LOGGER.fine("Segment has no merge sources - cannot unmerge");
			return;
		}

		// unmerge the two segments in the median - this is only a copy of the profile
		// collection
		medianProfile.unmergeSegment(segId);

		// put the new segment pattern back with the appropriate offset
		collection.getProfileCollection().setSegments(medianProfile.getSegments());

		/*
		 * With the median profile segments unmerged, also unmerge the segments in the
		 * individual nuclei
		 */
		if (collection.isReal()) {
			for (Nucleus n : collection.getNuclei()) {
				boolean wasLocked = n.isLocked();
				n.setLocked(false);
				unmergeSegments(n, segId);
				n.setLocked(wasLocked);
			}
		}

		/* Update the consensus if present */
		if (collection.hasConsensus()) {
			Nucleus n = collection.getRawConsensus();
			unmergeSegments(n, segId);
		}
	}

	private void unmergeSegments(@NonNull Taggable t, @NonNull UUID id)
			throws ProfileException, MissingComponentException {
		ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
		profile.unmergeSegment(id);
		t.setSegments(profile.getSegments());
	}

}
