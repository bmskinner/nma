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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.BorderTagObject.BorderTagType;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment.SegmentUpdateException;
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
public class ProfileManager {
	
	private static final Logger LOGGER = Logger.getLogger(ProfileManager.class.getName());
    private final ICellCollection collection;

    public ProfileManager(final ICellCollection collection) {
        this.collection = collection;
    }

    public int getProfileLength() {
        return collection.getProfileCollection().length();
    }

    /**
     * Update the given tag in each nucleus of the collection to the index with
     * a best fit of the profile to the given median profile
     * 
     * @param tag the tag to fit
     * @param type the profile type to fit against
     * @param median the template profile to offset against
     */
    public void updateTagToMedianBestFit(@NonNull Tag tag, 
    		@NonNull ProfileType type,
    		@NonNull IProfile median) {

        collection.getNuclei().parallelStream().forEach(n -> {
            if (!n.isLocked()) {
            	try {
            		// Find positive offset index of this profile which best
            		// matches the median profile
            		int newIndex = n.getProfile(type).findBestFitOffset(median);
            		
            		n.setBorderTag(tag, newIndex);

                } catch (ProfileException | UnavailableProfileTypeException e1) {
                    LOGGER.log(Level.SEVERE, 
                    		"Error updating tag by offset in nucleus " + n.getNameAndNumber(), 
                    		e1);
                    return;
                }

            	// Update any stats that are based on orientation
                if (tag.equals(Tag.TOP_VERTICAL) || tag.equals(Tag.BOTTOM_VERTICAL)) {
                    n.updateDependentStats();
                    setOpUsingTvBv(n);
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
     * Use the collection's ruleset to calculate the positions of the top and
     * bottom verticals in the median profile, and assign these to the nuclei
     */
    public void calculateTopAndBottomVerticals() {

        LOGGER.fine("Detecting top and bottom verticals in collection");

        try {
            ProfileIndexFinder finder = new ProfileIndexFinder();

            int topIndex = finder.identifyIndex(collection, Tag.TOP_VERTICAL);
            int btmIndex = finder.identifyIndex(collection, Tag.BOTTOM_VERTICAL);

            LOGGER.fine(()->String.format("TV in median is located at index %i", topIndex));
            LOGGER.fine(()->String.format("BV in median is located at index %i ", btmIndex));

            updateProfileCollectionOffsets(Tag.TOP_VERTICAL, topIndex);

            updateProfileCollectionOffsets(Tag.BOTTOM_VERTICAL, btmIndex);

        } catch (NoDetectedIndexException e) {
            LOGGER.fine("Cannot find TV or BV in median profile");
            return;
        }

        LOGGER.fine("Updating nuclei");

        IProfile topMedian;
        IProfile btmMedian;

        try {
        	topMedian = collection.getProfileCollection()
        			.getProfile(ProfileType.ANGLE,
        					Tag.TOP_VERTICAL,
        					Stats.MEDIAN);

        	btmMedian = collection.getProfileCollection()
        			.getProfile(ProfileType.ANGLE, 
        					Tag.BOTTOM_VERTICAL,
        					Stats.MEDIAN);
        } catch (ProfileException 
        		| UnavailableBorderTagException 
        		| UnavailableProfileTypeException e) {
        	LOGGER.log(Level.SEVERE, "Error getting TV or BV profile", e);
            return;
        }

        updateTagToMedianBestFit(Tag.TOP_VERTICAL, ProfileType.ANGLE, topMedian);
        updateTagToMedianBestFit(Tag.BOTTOM_VERTICAL, ProfileType.ANGLE, btmMedian);

        LOGGER.fine("Updated nuclei");
    }
    

    /**
     * Copy the tag index from cells in the given source collection
     * to cells with the same ID in this collection. This is intended
     * to be use to ensure tag indexes are consistent between cells after a
     * collection has been duplicated (e.g. after a merge of datasets) 
     * @param source the collection to take tag indexes from
     */
    public void copyTagIndexesToCells(@NonNull ICellCollection source) {
    	for(Nucleus n : collection.getNuclei()) {
    		if(!source.contains(n)) 
    			continue;
    		
    		Nucleus template = source.getNucleus(n.getID()).get();
    		
    		Map<Tag, Integer> tags = template.getBorderTags();
    		for(Entry<Tag, Integer> entry : tags.entrySet()) {
    			
    			// RP should never change in re-segmentation, so don't
    			// affect it here. This would risk moving RP off a
    			// segment boundary
    			if(entry.getKey().equals(Tag.REFERENCE_POINT))
    				continue;
    			n.setBorderTag(entry.getKey(), entry.getValue());
    		}    		
    	}
    	LOGGER.fine("Updated tag indexes from source collection");
    }

    /**
     * Update the location of the given border tag within the profile
     * 
     * @param tag the tag to be updated
     * @param index the new index of the tag in the median, relative to the current RP
     * @throws IndexOutOfBoundsException
     * @throws UnavailableBorderTagException
     * @throws ProfileException
     * @throws UnavailableProfileTypeException
     */
    public void updateBorderTag(Tag tag, int index) throws ProfileException,
            UnavailableBorderTagException, UnavailableProfileTypeException {

        LOGGER.finer( "Updating border tag " + tag);
        if (tag.type().equals(BorderTagType.CORE)) {
            try {
				updateCoreBorderTagIndex(tag, index);
			} catch (UnsegmentedProfileException e) {
				LOGGER.log(Loggable.STACK, "Profile is not segmented", e);
			}
        } else {
        	updateExtendedBorderTagIndex(tag, index);
        }
    }

    /**
     * Update the extended border tags that don't need resegmenting
     * 
     * @param tag the extended tag to be updated
     * @param index the new index of the tag in the median, relative to the current RP
     * @throws ProfileException
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     */
    private void updateExtendedBorderTagIndex(@NonNull Tag tag, int index) throws ProfileException,
            UnavailableBorderTagException, UnavailableProfileTypeException {

        int oldIndex = collection.getProfileCollection().getIndex(tag);

        if (oldIndex == -1)
            LOGGER.finer( "Border tag does not exist and will be created");
        
        // If the new index for the tag is the same as the RP, set directly
        
        List<Tag> tags = collection.getProfileCollection().getBorderTags();
        for(Tag existingTag : tags) {
        	if(existingTag.equals(tag))
        		continue;
        	int existingTagIndex = collection.getProfileCollection().getIndex(existingTag);
        	if(index==existingTagIndex) {
        		updateProfileCollectionOffsets(tag, index);
        		
        		// update nuclei - allow possible parallel processing
        		collection.getNuclei().parallelStream().filter(n->!n.isLocked())
        		.forEach(n->{
        			try {
        				int existingIndex = n.getBorderIndex(existingTag);
        				n.setBorderTag(tag, existingIndex);
        				if (tag.equals(Tag.TOP_VERTICAL) || tag.equals(Tag.BOTTOM_VERTICAL)) {
            				n.updateDependentStats();
            				setOpUsingTvBv(n);
        				}
        			} catch (UnavailableBorderTagException e) {
        				LOGGER.log(Loggable.STACK, "Border tag not available", e);
        			}
        		});
        		
        		//Update consensus
        		if (collection.hasConsensus()) {
        			Nucleus n = collection.getRawConsensus().component();
        			int existingIndex = n.getBorderIndex(existingTag);
        			n.setBorderTag(tag, existingIndex);
        			setOpUsingTvBv(n);
        		}

        		// Update signals as needed
        		collection.getSignalManager().recalculateSignalAngles();
            	return;
        	}
        }
                
        /*
         * Set the border tag in the median profile
         */
        LOGGER.finer( "Setting " + tag + " in median profiles to " + index + " from " + oldIndex);
        updateProfileCollectionOffsets(tag, index);

        // Use the median profile to set the tag in the nuclei

        IProfile median = collection.getProfileCollection().getProfile(ProfileType.ANGLE, tag, Stats.MEDIAN);

        LOGGER.finer( "Updating tag in nuclei");
        updateTagToMedianBestFit(tag, ProfileType.ANGLE, median);

        /*
         * Set the border tag in the consensus median profile
         */
        if (collection.hasConsensus()) {
            Nucleus n = collection.getRawConsensus().component();
            int newIndex = n.getProfile(ProfileType.ANGLE).findBestFitOffset(median);
            n.setBorderTag(tag, newIndex);
            setOpUsingTvBv(n);
        }
        
     // Update signals as needed
        collection.getSignalManager().recalculateSignalAngles();

    }
    
    
    /**
     * If the TV and BV are present, move the OP to a more sensible 
     * position for signal angle measurement: the border point directly
     * below the centre of mass.
     * @param n the nucleus to alter
     */
    private void setOpUsingTvBv(@NonNull final Nucleus n) {
    	// also update the OP to be directly below the CoM in vertically oriented nucleus
		if(n.hasBorderTag(Tag.TOP_VERTICAL) && n.hasBorderTag(Tag.BOTTOM_VERTICAL)) {
			LOGGER.finer( "Updating OP due to TV or BV change");
			Nucleus vertN = n.getVerticallyRotatedNucleus();
			IBorderPoint bottom = vertN.getBorderList().stream()
				.filter(p-> p.getY()<vertN.getCentreOfMass().getY())
				.min(Comparator.comparing(p->Math.abs(p.getX()-vertN.getCentreOfMass().getX()))).get();
			int newOp = vertN.getBorderIndex(bottom);
			n.setBorderTag(Tag.ORIENTATION_POINT, newOp);
		}
    }

    /**
     * If a core border tag is moved, segment boundaries must be moved. 
     * It is left to calling classes to perform a resegmentation of the dataset.
     * 
     * @param tag the core tag to be updated
     * @param index the new index of the tag in the median, relative to the current RP
     * @throws ProfileException
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     * @throws UnsegmentedProfileException 
     * @throws SegmentUpdateException 
     */
    private void updateCoreBorderTagIndex(@NonNull Tag tag, int index)
            throws UnavailableBorderTagException, ProfileException, UnavailableProfileTypeException, UnsegmentedProfileException {

        LOGGER.fine("Updating core border tag index");

        // Get the median zeroed on the RP
        ISegmentedProfile oldMedian = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

        moveRp(index, oldMedian);

        // Update signals as needed
        collection.getSignalManager().recalculateSignalAngles();
    }
    
    /**
     * Move the RP index in nuclei. Update segments flanking the RP
     * without moving any other segments.
     * @param newRpIndex the new index for the RP relative to the old RP
     * @param oldMedian the old median profile zeroed on the old RP
     * @throws ProfileException 
     */
    private void moveRp(int newRpIndex, @NonNull ISegmentedProfile oldMedian) throws ProfileException {
    	// This is the median we will use to update individual nuclei
    	ISegmentedProfile newMedian = oldMedian.offset(newRpIndex);

    	updateTagToMedianBestFit(Tag.REFERENCE_POINT, ProfileType.ANGLE, newMedian);
    	
    	// Rebuild the profile aggregate in the collection
    	collection.getProfileCollection().createProfileAggregate(collection, collection.getMedianArrayLength());
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
            LOGGER.log(Loggable.STACK, "Error getting segment count from collection " + collection.getName(), e);
            return 0;
        }
    }

    /**
     * Regenerate the profile aggregate in each of the profile types of the
     * collection. The length is set to the angle profile length. The zero index
     * of the profile aggregate is the RP.
     * @throws ProfileException 
     * 
     * @throws Exception
     */
    public void recalculateProfileAggregates() throws ProfileException {
        // use the same array length as the source collection to avoid segment slippage
        IProfileCollection pc = collection.getProfileCollection();
        pc.createProfileAggregate(collection, pc.length());
    }

    /**
     * Copy profile offsets from this collection to the destination and
     * build the median profiles for all profile types. Also copy the segments
     * from the regular angle profile onto all other profile types
     * 
     * @param destination the collection to update
     * @throws ProfileException if the copy fails
     */
    public void copyCollectionOffsets(@NonNull final ICellCollection destination) throws ProfileException {

        // Get the corresponding profile collection from the template
        IProfileCollection sourcePC = collection.getProfileCollection();

        List<IProfileSegment> segments;
        try {
            segments = sourcePC.getSegments(Tag.REFERENCE_POINT);
        } catch (UnavailableBorderTagException e1) {
            LOGGER.warning("RP not found in source collection");
            LOGGER.log(Loggable.STACK, "Error getting segments from RP", e1);
            return;
        }
        LOGGER.fine("Got existing list of " + segments.size() + " segments");

        // use the same array length as the source collection to avoid segment slippage
        int profileLength = sourcePC.length();

        
        // Create a new profile collection for the destination, so profiles are refreshed
        IProfileCollection destPC = destination.getProfileCollection();
        destPC.createProfileAggregate(destination, destination.getMedianArrayLength());
        LOGGER.fine("Created new profile aggregate with length " + destination.getMedianArrayLength());
                
        // Copy the tags from the source collection
        // Use proportional indexes to allow for a changed aggregate length
        // Note: only the RP must be at a segment boundary. Some mismatches may occur
        try {
            for (Tag key : sourcePC.getBorderTags()) {
            	double prop = sourcePC.getProportionOfIndex(key);
            	int adj = destPC.getIndexOfProportion(prop);
                destPC.addIndex(key, adj);
            }
            
           // Copy the segments, also adjusting the lengths using profile interpolation
            ISegmentedProfile sourceMedian = sourcePC.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
            ISegmentedProfile interpolatedMedian = sourceMedian.interpolate(destination.getMedianArrayLength());
            
            destPC.addSegments(Tag.REFERENCE_POINT, interpolatedMedian.getSegments());

        } catch (UnavailableBorderTagException | IllegalArgumentException | UnavailableProfileTypeException | UnsegmentedProfileException e) {
            LOGGER.log(Loggable.STACK, "Cannot add segments to RP", e);
        }
        LOGGER.fine("Copied tags to new collection");
        
        
        // Final sanity check - did the segment IDs get copied properly?
        LOGGER.fine("Testing profiles");
        List<IProfileSegment> newSegs;
        try {
            newSegs = destPC.getSegments(Tag.REFERENCE_POINT);
        } catch (UnavailableBorderTagException e1) {
            LOGGER.warning("RP not found in destination collection");
            LOGGER.log(Loggable.STACK, "Error getting destination segments from RP", e1);
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
     * @param b the segment lock state for all segments
     */
    public void setLockOnAllNucleusSegments(boolean b) {
        List<UUID> ids = collection.getProfileCollection().getSegmentIDs();
        collection.getNuclei().forEach(n -> ids.forEach(segID -> n.setSegmentStartLock(b, segID)));
    }

    /**
     * Update the start index of a segment in the angle profile of the given
     * cell.
     * 
     * @param cell the cell to alter
     * @param id the segment id
     * @param index the new start index of the segment
     * @throws ProfileException 
     * @throws UnavailableComponentException 
     */
    public void updateCellSegmentStartIndex(@NonNull ICell cell, @NonNull UUID id, int index) throws ProfileException, UnavailableComponentException {

    	LOGGER.fine("Updating segment start index");
    	
        Nucleus n = cell.getPrimaryNucleus();
        ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);

        IProfileSegment seg = profile.getSegment(id);

        int startPos = seg.getStartIndex();
        int newStart = index;
        int newEnd = seg.getEndIndex();

        int rawOldIndex = n.getOffsetBorderIndex(Tag.REFERENCE_POINT, startPos);

        try {
        	if (profile.update(seg, newStart, newEnd)) {
        		LOGGER.finer( String.format("Updating profile segment %s to %s-%s succeeded", seg.getName(), newStart, newEnd));
        		LOGGER.finer( "Profile now: "+profile.toString());
        		n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
        		LOGGER.finest( "Updated nucleus profile with new segment boundaries");

        		/*
        		 * Check the border tags - if they overlap the old index replace
        		 * them.
        		 */
        		int rawIndex = n.getOffsetBorderIndex(Tag.REFERENCE_POINT, index);

        		LOGGER.finest( "Updating to index " + index + " from reference point");
        		LOGGER.finest( "Raw old border point is index " + rawOldIndex);
        		LOGGER.finest( "Raw new border point is index " + rawIndex);

        		if (n.hasBorderTag(rawOldIndex)) {
        			Tag tagToUpdate = n.getBorderTag(rawOldIndex);
        			LOGGER.fine("Updating tag " + tagToUpdate);
        			n.setBorderTag(tagToUpdate, rawIndex);

        			// Update intersection point if needed
        			if (tagToUpdate.equals(Tag.ORIENTATION_POINT)) {
        				n.setBorderTag(Tag.INTERSECTION_POINT,
        						n.getBorderIndex(n.findOppositeBorder(n.getBorderPoint(Tag.ORIENTATION_POINT))));
        			}
        		}
        		n.updateDependentStats();

        	} else {
        		LOGGER.warning(String.format("Updating %s start index from %s to %s failed", seg.getName(), seg.getStartIndex(), index ));
        	}
        } catch(SegmentUpdateException e) {
        	LOGGER.warning(String.format("Updating %s start index from %s to %s failed", seg.getName(), seg.getStartIndex(), index ));
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

        IProfileSegment seg = oldProfile.getSegment(id);

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
                collection.getProfileCollection().addSegments(Tag.REFERENCE_POINT, oldProfile.getSegments());

                LOGGER.finest( "Segments added, refresh the charts");

            } else {
                LOGGER.warning("Updating " + seg.getStartIndex() + " to index " + index + " failed");
            }
        } catch (SegmentUpdateException e) {
            LOGGER.log(Loggable.STACK, "Error updating segments", e);
        }

    }

    /**
     * Check that the given segment pair can be merged given the 
     * positions of core border tags
     * 
     * @param seg1 the first in the pair to merge
     * @param seg2 the second in the pair to merge
     * @return true if the merge is possible, false otherwise
     * @throws UnavailableBorderTagException
     */
    public boolean testSegmentsMergeable(IProfileSegment seg1, IProfileSegment seg2)
            throws UnavailableBorderTagException {

    	if(!seg1.nextSegment().getID().equals(seg2.getID())) {
    		LOGGER.fine("Segments are not adjacent; cannot merge");
    		return false;
    	}
    		
    		
    	
        // check the boundaries of the segment - we do not want to merge across the RP
        for (Tag tag : BorderTagObject.values(BorderTagType.CORE)) {

             // Find the position of the border tag in the median profile
            int tagIndex = collection.getProfileCollection().getIndex(tag);            
            if(seg1.getEndIndex()==tagIndex || seg2.getStartIndex() == tagIndex) {
            	LOGGER.fine("Segments cross RP; cannot merge");
            	return false;
            }
        }
        return true;
    }
    
    /**
     * Merge the given segments from the median profile, and update each 
     * nucleus in the collection.
     * 
     * @param seg1 the first segment in the pair to merge
     * @param seg2  the second segment in the pair to merge
     * @param newID the id for the merged segment
     * @throws UnsegmentedProfileException if the median profile is not segmented
     * @throws ProfileException if the update fails
     * @throws UnavailableComponentException
     */
    public void mergeSegments(@NonNull IProfileSegment seg1, @NonNull IProfileSegment seg2, @NonNull UUID newID)
            throws ProfileException, UnsegmentedProfileException, UnavailableComponentException {
    	// Note - we can't do the root check here. It must be at the segmentation handler level
    	// otherwise updating child datasets to match a root will fail

        if (seg1 == null || seg2 == null || newID == null)
            throw new IllegalArgumentException("Segment ids cannot be null");
        
        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);

         // Only try the merge if both segments are present in the profile
        if (!medianProfile.hasSegment(seg1.getID()))
            throw new IllegalArgumentException("Median profile does not have segment 1 ID");

        if (!medianProfile.hasSegment(seg2.getID()))
            throw new IllegalArgumentException("Median profile does not have segment 2 ID");

        // Note - validation is run in segmentation handler
    
        // merge the two segments in the median
        try {
            medianProfile.mergeSegments(seg1, seg2, newID);
         // put the new segment pattern back with the appropriate offset
            collection.getProfileCollection().addSegments(Tag.REFERENCE_POINT, medianProfile.getSegments());
        } catch(ProfileException e){
        	LOGGER.log(Loggable.STACK, "Error merging segments in median profile; cancelling merge", e);
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

        /* Update the consensus if present */
        if (collection.hasConsensus()) {
            Nucleus n = collection.getRawConsensus().component();
            mergeSegments(n, seg1, seg2, newID);
        }
    }
    

    /**
     * Merge the segments with the given IDs into a new segment with the given
     * new ID
     * 
     * @param p the object with a segmented profile to merge
     * @param seg1 the first segment to be merged
     * @param seg2 the second segment to be merged
     * @param newID the new ID for the merged segment
     * @throws ProfileException
     * @throws UnavailableComponentException
     */
    private void mergeSegments(@NonNull Taggable p, @NonNull IProfileSegment seg1, @NonNull IProfileSegment seg2, @NonNull UUID newID)
            throws ProfileException, UnavailableComponentException {

        ISegmentedProfile profile = p.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        profile.mergeSegments(seg1.getID(), seg2.getID(), newID);
        p.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
    }

    /**
     * Split the given segment into two segmnets. The split is made at the given
     * index. The new segment ids are generated randomly
     * 
     * @param segName
     * @return
     * @throws UnavailableComponentException 
     * @throws UnsegmentedProfileException 
     * @throws ProfileException 
     * @throws Exception
     */
    public boolean splitSegment(IProfileSegment seg) throws ProfileException, UnsegmentedProfileException, UnavailableComponentException {
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
    public boolean splitSegment(@NonNull IProfileSegment seg, @Nullable UUID newID1, @Nullable UUID newID2)
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
        LOGGER.fine("Testing splittability of collection");
        if (!isCollectionSplittable(seg.getID(), proportion)) {
            LOGGER.warning("Segment cannot be split: profile failed testing");
            return false;
        }

        // split the two segments in the median
        @NonNull UUID nId1 = newID1 == null ? java.util.UUID.randomUUID() : newID1;
        @NonNull UUID nId2 = newID2 == null ? java.util.UUID.randomUUID() : newID2;
        medianProfile.splitSegment(seg, index, nId1, nId2);
        LOGGER.fine("Split median profile");

        // put the new segment pattern back with the appropriate offset
        collection.getProfileCollection().addSegments(Tag.REFERENCE_POINT, medianProfile.getSegments());

        /*
         * With the median profile segments unmerged, also split the segments in
         * the individual nuclei. Requires proportional alignment
         */
        if(collection.isReal()) {
        	for (Nucleus n : collection.getNuclei()) {
        		splitNucleusSegment(n, seg.getID(), proportion, newID1, newID2);
        	}
        }
        
        /*  Update the consensus if present */
        if (collection.hasConsensus()) {
        	Nucleus n = collection.getRawConsensus().component();
        	splitNucleusSegment(n, seg.getID(), proportion, newID1, newID2);
        }
        return true;
    }
    
    /**
     * Split the segment in the given nucleus, preserving lock state
     * @param n the nucleus
     * @param segId the segment to split
     * @param proportion the proportion of the segment to split at (0-1)
     * @param newId1 the first new segment id
     * @param newId2 the second new segment id
     * @throws ProfileException
     * @throws UnavailableComponentException
     */
    private void splitNucleusSegment(Nucleus n, UUID segId, double proportion, UUID newId1, UUID newId2) throws ProfileException, UnavailableComponentException {
    	boolean wasLocked = n.isLocked();
		n.setLocked(false); // not destructive
		splitSegment(n, segId, proportion, newId1, newId2);
		n.setLocked(wasLocked);
    }

    /**
     * Test all the nuclei of the collection to see if all segments can be split
     * before we carry out the split.
     * 
     * @param id the segment to test
     * @param proportion the proportion of the segment at which to split, from 0-1
     * @return true if the segment can be split at the index equivalent to the proportion, false otherwise
     * @throws ProfileException
     * @throws UnavailableComponentException
     * @throws UnsegmentedProfileException
     */
    private boolean isCollectionSplittable(@NonNull UUID id, double proportion)
            throws ProfileException, UnavailableComponentException, UnsegmentedProfileException {

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);

        int index = medianProfile.getSegment(id).getProportionalIndex(proportion);

        if (!medianProfile.isSplittable(id, index)) {
        	LOGGER.fine("Median profile in "+collection.getName()+" is not splittable");
        	return false;
        }
            
        // check consensus //TODO replace with remove consensus
        if (collection.hasConsensus()) {
        	Nucleus n = collection.getRawConsensus().component();
            if (!isSplittable(n, id, proportion)) {
                LOGGER.fine("Consensus not splittable");
                return false;
            }
        }

        if(collection.isReal()) {
        	boolean allNucleiSplittable = collection.getNuclei().parallelStream().allMatch( n->isSplittable(n, id, proportion) );
        	if(!allNucleiSplittable)
        		LOGGER.fine("At least one nucleus in "+collection.getName()+" is not splittable");
        	return allNucleiSplittable;
        }
        return true;
    }

    private boolean isSplittable(Taggable t, UUID id, double proportion) {
        
        try {
            ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
            IProfileSegment nSeg = profile.getSegment(id);
            int targetIndex = nSeg.getProportionalIndex(proportion);
            return profile.isSplittable(id, targetIndex);
        } catch (UnavailableComponentException | ProfileException e) {
        	LOGGER.log(Loggable.STACK, "Error getting profile", e);
            return false;
        }
        
    }

    private void splitSegment(Taggable t, UUID idToSplit, double proportion, UUID newID1, UUID newID2)
            throws ProfileException, UnavailableComponentException {

        ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        IProfileSegment nSeg = profile.getSegment(idToSplit);

        int targetIndex = nSeg.getProportionalIndex(proportion);
        profile.splitSegment(nSeg, targetIndex, newID1, newID2);
        t.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);

    }

    /**
     * Unmerge the given segment into two segments
     * 
     * @param seg the segment to unmerge
     * @return
     * @throws UnsegmentedProfileException
     * @throws ProfileException
     * @throws UnavailableComponentException
     */
    public void unmergeSegments(@NonNull UUID segId)
            throws ProfileException, UnsegmentedProfileException, UnavailableComponentException {
    	
        if(segId==null)
            throw new IllegalArgumentException("Segment to unmerge cannot be null");

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Tag.REFERENCE_POINT, Stats.MEDIAN);

        // Get the segments to merge
        IProfileSegment test = medianProfile.getSegment(segId);
        if (!test.hasMergeSources()) {
            LOGGER.fine("Segment has no merge sources - cannot unmerge");
            return;
        }
        
        // unmerge the two segments in the median - this is only a copy of the profile collection
        medianProfile.unmergeSegment(segId);

        // put the new segment pattern back with the appropriate offset
        collection.getProfileCollection().addSegments(Tag.REFERENCE_POINT, medianProfile.getSegments());

        /*
         * With the median profile segments unmerged, also unmerge the segments
         * in the individual nuclei
         */
        if(collection.isReal()) {
        	for (Nucleus n : collection.getNuclei()) {
        		boolean wasLocked = n.isLocked();
        		n.setLocked(false);
        		unmergeSegments(n, segId);
        		n.setLocked(wasLocked);
        	}
        }
        
        /* Update the consensus if present */
        if (collection.hasConsensus()) {
        	Nucleus n = collection.getRawConsensus().component();
            unmergeSegments(n, segId);
        }
    }

    private void unmergeSegments(@NonNull Taggable t, @NonNull UUID id) throws ProfileException, UnavailableComponentException {
        ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
        profile.unmergeSegment(id);
        t.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
    }

}
