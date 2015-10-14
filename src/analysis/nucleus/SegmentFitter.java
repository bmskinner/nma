/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utility.Logger;

import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

/**
 * This takes a median profile plus segments, and a real profile plus segments from
 * a Nucleus, and tries to optimise the segment endpoints by moving the segment
 * boundaries. The segments are interpolated to match the corresponding median segment,
 * and a best fit score is calculated. This should help overcome the 'sensory homunculus'
 * problem with Yqdels in an otherwise WT population
 */
public class SegmentFitter {
	
	private static Logger logger;

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
	
	private 	SegmentedProfile medianProfile; // the profile to align against
	
	private boolean debug = false; // log debug info to file
	private boolean optimise = false;
	
	// This holds tested profiles so that their scores do not have to be recalculated
	private List<SegmentedProfile> testedProfiles = new ArrayList<SegmentedProfile>();
	
	/**
	 * The number of points ahead and behind to test
	 * when creating new segment profiles
	 */
	private static int POINTS_TO_TEST = 20;
			
	/**
	 * Construct with a median profile containing segments. The originals will not be modified
	 * @param medianProfile the profile
	 * @param logFile the file for logging status
	 */
	public SegmentFitter(SegmentedProfile medianProfile, File logFile){
		if(medianProfile==null){
			logger.log("Segmented profile is null", Logger.ERROR);
			throw new IllegalArgumentException("Median profile is null");
		}
		
		logger = new Logger(logFile, "SegmentFitter");
		this.medianProfile = null;
		try {
			this.medianProfile  = new SegmentedProfile(medianProfile);
		} catch (Exception e) {
			logger.error("Error initialising fitter", e);
		}
		
		
	}
	
	/**
	 * Run the segment fitter on the given nucleus. It will take the segments
	 * loaded into the fitter upon cosntruction, and apply them to the nucleus
	 * angle profile.
	 * @param n the nucleus to fit to the current median profile
	 * @param pc the ProfileCollection from the CellCollection the nucleus belongs to
	 */
	public void fit(Nucleus n, ProfileCollection pc){
		
		// Input checks
		if(n==null){
			logger.log("Test nucleus is null", Logger.ERROR);
			throw new IllegalArgumentException("Test nucleus is null");
		}
		
		try {
			if(n.getAngleProfile().getSegments()==null || n.getAngleProfile().getSegments().isEmpty()){
				logger.log("Nucleus has no segments", Logger.ERROR);
				throw new IllegalArgumentException("Nucleus has no segments");
			}
		} catch (Exception e1) {
			logger.error("Error getting segments", e1);
		}
		
		long startTime = System.currentTimeMillis();
		// Begin fitting the segments to the median

		try{
			
			// get the best fit of segments to the median
			SegmentedProfile newProfile = this.runFitter(n.getAngleProfile());
			n.setAngleProfile(newProfile);
			
			// modify tail and head/tip point to nearest segment end
			remapBorderPoints(n, pc);
			
			logger.log("Fitted nucleus "+n.getPathAndNumber(), Logger.INFO);
			
			if(debug){
				long endTime = System.currentTimeMillis();
				long time = endTime - startTime;
				logger.log("Fitting took "+time+" milliseconds", Logger.DEBUG);
			}
			
		} catch(Exception e){
			logger.error("Error refitting segments", e);
		}
	}
		
	/**
	 * Join the segments within the given nucleus into Frankenstein's Profile. 
	 * @param n the nucleus to recombine
	 * @return a profile
	 */
	public Profile recombine(Nucleus n, String pointType){
		if(n==null){
			logger.log("Recombined nucleus is null", Logger.ERROR);
			throw new IllegalArgumentException("Test nucleus is null");
		}
		Profile frankenProfile = null;
		try {
			if(n.getAngleProfile().getSegments()==null){
				logger.log("Nucleus has no segments", Logger.ERROR);
				throw new IllegalArgumentException("Nucleus has no segments");
			}

			// Generate a segmented profile from the angle profile of the point type
			SegmentedProfile nucleusProfile = new SegmentedProfile(n.getAngleProfile(pointType));
			
			// stretch the segments to match the median profile of the collection
			frankenProfile = recombineSegments(n, nucleusProfile, pointType);
		} catch(Exception e){
			logger.error("Error recombining segments", e);
		}
		
		return frankenProfile;
	}
	
