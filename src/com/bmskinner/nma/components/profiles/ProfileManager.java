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
package com.bmskinner.nma.components.profiles;

import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.MissingLandmarkException;
import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

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
	 * Get the number of segments in the regular profile of the collection. On error
	 * return 0
	 * 
	 * @return
	 */
	public int getSegmentCount() {
		IProfileCollection pc = collection.getProfileCollection();
		try {
			return pc.getSegments(OrientationMark.REFERENCE).size();
		} catch (Exception e) {
			LOGGER.log(Loggable.STACK,
					"Error getting segment count from collection " + collection.getName(), e);
			return 0;
		}
	}

	/**
	 * Copy profile offsets from this collection to the destination and build the
	 * median profiles for all profile types. Also copy the segments from the
	 * regular angle profile onto all other profile types
	 * 
	 * @param destination the collection to update
	 * @throws ProfileException         if the copy fails
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 */
	public void copySegmentsAndLandmarksTo(@NonNull final ICellCollection destination)
			throws ProfileException, MissingProfileException, MissingLandmarkException {

		// Get the corresponding profile collection from the template
		IProfileCollection sourcePC = collection.getProfileCollection();

		List<IProfileSegment> segments = sourcePC.getSegments(OrientationMark.REFERENCE);
		if (segments.isEmpty())
			throw new ProfileException("No segments in profile of " + collection.getName());

		// Create a new profile collection for the destination, so profiles are
		// refreshed
		IProfileCollection destPC = destination.getProfileCollection();
		destPC.calculateProfiles();

		// Copy the tags from the source collection
		// Use proportional indexes to allow for a changed aggregate length
		// Note: only the RP must be at a segment boundary. Some mismatches may occur

		for (Landmark om : sourcePC.getLandmarks()) {
			double prop = sourcePC.getProportionOfIndex(om);
			int adj = destPC.getIndexOfProportion(prop);
			destPC.setLandmark(om, adj);
		}

		// Copy the segments, also adjusting the lengths using profile interpolation
		ISegmentedProfile interpolatedMedian = sourcePC
				.getSegmentedProfile(ProfileType.ANGLE, OrientationMark.REFERENCE, Stats.MEDIAN)
				.interpolate(destination.getMedianArrayLength());

		destPC.setSegments(interpolatedMedian.getSegments());

		// Final sanity check - did the segment IDs get copied properly?
		List<IProfileSegment> newSegs = destPC.getSegments(OrientationMark.REFERENCE);

		if (segments.size() != newSegs.size())
			throw new ProfileException(
					"Segments are not consistent with the old profile: were " + segments.size()
							+ ", now " + newSegs.size());

		for (int i = 0; i < newSegs.size(); i++) {
			// Start and end points can change, but id and lock state should be consistent
			// Check ids are in correct order
			if (!segments.get(i).getID().equals(newSegs.get(i).getID())) {
				throw new ProfileException(
						"Segment IDs are not consistent with the old profile");
			}

			// Check lock state preserved
			if (segments.get(i).isLocked() != newSegs.get(i).isLocked()) {
				throw new ProfileException(
						"Segment lock state not consistent with the old profile");
			}
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
		ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		IProfileSegment seg = profile.getSegment(id);

		int startPos = seg.getStartIndex();
		int newStart = index;
		int newEnd = seg.getEndIndex();

		int rawOldIndex = n.getIndexRelativeTo(OrientationMark.REFERENCE, startPos);

		try {
			if (profile.update(seg, newStart, newEnd)) {
				n.setSegments(profile.getSegments());

				/* Check the landmarks - if they overlap the old index replace them */
				int rawIndex = n.getIndexRelativeTo(OrientationMark.REFERENCE, index);
				for (Entry<OrientationMark, Integer> entry : n.getOrientationMarkMap().entrySet()) {
					if (entry.getValue().intValue() == rawOldIndex)
						n.setOrientationMark(entry.getKey(), rawIndex);
				}
				n.clearMeasurements();

			} else {
				LOGGER.warning(
						String.format("Updating %s start index from %s to %s failed", seg.getName(),
								seg.getStartIndex(), index));
			}
		} catch (SegmentUpdateException e) {
			LOGGER.warning(
					String.format("Updating %s start index from %s to %s failed: %s", seg.getName(),
							seg.getStartIndex(), index, e.getMessage()));
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
	 * @throws SegmentUpdateException
	 */
	public void updateMedianProfileSegmentStartIndex(UUID id, int index)
			throws ProfileException, MissingComponentException, SegmentUpdateException {

		ISegmentedProfile oldProfile = collection.getProfileCollection().getSegmentedProfile(
				ProfileType.ANGLE,
				OrientationMark.REFERENCE, Stats.MEDIAN);

		IProfileSegment seg = oldProfile.getSegment(id);

		// Remove merge sources from the template segment
		if (seg.hasMergeSources()) {
			seg.clearMergeSources();
			LOGGER.fine("Removed merge sources from " + collection.getName() + " segment " + id);
		}

		// If the previous segment to the template has merge sources, we also need to
		// remove them, since the previous segment may shrink
		if (seg.prevSegment().hasMergeSources()) {
			oldProfile.getSegment(seg.prevSegment().getID()).clearMergeSources();
			LOGGER.fine("Removed merge sources from " + collection.getName() + " segment "
					+ seg.prevSegment().getID());
		}

		// Select the new endpoints for the segment
		int newStart = index;
		int newEnd = seg.getEndIndex();

		// if the segment start is on the RP, we must move the RP as well
		if (seg.getStartIndex() == collection.getProfileCollection()
				.getLandmarkIndex(OrientationMark.REFERENCE)) {

			Landmark rp = collection.getRuleSetCollection().getLandmark(OrientationMark.REFERENCE)
					.orElseThrow(MissingLandmarkException::new);
			collection.getProfileCollection().setLandmark(rp, index);
		}

		// Move the boundaries in the median profile.
		if (oldProfile.update(seg, newStart, newEnd)) {
			collection.getProfileCollection().setSegments(oldProfile.getSegments());
		} else {
			LOGGER.warning(
					"Updating " + seg.getStartIndex() + " to index " + index + " failed");
		}

	}

}
