/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.generic;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;

/**
 * This details a profile that can have segments applied to it.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface ISegmentedProfile extends IProfile {

    /**
     * Create a new profile of the default type
     * 
     * @param profile
     * @return
     * @throws ProfileException
     */
    static ISegmentedProfile makeNew(@NonNull ISegmentedProfile profile) throws ProfileException {
        return new SegmentedFloatProfile(profile);
    }

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
    List<IBorderSegment> getSegments();

    /**
     * Fetch the segment with the given id, or null if not present. Fetches the
     * actual segment, not a copy
     * 
     * @param id
     * @return
     * @throws UnavailableComponentException if there is no segment with the given id
     * @throws IllegalArgumentException if the id is null
     */
    IBorderSegment getSegment(@NonNull UUID id) throws UnavailableComponentException;

    /**
     * Test if a segment with the given id is present
     * @param id the id
     * @return true if a segment is present with the id, false otherwise
     * @throws IllegalArgumentException if the id is null
     */
    boolean hasSegment(@NonNull UUID id);

    /**
     * Make this profile the length specified. Segments will be adjusted to the
     * closest proportional index
     * 
     * @param newLength
     *            the new array length
     * @return an interpolated profile
     * @throws ProfileException
     */
    @Override
    ISegmentedProfile interpolate(int newLength) throws ProfileException;

    /**
     * Fetch the segment list ordered to start from the segment with the given
     * id
     * 
     * @param id
     * @return
     * @throws Exception
     */
    List<IBorderSegment> getSegmentsFrom(@NonNull UUID id) throws Exception;

    /**
     * Get a copy of the segments in this profile, ordered from the zero index
     * of the profile
     * 
     * @return
     */
    List<IBorderSegment> getOrderedSegments();

    /**
     * Get the segment with the given name.     * 
     * @param name the segment name to find
     * @return the segment
     * @throws UnavailableComponentException if there is no segment with the given name
     */
    IBorderSegment getSegment(@NonNull String name) throws UnavailableComponentException;

    /**
     * Get the given segment. Returns null if no segment is found. Gets the
     * actual segment, not a copy
     * 
     * @param name
     * @return
     */
    IBorderSegment getSegment(@NonNull IBorderSegment segment);

    /**
     * Get the segment at the given position in the profile. is found. Gets the
     * actual segment, not a copy
     * 
     * @param name
     * @return
     */
    IBorderSegment getSegmentAt(int position);

    /**
     * Get the segment containing the given index
     * 
     * @param index
     * @return
     */
    IBorderSegment getSegmentContaining(int index);

    /**
     * Replace the segments in the profile with the given list
     * 
     * @param segments
     */
    void setSegments(@NonNull List<IBorderSegment> segments);

    /**
     * Remove the segments from this profile
     */
    void clearSegments();

    /**
     * Get the names of the segments in the profile
     * 
     * @return
     */
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
    double getDisplacement(@NonNull IBorderSegment segment);

    /**
     * Test if the profile contains the given segment. Copies are ok, it checks
     * position, length and name
     * 
     * @param segment
     * @return
     */
    boolean contains(@NonNull IBorderSegment segment);

    /**
     * Update the selected segment of the profile with the new start and end
     * positions. Checks the validity of the operation, and returns false if it
     * is not possible to perform the update
     * 
     * @param segment
     *            the segment to update
     * @param startIndex
     *            the new start
     * @param endIndex
     *            the new end
     * @return did the update succeed
     * @throws SegmentUpdateException
     */
    boolean update(@NonNull IBorderSegment segment, int startIndex, int endIndex) throws SegmentUpdateException;

    /**
     * Adjust the start position of the given segment by the given amount.
     * 
     * @param segment
     *            the segment to apply the change to
     * @param amount
     *            the number of indexes to move
     * @return did the update succeed
     * @throws SegmentUpdateException
     */
    boolean adjustSegmentStart(@NonNull UUID id, int amount) throws SegmentUpdateException;

    /**
     * Adjust the end position of the given segment by the given amount.
     * 
     * @param segment
     *            the segment to apply the change to
     * @param amount
     *            the number of indexes to move
     * @return did the update succeed
     * @throws SegmentUpdateException
     */
    boolean adjustSegmentEnd(@NonNull UUID id, int amount) throws SegmentUpdateException;

    /**
     * Move the positions of all segments by the given amount
     * @param amount
     * @throws ProfileException
     */
    void nudgeSegments(int amount) throws ProfileException;

    /*
     * (non-Javadoc)
     * 
     * @see no.components.Profile#offset(int) Offset the segment by the given
     * amount. Returns a copy of the profile.
     */
    ISegmentedProfile offset(int newStartIndex) throws ProfileException;

    /**
     * Interpolate the segments of this profile to the proportional lengths of
     * the segments in the template. The template must have the same number of
     * segments. Both this and the template must be already offset to start at
     * equivalent positions. The two profiles must have the same segment ids
     * 
     * @param template
     *            the profile with segments to copy.
     * @return a new profile with normalised segments
     * @throws Exception
     */
    ISegmentedProfile frankenNormaliseToProfile(@NonNull ISegmentedProfile template) throws ProfileException;

    /**
     * Attempt to merge the given segments into one segment. The segments must
     * belong to the profile, and be adjacent
     * 
     * @param segment1
     * @param segment2
     * @param id
     *            the new id to give the segment
     * @return
     */
    void mergeSegments(@NonNull IBorderSegment segment1, @NonNull IBorderSegment segment2, @NonNull UUID id) throws ProfileException;

    /**
     * Reverse a merge operation on a segment
     * 
     * @param segment
     */
    void unmergeSegment(@NonNull IBorderSegment segment) throws ProfileException;

    /**
     * Split a segment at the given index into two new segments. Splits the
     * segments, adds the split as merge sources to the old segment, then
     * unmerges
     * 
     * @param segment the segment to split
     * @param splitIndex the index to split at
     * @param id1 the id for the first new segment
     * @param id2 the id for the second new segment
     */
    void splitSegment(@NonNull IBorderSegment segment, int splitIndex, @NonNull UUID id1, @NonNull UUID id2) throws ProfileException;

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
     * Restore the toString from Profile
     * 
     * @return
     */
    String valueString();
    
    /**
     * Create a copy of this profile
     * @return
     */
    @Override
    ISegmentedProfile copy();
}
