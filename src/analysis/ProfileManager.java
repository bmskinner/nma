package analysis;

import java.util.List;
import java.util.UUID;


import logging.Loggable;
//import analysis.nucleus.DatasetSegmenter.SegmentFitter;
import utility.Constants;
import components.CellCollection;
import components.generic.BorderTag;
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
 * ProfileAggregates
 * @author bms41
 *
 */
public class ProfileManager implements Loggable {
	
	final private CellCollection collection;
	
	public ProfileManager(final CellCollection collection){
		this.collection = collection;
	}
	
	
	/**
	 * Update the location of the given border tag within the profile
	 * @param tag
	 * @param index
	 */
	public void updateBorderTag(BorderTag tag, int index){
		
		log("Updating border tag location");		
		
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
