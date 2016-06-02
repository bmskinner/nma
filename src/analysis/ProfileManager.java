package analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;










import analysis.profiles.ProfileIndexFinder;
import analysis.profiles.ProfileOffsetter;
import analysis.profiles.RuleSet;
import logging.Loggable;
//import analysis.nucleus.DatasetSegmenter.SegmentFitter;
import utility.Constants;
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
		
		List<RuleSet> top = collection.getRuleSetCollection().getRuleSets(BorderTag.TOP_VERTICAL);
		List<RuleSet> btm = collection.getRuleSetCollection().getRuleSets(BorderTag.BOTTOM_VERTICAL);
		
		if(top.size()>0 && btm.size()>0){
			
			fine("Detecting top and bottom verticals");
			
			ProfileIndexFinder finder = new ProfileIndexFinder();
			
			Profile median = collection
					.getProfileCollection(ProfileType.REGULAR)
					.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);

			int topIndex = finder.identifyIndex(median, top);
			int btmIndex = finder.identifyIndex(median, btm);
			
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
				
				fine("Updated nuclei");
			} else {
				fine("Cannot find TV or BV in median profile");
			}
		} else {
			fine("No rulesets for top and bottom verticals");
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
			updateCoreBorderTagIndex(tag, index);
			return;
		} else {
			updateExtendedBorderTagIndex(tag, index);
		}
		
	}
	
	/**
	 * Update the extended border tags that don't need resegmenting
	 * @param tag
	 * @param index
	 */
	private void updateExtendedBorderTagIndex(BorderTag tag, int index){
		int oldIndex =0;
		try {
			oldIndex = collection.getProfileCollection(ProfileType.REGULAR).getOffset(tag);
		} catch(IllegalArgumentException e){
			finer("Border tag does not exist and will be created");
		}

		/*
		 * Set the border tag in the median profile 
		 */
		finest("Setting border tag in median to "+index+ " from "+oldIndex);
		collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(tag, index);
		


		/* 
		 * Set the border tag in the individual nuclei
		 * using the segment proportion method
		 */
		
		ProfileOffsetter offsetter = new ProfileOffsetter(collection);
		try {
			offsetter.assignBorderTagToNucleiViaFrankenProfile(tag);
			
			for(Nucleus n : collection.getNuclei()){
				n.updateVerticallyRotatedNucleus();
			}
			
		} catch (Exception e1) {
			error("Error assigning tag", e1);
		}
		
		
		
		try{
			
			Profile median = collection.getProfileCollection(ProfileType.REGULAR)
					.getProfile(tag, Constants.MEDIAN); 

			
			
			
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

		} catch (Exception e){

			warn("Error updating "+tag+": resetting");
			
			collection.getProfileCollection(ProfileType.REGULAR)
			.addOffset(tag, oldIndex);
			
			warn("Individual nuclei not reset");
						
			return;
		}
	}
	
	/**
	 * Find the appropriate offset for each nucleus in the collection
	 * @param tag
	 * @param index
	 */
	private void applyBorderTagOffsetToNuclei(BorderTag tag, int index){
		
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
		
		
		fine("Resegmenting for core border tag change");
		// Store the existing core points in a map (OP and RP)
		Map<BorderTag, Integer> map = new HashMap<BorderTag, Integer>();
		for(BorderTag test : BorderTag.values(BorderTagType.CORE)){
			int i = collection.getProfileCollection(ProfileType.REGULAR).getOffset(test);
			map.put(test, i);
			finest("Storing existing median "+test+" at index "+i);
		}
		
		// Overwrite the new tag for segmentation
		map.put(tag, index);
		finest("Replacing median "+tag+" with index "+index+" in segmenter map");
		
		// Store the offset for the new point
		collection.getProfileCollection(ProfileType.REGULAR).addOffset(tag, index);
		
				
		/*
		 * Now we need to update the tag indexes in the nucleus
		 * profiles.
		 */
		Profile median = collection.getProfileCollection(ProfileType.REGULAR)
				.getProfile(tag, Constants.MEDIAN);
		
		offsetNucleusProfiles(tag, ProfileType.REGULAR, median);
		
		finer("Nucleus indexes for "+tag+" updated");
		

		
		if(tag.equals(BorderTag.REFERENCE_POINT)){
			
			// We need to rebuild the ProfileAggregate for the new RP
			// This will reset the RP to index zero
			// Make new profile collections
			createProfileCollections();
			finer("Recreated profile collections");
			map.put(tag, 0); // the RP is back at zero
		}
				
		// Resegment the median
		try {
			
			fine("Resegmenting the median profile");
			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

			ProfileSegmenter segmenter = new ProfileSegmenter(median, map);		

			List<NucleusBorderSegment> segments = segmenter.segment();
			
			pc.addSegments(BorderTag.REFERENCE_POINT, segments);
			
			fine("Resegmented the median profile");

		} catch (Exception e1) {

			error("Error resegmenting the median profile", e1);
			return;
		}
		
		
		
		// Run a new morphological analysis to apply the new segments
		// TODO: this needs to trigger the progressable action
		// Temp solution - if a core border tag is detected in the UI selection,
		// trigger morphological analysis after this step

	}
	
	
	
	private void updateRPIndex(int index){
		
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
