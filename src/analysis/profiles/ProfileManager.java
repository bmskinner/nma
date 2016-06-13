/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.profiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;













import analysis.AnalysisDataset;
import logging.Loggable;
//import analysis.nucleus.DatasetSegmenter.SegmentFitter;
import utility.Constants;
import components.AbstractCellularComponent;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.BorderTag.BorderTagType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

/**
 * This class is designed to simplify operations on CellCollections
 * involving copying and refreshing of ProfileCollections and 
 * ProfileAggregates. It handles movement of tag indexes within the median
 * and the nuclei
 * @author bms41
 *
 */
public class ProfileManager implements Loggable {
	
	final private CellCollection collection;
	
	public ProfileManager(final CellCollection collection){
		this.collection = collection;
	}
	
	/**
	 * Update the given tag in each nucleus of the collection to the index with a best fit
	 * of the profile to the given median profile
	 * @param tag
	 * @param type
	 * @param median
	 */
	public void offsetNucleusProfiles(BorderTag tag, ProfileType type, Profile median){
				
		for(Nucleus n : collection.getNuclei()){

			// returns the positive offset index of this profile which best matches the median profile
			int newIndex = n.getProfile(type).getSlidingWindowOffset(median);
			n.setBorderTag(tag, newIndex);		
			
			if(tag.equals(BorderTag.TOP_VERTICAL) || tag.equals(BorderTag.BOTTOM_VERTICAL)){
				
				n.updateVerticallyRotatedNucleus();
				
			}
		}
		
	}
	
	/**
	 * Create the profile collections to hold angles from nuclear
	 * profiles based on the current nucleus profiles
	 * @return
	 * @throws Exception
	 */
	public void createProfileCollections() {

		/*
		 * Build a first set of profile aggregates
		 * Default is to make profile aggregate from reference point
		 * Do not build an aggregate for the non-existent frankenprofile
		 */
		for(ProfileType type : ProfileType.values()){
			
			if(type.equals(ProfileType.FRANKEN)){
				continue;
			}
			
			fine("Creating profile aggregate: "+type);
			ProfileCollection pc = collection.getProfileCollection(type);
			pc.createProfileAggregate(collection, type);
		}
	}
	
	/**
	 * Add the given offset to each of the profile types in the ProfileCollection
	 * except for the frankencollection
	 * @param tag
	 * @param index
	 */
	public void updateProfileCollectionOffsets(BorderTag tag, int index){
		
		for(ProfileType type : ProfileType.values()){
			if(type.equals(ProfileType.FRANKEN)){
				continue;
			}
			
			collection
				.getProfileCollection(type)
				.addOffset(tag, index);

		}
		
	}
	
	/**
	 * Use the collection's ruleset to calculate the positions of the top and bottom
	 * verticals in the median profile, and assign these to the nuclei
	 */
	public void calculateTopAndBottomVerticals() {

		fine("Detecting top and bottom verticals");

		ProfileIndexFinder finder = new ProfileIndexFinder();

		int topIndex = finder.identifyIndex(collection, BorderTag.TOP_VERTICAL);
		int btmIndex = finder.identifyIndex(collection, BorderTag.BOTTOM_VERTICAL);

		if(topIndex > -1 && btmIndex > -1){

			fine("TV in median is located at index "+topIndex);
			fine("BV in median is located at index "+btmIndex);

			updateProfileCollectionOffsets(BorderTag.TOP_VERTICAL, topIndex);

			updateProfileCollectionOffsets(BorderTag.BOTTOM_VERTICAL, btmIndex);


			fine("Updating nuclei");
			Profile topMedian = collection
					.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.TOP_VERTICAL, Constants.MEDIAN);

			Profile btmMedian = collection
					.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.BOTTOM_VERTICAL, Constants.MEDIAN);

			offsetNucleusProfiles(BorderTag.TOP_VERTICAL, ProfileType.REGULAR, topMedian);

