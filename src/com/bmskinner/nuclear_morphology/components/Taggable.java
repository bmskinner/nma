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
package com.bmskinner.nuclear_morphology.components;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;

/**
 * Objects implementing this interface can have Landmarks assigned to points
 * around their periphery.
 * 
 * @author bms41
 *
 */
public interface Taggable extends CellularComponent {

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
			throws MissingProfileException, ProfileException, MissingLandmarkException;

	/**
	 * Get a copy of the profile offset to start at the given point
	 * 
	 * @param type profile type to fetch
	 * @param tag  the tag to offset the profile to
	 * @return a copy of the segmented profile
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 */
	ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Landmark tag)
			throws ProfileException, MissingLandmarkException, MissingProfileException;

	/**
	 * A quicker alternative to getProfile when segments are not needed
	 * 
	 * @param type
	 * @return
	 * @throws ProfileException
	 * @throws MissingLandmarkException
	 */
	IProfile getUnsegmentedProfile(@NonNull ProfileType type, @NonNull Landmark lm)
			throws ProfileException, MissingLandmarkException;

	/**
	 * Set segments from the reference point. The first segment must begin at index
	 * zero.
	 * 
	 * @param segments the segments covering the profile
	 * @throws ProfileException if the profile segments are not suitable
	 */
	void setSegments(@NonNull List<IProfileSegment> segments) throws MissingLandmarkException, ProfileException;

	/**
	 * Get the window size for generating the specificed profile
	 * 
	 * @return
	 */
	int getWindowSize();

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
	void reverse() throws ProfileException, MissingComponentException;

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
	int getIndexRelativeTo(@NonNull Landmark reference, int index) throws MissingLandmarkException;

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
	 * Get the tag at the given raw index in the border list
	 * 
	 * @param index
	 * @return the tag at the index
	 * @throws MissingLandmarkException if no tag is present at the index
	 */
	Landmark getBorderTag(int index) throws IndexOutOfBoundsException;

	/**
	 * Check if the nucleus has the given landmark
	 * 
	 * @param landmark the landmark to test
	 * @return true if the landmark is present, false otherwise
	 */
	boolean hasLandmark(@NonNull Landmark landmark);

	/**
	 * Set the index of the given landmark. Has no effect if this object is locked.
	 * 
	 * @param tag the tag
	 * @param i   the index of the border point to set the tag at
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 */

	void setLandmark(@NonNull Landmark tag, int i)
			throws IndexOutOfBoundsException, MissingProfileException, ProfileException, MissingLandmarkException;

	/**
	 * Get a copy of the mapping of landmarks to index positions within the border
	 * list of the nucleus
	 * 
	 * @return
	 */
	Map<Landmark, Integer> getLandmarks();

}
