package analysis.nucleus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import analysis.AbstractLoggable;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.BorderTag.BorderTagType;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;
import logging.Loggable;

public class SegmentFitter implements Loggable {

	/**
	 * The multiplier to add to best-fit scores when shrinking a segment below the 
	 * minimum segment size specified in ProfileSegmenter
	 */
	static final double PENALTY_SHRINK = 1.5;
	
	/**
	 * The multiplier to add to best-fit scores when shrinking a segment above the 
	 * median segment size
	 */
	static final double PENALTY_GROW   = 20;
	
	private final SegmentedProfile medianProfile; // the profile to align against
	
	private boolean optimise = false; // a flag to enable test optimisations.
	
	// This holds tested profiles so that their scores do not have to be recalculated
	private List<SegmentedProfile> testedProfiles = new ArrayList<SegmentedProfile>();
	
	/**
	 * The number of points ahead and behind to test
	 * when creating new segment profiles
	 */
//	private static final int POINTS_TO_TEST = 20;

	/**
	 * Construct with a median profile containing segments. The originals will not be modified
	 * @param medianProfile the profile
	 * @param logFile the file for logging status
	 * @throws Exception 
	 */
	public SegmentFitter(final SegmentedProfile medianProfile) throws Exception{
		if(medianProfile==null){
//			log(Level.SEVERE, "Segmented profile is null");
			throw new IllegalArgumentException("Median profile is null");
		}

		this.medianProfile  = new SegmentedProfile(medianProfile);
	}
	
	/**
	 * Run the segment fitter on the given nucleus. It will take the segments
	 * loaded into the fitter upon construction, and apply them to the nucleus
	 * angle profile.
	 * @param n the nucleus to fit to the current median profile
	 * @param pc the ProfileCollection from the CellCollection the nucleus belongs to
	 */
	public void fit(final Nucleus n, final ProfileCollection pc) throws Exception {
		
		// Input checks
		if(n==null){
			throw new IllegalArgumentException("Test nucleus is null");
		}
		
		if(        n.getProfile(ProfileType.REGULAR).getSegments()==null 
				|| n.getProfile(ProfileType.REGULAR).getSegments().isEmpty()){
			throw new IllegalArgumentException("Nucleus has no segments");
		}
		
		
	
		
		// Begin fitting the segments to the median
		// get the best fit of segments to the median
		SegmentedProfile newProfile = this.runFitter(n.getProfile(ProfileType.REGULAR));
		
		log(Level.FINEST, "Fitted profile against median for nucleus "+n.getNameAndNumber());
		log(Level.FINEST, newProfile.toString());
		
		n.log("Fitted profile against median:");
		n.log(newProfile.toString());

		n.setProfile(ProfileType.REGULAR, newProfile);

		// modify tail and head/tip point to nearest segment end
		remapBorderPoints(n, pc);
		
		log(Level.FINEST, "Remapped border points for nucleus "+n.getNameAndNumber());
		log(Level.FINEST, n.getProfile(ProfileType.REGULAR).toString());
		
		n.log("Remapped border points:");
		n.log(n.getProfile(ProfileType.REGULAR).toString());
		
	}
		
	/**
	 * Join the segments within the given nucleus into Frankenstein's Profile. 
	 * @param n the nucleus to recombine
	 * @param tag the BorderTag to start from
	 * @return a profile
	 * @throws Exception  
	 */
	public Profile recombine(Nucleus n, BorderTag tag) throws Exception {
		if(n==null){
			throw new IllegalArgumentException("Test nucleus is null");
		}
		
		if( ! n.getProfile(ProfileType.REGULAR).hasSegments()){
			throw new IllegalArgumentException("Test nucleus has no segments");
		}
		
		n.log("Recombining profile");
		n.log("Template profile:");
		n.log(medianProfile.toString());

		/*
		 * Generate a segmented profile from the angle profile of the point type.
		 * The zero index of the profile is the border tag. The segment list for the profile
		 * begins with seg 0 at the border tag.
		 */

		SegmentedProfile nucleusProfile = new SegmentedProfile(n.getProfile(ProfileType.REGULAR, tag));
		
		n.log("Initial profile starting at "+tag+":");
		n.log(nucleusProfile.toString());


		//				log(Level.FINEST, "    Segmentation beginning from "+tag);
		//				log(Level.FINEST, "    The border tag "+tag+" in this nucleus is at raw index "+n.getBorderIndex(tag));
		//				log(Level.FINEST, "    Angle at incoming segmented profile index 0 ("+tag+") is "+nucleusProfile.get(0));

		// stretch or squeeze the segments to match the length of the median profile of the collection
		SegmentedProfile frankenProfile = nucleusProfile.frankenNormaliseToProfile(medianProfile);
		n.log("Complete frankenprofile:");
		n.log(frankenProfile.toString());

		return frankenProfile;
	}
	
