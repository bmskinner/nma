package no.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import utility.Logger;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.SegmentedProfile;
import no.nuclei.Nucleus;

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
	
	private boolean debug = false;
	
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
	 */
	public void fit(Nucleus n){
		
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
		
		
		// Begin fitting the segments to the median
		logger.log("Fitting nucleus "+n.getPathAndNumber(), Logger.INFO);
		try{
			
			// get the best fit of segments to the median
			SegmentedProfile newProfile = this.runFitter(n.getAngleProfile());
			n.setAngleProfile(newProfile);
			
			// modify tail point to nearest segment end
//			this.remapBorderPoints(n, newList);
			logger.log("Fitted nucleus "+n.getPathAndNumber(), Logger.INFO);
			
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

			SegmentedProfile nucleusProfile = new SegmentedProfile(n.getAngleProfile(pointType));
			frankenProfile = recombineSegments(nucleusProfile);
		} catch(Exception e){
			logger.error("Error recombining segments", e);
		}
		
		return frankenProfile;
	}
	
	/**
	 * Move any border points to their closest segment end
	 * @param n the nucleus to fit
	 * @param list the segments in the nucleus
	 */
//	private void remapBorderPoints(INuclearFunctions n, List<NucleusBorderSegment> list){
//		
//		for(String pointTag : this.getFeatureKeys()){
//			
//		}
//				
//		for(String pointTag: n.getBorderTags().keySet()){
//			int index = n.getBorderTags().get(pointTag); 
//			
//			int nearestIndex = index;
//			int smallestDiff = n.getLength();
//			
//			for(NucleusBorderSegment seg : n.getSegments()){
//				int difference = Math.abs(index - seg.getStartIndex());
//				// find the best fitting segment start, so long as it is not too far
//				// needed to stop head being moved in rodent or pig sperm
//				if(difference < smallestDiff && difference <  (double)n.getLength()/4){
//					nearestIndex = seg.getStartIndex();
//					smallestDiff = difference;
//				}
//			}
//			n.addBorderTag(pointTag, nearestIndex);
//		}
//		
//	}
	
	/**
	 * Perform the recombination of segments from a nucleus. It takes each segment
	 * and interpolates it to the length of the corresponding median segment.
	 * @param testSegs the segments to adjust
	 * @param testMedian the median profile
	 * @return a profile constructed from the stretched segments
	 */
	private Profile recombineSegments(SegmentedProfile profile){
		
		if(profile==null){
			throw new IllegalArgumentException("Test profile is null in recombiner");
		}
		logger.log("Recombining segments to FrankenProfile", Logger.DEBUG);
		
		
		// the profiles derived from each segment will be merged to a single 
		// profile at the end
		List<Profile> finalSegmentProfiles = new ArrayList<Profile>(0);

		// The reference point is between segment 0 and 1 in rodent sperm
		// This may need to change if it does not hold for other cells.
		// The goal is to start the frankenprofile from the reference point
		for(int i = 1; i<medianProfile.getSegmentCount();i++){
			String name = "Seg_"+i;
			Profile revisedProfile = interpolateSegment(name, profile);
			finalSegmentProfiles.add(revisedProfile);
		}
		String name = "Seg_0";
		Profile revisedProfile = interpolateSegment(name, profile);
		finalSegmentProfiles.add(revisedProfile);

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

		logger.log("Fitting segments", Logger.DEBUG);
		
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
					+"-"+segment.getEndIndex() );
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
		
		// Try all the possible values in the valid range of changes
		for(int changeValue = minimumChange; changeValue < maximumChange; changeValue++){
			
			// apply all changes to a fresh copy of the profile
			SegmentedProfile testProfile = new SegmentedProfile(profile);
//			if(debug){
//				logger.log("\tTesting length change "+changeValue);
//			}

			// not permitted if it violates length constraint
			if(testProfile.adjustSegmentEnd(name, changeValue)){
					
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
										
			} else {
				if(debug){
					logger.log("\tLengthen "+changeValue
						+":\tInvalid length change:\t"
						+testProfile.getSegment(name).getLastFailReason()
						+"\t"+segment.toString(), Logger.DEBUG);
				}
			}
		}
		return result;
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
		
		return refProfile.weightedSquareDifference(subjProfile);
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
