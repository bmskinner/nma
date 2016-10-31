/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package components.generic;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import analysis.profiles.ProfileException;
import components.active.generic.SegmentedFloatProfile;
import components.nuclear.IBorderSegment;
import components.nuclear.NucleusBorderSegment;

public interface ISegmentedProfile extends IProfile {

	/**
	 * Create a new profile of the default type
	 * @param profile
	 * @return
	 */
	static ISegmentedProfile makeNew(ISegmentedProfile profile){
		return new SegmentedFloatProfile(profile);
	}
	
	/**
	 * Check if this profile contains segments
	 * @return
	 */
	boolean hasSegments();

	/**
	 * Get a copy of the segments in this profile. The order will be the same
	 * as the original segment list. No point type sepecifiers are available;
	 * if you need to offset a profile, do it by a profile offset 
	 * @return
	 */
	List<IBorderSegment> getSegments();

	/**
	 * Get an iterator that begins with the segment at position zero in the profile
	 * @return
	 * @throws Exception
	 */
	Iterator<IBorderSegment> segmentIterator() throws Exception;

	/**
	 * Fetch the segment with the given id, or null if not present.
	 * Fetches the actual segment, not a copy
	 * @param id
	 * @return
	 */
	IBorderSegment getSegment(UUID id);

	boolean hasSegment(UUID id);
	
	/**
	 * Make this profile the length specified. Segments
	 * will be adjusted to the closest proportional index
	 * @param newLength the new array length
	 * @return an interpolated profile
	 * @throws Exception 
	 */
	ISegmentedProfile interpolate(int newLength);

	/**
	 * Fetch the segment list ordered to start from the segment with the given id
	 * @param id
	 * @return
	 * @throws Exception
	 */
	List<IBorderSegment> getSegmentsFrom(UUID id) throws Exception;

	/**
	 * Get a copy of the segments in this profile, ordered 
	 * from the zero index of the profile
	 * @return
	 */
	List<IBorderSegment> getOrderedSegments();

	/**
	 * Get the segment with the given name. Returns null if no segment
	 * is found. Gets the actual segment, not a copy
	 * @param name
	 * @return
	 */
	IBorderSegment getSegment(String name);

	/**
	 * Get the given segment. Returns null if no segment
	 * is found. Gets the actual segment, not a copy
	 * @param name
	 * @return
	 */
	IBorderSegment getSegment(IBorderSegment segment);

	/**
	 * Get the segment at the given position in the profile.
	 * is found. Gets the actual segment, not a copy
	 * @param name
	 * @return
	 */
	IBorderSegment getSegmentAt(int position);

	/**
	 * Get the segment containing the given index
	 * @param index
	 * @return
	 */
	IBorderSegment getSegmentContaining(int index);

	/**
	 * Replace the segments in the profile with the given list
	 * @param segments
	 */
	void setSegments(List<IBorderSegment> segments);

	/**
	 * Remove the segments from this profile
	 */
	void clearSegments();

	/**
	 * Get the names of the segments in the profile
	 * @return
	 */
	List<String> getSegmentNames();

	/**
	 * Get the names of the segments in the profile
	 * @return
	 */
	List<UUID> getSegmentIDs();

	/**
	 * Get the number of segments in the profile
	 * @return
	 */
	int getSegmentCount();

	/**
	 * Find the value displacement of the given segment in the profile.
	 * i.e the difference between the start value and the end value
	 * @param segment the segment to measure
	 * @return the displacement, or 0 if the segment was not found
	 */
	double getDisplacement(IBorderSegment segment);

	/**
	 * Test if the profile contains the given segment. Copies are ok,
	 * it checks position, length and name
	 * @param segment
	 * @return
	 */
	boolean contains(IBorderSegment segment);

	/**
	 * Update the selected segment of the profile with the new start and end
	 * positions. Checks the validity of the operation, and returns false if
	 * it is not possible to perform the update
	 * @param segment the segment to update
	 * @param startIndex the new start
	 * @param endIndex the new end
	 * @return did the update succeed
	 */
	boolean update(IBorderSegment segment, int startIndex, int endIndex);

	/**
	 * Adjust the start position of the given segment by the given amount.
	 * @param segment the segment to apply the change to
	 * @param amount the number of indexes to move
	 * @return did the update succeed
	 */
	boolean adjustSegmentStart(UUID id, int amount);

	/**
	 * Adjust the end position of the given segment by the given amount.
	 * @param segment the segment to apply the change to
	 * @param amount the number of indexes to move
	 * @return did the update succeed
	 */
	boolean adjustSegmentEnd(UUID id, int amount);

	void nudgeSegments(int amount);

	/* (non-Javadoc)
	 * @see no.components.Profile#offset(int)
	 * Offset the segment by the given amount. Returns a copy
	 * of the profile.
	 */
	ISegmentedProfile offset(int newStartIndex);

	/**
	 * Interpolate the segments of this profile to the proportional lengths of the
	 * segments in the template. The template must have the same number of segments.
	 * Both this and the template must be already offset to start at equivalent positions.
	 * The two profiles must have the same segment ids
	 * @param template the profile with segments to copy.
	 * @return a new profile with normalised segments
	 * @throws Exception
	 */
	ISegmentedProfile frankenNormaliseToProfile(ISegmentedProfile template)
			throws ProfileException;

	/**
	 * Test if the given profile values are the same as in
	 * this profile.
	 * @param profile
	 * @return
	 */
	boolean equals(ISegmentedProfile profile);

	void reverse();

	/**
	 * Attempt to merge the given segments into one segment. The segments must
	 * belong to the profile, and be adjacent
	 * @param segment1
	 * @param segment2
	 * @param id the new id to give the segment
	 * @return
	 */
	void mergeSegments(IBorderSegment segment1,
			IBorderSegment segment2, UUID id) throws ProfileException;

	/**
	 * Reverse a merge operation on a segment
	 * @param segment
	 */
	void unmergeSegment(IBorderSegment segment) throws ProfileException;

	/**
	 * Split a segment at the given index into two new segments. Splits the segmnets, 
	 * adds the split as merge sources to the old segmnet, then unmerges
	 * @param segment the segment to split
	 * @param splitIndex the index to split at
	 * @throws Exception
	 */
	void splitSegment(IBorderSegment segment, int splitIndex, UUID id1,
			UUID id2) throws ProfileException;

	String toString();

	/**
	 * Restore the toString from Profile
	 * @return
	 */
	String valueString();

	int hashCode();

	boolean equals(Object obj);

}