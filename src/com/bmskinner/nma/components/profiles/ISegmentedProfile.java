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
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingComponentException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

/**
 * This details a profile that can have segments applied to it.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface ISegmentedProfile extends IProfile {

    /**
     * Check if this profile contains segments
     * 
     * @return
     */
    boolean hasSegments();

    /**
     * Get a copy of the segments in this profile. The order will be the same as
     * the original segment list. No point type specifiers are available; if
     * you need to offset a profile, do it by a profile offset
     * 
     * @return
     */
    @NonNull List<IProfileSegment> getSegments();

    /**
     * Fetch the segment with the given id, or null if not present. Fetches the
     * actual segment, not a copy
     * 
     * @param id
     * @return
     * @throws MissingComponentException if there is no segment with the given id
     * @throws IllegalArgumentException if the id is null
     */
    @NonNull IProfileSegment getSegment(@NonNull UUID id) throws MissingComponentException;

    /**
     * Test if a segment with the given id is present within the profile, or
     * as a merge source of one of the segments in the profile
     * @param id the id
     * @return true if a segment is present with the id, false otherwise
     * @throws IllegalArgumentException if the id is null
     */
    boolean hasSegment(@NonNull UUID id);

    /**
     * Make this profile the length specified. Segments will be adjusted to the
     * closest proportional index that preserves minimum segment lengths. If segments
     * cannot be properly interpolated, a profile exception will be thrown.
     * 
     * @param newLength the new profile length
     * @return the interpolated profile
     * @throws ProfileException if the interpolation fails
     */
    @Override
    ISegmentedProfile interpolate(int newLength) throws ProfileException;

    /**
     * Fetch the segment list ordered to start from the segment with the given
     * id
     * 
     * @param id the segment id
     * @return
     * @throws Exception
     */
    List<IProfileSegment> getSegmentsFrom(@NonNull UUID id) throws MissingComponentException, ProfileException;

    /**
     * Get a copy of the segments in this profile, ordered from the zero index
     * of the profile. The first segment is the segment that starts with index zero or 
     * contains index zero at any index other than the end index.
     * 
     * @return
     */
    List<IProfileSegment> getOrderedSegments();

    /**
     * Get the segment with the given name.
     * @param name the segment name to find
     * @return the segment
     * @throws MissingComponentException if there is no segment with the given name
     * @deprecated since 1.14.0. Start replacing calls with indexes or UUIDs
     */
    @Deprecated
    IProfileSegment getSegment(@NonNull String name) throws MissingComponentException;

    /**
     * Get the given segment. Returns null if no segment is found. Gets the
     * actual segment, not a copy
     * 
     * @param name
     * @return
     * @throws ProfileException 
     */
    IProfileSegment getSegment(@NonNull IProfileSegment segment) throws ProfileException;


    /**
     * Get the segment containing the given index
     * 
     * @param index
     * @return
     */
    IProfileSegment getSegmentContaining(int index);

    /**
     * Replace the segments in the profile with the given list
     * 
     * @param segments
     */
    void setSegments(@NonNull List<IProfileSegment> segments);

    /**
     * Remove the segments from this profile
     * @throws ProfileException 
     */
    void clearSegments() throws ProfileException;

    /**
     * Get the names of the segments in the profile
     * 
     * @return
     * @deprecated since segments are referred to by id
     */
    @Deprecated
    List<String> getSegmentNames();

    /**
     * Get the names of the segments in the profile
     * 
     * @return
     */
    List<UUID> getSegmentIDs();

    /**
     * Get the number of segments in the profile
     * 
     * @return
     */
    int getSegmentCount();

    /**
     * Find the value displacement of the given segment in the profile. i.e the
     * difference between the start value and the end value
     * 
     * @param segment
     *            the segment to measure
     * @return the displacement, or 0 if the segment was not found
     */
    double getDisplacement(@NonNull IProfileSegment segment);

    /**
     * Test if the profile contains the given segment. Copies are ok, it checks
     * position, length and name.
     * 
     * @param segment
     * @return
     */
    boolean contains(@NonNull IProfileSegment segment);

    /**
     * Update the selected segment of the profile with the new start and end
     * positions. Checks the validity of the operation, and returns false if it
     * is not possible to perform the update
     * 
     * @param segment the segment to update
     * @param startIndex the new start
     * @param endIndex the new end
     * @throws SegmentUpdateException the update failed
     * @throws ProfileException 
     */
    boolean update(@NonNull IProfileSegment segment, int startIndex, int endIndex) throws SegmentUpdateException, ProfileException;

    /**
     * Move the start and end indexes of all segments by the given amount.
     * Start and end indexes of all segments are increased by the
     * given value, with wrapping applied.
     * 
     * <pre>
     *  0     5          15                      Index
     *  |-----|----------|--------------------   Seg boundaries
     * </pre>
     * After moving segments by 5, the profile looks like this:
     * <pre>
     *        5     10         20            
     *  ------|-----|----------|--------------
     * </pre>
     * @param amount the amount to increase segment start and end indexes
     * @throws ProfileException
     */
    void moveSegments(int amount) throws ProfileException;


    /**
     * Adjust the segmented profile to start from the given new index.
     * This works by adjusting the segment start and end indexes. Assume a
     * segmented profile like this:
     * <pre>
     *  0     5          15                   35   Index
     *  |-----|----------|--------------------     Seg boundaries
     * </pre>
     * After starting from index 5, the profile looks like this:
     * <pre>
     *  0          10                   30    35
     *  |----------|--------------------|-----
     * </pre>
     * The new profile starts at index 'newStartIndex' in the original profile.
     * This means that we must subtract 'newStartIndex' from the segment positions to
     * make them line up.
     * 
     * If you want to move segment bounds without assigning a new start position for
     * the overall profile, use  {@link ISegmentedProfile::nudgeSegments}.
     * 
     * @param newStartIndex the index from which the profile should start
     * @return a new profile with the desired start index
     */
    @Override
	ISegmentedProfile startFrom(int newStartIndex) throws ProfileException;

    /**
     * Attempt to merge the given segments into one segment. The segments must
     * belong to the profile, and be adjacent
     * 
     * @param segment1 the id of the first segment to be merged
     * @param segment2 the id of the first segment to be merged
     * @param mergedId the new id to give the segment
     * @return
     */
    void mergeSegments(@NonNull UUID segment1, @NonNull UUID segment2, @NonNull UUID mergedId) throws ProfileException;

    
    /**
     * Attempt to merge the given segments into one segment. The segments must
     * belong to the profile, and be adjacent
     * 
     * @param segment1
     * @param segment2
     * @param id the new id to give the segment
     * @return
     */
    void mergeSegments(@NonNull IProfileSegment segment1, @NonNull IProfileSegment segment2, @NonNull UUID id) throws ProfileException;

    /**
     * Reverse a merge operation on a segment
     * 
     * @param segment
     */
    void unmergeSegment(@NonNull IProfileSegment segment) throws ProfileException;
    
    /**
     * Reverse a merge operation on a segment
     * 
     * @param segment
     */
    void unmergeSegment(@NonNull UUID segId) throws ProfileException;

    /**
     * Split the segment containing at the given index into two new segments.
     *  The new segments will have the split index as their end and start indexes
     * respectively.
     * 
     * @param segment the segment to be split
     * @param splitIndex the index to split at
     * @param id1 the id for the first new segment
     * @param id2 the id for the second new segment
     * @throws ProfileException if the split would cause a segment to become too short
     * @throws IllegalArgumentException if the segment does not contain the splitting index or the segment is not part of the profile
     */
    void splitSegment(@NonNull IProfileSegment segment, int splitIndex, @NonNull UUID id1, @NonNull UUID id2) throws ProfileException;

    /**
     * Test if the given segment can be split at the given index and produce two
     * new valid segments
     * 
     * @param segment
     * @param splitIndex
     * @return
     */
    boolean isSplittable(@NonNull UUID id, int splitIndex);
    
    /**
     * Create a copy of this profile
     * @return
     */
    @Override
    ISegmentedProfile duplicate() throws ProfileException;
}
