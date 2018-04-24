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


package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileSegmenter.UnsegmentableProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.BorderTag.BorderTagType;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This class is designed to simplify operations on cell collections involving
 * copying and refreshing of ProfileCollections and ProfileAggregates. It
 * handles movement of tag indexes within the median and the nuclei
 * 
 * @author bms41
 *
 */
public class ProfileManager implements Loggable {
    final private ICellCollection collection;

    public ProfileManager(final ICellCollection collection) {
        this.collection = collection;
    }

    public int getProfileLength() {
        return collection.getProfileCollection().length();
    }

    /**
     * Get the average profile window size in the population.
     * 
     * @param type
     * @return
     */
    public int getProfileWindowSize(ProfileType type) {

        int total = 0;
        Set<Nucleus> nuclei = collection.getNuclei();

        for (Nucleus n : nuclei) {
            total += n.getWindowSize(type); // use the first window size found
                                            // for now
        }

        return total / nuclei.size();
    }

    /**
     * Update the given tag in each nucleus of the collection to the index with
     * a best fit of the profile to the given median profile
     * 
     * @param tag
     * @param type
     * @param median
     */
    public void offsetNucleusProfiles(Tag tag, ProfileType type, IProfile median) {

        collection.getNuclei().parallelStream().forEach(n -> {
            if (!n.isLocked()) {

                // returns the positive offset index of this profile which best
                // matches the median profile
                int newIndex;
                try {
                    newIndex = n.getProfile(type).getSlidingWindowOffset(median);

                } catch (ProfileException | UnavailableProfileTypeException e1) {
                    warn("Error getting offset from nucleus " + n.getNameAndNumber());
                    stack(e1.getMessage(), e1);
                    return;
                }
                try {
                    n.setBorderTag(tag, newIndex);
                } catch (IndexOutOfBoundsException e) {
                    warn("Error updating nucleus tag " + n.getNameAndNumber());
                    stack(e.getMessage(), e);
                    return;
                }

                if (tag.equals(Tag.TOP_VERTICAL) || tag.equals(Tag.BOTTOM_VERTICAL)) {

                    n.updateVerticallyRotatedNucleus();
                    n.updateDependentStats();

                }
            }
        });

    }

    /**
     * Add the given offset to each of the profile types in the
     * ProfileCollection except for the frankencollection
     * 
     * @param tag
     * @param index
     */
    public void updateProfileCollectionOffsets(Tag tag, int index) {

        // check the index for wrapping - observed problem when OP==RP in
        // rulesets

        index = CellularComponent.wrapIndex(index, getProfileLength());

        for (ProfileType type : ProfileType.values()) {
            if (type.equals(ProfileType.FRANKEN)) {
                continue;
            }

            collection.getProfileCollection().addIndex(tag, index);

        }

    }

    /**
     * Change the RP to the given index in the current median from the profile
     * collection.
     * 
     * @param index
     */
    public void updateRP(int index) {

        // Get the existing median, and offset it to the new index
        IProfile median;
        try {
            median = collection.getProfileCollection()
                    .getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN).offset(index);
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
            fine("Error updating the RP", e);
            return;
        }

        finer("Fetched median from new offset of RP to " + index);

        finest("New median from " + Tag.REFERENCE_POINT + ":");
        finest(median.toString());

        finest("Offsetting individual nucleus indexes");
        offsetNucleusProfiles(Tag.REFERENCE_POINT, ProfileType.ANGLE, median);

