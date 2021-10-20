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
package com.bmskinner.nuclear_morphology.components.profiles;

import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

public interface IProfileCollection extends XmlSerializable {

    static final int ZERO_INDEX = 0;
    
    /**
     * If a profile is created without segments, this is the ID of the default segment spanning the entire profile.
     */
    static final @NonNull UUID DEFAULT_SEGMENT_ID = UUID.fromString("11111111-2222-3333-4444-555566667777");
    

    static IProfileCollection makeNew() {
        return new DefaultProfileCollection();
    }
    
    
    /**
     * Craete a copy of the collection
     * @return
     */
    IProfileCollection duplicate();
    
    /**
     * Get the index of the tag in the profile, zeroed on the reference point
     * 
     * @param tag the tag to find
     * @return the index of the tag
     * @throws MissingLandmarkException if the tag is not present
     */
    int getIndex(@NonNull Landmark tag) throws MissingLandmarkException;
    
    /**
     * Get the proportion of the index along the profile, zeroed on the reference point
     * 
     * @param index the index to find
     * @return the proportion of the index along the profile, from 0-1
     */
    double getProportionOfIndex(int index);
    
    /**
     * Get the proportion of the tag along the profile, zeroed on the reference point
     * 
     * @param tag the tag to find
     * @return the proportion of the tag along the profile, from 0-1
     * @throws MissingLandmarkException if the tag is not present
     */
    double getProportionOfIndex(@NonNull Landmark tag) throws MissingLandmarkException;
    
    
    /**
     * Get the index closest to the given proportion along the profile
     * @param proportion the proportion along the profile from 0-1
     * @return the closest index
     */
    int getIndexOfProportion(double proportion);
    
    /**
     * Get all the offset keys attached to this profile collection
     * 
     * @return
     */
    List<Landmark> getLandmarks();

    /**
     * Test if the given tag is present in the collection
     * 
     * @param tag
     * @return
     */
    boolean hasLandmark(@NonNull Landmark tag);

    /**
     * Get the requested profile from the cached profiles, or generate it from
     * the ProfileAggregate if it is not cached.
     * 
     * @param type the type of profile to fetch
     * @param tag the Tag to use as index zero
     * @param quartile the collection quartile to return (0-100)
     * @return the quartile profile from the given tag
     * @throws MissingLandmarkException when the tag is not present
     * @throws ProfileException
     * @throws MissingProfileException when the profile type does not have an associated aggregate
     */
    IProfile getProfile(@NonNull ProfileType type, @NonNull Landmark tag, double quartile)
            throws MissingLandmarkException, ProfileException, MissingProfileException;

    /**
     * Get a segmented profile offset to start from the given tag. 
     * 
     * @param  type the profile type to fetch
     * @param tag the starting index of the profile
     * @param quartile the quartile to fetch
     * @return the profile
     * @throws ProfileException
     * @throws MissingLandmarkException when the tag is not present as an offset
     * @throws MissingProfileException when the profile type does not have an associated aggregate
     * @throws UnsegmentedProfileException when no segments are available for the profile
     */
    ISegmentedProfile getSegmentedProfile(@NonNull ProfileType type, @NonNull Landmark tag, double quartile)
            throws MissingLandmarkException, ProfileException, MissingProfileException;

    /**
     * Get the length of the profile aggregate (this is the integer value of the
     * median ICellCollection array length)
     * 
     * @return Length, or zero if the aggregate is not yet created
     */
    int length();
    
    
    /**
     * The number of segments in the profile
     * @return
     */
    int segmentCount();
    
    /**
     * Is the number of segments greater than zero?
     * @return
     */
    boolean hasSegments();

    /**
     * Get a copy of the segments in the collection ordered from the given tag. The first 
     * segment in the list will contain the tag at any index except the end index. The
     * segments will have their positions offset such that the tag is at the zero index
     * of the underlying profile. 
     * 
     * @param tag the tag to offset by
     * @return a copy of the segments in the profile, offset to start at the tag
     * @throws ProfileException
     */
    List<IProfileSegment> getSegments(@NonNull Landmark tag) throws MissingLandmarkException, ProfileException;