	/**
	 * Move any border points to the appropriate segment end
	 * @param n the nucleus to fit
	 * @param pc the profile collection from the CellCollection
	 */
	private void remapBorderPoints(Nucleus n, ProfileCollection pc) throws Exception {
		
		if(pc==null){
			logger.log("No profile collection found, skipping remapping", Logger.DEBUG);
			return; // this allows the unit tests to skip this section if a profile collection has not been created
		}
		
		/*
		 * Not all the tags will be associated with endpoints;
		 * e.g. the intersection point. The orientation and 
		 * reference points should be updated though
		 */
		
		String[] tags = { n.getReferencePoint() , n.getOrientationPoint() };
		
		for(String tag : tags){
			
			// get the segments the point should lie between
			// from the median profile
			String segName = null;
			
			// get the name of the segment with the tag at the start
			for(NucleusBorderSegment seg : pc.getSegments(tag)){

				/*
				 What happens when the tag does not lie on the segment start?
				 This occurrs in pig sperm, where the reference point (head) is in 
				 the centre of a segment.
				 
				 In these cases, do not try remapping the point; just leave it
				 
				 
				 It can also be the last index of the profile?
				 * 
				 */
				if(  seg.getStartIndex()==0 ){
					segName = seg.getName();
				}
			}
			
			if(segName!=null){
				// Get the same segment in the nucleus, and update the tag
				NucleusBorderSegment nSeg = n.getAngleProfile().getSegment(segName);
				n.addBorderTag(tag, nSeg.getStartIndex());
				logger.log("Remapped border point "+tag, Logger.DEBUG);
			} else {
				logger.log("Unable to remap border point "+tag, Logger.DEBUG);
			}
		}
	}
	
	/**
	 * Perform the recombination of segments from a nucleus. It takes each segment
	 * and interpolates it to the length of the corresponding median segment.
	 * @param profile the segmented nucleus profile to adjust
	 * @return a frankenprofile constructed from the stretched segments
	 */
	private Profile recombineSegments(Nucleus n, SegmentedProfile profile, String pointType) throws Exception {
		
		if(profile==null){
			throw new IllegalArgumentException("Test profile is null in recombiner");
		}
		logger.log("Recombining segments to FrankenProfile", Logger.DEBUG);
		logger.log("The pointType "+pointType+" in this nucleus is at raw index "+n.getBorderIndex(pointType), Logger.DEBUG);
		
		
		/*
		 * The profiles derived from each segment will be merged to a single 
		 * profile at the end. Store them ordered in a list
		 */
		List<Profile> finalSegmentProfiles = new ArrayList<Profile>(0);
		
		/*
		 * The incoming nucleus profile has been offset to begin from 
		 * the pointType (usually the reference point)
		 * 
		 * The first segment in the profile should therefore be directly after the reference point,
		 * and no further offsets are needed.
		 * 
		 * The profile segmenter should be forcing a segment end at the reference point
		 * Using getOrderedSegments() rather than getSegments() to force the zero index segment
		 * to be first in the list.
		 * 
		 * An error arises when no zero index is available
		 * 
		 * 
		 */
		for(NucleusBorderSegment seg : profile.getOrderedSegments()){

			Profile revisedProfile = interpolateSegment(seg.getName(), profile);
			finalSegmentProfiles.add(revisedProfile);
			
//			if(debug){
				logger.log("Recombining segment: "+seg.toString(), Logger.DEBUG);
//			}
		}

		// The reference point is between segment 0 and 1 in rodent sperm
		// This may need to change if it does not hold for other cells.
		// The goal is to start the frankenprofile from the reference point
		//TODO: The reference point is not always between seg0 and seg 1

//		for(int i = 1; i<medianProfile.getSegmentCount();i++){
//			String name = "Seg_"+i;
//			Profile revisedProfile = interpolateSegment(name, profile);
//			finalSegmentProfiles.add(revisedProfile);
//		}
//		String name = "Seg_0";
//		Profile revisedProfile = interpolateSegment(name, profile);
//		finalSegmentProfiles.add(revisedProfile);

		Profile mergedProfile = new Profile( Profile.merge(finalSegmentProfiles));
		return mergedProfile;
	}
	
