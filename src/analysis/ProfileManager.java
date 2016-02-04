package analysis;

import gui.DatasetEvent.DatasetMethod;
import gui.InterfaceEvent.InterfaceMethod;
import gui.tabs.SegmentsEditingPanel;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import javax.swing.JOptionPane;

import analysis.nucleus.DatasetSegmenter;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;
import analysis.nucleus.DatasetSegmenter.SegmentFitter;
import utility.Constants;
import components.Cell;
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
 * ProfileAggregates
 * @author bms41
 *
 */
public class ProfileManager {
	
	private CellCollection collection;
	
	public ProfileManager(CellCollection collection){
		this.collection = collection;
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
		
//		programLogger.log(Level.FINE, "Updating median profile segment: "+segName+" to index "+index);
		// Get the median profile from the reference point
		
		SegmentedProfile oldProfile = collection
				.getProfileCollection(ProfileType.REGULAR)
				.getSegmentedProfile(BorderTag.REFERENCE_POINT);
		
//		programLogger.log(Level.FINEST, "Old profile: "+oldProfile.toString());
		


		NucleusBorderSegment seg = oldProfile.getSegment(id);

		int newStart = start ? index : seg.getStartIndex();
		int newEnd = start ? seg.getEndIndex() : index;
		
		// TODO - if the segment is the orientation point boundary, update it
		if(start){
			if(seg.getStartIndex()==collection
					.getProfileCollection(ProfileType.REGULAR).getOffset(BorderTag.ORIENTATION_POINT)){
				collection
				.getProfileCollection(ProfileType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, index);
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
			
//			programLogger.log(Level.FINEST, "Segments added, refresh the charts");
							
		} else {
//			programLogger.log(Level.WARNING, "Updating "+seg.getStartIndex()+" to index "+index+" failed: "+seg.getLastFailReason());
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
			int offsetForOp = collection.getProfileCollection(ProfileType.REGULAR).getOffset(BorderTag.ORIENTATION_POINT);
			
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

			UUID newID = java.util.UUID.randomUUID();
			// merge the two segments in the median - this is only a copy of the profile collection
			medianProfile.mergeSegments(seg1, seg2, newID);

			// put the new segment pattern back with the appropriate offset
			collection.getProfileCollection(ProfileType.REGULAR).addSegments( BorderTag.REFERENCE_POINT,  medianProfile.getSegments());

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

		}
	}
	
	/**
	 * Split the given segment into two segmnets. The split is made at the midpoint
	 * @param segName
	 * @return
	 * @throws Exception
	 */
	public boolean splitSegment(NucleusBorderSegment seg) throws Exception {
		
		boolean result = false;
				
		SegmentedProfile medianProfile = collection.getProfileCollection(ProfileType.REGULAR).getSegmentedProfile(BorderTag.REFERENCE_POINT);
		
		// Get the segments to merge
//		NucleusBorderSegment seg = medianProfile.getSegment(segName);
		
		// Do not try to split segments that are a merge of other segments
		if(seg.hasMergeSources()){
			return false;
		}
						
		try{

			int index = seg.getMidpointIndex();
			//				int index = (Integer) spinner.getModel().getValue();
			if(seg.contains(index)){

				double proportion = seg.getIndexProportion(index);

				// merge the two segments in the median - this is only a copy of the profile collection
				medianProfile.splitSegment(seg, index);

				// put the new segment pattern back with the appropriate offset
				collection.getProfileCollection(ProfileType.REGULAR).addSegments( BorderTag.REFERENCE_POINT,  medianProfile.getSegments());

				/*
				 * With the median profile segments unmerged, also split the segments
				 * in the individual nuclei. Requires proportional alignment
				 */
				for(Nucleus n : collection.getNuclei()){


					SegmentedProfile profile = n.getProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT);
					NucleusBorderSegment nSeg = profile.getSegment(seg.getID());

					int targetIndex = nSeg.getProportionalIndex(proportion);
					profile.splitSegment(nSeg, targetIndex);
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
					profile.splitSegment(nSeg1, targetIndex);
					n.setProfile(ProfileType.REGULAR, BorderTag.REFERENCE_POINT, profile);
				}

				result = true;
			} else {
				return false;
			}
		} catch(Exception e){
			return false;
		}
		
		return result;
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
	}
	
}
