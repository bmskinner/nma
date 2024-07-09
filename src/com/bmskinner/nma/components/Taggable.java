/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General License for more details.
 * 
 * You should have received a copy of the GNU General License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.components;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.MissingMeasurementException;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;

/**
 * Objects implementing this interface can have Landmarks assigned to points
 * around their periphery.
 * 
 * @author bms41
 *
 */
public interface Taggable extends CellularComponent, Orientable {

	static final double BORDER_POINT_NOT_PRESENT = -2d;
	static final int BORDER_INDEX_NOT_FOUND = -1;

	static final int DEFAULT_PROFILE_WINDOW = 15;
	static final double DEFAULT_PROFILE_WINDOW_PROPORTION = 0.05;

	/**
	 * Calculate profiles based on the desired window proportion
	 * 
	 * @param proportion
	 * @throws ComponentCreationException
	 */
	void createProfiles(double proportion) throws ComponentCreationException;

	/**
	 * Check if the object has a profile of the given type
	 * 
	 * @param type
	 * @return
	 */
	boolean hasProfile(@NonNull ProfileType type);

	/**
	 * Get a copy of the angle profile. The first index of the profile is the
	 * reference point. If no reference point has been explicitly assigned, it is
	 * assumed to be the first index of the profile.
	 * 
	 * @return the profile for the object
	 * @throws MissingProfileException  if the profile type is not found
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 */
	ISegmentedProfile getProfile(@NonNull ProfileType type)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get a copy of the profile offset to start at the given point
	 * 
	 * @param type profile type to fetch
	 * @param tag  the tag to offset the profile to
	 * @return a copy of the segmented profile
	 * @throws ProfileException
	 * @throws SegmentUpdateException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 */
	@NonNull
	ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Landmark lm)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get a copy of the profile offset to start at the given point
	 * 
	 * @param type profile type to fetch
	 * @param om   the tag to offset the profile to
	 * @return a copy of the segmented profile
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 */
	ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull OrientationMark om)
			throws SegmentUpdateException, MissingDataException;

	/**
	 * Get a copy of the profile offset to start at the given point
	 * 
	 * @param type profile type to fetch
	 * @param om   the landmark to offset the profile to
	 * @return a copy of the unsegmented profile
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 */
	IProfile getUnsegmentedProfile(@NonNull ProfileType type, @NonNull OrientationMark om)
			throws SegmentUpdateException, MissingDataException;

	/**
	 * Set segments from the reference point. The first segment must begin at index
	 * zero.
	 * 
	 * @param segments the segments covering the profile
	 * @throws SegmentUpdateException if the profile segments are not suitable
	 */
	void setSegments(@NonNull List<IProfileSegment> segments)
			throws MissingDataException, SegmentUpdateException;

	/**
	 * Get the window size for generating the specificed profile
	 * 
	 * @return
	 * @throws MissingMeasurementException
	 * @throws SegmentUpdateException
	 * @throws ComponentCreationException
	 * @throws MissingDataException
	 */
	int getWindowSize() throws MissingMeasurementException, MissingDataException,
			ComponentCreationException, SegmentUpdateException;

	/**
	 * Get the fraction of the perimeter to use for calculating the window size in
	 * pixels
	 * 
	 * @return a fraction between 0 and 1
	 * @throws MissingProfileException
	 */
	double getWindowProportion();

	/**
	 * Set the fraction of the perimeter to use for calculating the window size in
	 * pixels.
	 * 
	 * @param d Proportion from 0 to 1, or
	 *          Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION if not previously set
	 */
	public void setWindowProportion(double d);

	/**
	 * Check if the segments and tags are able to be modified
	 * 
	 * @return
	 */
	boolean isLocked();

	/**
	 * Set if the segments and tags are able to be modified
	 * 
	 * @param b
	 */
	void setLocked(boolean b);

	/**
	 * Set the lock state for this segment
	 * 
	 * @param isLocked
	 * @param segId
	 */
	void setSegmentStartLock(boolean isLocked, @NonNull UUID segId);

	/**
	 * Reverse the angle profile of the object. Also reverses the distance profile,
	 * the border list and updates landmarks to the new positions
	 */
	@Override
	void reverse() throws MissingDataException, SegmentUpdateException, ComponentCreationException;

	/**
	 * Get the border index of point in the border list, removing offset to a
	 * reference tag. This will return the index of the point in the original border
	 * list of the object.
	 * 
	 * @param reference the border tag with index zero
	 * @param index     the index in a profile zeroed on the reference tag
	 * @return the index of the point translated back to the original border list
	 * @throws MissingLandmarkException if the reference tag is not present
	 */
	int getIndexRelativeTo(@NonNull OrientationMark reference, int index)
			throws MissingLandmarkException;

	/**
	 * Get the index of the border point with the given tag.
	 * 
	 * @param s the tag
	 * @return the index of the border point with the tag
	 * @throws MissingLandmarkException if the tag is not present in the object
	 */
	int getBorderIndex(@NonNull OrientationMark tag) throws MissingLandmarkException;

	/**
	 * Get a copy of the border point mapped to the given tag
	 * 
	 * @param tag the tag to fetch
	 * @return a copy of the border point at the tag
	 * @throws IndexOutOfBoundsException
	 * @throws MissingLandmarkException
	 */
	IPoint getBorderPoint(@NonNull OrientationMark tag) throws MissingLandmarkException;

	/**
	 * Get the index of the border point with the given tag.
	 * 
	 * @param s the tag
	 * @return the index of the border point with the tag
	 * @throws MissingLandmarkException if the tag is not present in the object
	 */
	int getBorderIndex(@NonNull Landmark tag) throws MissingLandmarkException;

	/**
	 * Get a copy of the border point mapped to the given tag
	 * 
	 * @param tag the tag to fetch
	 * @return a copy of the border point at the tag
	 * @throws IndexOutOfBoundsException
	 * @throws MissingLandmarkException
	 */
	IPoint getBorderPoint(@NonNull Landmark tag) throws MissingLandmarkException;

	/**
	 * Check if the nucleus has the given landmark
	 * 
	 * @param landmark the landmark to test
	 * @return true if the landmark is present, false otherwise
	 */
	boolean hasLandmark(@NonNull OrientationMark landmark);

	/**
	 * Set the index of the given landmark. Has no effect if this object is locked.
	 * 
	 * @param tag the tag
	 * @param i   the index of the border point to set the tag at
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	void setLandmark(@NonNull OrientationMark tag, int i)
			throws IndexOutOfBoundsException, MissingDataException, SegmentUpdateException;

	void setLandmark(@NonNull Landmark tag, int i)
			throws IndexOutOfBoundsException, MissingDataException, SegmentUpdateException;

	/**
	 * Set the index of the given landmark. Has no effect if this object is locked.
	 * 
	 * @param tag the tag
	 * @param i   the index of the border point to set the tag at
	 * @throws SegmentUpdateException
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 */
	void setOrientationMark(@NonNull OrientationMark tag, int i)
			throws IndexOutOfBoundsException, MissingDataException, SegmentUpdateException;

	/**
	 * Get the indexes of orientation marks within the border of this object
	 * 
	 * @return
	 */
	Map<OrientationMark, Integer> getOrientationMarkMap();

	/**
	 * Get the landmark corresponding to the given orientation point
	 * 
	 * @param om
	 * @return
	 */
	@Override
	Landmark getLandmark(OrientationMark om);

	/**
	 * Get the border of the object starting from the given landmark
	 * 
	 * @param lm the landmark to be the first index in the object
	 * @return
	 * @throws MissingLandmarkException
	 */
	List<IPoint> getBorderList(@NonNull Landmark lm) throws MissingLandmarkException;

	/**
	 * Get the border of the object starting from the given orientation mark
	 * 
	 * @param om the orientation mark to be the first index in the object
	 * @return
	 */
	List<IPoint> getBorderList(@NonNull OrientationMark om) throws MissingLandmarkException;

}
