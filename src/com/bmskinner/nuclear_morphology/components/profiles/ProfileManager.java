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
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.analysis.profiles.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
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
     * @param lm the landmark to fit
     * @param type the profile type to fit against
     * @param median the template profile to offset against
     * @throws ProfileException 
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     * @throws  
     */
    public void updateLandmarkToMedianBestFit(@NonNull Landmark lm, 
    		@NonNull ProfileType type,
    		@NonNull IProfile median) throws MissingProfileException, ProfileException, MissingLandmarkException {
    	
    	for(Nucleus n : collection.getNuclei()) {
    		if(n.isLocked())
    			continue;
    		
    		// Get the nucleus profile starting at the landmark
    		// Find the best offset needed to make it match the median profile
    		int offset = n.getProfile(type, lm).findBestFitOffset(median);
    		
    		// Update the landmark position to the original index plus the offset
    		n.setLandmark(lm, n.wrapIndex(n.getBorderIndex(lm)+offset));
    		
    		// Update any stats that are based on orientation
            if (lm.equals(Landmark.TOP_VERTICAL) || lm.equals(Landmark.BOTTOM_VERTICAL)) {
                n.updateDependentStats();
                setOpUsingTvBv(n);
            }
    	}
    }

    /**
     * Add the given offset to each of the profile types in the
     * ProfileCollection except for the frankencollection
     * 
     * @param tag
     * @param index
     */
    public void updateProfileCollectionOffsets(Landmark tag, int index) {

        // check the index for wrapping - observed problem when OP==RP in
        // rulesets

        index = CellularComponent.wrapIndex(index, getProfileLength());

        for (ProfileType type : ProfileType.values()) {
//            if (type.equals(ProfileType.FRANKEN)) {
//                continue;
//            }

            collection.getProfileCollection().addIndex(tag, index);
        }
    }
    
    /**
     * Use the collection's ruleset to calculate the positions of the top and
     * bottom verticals in the median profile, and assign these to the nuclei
     * @throws ProfileException 
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     */
    public void calculateTopAndBottomVerticals() throws MissingProfileException, ProfileException, MissingLandmarkException {

        LOGGER.fine("Detecting top and bottom verticals in collection");

        try {
            int topIndex = ProfileIndexFinder.identifyIndex(collection, Landmark.TOP_VERTICAL);
            int btmIndex = ProfileIndexFinder.identifyIndex(collection, Landmark.BOTTOM_VERTICAL);

            LOGGER.fine(()->String.format("TV in median is located at index %i", topIndex));
            LOGGER.fine(()->String.format("BV in median is located at index %i ", btmIndex));

            updateProfileCollectionOffsets(Landmark.TOP_VERTICAL, topIndex);

            updateProfileCollectionOffsets(Landmark.BOTTOM_VERTICAL, btmIndex);

        } catch (NoDetectedIndexException e) {
            LOGGER.fine("Cannot find TV or BV in median profile");
            return;
        }

        IProfile topMedian = collection.getProfileCollection()
        			.getProfile(ProfileType.ANGLE,
        					Landmark.TOP_VERTICAL,
        					Stats.MEDIAN);

        IProfile btmMedian = collection.getProfileCollection()
        			.getProfile(ProfileType.ANGLE, 
        					Landmark.BOTTOM_VERTICAL,
        					Stats.MEDIAN);
        updateLandmarkToMedianBestFit(Landmark.TOP_VERTICAL, ProfileType.ANGLE, topMedian);
        updateLandmarkToMedianBestFit(Landmark.BOTTOM_VERTICAL, ProfileType.ANGLE, btmMedian);

        LOGGER.fine("Updated nuclei");
    }
    

    /**
     * Copy the tag index from cells in the given source collection
     * to cells with the same ID in this collection. This is intended
     * to be use to ensure tag indexes are consistent between cells after a
     * collection has been duplicated (e.g. after a merge of datasets) 
     * @param source the collection to take tag indexes from
     * @throws ProfileException 
     * @throws MissingLandmarkException 
     * @throws MissingProfileException 
     * @throws IndexOutOfBoundsException 
     */
    public void copyTagIndexesToCells(@NonNull ICellCollection source) throws IndexOutOfBoundsException, MissingProfileException, MissingLandmarkException, ProfileException {
    	for(Nucleus n : collection.getNuclei()) {
    		if(!source.contains(n)) 
    			continue;
    		
    		Nucleus template = source.getNucleus(n.getID()).get();
    		
    		Map<Landmark, Integer> tags = template.getLandmarks();
    		for(Entry<Landmark, Integer> entry : tags.entrySet()) {
    			
    			// RP should never change in re-segmentation, so don't
    			// affect it here. This would risk moving RP off a
    			// segment boundary
    			if(entry.getKey().equals(Landmark.REFERENCE_POINT))
    				continue;
    			n.setLandmark(entry.getKey(), entry.getValue());
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
     * @throws MissingLandmarkException
     * @throws ProfileException
     * @throws MissingProfileException
     */
    public void updateBorderTag(Landmark tag, int index) throws ProfileException,
            MissingLandmarkException, MissingProfileException {

        LOGGER.finer( "Updating border tag " + tag);
        if (tag.type().equals(LandmarkType.CORE)) {
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
     * @throws MissingLandmarkException
     * @throws MissingProfileException
     */
    private void updateExtendedBorderTagIndex(@NonNull Landmark tag, int index) throws ProfileException,
            MissingLandmarkException, MissingProfileException {

        int oldIndex = collection.getProfileCollection().getIndex(tag);

        if (oldIndex == -1) {
            LOGGER.finer( "Landmark does not exist and will be created in each nucleus");
            for(Nucleus n : collection.getNuclei()) {
            	n.setLandmark(tag, 0);
            }
        }
        
        // If the new index for the tag is the same as the RP, set directly
        
        List<Landmark> tags = collection.getProfileCollection().getLandmarks();
        for(Landmark existingTag : tags) {
        	if(existingTag.equals(tag))
        		continue;
        	int existingTagIndex = collection.getProfileCollection().getIndex(existingTag);
        	if(index==existingTagIndex) {
        		updateProfileCollectionOffsets(tag, index);
        		
        		// update nuclei - allow possible parallel processing
        		for(Nucleus  n : collection.getNuclei()) {
        			int existingIndex = n.getBorderIndex(existingTag);
    				n.setLandmark(tag, existingIndex);
    				if (tag.equals(Landmark.TOP_VERTICAL) || tag.equals(Landmark.BOTTOM_VERTICAL)) {
        				n.updateDependentStats();
        				setOpUsingTvBv(n);
    				}
        		}
        		
        		//Update consensus
        		if (collection.hasConsensus()) {
        			Nucleus n = collection.getRawConsensus();
        			int existingIndex = n.getBorderIndex(existingTag);
        			n.setLandmark(tag, existingIndex);
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
        updateLandmarkToMedianBestFit(tag, ProfileType.ANGLE, median);

        /*
         * Set the border tag in the consensus median profile
         */
        if (collection.hasConsensus()) {
            Nucleus n = collection.getRawConsensus();
            int newIndex = n.getProfile(ProfileType.ANGLE).findBestFitOffset(median);
            n.setLandmark(tag, newIndex);
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
     * @throws ProfileException 
     * @throws MissingLandmarkException 
     * @throws MissingProfileException 
     * @throws IndexOutOfBoundsException 
     */
    private void setOpUsingTvBv(@NonNull final Nucleus n) throws IndexOutOfBoundsException, MissingProfileException, MissingLandmarkException, ProfileException {
    	// also update the OP to be directly below the CoM in vertically oriented nucleus
		if(n.hasLandmark(Landmark.TOP_VERTICAL) && n.hasLandmark(Landmark.BOTTOM_VERTICAL)) {
			LOGGER.finer( "Updating OP due to TV or BV change");
			Nucleus vertN = n.getOrientedNucleus();
			IPoint bottom = vertN.getBorderList().stream()
				.filter(p-> p.getY()<vertN.getCentreOfMass().getY())
				.min(Comparator.comparing(p->Math.abs(p.getX()-vertN.getCentreOfMass().getX()))).get();
			int newOp = vertN.getBorderIndex(bottom);
			n.setLandmark(Landmark.ORIENTATION_POINT, newOp);
		}
    }

    /**
     * If a core border tag is moved, segment boundaries must be moved. 
     * It is left to calling classes to perform a resegmentation of the dataset.
     * 
     * @param tag the core tag to be updated
     * @param index the new index of the tag in the median, relative to the current RP
     * @throws ProfileException
     * @throws MissingLandmarkException
     * @throws MissingProfileException
     * @throws UnsegmentedProfileException 
     * @throws SegmentUpdateException 
     */
    private void updateCoreBorderTagIndex(@NonNull Landmark tag, int index)
            throws MissingLandmarkException, ProfileException, MissingProfileException, UnsegmentedProfileException {

        LOGGER.fine("Updating core border tag index");

        // Get the median zeroed on the RP
        ISegmentedProfile oldMedian = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);

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
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     */
    private void moveRp(int newRpIndex, @NonNull ISegmentedProfile oldMedian) throws ProfileException, MissingProfileException, MissingLandmarkException {
    	// This is the median we will use to update individual nuclei
    	ISegmentedProfile newMedian = oldMedian.startFrom(newRpIndex);

    	updateLandmarkToMedianBestFit(Landmark.REFERENCE_POINT, ProfileType.ANGLE, newMedian);
    	
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
            return pc.getSegments(Landmark.REFERENCE_POINT).size();
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
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     * 
     * @throws Exception
     */
    public void recalculateProfileAggregates() throws ProfileException, MissingLandmarkException, MissingProfileException {
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
     * @throws MissingProfileException 
     */
    public void copySegmentsAndLandmarksTo(@NonNull final ICellCollection destination) throws ProfileException, MissingProfileException {

        // Get the corresponding profile collection from the template
    	IProfileCollection sourcePC = collection.getProfileCollection();
    	try {
    		List<IProfileSegment> segments = sourcePC.getSegments(Landmark.REFERENCE_POINT);
    		if(segments.isEmpty())
    			throw new ProfileException("No segments in profile of "+collection.getName());
    		
    		LOGGER.fine("Got existing list of " + segments.size() + " segments");

    		// Create a new profile collection for the destination, so profiles are refreshed
    		IProfileCollection destPC = destination.getProfileCollection();
    		destPC.createProfileAggregate(destination, destination.getMedianArrayLength());
    		LOGGER.fine("Created new profile aggregate with length " + destination.getMedianArrayLength());

    		// Copy the tags from the source collection
    		// Use proportional indexes to allow for a changed aggregate length
    		// Note: only the RP must be at a segment boundary. Some mismatches may occur

    		for (Landmark key : sourcePC.getLandmarks()) {
    			double prop = sourcePC.getProportionOfIndex(key);
    			int adj = destPC.getIndexOfProportion(prop);
    			destPC.addIndex(key, adj);
    		}

    		// Copy the segments, also adjusting the lengths using profile interpolation
    		ISegmentedProfile sourceMedian = sourcePC.getSegmentedProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT, Stats.MEDIAN);
    		ISegmentedProfile interpolatedMedian = sourceMedian.interpolate(destination.getMedianArrayLength());

    		destPC.addSegments(Landmark.REFERENCE_POINT, interpolatedMedian.getSegments());


    		LOGGER.fine("Copied tags to new collection");


    		// Final sanity check - did the segment IDs get copied properly?
    		LOGGER.fine("Testing profiles");
    		List<IProfileSegment> newSegs;
    		try {
    			newSegs = destPC.getSegments(Landmark.REFERENCE_POINT);
    		} catch (MissingLandmarkException e1) {
    			LOGGER.warning("RP not found in destination collection");
    			LOGGER.log(Loggable.STACK, "Error getting destination segments from RP", e1);
    			return;
    		}

    		if(segments.size()!=newSegs.size())
    			throw new ProfileException("Segments are not consistent with the old profile: were "+segments.size()+", now "+newSegs.size());

    		for(int i=0; i<newSegs.size(); i++){
    			// Start and end points can change, but id and lock state should be consistent
    			// Check ids are in correct order
    			if(!segments.get(i).getID().equals(newSegs.get(i).getID())){
    				throw new ProfileException("Segment IDs are not consistent with the old profile");
    			}
    			
    			// Check lock state preserved
    			if(segments.get(i).isLocked() != newSegs.get(i).isLocked()){
    				throw new ProfileException("Segment lock state not consistent with the old profile");
    			}
    		}

    	} catch (MissingLandmarkException e1) {
    		LOGGER.warning("RP not found in source collection");
    		LOGGER.log(Loggable.STACK, "Error getting segments from RP", e1);
    	}
    }
    
    /**
     * Set the lock state for the given segment across the collection
     * @param segId
     * @param lockState
     */
    public void setLockOnSegment(@NonNull UUID segId, boolean lockState) {
    	collection.getNuclei().forEach(n-> n.setSegmentStartLock(lockState, segId));
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
     * @throws MissingComponentException 
     */
    public void updateCellSegmentStartIndex(@NonNull ICell cell, @NonNull UUID id, int index) throws ProfileException, MissingComponentException {

    	LOGGER.fine("Updating segment start index");
    	
        Nucleus n = cell.getPrimaryNucleus();
        ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);

        IProfileSegment seg = profile.getSegment(id);

        int startPos = seg.getStartIndex();
        int newStart = index;
        int newEnd = seg.getEndIndex();

        int rawOldIndex = n.getIndexRelativeTo(Landmark.REFERENCE_POINT, startPos);

        try {
        	if (profile.update(seg, newStart, newEnd)) {
        		LOGGER.finer( String.format("Updating profile segment %s to %s-%s succeeded", seg.getName(), newStart, newEnd));
        		LOGGER.finer( "Profile now: "+profile.toString());
        		n.setSegments(profile.getSegments());
        		LOGGER.finest( "Updated nucleus profile with new segment boundaries");

        		/*
        		 * Check the landmarks - if they overlap the old index replace
        		 * them.
        		 */
        		int rawIndex = n.getIndexRelativeTo(Landmark.REFERENCE_POINT, index);

        		LOGGER.finest( "Updating to index " + index + " from reference point");
        		LOGGER.finest( "Raw old border point is index " + rawOldIndex);
        		LOGGER.finest( "Raw new border point is index " + rawIndex);

        		Landmark landmarkToUpdate = n.getBorderTag(rawOldIndex);
        		LOGGER.finer("Updating tag " + landmarkToUpdate);
        		n.setLandmark(landmarkToUpdate, rawIndex);
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
     * @throws MissingComponentException
     * @throws Exception
     */
    public void updateMedianProfileSegmentIndex(boolean start, UUID id, int index)
            throws ProfileException, UnsegmentedProfileException, MissingComponentException {

        ISegmentedProfile oldProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Landmark.REFERENCE_POINT, Stats.MEDIAN);

        IProfileSegment seg = oldProfile.getSegment(id);

        // Check if the start or end of the segment is updated, and select the
        // new endpoints appropriately
        int newStart = start ? index : seg.getStartIndex();
        int newEnd = start ? seg.getEndIndex() : index;

        // if the segment is the orientation point or reference point boundary,
        // update it

        if (start) {
            if (seg.getStartIndex() == collection.getProfileCollection().getIndex(Landmark.ORIENTATION_POINT)) {
                collection.getProfileCollection().addIndex(Landmark.ORIENTATION_POINT, index);
            }

            if (seg.getStartIndex() == collection.getProfileCollection().getIndex(Landmark.REFERENCE_POINT)) {
                collection.getProfileCollection().addIndex(Landmark.REFERENCE_POINT, index);
            }
        }

        // Move the appropriate segment endpoint
        try {
            if (oldProfile.update(seg, newStart, newEnd)) {
                collection.getProfileCollection().addSegments(Landmark.REFERENCE_POINT, oldProfile.getSegments());

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
     * @throws MissingLandmarkException
     */
    public boolean testSegmentsMergeable(IProfileSegment seg1, IProfileSegment seg2)
            throws MissingLandmarkException {
    	
    	if(!seg1.nextSegment().getID().equals(seg2.getID())) {
    		LOGGER.fine("Segments are not adjacent; cannot merge");
    		return false;
    	}

        // check the boundaries of the segment - we do not want to merge across the RP
        for (Landmark tag : DefaultLandmark.values(LandmarkType.CORE)) {

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
     * @throws MissingComponentException
     */
    public void mergeSegments(@NonNull IProfileSegment seg1, @NonNull IProfileSegment seg2, @NonNull UUID newID)
            throws ProfileException, MissingComponentException {
    	// Note - we can't do the root check here. It must be at the segmentation handler level
    	// otherwise updating child datasets to match a root will fail
    	
        ISegmentedProfile medianProfile = collection.getProfileCollection()
        		.getSegmentedProfile(ProfileType.ANGLE,
                Landmark.REFERENCE_POINT, Stats.MEDIAN);

         // Only try the merge if both segments are present in the profile
        if (!medianProfile.hasSegment(seg1.getID()))
            throw new IllegalArgumentException("Median profile does not have segment 1 with ID "+seg1.getID());

        if (!medianProfile.hasSegment(seg2.getID()))
            throw new IllegalArgumentException("Median profile does not have segment 2 with ID "+seg1.getID());

        // Note - validation is run in segmentation handler
    
        // merge the two segments in the median
        medianProfile.mergeSegments(seg1, seg2, newID);
        // put the new segment pattern back with the appropriate offset
        collection.getProfileCollection().addSegments(Landmark.REFERENCE_POINT, medianProfile.getSegments());


        /*
         * With the median profile segments merged, also merge the segments
         * in the individual nuclei
         */
        for (Nucleus n : collection.getNuclei()) {
        	boolean wasLocked = n.isLocked();
        	n.setLocked(false); // Merging segments is not destructive
        	mergeSegments(n, seg1, seg2, newID);
        	n.setLocked(wasLocked);
        }

        /* Update the consensus if present */
        if (collection.hasConsensus()) {
            Nucleus n = collection.getRawConsensus();
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
     * @throws MissingComponentException
     */
    private void mergeSegments(@NonNull Taggable p, @NonNull IProfileSegment seg1, 
    		@NonNull IProfileSegment seg2, @NonNull UUID newID)
            throws ProfileException, MissingComponentException {
        ISegmentedProfile profile = p.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
        
        // Only try the merge if both segments are present in the profile
        if (!profile.hasSegment(seg1.getID()))
            throw new IllegalArgumentException("Median profile does not have segment 1 with ID "+seg1.getID());

        if (!profile.hasSegment(seg2.getID()))
            throw new IllegalArgumentException("Median profile does not have segment 2 with ID "+seg1.getID());

        profile.mergeSegments(seg1.getID(), seg2.getID(), newID);
        p.setSegments(profile.getSegments());
    }

    /**
     * Split the given segment into two segmnets. The split is made at the given
     * index. The new segment ids are generated randomly
     * 
     * @param segName
     * @return
     * @throws MissingComponentException 
     * @throws UnsegmentedProfileException 
     * @throws ProfileException 
     * @throws Exception
     */
    public boolean splitSegment(IProfileSegment seg) throws ProfileException, UnsegmentedProfileException, MissingComponentException {
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
     * @throws MissingComponentException
     *             if the reference point tag is missing, or the segment is
     *             missing
     */
    public boolean splitSegment(@NonNull IProfileSegment seg, @Nullable UUID newID1, @Nullable UUID newID2)
            throws ProfileException, UnsegmentedProfileException, MissingComponentException {

        if (seg == null)
            throw new IllegalArgumentException("Segment cannot be null");

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Landmark.REFERENCE_POINT, Stats.MEDIAN);

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
        collection.getProfileCollection().addSegments(Landmark.REFERENCE_POINT, medianProfile.getSegments());

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
        	Nucleus n = collection.getRawConsensus();
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
     * @throws MissingComponentException
     */
    private void splitNucleusSegment(Nucleus n, UUID segId, double proportion, UUID newId1, UUID newId2) throws ProfileException, MissingComponentException {
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
     * @throws MissingComponentException
     * @throws UnsegmentedProfileException
     */
    private boolean isCollectionSplittable(@NonNull UUID id, double proportion)
            throws ProfileException, MissingComponentException, UnsegmentedProfileException {

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Landmark.REFERENCE_POINT, Stats.MEDIAN);

        int index = medianProfile.getSegment(id).getProportionalIndex(proportion);

        if (!medianProfile.isSplittable(id, index)) {
        	LOGGER.fine("Median profile in "+collection.getName()+" is not splittable");
        	return false;
        }
            
        // check consensus //TODO replace with remove consensus
        if (collection.hasConsensus()) {
        	Nucleus n = collection.getRawConsensus();
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
            ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
            IProfileSegment nSeg = profile.getSegment(id);
            int targetIndex = nSeg.getProportionalIndex(proportion);
            return profile.isSplittable(id, targetIndex);
        } catch (MissingComponentException | ProfileException e) {
        	LOGGER.log(Loggable.STACK, "Error getting profile", e);
            return false;
        }
        
    }

    private void splitSegment(Taggable t, UUID idToSplit, double proportion, UUID newID1, UUID newID2)
            throws ProfileException, MissingComponentException {

        ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
        IProfileSegment nSeg = profile.getSegment(idToSplit);

        int targetIndex = nSeg.getProportionalIndex(proportion);
        profile.splitSegment(nSeg, targetIndex, newID1, newID2);
        t.setSegments(profile.getSegments());

    }

    /**
     * Unmerge the given segment into two segments
     * 
     * @param seg the segment to unmerge
     * @return
     * @throws UnsegmentedProfileException
     * @throws ProfileException
     * @throws MissingComponentException
     */
    public void unmergeSegments(@NonNull UUID segId)
            throws ProfileException, UnsegmentedProfileException, MissingComponentException {
    	
        if(segId==null)
            throw new IllegalArgumentException("Segment to unmerge cannot be null");

        ISegmentedProfile medianProfile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE,
                Landmark.REFERENCE_POINT, Stats.MEDIAN);

        // Get the segments to merge
        IProfileSegment test = medianProfile.getSegment(segId);
        if (!test.hasMergeSources()) {
            LOGGER.fine("Segment has no merge sources - cannot unmerge");
            return;
        }
        
        // unmerge the two segments in the median - this is only a copy of the profile collection
        medianProfile.unmergeSegment(segId);

        // put the new segment pattern back with the appropriate offset
        collection.getProfileCollection().addSegments(Landmark.REFERENCE_POINT, medianProfile.getSegments());

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
        	Nucleus n = collection.getRawConsensus();
            unmergeSegments(n, segId);
        }
    }

    private void unmergeSegments(@NonNull Taggable t, @NonNull UUID id) throws ProfileException, MissingComponentException {
        ISegmentedProfile profile = t.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT);
        profile.unmergeSegment(id);
        t.setSegments(profile.getSegments());
    }

}
