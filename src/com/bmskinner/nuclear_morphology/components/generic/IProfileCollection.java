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

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public interface IProfileCollection extends Serializable, Loggable {

    static final int ZERO_INDEX = 0;

    static IProfileCollection makeNew() {
        return new DefaultProfileCollection();
    }

    /**
     * Get the offset needed to transform a profile to start from the given
     * point type. Returns -1 if the border tag is not found
     * 
     * @param pointType
     * @return the offset or -1
     */
    int getIndex(Tag pointType) throws UnavailableBorderTagException;

    /**
     * Get all the offset keys attached to this profile collection
     * 
     * @return
     */
    List<Tag> getBorderTags();

    /**
     * Test if the given tag is present in the collection
     * 
     * @param tag
     * @return
     */
    boolean hasBorderTag(Tag tag);

    /**
     * Get the requested profile from the cached profiles, or generate it from
     * the ProfileAggregate if it is not cached.
     * 
     * @param type
     *            the type of profile to fetch
     * @param tag
     *            the Tag to use as index zero
     * @param quartile
     *            the collection quartile to return (0-100)
     * @return the quartile profile from the given tag
     * @throws UnavailableBorderTagException
     *             when the tag is not present
     * @throws ProfileException
     * @throws UnavailableProfileTypeException
     *             when the profile type does not have an associated aggregate
     */
    IProfile getProfile(ProfileType type, Tag tag, double quartile)
            throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException;

    /**
     * Get the requested segmented profile. Generates it dynamically from the
     * appropriate ProfileAggregate.
     * 
     * @param s
     *            the pointType of the profile to find
     * @return the profile
     * @throws ProfileException
     * @throws UnavailableBorderTagException
     *             when the tag is not present as an offset
     * @throws UnavailableProfileTypeException
     *             when the profile type does not have an associated aggregate
     * @throws UnsegmentedProfileException
     *             when no segments are available for the profile
     */
    ISegmentedProfile getSegmentedProfile(ProfileType type, Tag tag, double quartile)
            throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException,
            UnsegmentedProfileException;

    /**
     * Get the profile aggregate for this collection
     * 
     * @return the aggregate
     */
    // IProfileAggregate getAggregate();

    /**
     * Test if the profile aggregate has been created
     * 
     * @return
     */
    // boolean hasAggregate();

    /**
     * Get the length of the profile aggregate (this is the integer value of the
     * median CellCollection array length)
     * 
     * @return Length, or zero if the aggregate is not yet created
     */
    int length();

    /**
     * Create a list of segments based on an offset of existing segments.
     * 
     * @param s
     *            the name of the tag
     * @return a copy of the segments in the profile, offset to start at the tag
     * @throws ProfileException
     */
    List<IBorderSegment> getSegments(Tag tag) throws UnavailableBorderTagException, ProfileException;

    /**
     * Get the ids of the segments in this collection
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
     */
    IBorderSegment getSegmentAt(Tag tag, int position) throws UnavailableBorderTagException, ProfileException;

    /**
     * Test if the collection contains a segment beginning at the given tag
     * 
     * @param tag
     * @return
     * @throws UnsegmentedProfileException
     * @throws UnavailableBorderTagException
     */
    boolean hasSegmentStartingWith(Tag tag) throws UnavailableBorderTagException, UnsegmentedProfileException;

    /**
     * Fetch the segment from the profile beginning at the given tag; i.e. the
     * segment with a start index of zero, when the profile is offset to the
     * tag.
     * 
     * @param tag
     *            the border tag
     * @return a copy of the segment with the tag at its start index, or null
     * @throws UnsegmentedProfileException
     */
    IBorderSegment getSegmentStartingWith(Tag tag) throws UnavailableBorderTagException, UnsegmentedProfileException;

    /**
     * Test if the collection contains a segment beginning at the given tag
     * 
     * @param tag
     * @return
     * @throws Exception
     */
    boolean hasSegmentEndingWith(Tag tag) throws UnavailableBorderTagException, UnsegmentedProfileException;

    /**
     * Fetch the segment from the profile beginning at the given tag; i.e. the
     * segment with a start index of zero, when the profile is offset to the
     * tag.
     * 
     * @param tag
     *            the border tag
     * @return a copy of the segment with the tag at its start index, or null
     */
    IBorderSegment getSegmentEndingWith(Tag tag) throws UnavailableBorderTagException, UnsegmentedProfileException;

    /**
     * Fetch the segment from the profile containing the given index. The zero
     * index is the reference point
     * 
     * @param index
     * @return a copy of the segment with the index inside, or null
     */
    IBorderSegment getSegmentContaining(int index) throws UnsegmentedProfileException;

    /**
     * Fetch the segment from the profile containing at the given tag;
     * 
     * @param tag
     *            the border tag
     * @return a copy of the segment with the tag index inside, or null
     */
    IBorderSegment getSegmentContaining(Tag tag) throws ProfileException, UnsegmentedProfileException;

    /**
     * Add an offset for the given point type. The offset is used to fetch
     * profiles the begin at the point of interest.
     * 
     * @param pointType
     *            the point
     * @param offset
     *            the position of the point in the profile
     */
    void addIndex(Tag tag, int offset);

    /**
     * Add a list of segments for the profile. The segments must have the
     * correct offset to be added directly
     * 
     * @param n
     *            the segment list
     */
    void addSegments(List<IBorderSegment> n);

    /**
     * Add a list of segments for the profile, where the segments are zeroed to
     * the given point type. The indexes will be corrected for storage. I
     * previously disabled this - unknown why.
     * 
     * @param pointType
     *            the point with the zero index in the segments
     * @param n
     *            the segment list
     * @throws ProfileException
     * @throws UnavailableBorderTagException
     */
    void addSegments(Tag tag, List<IBorderSegment> n) throws ProfileException, UnavailableBorderTagException;

    /**
     * Create profile aggregates from the given collection, with a set length.
     * By default, the profiles are zeroed on the reference point
     * 
     * @param collection
     *            the CellCollection
     * @param length
     *            the length of the aggregate
     */
    void createProfileAggregate(@NonNull ICellCollection collection, int length);

    /**
     * Create profile aggregates from the given collection, with a length set by
     * any segments the collection contains. By default, the profiles are zeroed
     * on the reference point
     * 
     * @param collection
     *            the CellCollection
     */
    void createAndRestoreProfileAggregate(@NonNull ICellCollection collection);

    /**
     * Create the profile aggregate from the given collection, using the
     * collection median length to determine bin sizes
     * 
     * @param collection
     *            the Cellcollection
     * @throws Exception
     */
    // void createProfileAggregate(ICellCollection collection);

    /**
     * Get the points associated with offsets currently present
     * 
     * @return a string with the points
     */
    String tagString();

    /**
     * Turn the IQR (difference between Q25, Q75) of the median into a
     * profile. @param pointType the profile type to use @return the
     * profile @throws ProfileException @throws
     * UnavailableBorderTagException @throws
     * UnavailableProfileTypeException @throws
     */
    IProfile getIQRProfile(ProfileType type, Tag tag)
            throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException;

    /**
     * Find the points in the profile that are most variable
     */
    List<Integer> findMostVariableRegions(ProfileType type, Tag tag) throws UnavailableBorderTagException;

    /**
     * Get the values within the profile aggregate for the given position
     * 
     * @param type
     *            the profile type to search
     * @param position
     *            the position between zero and one
     * @return the values at that position
     * @throws UnavailableProfileTypeException
     *             if the profile type is not present
     */
    double[] getValuesAtPosition(ProfileType type, double position) throws UnavailableProfileTypeException;

    List<Double> getXKeyset(ProfileType type);

}