    /**
     * Get the IDs of the segments in this collection. The IDs are ordered by position within
     * the profile collection.
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
    IProfileSegment getSegmentAt(@NonNull Landmark tag, int position) throws MissingLandmarkException, ProfileException;

    /**
     * Test if the collection contains a segment beginning at the given tag
     * 
     * @param tag
     * @return
     * @throws UnsegmentedProfileException
     * @throws MissingLandmarkException
     */
    boolean hasSegmentStartingWith(@NonNull Landmark tag) throws MissingLandmarkException, UnsegmentedProfileException;

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
    IProfileSegment getSegmentStartingWith(@NonNull Landmark tag) throws MissingLandmarkException, UnsegmentedProfileException;

    /**
     * Test if the collection contains a segment beginning at the given tag
     * 
     * @param tag
     * @return
     * @throws Exception
     */
    boolean hasSegmentEndingWith(@NonNull Landmark tag) throws MissingLandmarkException, UnsegmentedProfileException;

    /**
     * Fetch the segment from the profile beginning at the given tag; i.e. the
     * segment with a start index of zero, when the profile is offset to the
     * tag.
     * 
     * @param tag
     *            the border tag
     * @return a copy of the segment with the tag at its start index, or null
     */
    IProfileSegment getSegmentEndingWith(@NonNull Landmark tag) throws MissingLandmarkException, UnsegmentedProfileException;

    /**
     * Fetch the segment from the profile containing the given index. The zero
     * index is the reference point
     * 
     * @param index
     * @return a copy of the segment with the index inside, or null
     */
    IProfileSegment getSegmentContaining(int index) throws ProfileException;

    /**
     * Fetch the segment from the profile containing at the given tag;
     * 
     * @param tag
     *            the border tag
     * @return a copy of the segment with the tag index inside, or null
     */
    IProfileSegment getSegmentContaining(@NonNull Landmark tag) throws ProfileException;

    /**
     * Add an index for the given tag. Note that setting the index of the RP
     * will have no effect; the RP is always index zero.
     * 
     * @param tag the tag
     * @param index the index of the point in the profile
     */
    void addIndex(@NonNull Landmark tag, int index);

    /**
     * Set the segments in the profile collection to the given list,
     * where the segments are zeroed to the reference point. Any
     * existing segments are replaced.
     * 
     * @param segments the segments to add
     */
    void addSegments(@NonNull List<IProfileSegment> segments);

    /**
     * Set the segments in the profile collection to the given list,
     * where the segments are zeroed to the given point type. Any
     * existing segments are replaced.
     * 
     * @param tag the tag with the zero index in the collection
     * @param segments the segments to add
     * @throws ProfileException
     * @throws MissingLandmarkException
     */
    void addSegments(@NonNull Landmark tag, @NonNull List<IProfileSegment> segments) throws ProfileException, MissingLandmarkException;

    /**
     * Create profile aggregates from the given collection, with a set length.
     * By default, the profiles are zeroed on the reference point. If the profile
     * collection already has segments assigned, these will be retained following 
     * aggregate creation.
     * 
     * @param collection the CellCollection
     * @param length the length of the aggregates
     * @throws ProfileException 
     */
    void createProfileAggregate(@NonNull ICellCollection collection, int length) throws ProfileException;

    /**
     * Create profile aggregates from the given collection, with a length set by
     * any segments the collection contains. By default, the profiles are zeroed
     * on the reference point
     * 
     * @param collection the CellCollection
     * @throws ProfileException if the profile aggregate cannot be restored
     */
    void createAndRestoreProfileAggregate(@NonNull ICellCollection collection) throws ProfileException;


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
    IProfile getIQRProfile(@NonNull ProfileType type, @NonNull Landmark tag)
            throws MissingLandmarkException, ProfileException, MissingProfileException;

    /**
     * Find the points in the profile that are most variable
     */
    List<Integer> findMostVariableRegions(@NonNull ProfileType type, @NonNull Landmark tag) throws MissingLandmarkException;

    /**
     * Get the values within the profile aggregate for the given position
     * 
     * @param type the profile type to search
     * @param position the position between zero and one
     * @return the values at that position
     * @throws MissingProfileException
     *             if the profile type is not present
     */
    double[] getValuesAtPosition(@NonNull ProfileType type, double position) throws MissingProfileException;
}