	/**
	 * The interpolation step of frankenprofile creation
	 * @param name the segment to interpolate
	 * @param profile the profile to take it from
	 * @return the interpolated profile
	 */
	private Profile interpolateSegment(String name, SegmentedProfile profile){

		NucleusBorderSegment testSeg = profile.getSegment(name);
		// The relevant segment from the median profile
		NucleusBorderSegment 	medianSegment = this.medianProfile.getSegment(name);

		// get the region within the segment as a new profile
		Profile testSegProfile = profile.getSubregion(testSeg);

		// interpolate the test segments to the length of the median segments
		Profile revisedProfile = testSegProfile.interpolate(medianSegment.length());

		if(debug){
			logger.log("\tAdjusted segment "+name+":\t"+testSeg.getStartIndex()+"-"+testSeg.getEndIndex()+"\t"+testSeg.length()+" -> "+medianSegment.length(), Logger.DEBUG);
		}
		return revisedProfile;
	}
	
	/**
	 * In progress version of fitter for 1.10.0
	 * @param profile the profile to fit against the median profile
	 * @return a profile with best-fit segmentation to the median
	 * @throws Exception 
	 */
	private SegmentedProfile runFitter(SegmentedProfile profile) throws Exception {
		// Input check
		if(profile==null){
			logger.log("Profile is null", Logger.ERROR);
			throw new IllegalArgumentException("Profile is null in runFitter()");
		}
		
		testedProfiles = new ArrayList<SegmentedProfile>();

		logger.log("Fitting segments", Logger.INFO);
		
		// By default, return the input profile
		SegmentedProfile result 	 = new SegmentedProfile(profile);

		// A new list to hold the fitted segments
		SegmentedProfile tempProfile = new SegmentedProfile(profile);
		
		// fit each segment independently
		for(String name : tempProfile.getSegmentNames()){
			
			// get the current segment
			NucleusBorderSegment segment = tempProfile.getSegment(name);
			
			// get the initial score for the segment and log it
			if(debug){
				double score = compareSegmentationPatterns(medianProfile, tempProfile);
				logger.log("Segment\t"+segment.getName()
						+"\tLength "+segment.length()
						+"\t"+segment.getStartIndex()
						+"-"+segment.getEndIndex(), Logger.DEBUG );
				logger.log("\tInitial score: "+score, Logger.DEBUG);
			}
			
			// find the best length and offset change
			// apply them to the profile
			tempProfile = testLength(tempProfile, name);
			
			// copy the best fit profile to the result
			result 	 = new SegmentedProfile(tempProfile);				
		}
		
		if(debug){
			for(String name : result.getSegmentNames()){
				logger.log("Fitted segment: "+result.getSegment(name).toString(), Logger.DEBUG);
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
	private SegmentedProfile testLength(SegmentedProfile profile, String name) throws Exception {
		
		// by default, return the same profile that came in
		SegmentedProfile result = new SegmentedProfile(profile);
				
		
		// the segment in the input profile to work on
		NucleusBorderSegment segment = profile.getSegment(name);
		
		
		// Get the initial score to beat
		double bestScore = compareSegmentationPatterns(medianProfile, profile);

		
		// the most extreme negative offset to apply to the end of this segment
		// without making the length invalid
		int minimumChange = 0 - (segment.length() - NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH);
		
		// the maximum length offset to apply
		// we can't go beyond the end of the next segment anyway, so use that as the cutoff
		// how far from current end to next segment end?
		int maximumChange = segment.testLength(segment.getEndIndex(), segment.nextSegment().getEndIndex());	
		if(debug){
			logger.log("\tMin change\t"+minimumChange+"\tMax change "+maximumChange );
		}
		
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
			if(debug){
				logger.log("\tTesting length change "+changeWindow);
			}
			testProfile = testChange(profile, name, changeWindow);
			double score = compareSegmentationPatterns(medianProfile, testProfile);
			if(score < bestScore){
				bestChangeWindow = changeWindow;
			}
		}
		
		if(debug){
			logger.log("\tBest fit window is length "+bestChangeWindow );
		}
		
		int halfWindow = changeWindowSize / 2;
		// now we have the best window,  drop down to a changeValue
		for(int changeValue = bestChangeWindow - halfWindow; changeValue < bestChangeWindow+halfWindow; changeValue++){
			SegmentedProfile testProfile = new SegmentedProfile(profile);
			if(debug){
				logger.log("\tTesting length change "+changeValue);
			}
			testProfile = testChange(profile, name, changeValue);
			double score = compareSegmentationPatterns(medianProfile, testProfile);
			if(score < bestScore){
				result = testProfile;
			}
		}
		
		return result;
	}
		
	private SegmentedProfile testChange(SegmentedProfile profile, String name, int changeValue) throws Exception {
		
		double bestScore = compareSegmentationPatterns(medianProfile, profile);
		
		// apply all changes to a fresh copy of the profile
		SegmentedProfile result = new SegmentedProfile(profile);
		SegmentedProfile testProfile = new SegmentedProfile(profile);
		NucleusBorderSegment segment = profile.getSegment(name);
		if(debug){
			logger.log("\tTesting length change "+changeValue);
		}
		
		// not permitted if it violates length constraint
		if(testProfile.adjustSegmentEnd(name, changeValue)){
			
			// testProfile should now contain updated segment endpoints
			SegmentedProfile compareProfile = new SegmentedProfile(testProfile);
			
////				 if this pattern has been seen, skip the rest of the test
//			if(optimise){
//				if(hasBeenTested(compareProfile)){
//					if(debug){
//						logger.log("\tProfile has been tested");
//					}
//					continue;
//				}
//			}
				
			// anything that gets in here should be valid
			try{
				double score = compareSegmentationPatterns(medianProfile, testProfile);
				if(debug){
					logger.log("\tLengthen "+changeValue+":\tScore:\t"+score, Logger.DEBUG);
				}
				
				if(score < bestScore){
					bestScore 	= score;
					result = new SegmentedProfile(testProfile);
					if(debug){
						logger.log("\tNew best score:\t"+score+"\tLengthen:\t"+changeValue, Logger.DEBUG);
					}
				}
			}catch(IllegalArgumentException e){
				// throw a new edxception rather than trying a nudge a problem profile
				logger.log(e.getMessage(), Logger.ERROR);
				throw new Exception("Error getting segmentation pattern: "+e.getMessage());
			}
			
			
			// test if nudging the lengthened segment with will help
			int nudge = testNudge(testProfile, segment.length());
			testProfile.nudgeSegments(nudge);

			double score = compareSegmentationPatterns(medianProfile, testProfile);
			if(score < bestScore){
				bestScore = score;
				result = new SegmentedProfile(testProfile);
				if(debug){
					logger.log("\tNew best score:\t"+score+"\tNudge:\t"+nudge, Logger.DEBUG);
				}
			}
			if(optimise){
				testedProfiles.add(compareProfile);
			}
			
									
		} else {
			if(debug){
				logger.log("\tLengthen "+changeValue
					+":\tInvalid length change:\t"
					+testProfile.getSegment(name).getLastFailReason()
					+"\t"+segment.toString(), Logger.DEBUG);
			}
		}
		return result;
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
				
		for( int nudge = -length; nudge<length; nudge++){
			SegmentedProfile newProfile = new SegmentedProfile(profile);
			newProfile.nudgeSegments(nudge);
			
			if(optimise){
				if(hasBeenTested(newProfile)){
					continue;
				}
			}
			
			try{
				score = compareSegmentationPatterns(medianProfile, newProfile);
//				logger.log("\tNudge "+nudge+":\tScore:\t"+score, Logger.DEBUG);
				
			}catch(IllegalArgumentException e){
				logger.error("Nudge error getting segmentation pattern: ", e);
				throw new Exception("Nudge error getting segmentation pattern");
			}
			
			if(score < bestScore){
				bestScore = score;
				bestNudge = nudge;
			}
			if(optimise){
				testedProfiles.add(newProfile);
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
	 */
	private double compareSegments(String name, SegmentedProfile referenceProfile, SegmentedProfile testProfile){
		if(name == null){
			throw new IllegalArgumentException("Segment name is null");
		}
		
		NucleusBorderSegment reference  = referenceProfile.getSegment(name);
		NucleusBorderSegment test		= testProfile.getSegment(name);

		Profile refProfile  = referenceProfile.getSubregion(reference);
		Profile subjProfile = testProfile.getSubregion(test);
		
		return refProfile.absoluteSquareDifference(subjProfile);
	}
	
	/**
	 * Get the score for an entire segment list of a profile. Tests the effect of  changing one segment
	 * on the entire set
	 * @param reference
	 * @param test
	 * @return the score
	 */
	private double  compareSegmentationPatterns(SegmentedProfile referenceProfile, SegmentedProfile testProfile){
		
		if(referenceProfile.getSegmentCount()!=testProfile.getSegmentCount()){
			throw new IllegalArgumentException("Lists are of different lengths");
		}
		
		double result = 0;
		for(String name : referenceProfile.getSegmentNames()){
			result += compareSegments(name, referenceProfile, testProfile);
		}
		return result;
	}
}