			offsetNucleusProfiles(BorderTag.BOTTOM_VERTICAL, ProfileType.REGULAR, btmMedian);

			for(Nucleus n : collection.getNuclei()){
				n.updateVerticallyRotatedNucleus();
			}

			fine("Updated nuclei");
		} else {
			fine("Cannot find TV or BV in median profile");
		}

	}
	
	
	/**
	 * Update the location of the given border tag within the profile
	 * @param tag
	 * @param index the new index within the median profile
	 */
	public void updateBorderTag(BorderTag tag, int index){
		
		finer("Updating border tag "+tag);
		
		if(tag.type().equals(BorderTagType.CORE )){
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
	 * @param tag
	 * @param index
	 */
	private void updateExtendedBorderTagIndex(BorderTag tag, int index){
		
		int oldIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(tag);
		
		if(oldIndex == -1){
			finer("Border tag does not exist and will be created");
		}

		/*
		 * Set the border tag in the median profile 
		 */
		finest("Setting border tag in median profiles to "+index+ " from "+oldIndex);
		updateProfileCollectionOffsets(tag, index);
		
		// Use the median profile to set the tag in the nuclei

		Profile median = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(tag, Constants.MEDIAN); 
		
		offsetNucleusProfiles(tag, ProfileType.REGULAR, median);


		/*
		 * Set the border tag in the consensus median profile 
		 */
		if(collection.hasConsensusNucleus()){
			Nucleus n = collection.getConsensusNucleus();
			int oldNIndex = n.getBorderIndex(tag);
			int newIndex = n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(median);
			n.setBorderTag(tag, newIndex);

			if(n.hasBorderTag(BorderTag.TOP_VERTICAL) && n.hasBorderTag(BorderTag.BOTTOM_VERTICAL)){
				n.alignPointsOnVertical(n.getBorderTag(BorderTag.TOP_VERTICAL), n.getBorderTag(BorderTag.BOTTOM_VERTICAL));
			} else {
				n.rotatePointToBottom(n.getBorderTag(BorderTag.ORIENTATION_POINT));
			}
			//				
			finest("Set border tag in consensus to "+newIndex+ " from "+oldNIndex);
		}

	}
	
	
	/**
	 * If a core border tag is moved, the profile needs to be resegmented.
	 * @param tag
	 * @param index
	 */
	private void updateCoreBorderTagIndex(BorderTag tag, int index){
		
		
		/*
		 * Updating core border tags:

			1) Identify the new OP or RP index in the median
			   - save out the offsets for the old border tags against the old RP

			2) Update the RP / OP location in nuclei using frankenprofiling
			   - RP update requires ofsetting OP also
			   - border tags should save offsets too

			3) Create a new profile aggregate and profile collection 
			   - use the saved offsets from the old RP to calculate the new offsets for border tags

			4) Resegment the median profile, with the new border tag map

			5) Apply the new segments to the nucleus profiles
			*/
		
		
		fine("Updating core border tag index");
		// Store the existing core points in a map (OP and RP)
		Map<BorderTag, Integer> map = new HashMap<BorderTag, Integer>();
		for(BorderTag test : BorderTag.values(BorderTagType.CORE)){
			int i = collection.getProfileCollection(ProfileType.REGULAR).getOffset(test);
			map.put(test, i);
			finer("Storing existing median "+test+" at index "+i+" in map");
		}
		
		// Overwrite the new tag for segmentation
		map.put(tag, index);
		finer("Replacing median "+tag+" with index "+index+" in segmenter map");
		
		// Store the offset for the new point
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(tag, index);
		finer("Offset the "+tag+"index in the regular profile to "+index);
				
		/*
		 * Now we need to update the tag indexes in the nucleus
		 * profiles.
		 */
		Profile median = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(tag, Constants.MEDIAN);
		finer("Fetched median from new offset of "+tag);
		
		finest("New median from "+tag+":");
		finest(median.toString());
		
		finest("Current state of regular profile collection:");
		finest(collection.getProfileCollection(ProfileType.REGULAR).toString());
		
		offsetNucleusProfiles(tag, ProfileType.REGULAR, median);
		
		finer("Nucleus indexes for "+tag+" updated");
		

		
		if(tag.equals(BorderTag.REFERENCE_POINT)){
			
			// We need to rebuild the ProfileAggregate for the new RP
			// This will reset the RP to index zero
			// Make new profile collections
			int rpIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(tag);
			finer("RP index is changing - moved to index "+rpIndex);
			
			createProfileCollections();
			finer("Recreated profile collections");
			
			rpIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(tag);
			finer("New ProfileAggregates move RP index to index "+rpIndex);
			
			// We need to update the offsets for the BorderTags since zero has moved
			for(BorderTag test : BorderTag.values()){
				if(test.equals(BorderTag.REFERENCE_POINT)){
					collection.getProfileCollection(ProfileType.REGULAR).addOffset(tag, 0);
					finer("Explicit setting of RP index to zero");
					continue;
				} else {
					int oldIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(test);
					int newIndex = AbstractCellularComponent.wrapIndex(oldIndex - index, median.size());
					collection.getProfileCollection(ProfileType.REGULAR).addOffset(test, newIndex);
					finer("Explicit setting of "+test+" index to "+newIndex+" from "+oldIndex);
				}
				
			}
						
			rpIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(tag);
			finer("After explicit set, RP index is "+rpIndex);
			
			map.put(tag, 0); // the RP is back at zero
			
			
		}
				
		// Resegment the median
			
		fine("Resegmenting the median profile from the RP");
		ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

		Profile medianToSegment = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);

		ProfileSegmenter segmenter = new ProfileSegmenter(medianToSegment, map);		

		List<NucleusBorderSegment> segments = segmenter.segment();

		pc.addSegments(BorderTag.REFERENCE_POINT, segments);

		fine("Resegmented the median profile");
	
		
		
		// Run a new morphological analysis to apply the new segments
		// TODO: this needs to trigger the progressable action
		// Temp solution - if a core border tag is detected in the UI selection,
		// trigger morphological analysis after this step

	}
	

	
	
	/**
	 * Test if the regular median profiles of the given datasets have the same segment counts
	 * @param list
	 * @return
	 */
	public static boolean segmentCountsMatch(List<AnalysisDataset> list){
		
		int segCount = list.get(0).getCollection().getProfileManager().getSegmentCount();
		for(AnalysisDataset d : list){
			if( d.getCollection().getProfileManager().getSegmentCount() != segCount){
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Get the number of segments in the regular profile of the collection. On error
	 * return 0
	 * @return
	 */
	public int getSegmentCount(){
		ProfileCollection pc =    collection.getProfileCollection(ProfileType.REGULAR);
		try {
			return pc.getSegments(BorderTag.REFERENCE_POINT).size();
		} catch (Exception e) {
			error("Error getting segment count from collection "+collection.getName(), e);
			return 0;
		}
	}

	
	/**
	 * Regenerate the profile aggregate in each of the profile types of the
	 * collection. The length is set to the angle profile length
	 * @throws Exception
	 */
	public void recalculateProfileAggregates() throws Exception{

		// use the same array length as the source collection to avoid segment slippage
		int profileLength = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN) 
				.size(); 

		for(ProfileType type : ProfileType.values()){
			
			/*
			 * Get the corresponding profile collection from the tempalte
			 */
			ProfileCollection pc =    collection.getProfileCollection(type);
			
			/*
			 * Create an aggregate from the nuclei in the collection. 
			 * A new median profile will result.
			 * By default, the aggregates are created from the reference point
			 */
			pc.createProfileAggregate(collection, 
					type, 
					profileLength);

		}
	}
	
	/**
	 * Copy profile offsets from the this collection, to the
	 * destination and  build the median profiles for all profile types. 
	 * Also copy the segments from the regular angle profile onto
	 * all other profile types
	 * @param destination the collection to update
	 * @throws Exception 
	 */
	public void copyCollectionOffsets( final CellCollection destination) throws Exception{
		
		List<NucleusBorderSegment> segments = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegments(BorderTag.REFERENCE_POINT);


		// use the same array length as the source collection to avoid segment slippage
		int profileLength = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN) 
				.size(); 

		for(ProfileType type : ProfileType.values()){
			
			
			/*
			 * Get the empty profile collection from the new CellCollection
			 */
			ProfileCollection newPC = destination.getProfileCollection(type);
			
			/*
			 * Get the corresponding profile collection from the tempalte
			 */
			ProfileCollection oldPC =    collection.getProfileCollection(type);
			
			/*
			 * Create an aggregate from the nuclei in the collection. 
			 * A new median profile will result.
			 * By default, the aggregates are created from the reference point
			 */
			newPC.createProfileAggregate(destination, 
					type, 
					profileLength);
			
			/*
			 * Copy the offset keys from the source collection
			 */

			for(BorderTag key : oldPC.getOffsetKeys()){
				newPC.addOffset(key, oldPC.getOffset(key));
			}
			newPC.addSegments(BorderTag.REFERENCE_POINT, segments);

		}
	}

	/**
	 * Update the segment with the given id to start at the given index.
	 * Also updates the individual nuclei in the collection
	 * @param id
	 * @param index
	 * @throws Exception
	 */
	private void updateSegmentStartIndex(UUID id, int index) throws Exception{

		// Update the median profile
		collection
			.getProfileManager()
			.updateMedianProfileSegmentIndex(true, id, index);

		// Lock all segments except the one to change
		setLockOnAllNucleusSegmentsExcept(id, true);
		
		// Now run the segment fitting from REFRESH_MORPHOLOGY
		
		
		
		
//		Restore the segment locks
		setLockOnAllNucleusSegments(false);
		
		
	}
	
	/**
	 * Lock the start index of all segments of all profile types in 
	 * all nuclei of the collection except for the segment with the
	 * given id
	 * @param id the segmnet to leave unlocked, or to unlock if locked
	 * @throws Exception
	 */
	public void setLockOnAllNucleusSegmentsExcept(UUID id, boolean b) throws Exception{
		
		List<UUID> ids = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(BorderTag.REFERENCE_POINT)
				.getSegmentIDs();
		
		for(Nucleus n : collection.getNuclei()){

			for(UUID segID : ids){
				if(segID.equals(id)){
					n.setSegmentStartLock(!b, segID);
				} else {
					n.setSegmentStartLock(b, segID);
				}
			}

		}
	}
	

	
	/**
	 * Set the lock on the start index of all segments of all profile types in 
	 * all nuclei of the collection
	 * @param id the segmnet to leave unlocked, or to unlock if locked
	 * @throws Exception
	 */
	public void setLockOnAllNucleusSegments(boolean b) throws Exception{
		
		List<UUID> ids = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(BorderTag.REFERENCE_POINT)
				.getSegmentIDs();
		
		for(Nucleus n : collection.getNuclei()){

			for(UUID segID : ids){
				
				n.setSegmentStartLock(b, segID);
				
			}

		}
	}
	
	/**
	 * Update the given median profile index in the given segment to a new value
	 * @param start
	 * @param segName
	 * @param index
	 * @throws Exception
	 */
	public void updateMedianProfileSegmentIndex(boolean start, UUID id, int index) throws Exception {
		
//		fine("Updating median profile segment: "+segName+" to index "+index);
		// Get the median profile from the reference point
		
		SegmentedProfile oldProfile = collection
				.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(BorderTag.REFERENCE_POINT);
		
//		programLogger.log(Level.FINEST, "Old profile: "+oldProfile.toString());
		


		NucleusBorderSegment seg = oldProfile.getSegment(id);

		// Check if the start or end of the segment is updated, and select the
		// new endpoints appropriately
		int newStart = start ? index : seg.getStartIndex();
		int newEnd = start ? seg.getEndIndex() : index;
		
		// if the segment is the orientation point or reference point boundary, update it
		
		if(start){
			if(seg.getStartIndex()==collection
					.getProfileCollection(ProfileType.REGULAR)
					.getOffset(BorderTag.ORIENTATION_POINT)){
				collection
				.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, index);
			}
			
			if(seg.getStartIndex()==collection
					.getProfileCollection(ProfileType.REGULAR)
					.getOffset(BorderTag.REFERENCE_POINT)){
				collection
				.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, index);
			}
		}

		 // Move the appropriate segment endpoint
		if(oldProfile.update(seg, newStart, newEnd)){
			
//			programLogger.log(Level.FINEST, "Segment position update succeeded");
			// Replace the old segments in the median
//			programLogger.log(Level.FINEST, "Updated profile: "+oldProfile.toString());

//			programLogger.log(Level.FINEST, "Adding segments to profile collection");
			
			collection
			.getProfileCollection(ProfileType.REGULAR)
			.addSegments(BorderTag.REFERENCE_POINT, oldProfile.getSegments());
			
			finest("Segments added, refresh the charts");
							
		} else {
			warn("Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
		}
		
	}
	
	/**
	 * Check that the given segment pair does not cross a core border tag
	 * @param seg1
	 * @param seg2
	 * @return
	 * @throws Exception
	 */
	public boolean testSegmentsMergeable(NucleusBorderSegment seg1, NucleusBorderSegment seg2) throws Exception{
			
		
		// check the boundaries of the segment - we do not want to merge across the BorderTags
		boolean ok = true;

		for(BorderTag tag : BorderTag.values(BorderTagType.CORE)){
			
			/*
			 * Find the position of the border tag in the median profile
			 * 
			 */
			int offsetForOp = collection.getProfileCollection(ProfileType.REGULAR).getOffset(BorderTag.REFERENCE_POINT);
			
			int offset = collection.getProfileCollection(ProfileType.REGULAR).getOffset(tag);
			
			// this should be zero for the orientation point and  totalLength+difference for the reference point
			int difference = offset - offsetForOp;

			if(seg2.getStartIndex()==seg2.getTotalLength()+difference || seg2.getStartIndex()==difference){
				ok=false;
			}

		}
		return ok;
	}
	
	/**
	 * Merge the given segments
	 * @param seg1
	 * @param seg2
	 * @throws Exception
	 */
	public void mergeSegments(NucleusBorderSegment seg1, NucleusBorderSegment seg2) throws Exception {

		SegmentedProfile medianProfile = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(BorderTag.REFERENCE_POINT);

		/*
		 * Only try the merge if both segments are present in the profile
		 */
		if(medianProfile.hasSegment(seg1.getID())  && medianProfile.hasSegment(seg2.getID()) ){

			// Give the new merged segment a new ID
			UUID newID = java.util.UUID.randomUUID();
			
			// merge the two segments in the median
			medianProfile.mergeSegments(seg1, seg2, newID);

			// put the new segment pattern back with the appropriate offset
			collection.getProfileCollection(ProfileType.REGULAR)
				.addSegments( BorderTag.REFERENCE_POINT,  medianProfile.getSegments());

			/*
			 * With the median profile segments merged, also merge the segments
			 * in the individual nuclei
			 */
			for(Nucleus n : collection.getNuclei()){

				SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
				NucleusBorderSegment nSeg1 = profile.getSegment(seg1.getID());
				NucleusBorderSegment nSeg2 = profile.getSegment(seg2.getID());
				profile.mergeSegments(nSeg1, nSeg2, newID);
				n.setProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT, profile);
			}

			/*
			 * Update the consensus if present
			 */
			if(collection.hasConsensusNucleus()){
				ConsensusNucleus n = collection.getConsensusNucleus();
				SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
				NucleusBorderSegment nSeg1 = profile.getSegment(seg1.getID());
				NucleusBorderSegment nSeg2 = profile.getSegment(seg2.getID());
				profile.mergeSegments(nSeg1, nSeg2, newID);
				n.setProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT, profile);
			}
			
			// Ensure the vertical nuclei have the same segment pattern
			collection.updateVerticalNuclei();

		}
	}
	
	/**
	 * Split the given segment into two segmnets. The split is made at the given index
	 * @param segName
	 * @return
	 * @throws Exception
	 */
	public boolean splitSegment(NucleusBorderSegment seg, int index)  throws Exception {
		
		SegmentedProfile medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getSegmentedProfile(BorderTag.REFERENCE_POINT);
		

		// Do not try to split segments that are a merge of other segments
		if(seg.hasMergeSources()){
			return false;
		}
						
		try{

			if(seg.contains(index)){

				double proportion = seg.getIndexProportion(index);

				UUID newID1 = java.util.UUID.randomUUID();
				UUID newID2 = java.util.UUID.randomUUID();
				// split the two segments in the median
				medianProfile.splitSegment(seg, index, newID1, newID2);

				// put the new segment pattern back with the appropriate offset
				collection.getProfileCollection(ProfileType.REGULAR)
					.addSegments( BorderTag.REFERENCE_POINT,  medianProfile.getSegments());

				/*
				 * With the median profile segments unmerged, also split the segments
				 * in the individual nuclei. Requires proportional alignment
				 */
				for(Nucleus n : collection.getNuclei()){


					SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
					NucleusBorderSegment nSeg = profile.getSegment(seg.getID());

					int targetIndex = nSeg.getProportionalIndex(proportion);
					profile.splitSegment(nSeg, targetIndex, newID1, newID2);
					n.setProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT, profile);
				}

				/*
				 * Update the consensus if present
				 */
				if(collection.hasConsensusNucleus()){
					ConsensusNucleus n = collection.getConsensusNucleus();
					SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
					NucleusBorderSegment nSeg1 = profile.getSegment(seg.getID());
					int targetIndex = nSeg1.getProportionalIndex(proportion);
					profile.splitSegment(nSeg1, targetIndex, newID1, newID2);
					n.setProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT, profile);
				}
				
				// Ensure the vertical nuclei have the same segment pattern
				collection.updateVerticalNuclei();

				return true;
			} else {
				return false;
			}
		} catch(Exception e){
			return false;
		}
	}
	
	/**
	 * Split the given segment into two segmnets. The split is made at the midpoint
	 * @param segName
	 * @return
	 * @throws Exception
	 */
	public boolean splitSegment(NucleusBorderSegment seg) throws Exception {
			int index = seg.getMidpointIndex();
			
			return splitSegment(seg, index);
	}
	
	public void unmergeSegments(NucleusBorderSegment seg) throws Exception {
		
		SegmentedProfile medianProfile = collection.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(BorderTag.REFERENCE_POINT);
		
		// Get the segments to merge
		
		// merge the two segments in the median - this is only a copy of the profile collection
		medianProfile.unmergeSegment(seg);
		
		// put the new segment pattern back with the appropriate offset
		collection.getProfileCollection(ProfileType.REGULAR).addSegments( BorderTag.REFERENCE_POINT,  medianProfile.getSegments());

		/*
		 * With the median profile segments unmerged, also unmerge the segments
		 * in the individual nuclei
		 */
		for(Nucleus n : collection.getNuclei()){

			SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
			NucleusBorderSegment nSeg = profile.getSegment(seg.getID());
			profile.unmergeSegment(nSeg);
			n.setProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT, profile);
		}
		
		/*
		 * Update the consensus if present
		 */
		if(collection.hasConsensusNucleus()){
			ConsensusNucleus n = collection.getConsensusNucleus();
			SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
			NucleusBorderSegment nSeg1 = profile.getSegment(seg.getID());
			profile.unmergeSegment(nSeg1);
			n.setProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT, profile);
		}
		
		// Ensure the vertical nuclei have the same segment pattern
		collection.updateVerticalNuclei();
	}
	
}
