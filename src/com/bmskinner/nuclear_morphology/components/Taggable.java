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

import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;

/**
 * Objects implementing this interface can have BorderTags assigned to points
 * around their periphery. These points are linked to index positions within the
 * various profiles of the Profileable interface.
 * 
 * @author bms41
 *
 */
public interface Taggable extends CellularComponent {

    static final double BORDER_POINT_NOT_PRESENT = -2d;
    static final int    BORDER_INDEX_NOT_FOUND   = -1;
    
    static final int    DEFAULT_PROFILE_WINDOW            = 15;
    static final double DEFAULT_PROFILE_WINDOW_PROPORTION = 0.05;

    /**
     * Calculate profiles based on the desired window proportion
     * 
     * @param proportion
     * @throws ComponentCreationException
     */
    void initialise(double proportion) throws ComponentCreationException;


    /**
     * Finds the key points of interest around the border of the object. Can
     * use several different methods, and take a best-fit, or just use one. The
     * default in a round nucleus is to get the longest diameter and set this as
     * the head/tail axis.
     * @throws ComponentCreationException
     */
    public void findPointsAroundBorder() throws ComponentCreationException;

    /**
     * Check if the object has a profile of the given type
     * 
     * @param type
     * @return
     */
    boolean hasProfile(@NonNull ProfileType type);

    /**
     * Get a copy of the angle profile. The first index of the profile is the
     * first border point in the border list. That is, there is no consistency
     * to the order of values across multiple nuclei. If consistency is needed,
     * specify a pointType
     * @return the profile for the object
     * @throws UnavailableProfileTypeException if the profile type is not found
     */
    ISegmentedProfile getProfile(@NonNull ProfileType type) throws UnavailableProfileTypeException;

    /**
     * Update the profile of the given type. Since only franken profiles are not
     * calculated internally, the other profiles just replace the segment list.
     * This will replace the segment list on all other profile types, to keep
     * the segmentation pattern consistent.
     * 
     * @param type
     * @param profile
     * @throws Exception
     */
    void setProfile(@NonNull ProfileType type, @NonNull ISegmentedProfile profile);

    /**
     * Get the window size for generating the specificed profile
     * 
     * @param type
     * @return
     */
    int getWindowSize(@NonNull ProfileType type);

    /**
     * Get the fraction of the perimeter to use for calculating the window size
     * in pixels
     * 
     * @return a fraction between 0 and 1
     * @throws UnavailableProfileTypeException
     */
    double getWindowProportion(@NonNull ProfileType type);

    /**
     * Set the fraction of the perimeter to use for calculating the window size
     * in pixels.
     * 
     * @param d
     *            Proportion from 0 to 1, or
     *            Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION if not
     *            previously set
     */
    public void setWindowProportion(@NonNull ProfileType type, double d);

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
     * Set the lock on this segment for all profile types
     * 
     * @param lock
     * @param segID
     */
    void setSegmentStartLock(boolean lock, @NonNull UUID segID);

    /**
     * Reverse the angle profile of the object. Also reverses the distance
     * profile, the border list and updates the border tags to the new positions
     */
    @Override
	void reverse();

    /**
     * Calculate new profiles for the object. Angle profiles are calculated with
     * the internal window size - change this with
     * setWindowSize(ProfileType.ANGLE). It will replace the existing profiles.
     * 
     * @throws ProfileException if there was an error in calculating the profiles
     */
    void calculateProfiles() throws ProfileException;

    /**
     * Go around the border of the object, measuring the angle to the OP. If the
     * angle is closest to target angle, return the distance to the CoM.
     * 
     * @param angle
     *            the target angle
     * @return the distance from the closest border point at the requested angle
     *         to the CoM
     */
    double getDistanceFromCoMToBorderAtAngle(double angle);

    /**
     * Get the index of the border point with the given tag.
     * 
     * @param s the tag
     * @return the index of the border point with the tag
     * @throws UnavailableBorderTagException if the tag is not present in the object
     */
    int getBorderIndex(@NonNull Landmark tag) throws UnavailableBorderTagException;