        finer("Nucleus indexes for " + Tag.REFERENCE_POINT + " updated");
        collection.createProfileCollection();
        // createProfileCollections(false);
        finer("Rebuilt the profile collcctions");
    }

    /**
     * Use the collection's ruleset to calculate the positions of the top and
     * bottom verticals in the median profile, and assign these to the nuclei
     */
    public void calculateTopAndBottomVerticals() {

        fine("Detecting top and bottom verticals in collection");

        try {
            ProfileIndexFinder finder = new ProfileIndexFinder();

            int topIndex = finder.identifyIndex(collection, Tag.TOP_VERTICAL);
            int btmIndex = finder.identifyIndex(collection, Tag.BOTTOM_VERTICAL);

            fine("TV in median is located at index " + topIndex);
            fine("BV in median is located at index " + btmIndex);

            updateProfileCollectionOffsets(Tag.TOP_VERTICAL, topIndex);

            updateProfileCollectionOffsets(Tag.BOTTOM_VERTICAL, btmIndex);

        } catch (NoDetectedIndexException e) {
            fine("Cannot find TV or BV in median profile");
            return;
        }

        fine("Updating nuclei");

        IProfile topMedian;
        IProfile btmMedian;

        try {
            topMedian = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.TOP_VERTICAL,
                    Stats.MEDIAN);

            btmMedian = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.BOTTOM_VERTICAL,
                    Stats.MEDIAN);
        } catch (ProfileException | UnavailableBorderTagException | UnavailableProfileTypeException e) {
            fine("Error getting TV or BV profile", e);
            return;
        }

        offsetNucleusProfiles(Tag.TOP_VERTICAL, ProfileType.ANGLE, topMedian);

        offsetNucleusProfiles(Tag.BOTTOM_VERTICAL, ProfileType.ANGLE, btmMedian);

        collection.updateVerticalNuclei();

        fine("Updated nuclei");
    }

    /**
     * Update the location of the given border tag within the profile
     * 
     * @param tag
     * @param index
     *            the new index within the median profile
     * @throws IndexOutOfBoundsException
     * @throws UnavailableBorderTagException
     * @throws ProfileException
     * @throws UnavailableProfileTypeException
     */
    public void updateBorderTag(Tag tag, int index) throws IndexOutOfBoundsException, ProfileException,
            UnavailableBorderTagException, UnavailableProfileTypeException {

        finer("Updating border tag " + tag);

        if (tag.equals(Tag.REFERENCE_POINT)) {
            updateRP(index);
            return;
        }

        if (tag.type().equals(BorderTagType.CORE)) {
            finer("Updating core border tag");
            updateCoreBorderTagIndex(tag, index);
            return;
        } else {
            finer("Updating extended border tag");
            updateExtendedBorderTagIndex(tag, index);
        }

    }

    /**
     * Update the extended border tags that don't need resegmenting
     * 
     * @param tag
     * @param index
     * @throws IndexOutOfBoundsException
     * @throws ProfileException
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     */
    private void updateExtendedBorderTagIndex(Tag tag, int index) throws IndexOutOfBoundsException, ProfileException,
            UnavailableBorderTagException, UnavailableProfileTypeException {

        int oldIndex = collection.getProfileCollection().getIndex(tag);

        if (oldIndex == -1) {
            finer("Border tag does not exist and will be created");
        }

        /*
         * Set the border tag in the median profile
         */
        finer("Setting " + tag + " in median profiles to " + index + " from " + oldIndex);
        updateProfileCollectionOffsets(tag, index);

        // Use the median profile to set the tag in the nuclei

        IProfile median = collection.getProfileCollection().getProfile(ProfileType.ANGLE, tag, Stats.MEDIAN);

        finer("Updating tag in nuclei");
        offsetNucleusProfiles(tag, ProfileType.ANGLE, median);

        collection.updateVerticalNuclei();

        /*
         * Set the border tag in the consensus median profile
         */
        if (collection.hasConsensus()) {
            Nucleus n = collection.getConsensus();
            int oldNIndex = n.getBorderIndex(tag);
            int newIndex = n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(median);
            n.setBorderTag(tag, newIndex);

            if (n.hasBorderTag(Tag.TOP_VERTICAL) && n.hasBorderTag(Tag.BOTTOM_VERTICAL)) {
                n.alignPointsOnVertical(n.getBorderTag(Tag.TOP_VERTICAL), n.getBorderTag(Tag.BOTTOM_VERTICAL));

                if (n.getBorderPoint(Tag.REFERENCE_POINT).getX() > n.getCentreOfMass().getX()) {
                    // need to flip about the CoM
                    n.flipXAroundPoint(n.getCentreOfMass());
                }

            } else {
                n.rotatePointToBottom(n.getBorderTag(Tag.ORIENTATION_POINT));
            }
            //
            finest("Set border tag in consensus to " + newIndex + " from " + oldNIndex);
        }

    }

    /**
     * If a core border tag is moved, the profile needs to be resegmented.
     * 
     * @param tag
     * @param index
     * @throws ProfileException
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     */
    private void updateCoreBorderTagIndex(Tag tag, int index)
            throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException {

        /*
         * Updating core border tags:
         * 
         * 1) Identify the new OP or RP index in the median - save out the
         * offsets for the old border tags against the old RP
         * 
         * 2) Update the RP / OP location in nuclei using frankenprofiling - RP
         * update requires ofsetting OP also - border tags should save offsets
         * too
         * 
         * 3) Create a new profile aggregate and profile collection - use the
         * saved offsets from the old RP to calculate the new offsets for border
         * tags
         * 
         * 4) Resegment the median profile, with the new border tag map
         * 
         * 5) Apply the new segments to the nucleus profiles
         */

        fine("Updating core border tag index");
        // Store the existing core points in a map (OP and RP)
        // This is to force segmentation at the OP and RP
        Map<Tag, Integer> map = new HashMap<Tag, Integer>();
        for (Tag test : BorderTagObject.values(BorderTagType.CORE)) {
            int i = collection.getProfileCollection().getIndex(test);
            map.put(test, i);
            finer("Storing existing median " + test + " at index " + i + " in map");
        }

        finest("Existing median from " + tag + ":");
        finest(collection.getProfileCollection().getProfile(ProfileType.ANGLE, tag, Stats.MEDIAN).toString());

        // Overwrite the new tag for segmentation
        map.put(tag, index);
        finer("Replacing median " + tag + " with index " + index + " in segmenter map");

        // Store the offset for the new point
        collection.getProfileCollection().addIndex(tag, index);
        finer("Offset the " + tag + " index in the regular profile to " + index);

        /*
         * Now we need to update the tag indexes in the nucleus profiles.
         */
        IProfile median = collection.getProfileCollection().getProfile(ProfileType.ANGLE, tag, Stats.MEDIAN);
        finer("Fetched median from new offset of " + tag);

        finest("New median from " + tag + ":");
        finest(median.toString());

        // finest("Current state of regular profile collection:");
        // finest(collection.getProfileCollection(ProfileType.REGULAR).toString());
        finest("Offsetting individual nucleus indexes");
        offsetNucleusProfiles(tag, ProfileType.ANGLE, median);

        finer("Nucleus indexes for " + tag + " updated");

        if (tag.equals(Tag.REFERENCE_POINT)) {

            // We need to rebuild the ProfileAggregate for the new RP
            // This will reset the RP to index zero
            // Make new profile collections
            int rpIndex = collection.getProfileCollection().getIndex(tag);
            finer("RP index is changing - moved to index " + rpIndex);

            collection.createProfileCollection();// createProfileCollections(false);
            finer("Recreated profile collections");

            // Get the recreated profile collections from the new RP
            IProfileCollection pc = collection.getProfileCollection();

            rpIndex = pc.getIndex(tag);
            finer("New ProfileAggregates move RP index to index " + rpIndex);

            // We need to update the offsets for the BorderTags since zero has
            // moved
            for (Tag test : BorderTagObject.values()) {

                // The RP is forced to start at zero
                if (test.equals(Tag.REFERENCE_POINT)) {
                    pc.addIndex(tag, 0);
                    finer("Explicit setting of RP index to zero");
                    continue;

                } else {

                    // Other points are offset by an appropriate amount relative
                    // to the new RP index
                    int oldIndex = pc.getIndex(test);
                    if (oldIndex != -1) { // Only bother if the tag exists

                        int newIndex = CellularComponent.wrapIndex((oldIndex - index), pc.length()); // offset
                                                                                                     // by
                                                                                                     // 1
                        pc.addIndex(test, newIndex);
                        finer("Explicit setting of " + test + " index to " + newIndex + " from " + oldIndex);

                        // Ensure that core tags (the OP) get segmented
                        if (test.type().equals(BorderTagType.CORE)) {
                            map.put(test, newIndex); // Overwrite the map
                            finest("Forcing segmentation at index " + newIndex + " for " + test);
                        }
                    }

                }

            }

            rpIndex = pc.getIndex(tag);
            finer("After explicit set, RP index is " + rpIndex);

            map.put(tag, 0); // the RP is back at zero

        }

        // Resegment the median

        fine("Resegmenting the median profile from the RP");
        IProfileCollection pc = collection.getProfileCollection();

        IProfile medianToSegment = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT,
                Stats.MEDIAN);

        ProfileSegmenter segmenter = new ProfileSegmenter(medianToSegment, map);

        List<IBorderSegment> segments;
        try {
            segments = segmenter.segment();
        } catch (UnsegmentableProfileException e) {
            warn("Cannot segment profile: " + e.getMessage());
            return;
        }

        pc.addSegments(Tag.REFERENCE_POINT, segments);

        fine("Resegmented the median profile");

        // Update signals as needed
        collection.getSignalManager().recalculateSignalAngles();

        collection.updateVerticalNuclei();

        // Run a new morphological analysis to apply the new segments
        // TODO: this needs to trigger the progressable action
        // Temp solution - if a core border tag is detected in the UI selection,
        // trigger morphological analysis after this step

    }

    /**
     * Get the number of segments in the regular profile of the collection. On
     * error return 0
     * 
     * @return
     */
    public int getSegmentCount() {
        IProfileCollection pc = collection.getProfileCollection();
        try {
            return pc.getSegments(Tag.REFERENCE_POINT).size();
        } catch (Exception e) {
            error("Error getting segment count from collection " + collection.getName(), e);
            return 0;
        }
    }

    /**
     * Regenerate the profile aggregate in each of the profile types of the
     * collection. The length is set to the angle profile length
     * 
     * @throws Exception
     */
    public void recalculateProfileAggregates() {

        // use the same array length as the source collection to avoid segment
        // slippage
        IProfileCollection pc = collection.getProfileCollection();

        pc.createProfileAggregate(collection, pc.length());

    }

    /**
     * Copy profile offsets from the this collection, to the destination and
     * build the median profiles for all profile types. Also copy the segments
     * from the regular angle profile onto all other profile types
     * 
     * @param destination
     *            the collection to update
     * @throws Exception
     */
    public void copyCollectionOffsets(final ICellCollection destination) throws ProfileException {

        /*
         * Get the corresponding profile collection from the tempalte
         */
        IProfileCollection sourcePC = collection.getProfileCollection();

        List<IBorderSegment> segments;
        try {
            segments = sourcePC.getSegments(Tag.REFERENCE_POINT);
        } catch (UnavailableBorderTagException e1) {
            warn("RP not found in source collection");
            fine("Error getting segments from RP", e1);
            return;
        }
        fine("Got existing list of " + segments.size() + " segments");

        // use the same array length as the source collection to avoid segment
        // slippage
        int profileLength = sourcePC.length();

        /*
         * Get the empty profile collection from the new CellCollection
         */
        IProfileCollection destPC = destination.getProfileCollection();

        /*
         * Create an aggregate from the nuclei in the collection. This will have
         * the length of the source collection.
         */
        destPC.createProfileAggregate(destination, profileLength);
        fine("Created new profile aggregates with length " + profileLength);
        
        
        /*
         * Copy the offset keys from the source collection
         */
        try {
            for (Tag key : sourcePC.getBorderTags()) {

                destPC.addIndex(key, sourcePC.getIndex(key));

            }

            destPC.addSegments(Tag.REFERENCE_POINT, segments);

        } catch (UnavailableBorderTagException | IllegalArgumentException e) {
            warn("Cannot add segments to RP: " + e.getMessage());
            fine("Cannot add segments to RP", e);
        }
        fine("Copied tags to new collection");
        
        
        // Final sanity check - did the segment IDs get copied properly?
        fine("Testing profiles");
        List<IBorderSegment> newSegs;
        try {
            newSegs = destPC.getSegments(Tag.REFERENCE_POINT);
        } catch (UnavailableBorderTagException e1) {
            warn("RP not found in destination collection");
            fine("Error getting destination segments from RP", e1);
            return;
        }
        
        for(int i=0; i<newSegs.size(); i++){
            if(!segments.get(i).getID().equals(newSegs.get(i).getID())){
                throw new ProfileException("Segment IDs are not consistent with the old profile");
            }
        }
    }

    /**
     * Lock the start index of all segments of all profile types in all nuclei
     * of the collection except for the segment with the given id
     * 
     * @param id
     *            the segmnet to leave unlocked, or to unlock if locked
     * @throws Exception
     */
    public void setLockOnAllNucleusSegmentsExcept(UUID id, boolean b) {

        List<UUID> ids = collection.getProfileCollection().getSegmentIDs();

        collection.getNuclei().forEach(n -> {

            ids.forEach(segID -> {

                if (segID.equals(id)) {
                    n.setSegmentStartLock(!b, segID);
                } else {
                    n.setSegmentStartLock(b, segID);
                }

            });

        });

    }

    /**
     * Set the lock on the start index of all segments of all profile types in
     * all nuclei of the collection
     * 
     * @param id
     *            the segmnet to leave unlocked, or to unlock if locked
     * @throws Exception
     */
    public void setLockOnAllNucleusSegments(boolean b) throws Exception {

        List<UUID> ids = collection.getProfileCollection().getSegmentIDs();

        collection.getNuclei().forEach(n -> {

            ids.forEach(segID -> n.setSegmentStartLock(b, segID));
        });

    }

    /**
     * Update the start index of a segment in the angle profile of the given
     * cell.
     * 
     * @param cell
     *            the cell to alter
     * @param id
     *            the segment id
     * @param index
     *            the new start position of the segment
     * @throws Exception
     */
    public void updateCellSegmentStartIndex(ICell cell, UUID id, int index) throws Exception {

        if (collection.isVirtual()) {
            return;
        }

        Nucleus n = cell.getNucleus();
        ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

        IBorderSegment seg = profile.getSegment(id);

        int startPos = seg.getStartIndex();
        int newStart = index;
        int newEnd = seg.getEndIndex();

        int rawOldIndex = n.getOffsetBorderIndex(Tag.REFERENCE_POINT, startPos);

        if (profile.update(seg, newStart, newEnd)) {
            n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
            finest("Updated nucleus profile with new segment boundaries");

            /*
             * Check the border tags - if they overlap the old index replace
             * them.
             */
            int rawIndex = n.getOffsetBorderIndex(Tag.REFERENCE_POINT, index);

            finest("Updating to index " + index + " from reference point");
            finest("Raw old border point is index " + rawOldIndex);
            finest("Raw new border point is index " + rawIndex);

            if (n.hasBorderTag(rawOldIndex)) {
                Tag tagToUpdate = n.getBorderTag(rawOldIndex);
                fine("Updating tag " + tagToUpdate);
                n.setBorderTag(tagToUpdate, rawIndex);

                // Update intersection point if needed
                if (tagToUpdate.equals(Tag.ORIENTATION_POINT)) {
                    n.setBorderTag(Tag.INTERSECTION_POINT,
                            n.getBorderIndex(n.findOppositeBorder(n.getBorderTag(Tag.ORIENTATION_POINT))));
                }

            } else {
                finest("No border tag needing update at index " + rawOldIndex + " from reference point");
            }

            n.updateVerticallyRotatedNucleus();
            n.updateDependentStats();

        } else {
            log("Updating " + seg.getStartIndex() + " to index " + index + " failed");
        }
    }

    /**
     * Update the given median profile index in the given segment to a new value
     * 
     * @param start
     * @param segName
     * @param index
     * @throws UnsegmentedProfileException
     * @throws ProfileException
     * @throws UnavailableComponentException
     * @throws Exception
     */
    public void updateMedianProfileSegmentIndex(boolean start, UUID id, int index)
            throws ProfileException, UnsegmentedProfileException, UnavailableComponentException {

        ISegmentedProfile oldProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);

        IBorderSegment seg = oldProfile.getSegment(id);

        // Check if the start or end of the segment is updated, and select the
        // new endpoints appropriately
        int newStart = start ? index : seg.getStartIndex();
        int newEnd = start ? seg.getEndIndex() : index;

        // if the segment is the orientation point or reference point boundary,
        // update it

        if (start) {
            if (seg.getStartIndex() == collection.getProfileCollection().getIndex(Tag.ORIENTATION_POINT)) {
                collection.getProfileCollection().addIndex(Tag.ORIENTATION_POINT, index);
            }

            if (seg.getStartIndex() == collection.getProfileCollection().getIndex(Tag.REFERENCE_POINT)) {
                collection.getProfileCollection().addIndex(Tag.REFERENCE_POINT, index);
            }
        }

        // Move the appropriate segment endpoint
        try {
            if (oldProfile.update(seg, newStart, newEnd)) {

                // programLogger.log(Level.FINEST, "Segment position update
                // succeeded");
                // Replace the old segments in the median
                // programLogger.log(Level.FINEST, "Updated profile:
                // "+oldProfile.toString());

                // programLogger.log(Level.FINEST, "Adding segments to profile
                // collection");

                collection.getProfileCollection().addSegments(Tag.REFERENCE_POINT, oldProfile.getSegments());

                finest("Segments added, refresh the charts");

            } else {
                warn("Updating " + seg.getStartIndex() + " to index " + index + " failed");
            }
        } catch (SegmentUpdateException e) {
            warn("Error updating segments");
            stack(e);
        }

    }

    /**
     * Check that the given segment pair does not cross a core border tag
     * 
     * @param seg1
     * @param seg2
     * @return
     * @throws UnavailableBorderTagException
     * @throws Exception
     */
    public boolean testSegmentsMergeable(IBorderSegment seg1, IBorderSegment seg2)
            throws UnavailableBorderTagException {

        // check the boundaries of the segment - we do not want to merge across
        // the BorderTags
        boolean ok = true;

        for (Tag tag : BorderTagObject.values(BorderTagType.CORE)) {

            /*
             * Find the position of the border tag in the median profile
             * 
             */
            int offsetForOp = collection.getProfileCollection().getIndex(Tag.REFERENCE_POINT);

            int offset = collection.getProfileCollection().getIndex(tag);

            // this should be zero for the orientation point and
            // totalLength+difference for the reference point
            int difference = offset - offsetForOp;

            if (seg2.getStartIndex() == seg2.getTotalLength() + difference || seg2.getStartIndex() == difference) {
                ok = false;
            }

        }
        return ok;
    }
    
    /**
     * Count the number of nuclei in the collection that do not have segments matching the median profile
     * @return
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     * @throws ProfileException
     * @throws UnsegmentedProfileException
     */
    public int countNucleiNotMatchingMedianSegmentation() throws UnavailableComponentException, ProfileException, UnsegmentedProfileException{
     // Check that the state of all nuclei in the collection is consistent
        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);
        int error = 0;
        for (Nucleus n : collection.getNuclei()) {
            if(!hasSegmentsMatchingMedian(n, medianProfile)){
                error++;
            }
        }
        return error;
    }

    /**
     * Merge the given segments from the median profile
     * 
     * @param seg1
     * @param seg2
     * @throws UnsegmentedProfileException
     * @throws ProfileException
     * @throws UnavailableComponentException
     */
    public void mergeSegments(IBorderSegment seg1, IBorderSegment seg2, UUID newID)
            throws ProfileException, UnsegmentedProfileException, UnavailableComponentException {

        if (seg1 == null)
            throw new IllegalArgumentException("Segment 1 cannot be null");

        if (seg2 == null)
            throw new IllegalArgumentException("Segment 2 cannot be null");

        if (newID == null)
            throw new IllegalArgumentException("New segment UUID cannot be null");

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);

         // Only try the merge if both segments are present in the profile
        if (!medianProfile.hasSegment(seg1.getID()))
            throw new IllegalArgumentException("Median profile does not have segment 1 ID");

        if (!medianProfile.hasSegment(seg2.getID()))
            throw new IllegalArgumentException("Median profile does not have segment 2 ID");

        
        if(collection.isReal()){
            int error = countNucleiNotMatchingMedianSegmentation();

            if(error>0){
                warn(String.format("Segments are out of sync with median for %d nuclei", error));
                warn("Canceling merge");
                return;
            }
        }

                
        // merge the two segments in the median
        
        try {
            medianProfile.mergeSegments(seg1, seg2, newID);
         // put the new segment pattern back with the appropriate offset
            collection.getProfileCollection().addSegments(Tag.REFERENCE_POINT, medianProfile.getSegments());
        } catch(ProfileException e){
            fine("Error merging segments in median profile; cancelling merge", e);
            return;
        }

        /*
         * With the median profile segments merged, also merge the segments
         * in the individual nuclei
         */
        if(collection.isReal()){
            for (Nucleus n : collection.getNuclei()) {

                boolean wasLocked = n.isLocked();
                n.setLocked(false); // Merging segments is not destructive
                mergeSegments(n, seg1, seg2, newID);
                n.setLocked(wasLocked);
            }
        }

        /*
         * Update the consensus if present
         */
        if (collection.hasConsensus()) {
            Nucleus n = collection.getConsensus();
            mergeSegments(n, seg1, seg2, newID);
        }

        // Ensure the vertical nuclei have the same segment pattern
        collection.updateVerticalNuclei();
    }
    
    /**
     * Test if the segments in teh given Taggable match the segments in the median profile
     * of the collection
     * @param t
     * @return true if the segment ids match
     * @throws ProfileException 
     * @throws UnavailableProfileTypeException 
     * @throws UnavailableBorderTagException 
     */
    private boolean hasSegmentsMatchingMedian(Taggable t, ISegmentedProfile median) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException{
        ISegmentedProfile test = t.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        
        if(test.getSegmentCount() != median.getSegmentCount())
            return false;
        
        for(UUID id : median.getSegmentIDs()){
            if(!test.hasSegment(id))
                return false;
        }
        return true;
    }

    /**
     * Merge the segments with the given IDs into a new segment with the given
     * new ID
     * 
     * @param p
     *            the object with a segmented profile to merge
     * @param seg1
     *            the first segment to be merged
     * @param seg2
     *            the second segment to be merged
     * @param newID
     *            the new ID for the merged segment
     * @throws ProfileException
     * @throws UnavailableComponentException
     */
    private void mergeSegments(Taggable p, IBorderSegment seg1, IBorderSegment seg2, UUID newID)
            throws ProfileException, UnavailableComponentException {

        ISegmentedProfile profile = p.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        IBorderSegment nSeg1 = profile.getSegment(seg1.getID());
        IBorderSegment nSeg2 = profile.getSegment(seg2.getID());
        profile.mergeSegments(nSeg1, nSeg2, newID);
        p.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
    }

    /**
     * Split the given segment into two segmnets. The split is made at the given
     * index. The new segment ids are generated randomly
     * 
     * @param segName
     * @return
     * @throws Exception
     */
    public boolean splitSegment(IBorderSegment seg) throws Exception {
        return splitSegment(seg, null, null);
    }

    /**
     * Split the given segment into two segments. The split is made at the given
     * index
     * 
     * @param seg the segment to split
     * @param newID1 the id for the first new segment. Can be null.
     * @param newID2 the id for the second new segment. Can be null.
     * @return
     * @throws UnsegmentedProfileException
     * @throws ProfileException
     * @throws UnavailableComponentException
     *             if the reference point tag is missing, or the segment is
     *             missing
     */
    public boolean splitSegment(@NonNull IBorderSegment seg, @Nullable UUID newID1, @Nullable UUID newID2)
            throws ProfileException, UnsegmentedProfileException, UnavailableComponentException {

        if (seg == null)
            throw new IllegalArgumentException("Segment cannot be null");

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);

        // Replace the segment with the actual median profile segment - eg when
        // updating child datasets
        seg = medianProfile.getSegment(seg.getID());
        int index = seg.getMidpointIndex();

        if (!seg.contains(index))
            return false;

        double proportion = seg.getIndexProportion(index);

        // Validate that all nuclei have segments long enough to be split
        fine("Testing splittability of collection");
        if (collection.isReal() && !isCollectionSplittable(seg.getID(), proportion)) {
            warn("Segment cannot be split: profile failed testing");
            return false;
        }

        // split the two segments in the median
        @NonNull UUID nId1 = newID1 == null ? java.util.UUID.randomUUID() : newID1;
        @NonNull UUID nId2 = newID2 == null ? java.util.UUID.randomUUID() : newID2;
        medianProfile.splitSegment(seg, index, nId1, nId2);
        fine("Split median profile");

        // put the new segment pattern back with the appropriate offset
        collection.getProfileCollection().addSegments(Tag.REFERENCE_POINT, medianProfile.getSegments());

        /*
         * With the median profile segments unmerged, also split the segments in
         * the individual nuclei. Requires proportional alignment
         */
        if (collection.isReal()) { // do not handle nuclei in virtual
                                   // collections
            for (Nucleus n : collection.getNuclei()) {
                boolean wasLocked = n.isLocked();
                n.setLocked(false); // Merging segments is not destructive
                splitSegment(n, seg.getID(), proportion, newID1, newID2);
                n.setLocked(wasLocked);
            }
        }

        /*
         * Update the consensus if present
         */
        if (collection.hasConsensus()) {
            Nucleus n = collection.getConsensus();
            boolean wasLocked = n.isLocked();
            n.setLocked(false); // Merging segments is not destructive
            splitSegment(n, seg.getID(), proportion, newID1, newID2);
            n.setLocked(wasLocked);
        }

        // Ensure the vertical nuclei have the same segment pattern
        collection.updateVerticalNuclei();

        return true;

    }

    /**
     * Test all the nuclei of the collection to see if all segments can be split
     * before we carry out the split.
     * 
     * @param seg
     * @param proportion
     * @return
     * @throws ProfileException
     * @throws UnavailableComponentException
     * @throws UnsegmentedProfileException
     */
    private boolean isCollectionSplittable(@NonNull UUID id, double proportion)
            throws ProfileException, UnavailableComponentException, UnsegmentedProfileException {
        
        if(!collection.isReal())
            return false;

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);

        int index = medianProfile.getSegment(id).getProportionalIndex(proportion);

        if (!medianProfile.isSplittable(id, index)) {
            return false;
        }
        
        // check consensus //TODO replace with remove consensus
        if (collection.hasConsensus()) {
            Nucleus n = collection.getConsensus();
            if (!isSplittable(n, id, proportion)) {
                fine("Consensus not splittable");
                return false;
            }
        }

        return collection.isReal() && collection.getNuclei().parallelStream().allMatch(  n->isSplittable(n, id, proportion)  );
    }

    private boolean isSplittable(Taggable t, UUID id, double proportion) {
        
        try {
            ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
            IBorderSegment nSeg = profile.getSegment(id);
            int targetIndex = nSeg.getProportionalIndex(proportion);
            return profile.isSplittable(id, targetIndex);
        } catch (UnavailableComponentException | ProfileException e) {
            fine("Error getting profile", e);
            return false;
        }
        
    }

    private void splitSegment(Taggable t, UUID idToSplit, double proportion, UUID newID1, UUID newID2)
            throws ProfileException, UnavailableComponentException {

        ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        IBorderSegment nSeg = profile.getSegment(idToSplit);

        int targetIndex = nSeg.getProportionalIndex(proportion);
        profile.splitSegment(nSeg, targetIndex, newID1, newID2);
        t.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);

    }

    /**
     * Unmerge the given segment into two segments
     * 
     * @param seg
     *            the segment to unmerge
     * @return
     * @throws UnsegmentedProfileException
     * @throws ProfileException
     * @throws UnavailableComponentException
     */
    public void unmergeSegments(@NonNull IBorderSegment seg)
            throws ProfileException, UnsegmentedProfileException, UnavailableComponentException {
                
        if(seg==null)
            throw new IllegalArgumentException("Segment to unmerge cannot be null");

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);

        // Get the segments to merge
        IBorderSegment test = medianProfile.getSegment(seg.getID());
        if (!test.hasMergeSources()) {
            fine("Segment has no merge sources - cannot unmerge");
            return;
        }
        
        if(collection.isReal()){
            // Check that the state of all nuclei in the collection is consistent
            int error = 0;
            for (Nucleus n : collection.getNuclei()) {
                if(!hasSegmentsMatchingMedian(n, medianProfile)){
                    error++;
                }
            }

            if(error>0){
                warn(String.format("Segments are out of sync with median for %d nuclei", error));
                warn("Canceling unmerge");
                return;
            }
        }

        // unmerge the two segments in the median - this is only a copy of the
        // profile collection
        medianProfile.unmergeSegment(seg);

        // put the new segment pattern back with the appropriate offset
        collection.getProfileCollection().addSegments(Tag.REFERENCE_POINT, medianProfile.getSegments());

        /*
         * With the median profile segments unmerged, also unmerge the segments
         * in the individual nuclei
         */
        
        if(collection.isReal()){
            for (Nucleus n : collection.getNuclei()) {
                boolean wasLocked = n.isLocked();
                n.setLocked(false);
                unmergeSegments(n, seg.getID());
                n.setLocked(wasLocked);
            }
        }


        /*
         * Update the consensus if present
         */
        if (collection.hasConsensus()) {
            Nucleus n = collection.getConsensus();
            unmergeSegments(n, seg.getID());
        }

        // Ensure the vertical nuclei have the same segment pattern
        collection.updateVerticalNuclei();
    }

    private void unmergeSegments(Taggable t, UUID id) throws ProfileException, UnavailableComponentException {
        ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        IBorderSegment nSeg = profile.getSegment(id);
        profile.unmergeSegment(nSeg);
        t.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
    }

}