	/**
	 * Move core border points within a nucleus to the end of their appropriate segment
	 * based on the median profile segmentation pattern
	 * @param n the nucleus to fit
	 * @param pc the profile collection from the CellCollection
	 */
	private void remapBorderPoints(Nucleus n, ProfileCollection pc) throws Exception {
		
		if(pc==null){
			log(Level.WARNING, "No profile collection found, skipping remapping");
			return; // this allows the unit tests to skip this section if a profile collection has not been created
		}
		
		n.log("Remapping border points");
		/*
		 * Not all the tags will be associated with endpoints;
		 * e.g. the intersection point. The orientation and 
		 * reference points should be updated though - members of
		 * the core border tag population
		 */
		
		for(BorderTag tag : BorderTag.values(BorderTagType.CORE)){
			
			// get the segments the point should lie between
			// from the median profile
			
			/*
			 * The goal is to move the index of the border tag to the start index
			 * of the relevant segment.
			 * 
			 * Select the segments from the median profile, offset to begin from the tag.
			 * The relevant segment has a start index of 0
			 * Find the name of this segment, and adjust it's start position in the
			 * individual nucleus profile.
			 */		
			NucleusBorderSegment seg = pc.getSegmentStartingWith(tag);
			List<NucleusBorderSegment> segments = pc.getSegments(tag);
						
			if(seg!=null){
				// Get the same segment in the nucleus, and move the tag to the segment start point
				NucleusBorderSegment nSeg = n.getProfile(ProfileType.REGULAR).getSegment(seg.getName());
				n.setBorderTag(tag, nSeg.getStartIndex());
				log(Level.FINEST, "Remapped border point '"+tag+"' to "+nSeg.getStartIndex()+" in "+n.getNameAndNumber());
				n.log("Remapped border point '"+tag+"' to "+nSeg.getStartIndex());
			} else {
								
				// A segment was not found with a start index at zero; segName is null
				log(Level.WARNING, "Border tag '"+tag+"' not found in median profile");
				log(Level.WARNING, "No segment with start index zero in median profile");
				log(Level.WARNING, "Median profile:");
				log(Level.WARNING, pc.toString());
				log(Level.WARNING, "Median segment list:");
				log(Level.WARNING, NucleusBorderSegment.toString(segments));
				n.log("Could not remapped border point '"+tag+"'");
				// Check to see if the segments are reversed
				seg = pc.getSegmentEndingWith(tag);
				if(seg!=null){
					log(Level.WARNING, "Found segment "+seg.getName()+" ending with tag "+tag);
					
				} else {
					log(Level.WARNING, "No segments end with tag "+tag);
				}
				
			}
		}
	}
		