    /**
     * Get the tag at a given index, given the zero index is set at the given
     * reference. If there is no tag at the index, returns null
     * 
     * @param reference the reference border tag with index zero
     * @param index the index to fetch
     * @return the border tag at the index
     * @throws UnavailableBorderTagException if the reference tag is not present
     */
    Landmark getBorderTag(@NonNull Landmark reference, int index) throws UnavailableBorderTagException;

    /**
     * Get the tag at the given raw index in the border list
     * 
     * @param index
     * @return the tag at the index
     * @throws UnavailableBorderTagException if no tag is present at the index
     */
    Landmark getBorderTag(int index) throws IndexOutOfBoundsException;

    /**
     * Check if the nucleus has the given border tag
     * 
     * @param tag the tag to test
     * @return true if the tag is present, false otherwise
     */
    boolean hasBorderTag(@NonNull Landmark tag);

    /**
     * Check if the nucleus has any border tag at the given index (offset from
     * the provided tag)
     * 
     * @param tag the border tag with index zero
     * @param i the index to be tested
     * @return true if a tag is present at the index, false otherwise
     */
    boolean hasBorderTag(@NonNull Landmark tag, int i) throws IndexOutOfBoundsException;

    /**
     * Check if the nucleus has any border tag at the given index in the raw
     * border list
     * 
     * @param i the index to be tested
     * @return true if a tag is present at the index, false otherwise
     */
    boolean hasBorderTag(int index) throws IndexOutOfBoundsException;

    /**
     * Set the index of the given border tag. Has no effect if this object is locked.
     * 
     * @param tag the tag
     * @param i the index of the border point to set the tag at
     */

    void setBorderTag(@NonNull Landmark tag, int i) throws IndexOutOfBoundsException;

    /**
     * Set or update a border tag based on an index from a reference tag
     * 
     * @param reference the border tag with index zero
     * @param tag the new tag to use
     * @param i the index of the border point relative to the reference
     * @throws UnavailableBorderTagException if  the reference tag is not present
     */
    void setBorderTag(@NonNull Landmark reference, @NonNull Landmark tag, int i) throws IndexOutOfBoundsException, UnavailableBorderTagException;

    /**
     * Get a copy of the profile offset to start at the given point
     * 
     * @param type profile type to fetch
     * @param tag the tag to offset the profile to
     * @return a copy of the segmented profile
     * @throws ProfileException
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     */
    ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Landmark tag)
            throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException;

    /**
     * Set the profile for the given type, offset to a border tag. The profile can be
     * considered to start from the provided tag.
     * 
     * @param type the type of profile to set
     * @param tag the tag the profile starts from
     * @param profile the profile to be set
     * @throws UnavailableBorderTagException if the tag is not present
     * @throws UnavailableProfileTypeException if the profile type is not present
     */
    void setProfile(@NonNull ProfileType type, @NonNull Landmark tag, @NonNull ISegmentedProfile profile)
            throws UnavailableBorderTagException, UnavailableProfileTypeException;

    /**
     * Get a copy of the mapping of border tags to index positions within the
     * border list of the nucleus
     * 
     * @return
     */
    Map<Landmark, Integer> getBorderTags();

    /**
     * Get a copy of the border point mapped to the given tag
     * 
     * @param tag the tag to fetch
     * @return a copy of the border point at the tag
     * @throws IndexOutOfBoundsException
     * @throws UnavailableBorderTagException
     */
    IBorderPoint getBorderPoint(@NonNull Landmark tag) throws UnavailableBorderTagException;

    /**
     * Get the border index of point in the border list, removing offset to a
     * reference tag. This will return the index of the point in the original
     * border list of the object.
     * 
     * @param reference the border tag with index zero
     * @param index  the index in a profile zeroed on the reference tag
     * @return the index of the point translated back to the original border list
     * @throws UnavailableBorderTagException if the reference tag is not present
     */
    int getOffsetBorderIndex(@NonNull Landmark reference, int index) throws UnavailableBorderTagException;
}
