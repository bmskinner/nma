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

package analysis.profiles;

import java.util.Map;

import components.ProfileableCellularComponent.IndexOutOfBoundsException;
import components.generic.BorderTagObject;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.Tag;
import components.generic.UnavailableBorderTagException;
import components.generic.UnavailableProfileTypeException;
import components.nuclear.BorderPoint;
import components.nuclear.IBorderPoint;

/**
 * Objects implementing this interface can have BorderTags assigned
 * to points around their periphery. These points are linked to
 * index positions within the various profiles of the Profileable
 * interface.
 * @author bms41
 *
 */
public interface Taggable extends Profileable {
	
	public static final double BORDER_POINT_NOT_PRESENT = -2d;
	public static final int    BORDER_INDEX_NOT_FOUND   = -1;
	
	/**
	 * Get the index of the border point with the given tag.
	 * If the point does not exist, returns -1
	 * @param s the tag
	 * @return the index of the border in borderList
	 */
	public int getBorderIndex(Tag tag);
	
	/**
	 * Get the tag at a given index, given the zero index is
	 * set at the given tag
	 * If there is no tag at the index, returns null
	 * @param tag the border tag with index zero
	 * @param index the index to fetch
	 * @return the border tag at the index
	 */
	public Tag getBorderTag(Tag tag, int index);
	
	/**
	 * Get the tag at the given raw index in the border list
	 * @param index
	 * @return the tag at the index, or null if no tag present
	 */
	public Tag getBorderTag(int index) throws IndexOutOfBoundsException;

	
	/**
	 * Get a copy of the border point at the given tag,
	 * or null if the tag is not present
	 * @param tag
	 * @return
	 * @throws IndexOutOfBoundsException 
	 * @throws UnavailableBorderTagException 
	 */
	public IBorderPoint getBorderTag(Tag tag) throws UnavailableBorderTagException;
	
	/**
	 * Check if the nucleus has the given border tag
	 * @param tag
	 * @return
	 */
	public boolean hasBorderTag(Tag tag);
	
	/**
	 * Check if the nucleus has any border tag at the given index
	 * (offset from the provided tag)
	 * @param tag the border tag with index zero
	 * @param i the index to be tested
	 * @return true if a tag is present at the index
	 */
	public boolean hasBorderTag(Tag tag, int i) throws IndexOutOfBoundsException;
	
	/**
	 * Check if the nucleus has any border tag at the given index
	 * in the raw border list
	 * @param i the index to be tested
	 * @return true if a tag is present at the index
	 */
	public boolean hasBorderTag( int index) throws IndexOutOfBoundsException;
	
	/**
	 * Set the name of the given NucleusBorderPoint
	 * @param tag the new tag to use as a name
	 * @param i the index of the border point
	 */
	
	public void setBorderTag(Tag tag, int i) throws IndexOutOfBoundsException;
	
	/**
	 * Set or update a border tag based on an index from a reference tag
	 * @param reference the border tag with index zero
	 * @param tag the new tag to use
	 * @param i the index of the border point relative to the reference
	 */
	public void setBorderTag(Tag reference, Tag tag, int i) throws IndexOutOfBoundsException;
	
	/**
	 * Get a copy of the profile offset to start at the given point
	 * @param type profile type to fetch
	 * @param tag the tag to offset the profile to
	 * @return a copy of the segmented profile
	 * @throws ProfileException 
	 * @throws UnavailableBorderTagException 
	 * @throws UnavailableProfileTypeException 
	 */
	public ISegmentedProfile getProfile(ProfileType type, Tag tag) throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException;
	
		
	/**
	 * Set the profile for the given type, offsetting to a border tag
	 * @param type
	 * @param profile
	 * @throws components.generic.UnavailableBorderTagException 
	 */
	public void setProfile(ProfileType type, Tag tag, ISegmentedProfile profile) throws UnavailableBorderTagException, UnavailableProfileTypeException;
	
	/**
	 * Get a copy of the mapping of border tags to index positions within 
	 * the border list of the nucleus
	 * @return
	 */
	public Map<Tag, Integer> getBorderTags();
	
	/**
	 * Get a copy of the border point mapped to the given tag
	 * @param tag
	 * @return
	 * @throws IndexOutOfBoundsException 
	 * @throws UnavailableBorderTagException 
	 */
	public IBorderPoint getBorderPoint(Tag tag) throws UnavailableBorderTagException; 
	
	/**
	 * Get the border index of point in the border list, 
	 * removing offset to a reference tag
	 * @param reference the border tag with index zero
	 * @param index the index to offset. Should be counting from the reference tag
	 * @return the offset index, or -1 if the reference tag is not present
	 */
	public int getOffsetBorderIndex(Tag reference, int index);
	
	
	/**
	 * This will completely replace the map of border tags with new
	 * positions, without attempting vertical updating, or RP shifting
	 * until after all new points are set. Used in the CellBorderAdjustmentDialog
	 * @param tagMap
	 */
	public void replaceBorderTags(Map<Tag, Integer> tagMap);

}