	/**
	 * 
	 * @param profile the profile to fit against the median profile
	 * @return a profile with best-fit segmentation to the median
	 * @throws Exception 
	 */
	private SegmentedProfile runFitter(SegmentedProfile profile) throws Exception {
		// Input check
		if(profile==null){
//			log(Level.SEVERE, "Profile is null");
			throw new IllegalArgumentException("Input profile is null");
		}
		
		testedProfiles = new ArrayList<SegmentedProfile>();

//		log(Level.FINE, "Fitting segments");
		
		// By default, return the input profile
		SegmentedProfile result 	 = new SegmentedProfile(profile);

		// A new list to hold the fitted segments
		SegmentedProfile tempProfile = new SegmentedProfile(profile);
		
		// fit each segment independently
		List<UUID> idList = medianProfile.getSegmentIDs();
		
		for(UUID id : idList){
					
			// get the current segment
			NucleusBorderSegment segment = tempProfile.getSegment(id);
			
			if( ! segment.isStartPositionLocked()){ //only run the test if this segment is unlocked
			
				// get the initial score for the segment and log it
//				double score = compareSegmentationPatterns(medianProfile, tempProfile);
//
				// find the best length and offset change
				// apply them to the profile
				tempProfile = testLength(tempProfile, id);

				// copy the best fit profile to the result
				result 	 = new SegmentedProfile(tempProfile);		
			}
		}

		return result;
	}
	
	/**
	 * Test the effect of changing length on the given segment from the list
	 * @param list
	 * @param segmnetNumber the segment to test
	 * @return
	 */
	private SegmentedProfile testLength(SegmentedProfile profile, UUID id) throws Exception {
		
		
		
		// by default, return the same profile that came in
		SegmentedProfile result = new SegmentedProfile(profile);
				
		
		// the segment in the input profile to work on
		NucleusBorderSegment segment = profile.getSegment(id);
		
		
		// Get the initial score to beat
		double bestScore = compareSegmentationPatterns(medianProfile, profile);

		
		// the most extreme negative offset to apply to the end of this segment
		// without making the length invalid
		int minimumChange = 0 - (segment.length() - NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH);
		
		// the maximum length offset to apply
		// we can't go beyond the end of the next segment anyway, so use that as the cutoff
		// how far from current end to next segment end?
		int maximumChange = segment.testLength(segment.getEndIndex(), segment.nextSegment().getEndIndex());	

		
		/* Trying all possible lengths takes a long time. Try adjusting lengths in a window of 
		 * <changeWindowSize>, and finding the window with the best fit. Then drop down to individual
		 * index changes to refine the match 
		 */
		int bestChangeWindow = 0;
		int changeWindowSize = 10;
		for(int changeWindow = minimumChange; changeWindow < maximumChange; changeWindow+=changeWindowSize){

			// find the changeWindow with the best fit, 
			// apply all changes to a fresh copy of the profile
			SegmentedProfile testProfile = new SegmentedProfile(profile);

			testProfile = testChange(profile, id, changeWindow);
			double score = compareSegmentationPatterns(medianProfile, testProfile);
			if(score < bestScore){
				bestChangeWindow = changeWindow;
			}
		}
		

		
		int halfWindow = changeWindowSize / 2;
		// now we have the best window,  drop down to a changeValue
		for(int changeValue = bestChangeWindow - halfWindow; changeValue < bestChangeWindow+halfWindow; changeValue++){
			SegmentedProfile testProfile = new SegmentedProfile(profile);

			testProfile = testChange(profile, id, changeValue);
			double score = compareSegmentationPatterns(medianProfile, testProfile);
			if(score < bestScore){
				result = testProfile;
			}
		}
		
		return result;
	}
		
	/**
	 * Test the effect of moving the segment start boundary of the profile by a certain amount.
	 * If the change is a better fit to the median profile than before, it is kept.
	 * @param profile the profile to test
	 * @param id the segment to alter
	 * @param changeValue the amount to alter the segment by
	 * @return the original profile, or a better fit to the median
	 * @throws Exception
	 */
	private SegmentedProfile testChange(SegmentedProfile profile, UUID id, int changeValue) throws Exception {
		
		if(profile==null){
			throw new IllegalArgumentException("Input profile is null for segment "+id.toString()+" change "+changeValue);
		}


		double bestScore = compareSegmentationPatterns(medianProfile, profile);

		// apply all changes to a fresh copy of the profile
		SegmentedProfile testProfile = new SegmentedProfile(profile);


		// not permitted if it violates length constraint
		if(testProfile.adjustSegmentStart(id, changeValue)){

			SegmentedProfile result = testProfile;



			// anything that gets in here should be valid

			double score = compareSegmentationPatterns(medianProfile, testProfile);

			if(score < bestScore){
				bestScore 	= score;
				result = new SegmentedProfile(testProfile);

			}

			return result;	

		}

		return profile;
	}
	
