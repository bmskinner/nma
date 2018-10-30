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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;

/**
 * Objects implementing this interface can have BorderTags assigned to points
 * around their periphery. These points are linked to index positions within the
 * various profiles of the Profileable interface.
 * 
 * @author bms41
 *
 */
public interface Taggable extends Profileable {

    static final double BORDER_POINT_NOT_PRESENT = -2d;
    static final int    BORDER_INDEX_NOT_FOUND   = -1;

    /**
     * Get the index of the border point with the given tag.
     * 
     * @param s the tag
     * @return the index of the border point with the tag
     * @throws UnavailableBorderTagException if the tag is not present in the object
     */
    int getBorderIndex(@NonNull Tag tag) throws UnavailableBorderTagException;

    /**
     * Get the tag at a given index, given the zero index is set at the given
     * reference. If there is no tag at the index, returns null
     * 
     * @param reference the reference border tag with index zero
     * @param index the index to fetch
     * @return the border tag at the index
     * @throws UnavailableBorderTagException if the reference tag is not present
     */
    Tag getBorderTag(@NonNull Tag reference, int index) throws UnavailableBorderTagException;

    /**
     * Get the tag at the given raw index in the border list
     * 
     * @param index
     * @return the tag at the index
     * @throws UnavailableBorderTagException if no tag is present at the index
     */
    Tag getBorderTag(int index) throws IndexOutOfBoundsException;

    /**
     * Check if the nucleus has the given border tag
     * 
     * @param tag
     * @return
     */
    boolean hasBorderTag(@NonNull Tag tag);

    /**
     * Check if the nucleus has any border tag at the given index (offset from
     * the provided tag)
     * 
     * @param tag the border tag with index zero
     * @param i the index to be tested
     * @return true if a tag is present at the index
     */
    boolean hasBorderTag(@NonNull Tag tag, int i) throws IndexOutOfBoundsException;

    /**
     * Check if the nucleus has any border tag at the given index in the raw
     * border list
     * 
     * @param i
     *            the index to be tested
     * @return true if a tag is present at the index
     */
    boolean hasBorderTag(int index) throws IndexOutOfBoundsException;

    /**
     * Set the index of the given border tag
     * 
     * @param tag the  tag
     * @param i the index of the border point
     */

    void setBorderTag(@NonNull Tag tag, int i) throws IndexOutOfBoundsException;

    /**
     * Set or update a border tag based on an index from a reference tag
     * 
     * @param reference the border tag with index zero
     * @param tag the new tag to use
     * @param i the index of the border point relative to the reference
     * @throws UnavailableBorderTagException if  the reference tag is not present
     */
    void setBorderTag(@NonNull Tag reference, @NonNull Tag tag, int i) throws IndexOutOfBoundsException, UnavailableBorderTagException;

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
    ISegmentedProfile getProfile(@NonNull ProfileType type, @NonNull Tag tag)
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
    void setProfile(@NonNull ProfileType type, @NonNull Tag tag, @NonNull ISegmentedProfile profile)
            throws UnavailableBorderTagException, UnavailableProfileTypeException;

    /**
     * Get a copy of the mapping of border tags to index positions within the
     * border list of the nucleus
     * 
     * @return
     */
    Map<Tag, Integer> getBorderTags();

    /**
     * Get a copy of the border point mapped to the given tag
     * 
     * @param tag
     * @return
     * @throws IndexOutOfBoundsException
     * @throws UnavailableBorderTagException
     */
    IBorderPoint getBorderPoint(@NonNull Tag tag) throws UnavailableBorderTagException;

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
    int getOffsetBorderIndex(@NonNull Tag reference, int index) throws UnavailableBorderTagException;

    /**
     * This will completely replace the map of border tags with new positions,
     * without attempting vertical updating, or RP shifting until after all new
     * points are set. Used in the CellBorderAdjustmentDialog
     * 
     * @param tagMap
     */
    void replaceBorderTags(@NonNull Map<Tag, Integer> tagMap);

}
