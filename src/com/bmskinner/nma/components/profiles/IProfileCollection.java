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

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * Interface for profiles within a cell collection. Handles generation of median
 * profiles from individual nuclei.
 * 
 * @author ben
 *
 */
public interface IProfileCollection extends XmlSerializable {

	static final int ZERO_INDEX = 0;

	/**
	 * If a profile is created without segments, this is the ID of the default
	 * segment spanning the entire profile.
	 */
	static final @NonNull UUID DEFAULT_SEGMENT_ID = UUID
			.fromString("11111111-2222-3333-4444-555566667777");

	/**
	 * Create a copy of the collection
	 * 
	 * @return
	 */
	IProfileCollection duplicate() throws SegmentUpdateException;

	/**
	 * Discard any cached data, and calculate all median and other profiles from the
	 * cells in the collection.
	 * 
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	void calculateProfiles()
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the index of the landmark in the profile, zeroed on the reference point
	 * 
	 * @param landmark the landmark to find
	 * @return the index of the landmark
	 * @throws MissingLandmarkException if the landmark is not present
	 */
	int getLandmarkIndex(@NonNull OrientationMark landmark) throws MissingLandmarkException;

	/**
	 * Get the index of the landmark in the profile, zeroed on the reference point
	 * 
	 * @param landmark the landmark to find
	 * @return the index of the landmark
	 * @throws MissingLandmarkException if the landmark is not present
	 */
	int getLandmarkIndex(@NonNull Landmark lm) throws MissingLandmarkException;

	/**
	 * Get the proportion of the index along the profile, zeroed on the reference
	 * point
	 * 
	 * @param index the index to find
	 * @return the proportion of the index along the profile, from 0-1
	 */
	double getProportionOfIndex(int index);

	/**
	 * Get the proportion of the landmark along the profile, zeroed on the reference
	 * point
	 * 
	 * @param landmark the landmark to find
	 * @return the proportion of the landmark along the profile, from 0-1
	 * @throws MissingLandmarkException if the landmark is not present
	 */
	double getProportionOfIndex(@NonNull OrientationMark landmark) throws MissingLandmarkException;

	/**
	 * Get the proportion of the landmark along the profile, zeroed on the reference
	 * point
	 * 
	 * @param landmark the landmark to find
	 * @return the proportion of the landmark along the profile, from 0-1
	 * @throws MissingLandmarkException if the landmark is not present
	 */
	double getProportionOfIndex(@NonNull Landmark landmark) throws MissingLandmarkException;

	/**
	 * Get the index closest to the given proportion along the profile
	 * 
	 * @param proportion the proportion along the profile from 0-1
	 * @return the closest index
	 */
	int getIndexOfProportion(double proportion);

	/**
	 * Get all the landmarks attached to this profile collection
	 * 
	 * @return
	 */
	List<Landmark> getLandmarks();

	/**
	 * Get all the orientation marks attached to this profile collection
	 * 
	 * @return
	 */
	List<OrientationMark> getOrientationMarks();

	/**
	 * Test if the given landmark is present in the collection
	 * 
	 * @param landmark
	 * @return
	 */
	boolean hasLandmark(@NonNull OrientationMark landmark);

	/**
	 * Get the landmark corresponding to the given orientation mark
	 * 
	 * @param om
	 * @return
	 */
	Landmark getLandmark(@NonNull OrientationMark om);

	/**
	 * Get or calculate the requested profile.
	 * 
	 * @param type     the type of profile to fetch
	 * @param landmark the landmark to use as index zero
	 * @param quartile the collection quartile to return (0-100)
	 * @return the quartile profile from the given landmark
	 * @throws MissingLandmarkException when the landmark is not present
	 * @throws SegmentUpdateException
	 */
	IProfile getProfile(@NonNull ProfileType type, @NonNull OrientationMark landmark, int quartile)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get or calculate the requested profile.
	 * 
	 * @param type     the type of profile to fetch
	 * @param landmark the landmark to use as index zero
	 * @param quartile the collection quartile to return (0-100)
	 * @return the quartile profile from the given landmark
	 * @throws MissingLandmarkException when the landmark is not present
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	IProfile getProfile(@NonNull ProfileType type, @NonNull Landmark landmark, int quartile)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Turn the IQR (difference between Q25, Q75) of the median into a profile.
	 * 
	 * @param pointType the profile type to use
	 * @return the profile
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	IProfile getIQRProfile(@NonNull ProfileType type, @NonNull OrientationMark landmark)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get or calculate the requested segmented profile.
	 * 
	 * @param type     the profile type to fetch
	 * @param landmark the starting index of the profile
	 * @param quartile the quartile to fetch (0-100)
	 * @return the profile
	 * @throws ProfileException
	 * @throws MissingDataException   when the profile is missing landmarks, profile
	 *                                types or segments
	 * @throws SegmentUpdateException
	 */
	ISegmentedProfile getSegmentedProfile(@NonNull ProfileType type,
			@NonNull OrientationMark landmark, int quartile)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * The number of segments in the profile
	 * 
	 * @return
	 */
	int segmentCount();

	/**
	 * Is the number of segments greater than zero?
	 * 
	 * @return
	 */
	boolean hasSegments();

	/**
	 * Get a copy of the segments in the collection ordered from the RP. The first
	 * segment in the list will contain the landmark at any index except the end
	 * index. The segments will have their positions offset such that the landmark
	 * is at the zero index of the underlying profile.
	 * 
	 * @param landmark the landmark to offset by
	 * @return a copy of the segments in the profile, offset to start at the
	 *         landmark
	 * @throws ProfileException
	 */
	List<IProfileSegment> getSegments(@NonNull OrientationMark landmark)
			throws MissingLandmarkException, SegmentUpdateException;

	/**
	 * Get the IDs of the segments in this collection, ordered by position from the
	 * RP
	 * 
	 * @return
	 */
	List<UUID> getSegmentIDs();

	/**
	 * Get the segment at the given position in the profile
	 * 
	 * @param position
	 * @return
	 * @throws ProfileException
	 * @throws SegmentUpdateException
	 */
	IProfileSegment getSegmentAt(@NonNull OrientationMark landmark, int position)
			throws MissingLandmarkException, SegmentUpdateException;

	/**
	 * Fetch the segment from the profile containing at the given landmark;
	 * 
	 * @param landmark the border landmark
	 * @return a copy of the segment with the landmark index inside, or null
	 * @throws MissingLandmarkException
	 */
	IProfileSegment getSegmentContaining(@NonNull OrientationMark landmark)
			throws SegmentUpdateException, MissingLandmarkException;

	/**
	 * Set the index for a given landmark, with RP at index zero. Setting the index
	 * of the RP will have no effect since the RP is always index zero.
	 * 
	 * @param lm       the landmark to set - RP will have no effect
	 * @param newIndex the index of the landmark in the profile from RP
	 */
	void setLandmark(@NonNull Landmark lm, int newIndex);

	/**
	 * Set the segments in the profile collection to the given list, where the
	 * segments are zeroed to the reference point. Any existing segments are
	 * replaced.
	 * 
	 * @param segments the segments to add
	 * @throws MissingLandmarkException
	 */
	void setSegments(@NonNull List<IProfileSegment> segments) throws MissingLandmarkException;

}