	/**
	 * Check if the given profile has already been tested
	 * @param test
	 * @return
	 */
	private boolean hasBeenTested(SegmentedProfile test){
		if(testedProfiles.contains(test)){
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Find the nudge to the given list of segments that gives the best
	 * fit to the median profile
	 * @param list the segment list
	 * @param length the length to cycle through. Use the segment length for simple measure
	 * @return the best nudge value to use
	 * @throws Exception when the segmentation comparison fails
	 */
	private int testNudge(SegmentedProfile profile, int length) throws Exception {
		
//		int totalLength = list.get(0).getTotalLength();
		double score 		= 0;
		double bestScore 	= 0;
		int bestNudge 		= 0;
				
		
		SegmentedProfile newProfile = new SegmentedProfile(profile);
		newProfile.nudgeSegments(-length);
		
		for( int nudge = -length; nudge<length; nudge++){
//			SegmentedProfile newProfile = new SegmentedProfile(profile);
			newProfile.nudgeSegments(1); // keep the same profile
							
			try{
				score = compareSegmentationPatterns(medianProfile, newProfile);
//				fileLogger.log("\tNudge "+nudge+":\tScore:\t"+score, fileLogger.DEBUG);
				
			}catch(IllegalArgumentException e){
//				logError("Nudge error getting segmentation pattern: ", e);
				throw new Exception("Nudge error getting segmentation pattern");
			}
			
			if(score < bestScore){
				bestScore = score;
				bestNudge = nudge;
			}	
		}
		return bestNudge;
	}
				
	/**
	 * Get the sum-of-squares difference betweene two segments in the given profile
	 * @param name the name of the segment
	 * @param referenceProfile the profile to measure against
	 * @param testProfile the profile to measure
	 * @return the sum of square differences between the segments
	 * @throws Exception 
	 */
	private double compareSegments(UUID id, SegmentedProfile referenceProfile, SegmentedProfile testProfile) throws Exception{
		if(id == null){
			throw new IllegalArgumentException("Segment id is null");
		}
		
		if(referenceProfile == null || testProfile==null){
			throw new IllegalArgumentException("Test or reference profile is null");
		}
		
		NucleusBorderSegment reference  = referenceProfile.getSegment(id);
		NucleusBorderSegment test		=      testProfile.getSegment(id);
		
		double result = 0;
		
		try{
			
			Profile refProfile  = referenceProfile.getSubregion(reference);
			Profile subjProfile =      testProfile.getSubregion(test);

			result = refProfile.absoluteSquareDifference(subjProfile);
			
		} catch(Exception e){
			logError("Error calculating absolute square difference between segments", e);
			log(Level.SEVERE, "Ref  seg: "+ reference.toString());
			log(Level.SEVERE, "Test seg: "+      test.toString());
			log(Level.SEVERE, "Test profile: "+  testProfile.toString());
		}
		return result;
	}
	
	/**
	 * Get the score for an entire segment list of a profile. Tests the effect of  changing one segment
	 * on the entire set
	 * @param reference
	 * @param test
	 * @return the score
	 * @throws Exception 
	 */
	private double  compareSegmentationPatterns(SegmentedProfile referenceProfile, SegmentedProfile testProfile) throws Exception{
		
		if(referenceProfile==null || testProfile==null){
			throw new IllegalArgumentException("An input profile is null");
		}
		
		if(referenceProfile.getSegmentCount()!=testProfile.getSegmentCount()){
			throw new IllegalArgumentException("Segment counts are different for profiles");
		}
		
		double result = 0;

		for(UUID id : referenceProfile.getSegmentIDs()){
			result += compareSegments(id, referenceProfile, testProfile);
		}
//		for(String name : referenceProfile.getSegmentNames()){
//			result += compareSegments(name, referenceProfile, testProfile);
//		}
		return result;
	}
}
